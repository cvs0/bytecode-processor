package io.github.cvs0.bytecode;

import io.github.cvs0.bytecode.clazz.LibraryClass;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.util.JarLayout;
import io.github.cvs0.bytecode.util.JarReader;
import io.github.cvs0.bytecode.util.JarWriter;
import io.github.cvs0.bytecode.util.ManifestPatcher;
import io.github.cvs0.bytecode.util.ServiceLoaderResourcePatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory model of a JAR: every {@code .class} entry (as {@link ProgramClass}), {@link ModuleInfoClass module
 * descriptors}, {@link PackageInfoClass package-info}, {@link LibraryClass library} stubs, and all non-class resources.
 *
 * <p>All bytecode-backed types are {@link ProgramClass} (keyed by {@linkplain ProgramClass#getJarEntryName() JAR entry path},
 * including multi-release variants). {@link ProgramClass#isEmbeddedLibrary()} distinguishes shaded dependencies from
 * the application. {@link #getApplicationClasses()} is for host-project transforms; {@link #getProgramClasses()} returns
 * every loaded class. {@link #getProgramClass(String)} resolves by internal (slash) name and returns an arbitrary match
 * when several entries share the same name.</p>
 *
 * <p><b>Lifecycle</b> — Load with {@link JarReader#read(java.io.File, JarMapping)}; transform with
 * {@link io.github.cvs0.bytecode.transform.ClassTransformer} and/or {@link io.github.cvs0.bytecode.plugin.PluginManager};
 * optional {@link #remapManifestMainClass}, {@link #remapServiceLoaderResourcePaths},
 * {@link #remapServiceLoaderImplementations}; write with {@link #writeToJar(java.nio.file.Path)} or {@link JarWriter}.
 * Metadata reconciliation after renames runs inside
 * {@link io.github.cvs0.bytecode.transform.ClassTransformer#applyTransformations()} via
 * {@link io.github.cvs0.bytecode.util.JarGraphMetadataReconciler}.</p>
 *
 * @see io.github.cvs0.bytecode.util.BytecodeNames
 */
public class JarMapping {
    /**
     * Program classes keyed by JAR entry path (e.g. {@code com/foo/Bar.class}), unique per {@code ZipEntry}.
     */
    private final Map<String, ProgramClass> programClassesByJarEntry = new ConcurrentHashMap<>();
    private final Map<String, LibraryClass> libraryClasses = new ConcurrentHashMap<>();
    private final Map<String, byte[]> resources = new ConcurrentHashMap<>();
    private final Map<String, ModuleInfoClass> moduleInfos = new ConcurrentHashMap<>();
    private final Map<String, PackageInfoClass> packageInfos = new ConcurrentHashMap<>();
    private final String jarPath;

    public JarMapping(String jarPath) {
        this.jarPath = Objects.requireNonNull(jarPath, "jarPath");
    }

    public static JarMapping fromJar(String jarPath) throws IOException {
        Objects.requireNonNull(jarPath, "jarPath");
        JarMapping mapping = new JarMapping(jarPath);
        JarReader.read(new File(jarPath), mapping);
        return mapping;
    }

    public static JarMapping fromJar(Path jarPath) throws IOException {
        Objects.requireNonNull(jarPath, "jarPath");
        Path normalized = jarPath.toAbsolutePath().normalize();
        JarMapping mapping = new JarMapping(normalized.toString());
        JarReader.read(normalized.toFile(), mapping);
        return mapping;
    }

    public void remapManifestMainClass(Map<String, String> internalOldToNew) {
        byte[] raw = resources.get(JarLayout.MANIFEST);
        byte[] patched = ManifestPatcher.remapLaunchClassAttributes(raw, internalOldToNew);
        if (patched != null) {
            resources.put(JarLayout.MANIFEST, patched);
        }
    }

    public void remapServiceLoaderResourcePaths(Map<String, String> internalOldToNew) {
        Objects.requireNonNull(internalOldToNew, "internalOldToNew");
        if (internalOldToNew.isEmpty()) {
            return;
        }
        String prefix = "META-INF/services/";
        for (String name : new ArrayList<>(resources.keySet())) {
            if (!name.startsWith(prefix) || name.length() <= prefix.length()) {
                continue;
            }
            String remainder = name.substring(prefix.length());
            if (remainder.indexOf('/') >= 0) {
                continue;
            }
            String internal = remainder.replace('.', '/');
            String nw = internalOldToNew.get(internal);
            if (nw == null) {
                continue;
            }
            String newPath = prefix + nw.replace('/', '.');
            if (newPath.equals(name)) {
                continue;
            }
            byte[] data = resources.remove(name);
            if (data != null) {
                resources.put(newPath, data);
            }
        }
    }

    public void remapServiceLoaderImplementations(Map<String, String> internalOldToNew) {
        Objects.requireNonNull(internalOldToNew, "internalOldToNew");
        if (internalOldToNew.isEmpty()) {
            return;
        }
        for (String name : new ArrayList<>(resources.keySet())) {
            if (!name.startsWith("META-INF/services/") || "META-INF/services/".equals(name)) {
                continue;
            }
            byte[] raw = resources.get(name);
            byte[] patched = ServiceLoaderResourcePatcher.remapImplementations(raw, internalOldToNew);
            if (patched != null) {
                resources.put(name, patched);
            }
        }
    }

    public void addClass(ProgramClass clazz) {
        Objects.requireNonNull(clazz, "clazz");
        programClassesByJarEntry.put(clazz.getJarEntryName(), clazz);
    }

    public void addLibraryClass(LibraryClass clazz) {
        Objects.requireNonNull(clazz, "clazz");
        libraryClasses.put(clazz.getName(), clazz);
    }

    public void addResource(String name, byte[] data) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(data, "data");
        resources.put(name, data);
    }

    public void addModuleInfo(String jarEntryName, ModuleInfoClass moduleInfo) {
        Objects.requireNonNull(jarEntryName, "jarEntryName");
        Objects.requireNonNull(moduleInfo, "moduleInfo");
        moduleInfos.put(jarEntryName, moduleInfo);
    }

    public void addPackageInfo(String jarEntryName, PackageInfoClass packageInfo) {
        Objects.requireNonNull(jarEntryName, "jarEntryName");
        Objects.requireNonNull(packageInfo, "packageInfo");
        packageInfos.put(jarEntryName, packageInfo);
    }

    public ModuleInfoClass getModuleInfo(String jarEntryName) {
        return moduleInfos.get(jarEntryName);
    }

    public PackageInfoClass getPackageInfo(String jarEntryName) {
        return packageInfos.get(jarEntryName);
    }

    public Collection<ModuleInfoClass> getModuleInfos() {
        return Collections.unmodifiableCollection(moduleInfos.values());
    }

    public Set<String> getModuleInfoEntryNames() {
        return Collections.unmodifiableSet(moduleInfos.keySet());
    }

    public Collection<PackageInfoClass> getPackageInfos() {
        return Collections.unmodifiableCollection(packageInfos.values());
    }

    public Set<String> getPackageInfoEntryNames() {
        return Collections.unmodifiableSet(packageInfos.keySet());
    }

    public void removeModuleInfo(String jarEntryName) {
        moduleInfos.remove(jarEntryName);
    }

    public void removePackageInfo(String jarEntryName) {
        packageInfos.remove(jarEntryName);
    }

    /**
     * Returns a bytecode-backed class by internal (slash) name, or {@code null}. Includes embedded libraries and
     * application classes. If several JAR entries share the same internal name (e.g. multi-release), one of them is returned.
     */
    public ProgramClass getProgramClass(String name) {
        for (ProgramClass c : programClassesByJarEntry.values()) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    public LibraryClass getLibraryClass(String name) {
        return libraryClasses.get(name);
    }

    public byte[] getResource(String name) {
        return resources.get(name);
    }

    /**
     * All {@code .class} entries (application and embedded libraries), for graph walks and reference propagation.
     */
    public Collection<ProgramClass> getProgramClasses() {
        return Collections.unmodifiableCollection(programClassesByJarEntry.values());
    }

    /**
     * Subset that {@link io.github.cvs0.bytecode.util.JarLibraryClassifier} marked as the host application (not shaded deps).
     */
    public Collection<ProgramClass> getApplicationClasses() {
        List<ProgramClass> out = new ArrayList<>();
        for (ProgramClass c : programClassesByJarEntry.values()) {
            if (!c.isEmbeddedLibrary()) {
                out.add(c);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Embedded third-party bytecode in the same JAR (full {@link org.objectweb.asm.tree.ClassNode} like application classes).
     */
    public Collection<ProgramClass> getEmbeddedLibraryProgramClasses() {
        List<ProgramClass> out = new ArrayList<>();
        for (ProgramClass c : programClassesByJarEntry.values()) {
            if (c.isEmbeddedLibrary()) {
                out.add(c);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public Collection<LibraryClass> getLibraryClasses() {
        return Collections.unmodifiableCollection(libraryClasses.values());
    }

    public Set<String> getResourceNames() {
        return Collections.unmodifiableSet(resources.keySet());
    }

    public void removeClass(String name) {
        for (Iterator<Map.Entry<String, ProgramClass>> it = programClassesByJarEntry.entrySet().iterator();
                it.hasNext(); ) {
            if (name.equals(it.next().getValue().getName())) {
                it.remove();
            }
        }
        libraryClasses.remove(name);
        for (var pit = packageInfos.entrySet().iterator(); pit.hasNext(); ) {
            var e = pit.next();
            if (name.equals(e.getValue().getInternalName())) {
                pit.remove();
            }
        }
    }

    public void removeResource(String name) {
        resources.remove(name);
    }

    public void renameClass(String oldName, String newName) {
        Objects.requireNonNull(oldName, "oldName");
        Objects.requireNonNull(newName, "newName");
        List<ProgramClass> toRename = new ArrayList<>();
        for (ProgramClass pc : programClassesByJarEntry.values()) {
            if (oldName.equals(pc.getName())) {
                toRename.add(pc);
            }
        }
        for (ProgramClass programClass : toRename) {
            String oldKey = programClass.getJarEntryName();
            programClass.remapJarEntryPath(oldName, newName);
            programClass.setName(newName);
            programClassesByJarEntry.remove(oldKey);
            programClassesByJarEntry.put(programClass.getJarEntryName(), programClass);
        }

        LibraryClass libraryClass = libraryClasses.remove(oldName);
        if (libraryClass != null) {
            libraryClass.setName(newName);
            libraryClasses.put(newName, libraryClass);
        }

        for (var e : new ArrayList<>(packageInfos.entrySet())) {
            PackageInfoClass pic = e.getValue();
            if (oldName.equals(pic.getInternalName())) {
                packageInfos.remove(e.getKey());
                pic.setJarEntryName(newName + ".class");
                pic.getClassNode().name = newName;
                packageInfos.put(pic.getJarEntryName(), pic);
            }
        }
    }

    public void writeToJar(String outputPath) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath");
        JarWriter.write(this, new File(outputPath));
    }

    public void writeToJar(Path outputPath) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath");
        JarWriter.write(this, outputPath);
    }

    public String getJarPath() {
        return jarPath;
    }

    public int getTotalClassCount() {
        return programClassesByJarEntry.size()
                + libraryClasses.size()
                + moduleInfos.size()
                + packageInfos.size();
    }

    public int getModuleInfoCount() {
        return moduleInfos.size();
    }

    public int getPackageInfoCount() {
        return packageInfos.size();
    }

    public int getResourceCount() {
        return resources.size();
    }

    public boolean containsClass(String name) {
        if (libraryClasses.containsKey(name)) {
            return true;
        }
        for (ProgramClass c : programClassesByJarEntry.values()) {
            if (name.equals(c.getName())) {
                return true;
            }
        }
        return false;
    }

    public List<String> getAllClassNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (ProgramClass c : programClassesByJarEntry.values()) {
            names.add(c.getName());
        }
        names.addAll(libraryClasses.keySet());
        return new ArrayList<>(names);
    }

    /** Number of {@code .class} entries modeled as {@link ProgramClass} (one per JAR path). */
    public int getProgramClassEntryCount() {
        return programClassesByJarEntry.size();
    }
}
