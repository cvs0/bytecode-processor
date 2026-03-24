package io.github.cvs0.bytecode;

import io.github.cvs0.bytecode.clazz.LibraryClass;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.util.JarLayout;
import io.github.cvs0.bytecode.util.JarReader;
import io.github.cvs0.bytecode.util.JarWriter;
import io.github.cvs0.bytecode.util.ManifestPatcher;
import io.github.cvs0.bytecode.util.MergedClasspathBytecodeRemapper;
import io.github.cvs0.bytecode.util.ServiceLoaderResourcePatcher;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Represents a mapping of classes and resources within a JAR file.
 * Provides methods to add, remove, and retrieve program and library classes, {@link ModuleInfoClass module descriptors},
 * {@link PackageInfoClass package-info} entries, and resources.
 * Supports reading from and writing to JAR files.
 *
 * <p>Program and library class maps are backed by {@link java.util.concurrent.ConcurrentHashMap}; individual
 * {@link ProgramClass} instances are not thread-safe unless documented otherwise.
 *
 * <p>Use {@link #mergeClasspathJar(Path)} (or {@link #mergeClasspathJars(Iterable)}) to fold dependency JARs into the
 * mapping for fat-JAR output; merged {@code .class} files are not modeled as {@link ProgramClass}. After obfuscation,
 * {@link #remapMergedClasspathBytecode} (invoked from {@link io.github.cvs0.bytecode.plugin.impl.ObfuscationPlugin})
 * should be used so merged bytecode still references renamed application code.</p>
 */
public class JarMapping {
    /** Map of program class names to their ProgramClass representations. */
    private final Map<String, ProgramClass> programClasses = new ConcurrentHashMap<>();
    /** Map of library class names to their LibraryClass representations. */
    private final Map<String, LibraryClass> libraryClasses = new ConcurrentHashMap<>();
    /** Map of resource names to their byte array data. */
    private final Map<String, byte[]> resources = new ConcurrentHashMap<>();
    /** {@code module-info.class} entries keyed by JAR entry path (supports multi-release). */
    private final Map<String, ModuleInfoClass> moduleInfos = new ConcurrentHashMap<>();
    /** {@code package-info.class} entries keyed by JAR entry path. */
    private final Map<String, PackageInfoClass> packageInfos = new ConcurrentHashMap<>();
    /** Path to the JAR file represented by this mapping. */
    private final String jarPath;
    /**
     * Raw JAR entries merged from classpath JARs (keys are JAR paths such as {@code com/foo/Bar.class}).
     * Not modeled as {@link ProgramClass}; writers emit them verbatim after program classes.
     */
    private final Map<String, byte[]> mergedEntries = new ConcurrentHashMap<>();
    
    /**
     * Constructs a new JarMapping for the specified JAR path.
     * @param jarPath the path to the JAR file
     */
    public JarMapping(String jarPath) {
        this.jarPath = Objects.requireNonNull(jarPath, "jarPath");
    }
    
    /**
     * Loads a JarMapping from the specified JAR file.
     * @param jarPath the path to the JAR file
     * @return a new JarMapping instance
     * @throws IOException if the JAR cannot be read
     */
    public static JarMapping fromJar(String jarPath) throws IOException {
        Objects.requireNonNull(jarPath, "jarPath");
        JarMapping mapping = new JarMapping(jarPath);
        JarReader.read(new File(jarPath), mapping);
        return mapping;
    }

    /**
     * Loads a JarMapping from a JAR file path.
     *
     * @param jarPath path to the JAR
     * @return a new JarMapping instance
     * @throws IOException if the JAR cannot be read
     */
    public static JarMapping fromJar(Path jarPath) throws IOException {
        Objects.requireNonNull(jarPath, "jarPath");
        Path normalized = jarPath.toAbsolutePath().normalize();
        JarMapping mapping = new JarMapping(normalized.toString());
        JarReader.read(normalized.toFile(), mapping);
        return mapping;
    }

    /**
     * Merges all entries from another JAR (typically a dependency) into this mapping. Class files become merged
     * bytecode (skipped when a {@link ProgramClass} with the same internal name already exists). Resources are added
     * only if absent. Skips the dependency manifest, JAR signatures, and {@code module-info.class} entries.
     *
     * @param classpathJar path to the JAR to merge
     * @throws IOException if reading fails
     */
    public void mergeClasspathJar(Path classpathJar) throws IOException {
        Objects.requireNonNull(classpathJar, "classpathJar");
        mergeClasspathJar(classpathJar.toFile());
    }

    /**
     * Merges each JAR in order. When the same class appears in multiple JARs, the first merge wins.
     *
     * @param jars dependency JAR paths
     * @throws IOException if reading fails
     */
    public void mergeClasspathJars(Iterable<? extends Path> jars) throws IOException {
        Objects.requireNonNull(jars, "jars");
        for (Path p : jars) {
            if (p != null) {
                mergeClasspathJar(p);
            }
        }
    }

    private void mergeClasspathJar(File jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (shouldSkipMergedEntry(name)) {
                    continue;
                }
                byte[] data;
                try (InputStream in = jar.getInputStream(entry)) {
                    data = in.readAllBytes();
                }
                if (name.endsWith(".class")) {
                    if (name.endsWith("module-info.class")) {
                        continue;
                    }
                    String internal;
                    try {
                        internal = new ClassReader(data).getClassName();
                    } catch (RuntimeException e) {
                        throw new IOException("Invalid class entry in " + jarFile + ": " + name, e);
                    }
                    if (programClasses.containsKey(internal)) {
                        continue;
                    }
                    offerMergedClassEntry(name, data);
                } else if (resources.get(name) == null) {
                    addResource(name, data);
                }
            }
        }
    }

    private static boolean shouldSkipMergedEntry(String name) {
        if (JarLayout.MANIFEST.equals(name)) {
            return true;
        }
        if (!name.startsWith("META-INF/")) {
            return false;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA");
    }

    /** First writer wins per JAR path; program classes always supersede at merge time. */
    private void offerMergedClassEntry(String jarEntryPath, byte[] classBytes) {
        mergedEntries.putIfAbsent(jarEntryPath, classBytes);
    }

    /**
     * Merged classpath entry paths, sorted for deterministic JAR output.
     */
    public List<String> getMergedEntryPaths() {
        List<String> paths = new ArrayList<>(mergedEntries.keySet());
        Collections.sort(paths);
        return Collections.unmodifiableList(paths);
    }

    /**
     * Raw bytes for a merged JAR entry, or {@code null} if absent.
     */
    public byte[] getMergedEntry(String jarEntryPath) {
        return mergedEntries.get(jarEntryPath);
    }

    public int getMergedEntryCount() {
        return mergedEntries.size();
    }

    /**
     * If {@link JarLayout#MANIFEST} is present and declares {@code Main-Class}, updates it when that class was
     * renamed. Keys are internal names with slashes (e.g. {@code com/foo/Bar}).
     */
    public void remapManifestMainClass(Map<String, String> internalOldToNew) {
        byte[] raw = resources.get(JarLayout.MANIFEST);
        byte[] patched = ManifestPatcher.remapLaunchClassAttributes(raw, internalOldToNew);
        if (patched != null) {
            resources.put(JarLayout.MANIFEST, patched);
        }
    }

    /**
     * Rewrites merged classpath {@code .class} entries so they still resolve renamed program classes, fields, and methods.
     *
     * @param classNameMappings  old internal → new internal (class renames)
     * @param fieldNameMappings  keys {@code OldOwner.fieldName}
     * @param methodNameMappings keys {@code OldOwner.name+descriptor}
     */
    public void remapMergedClasspathBytecode(
            Map<String, String> classNameMappings,
            Map<String, String> fieldNameMappings,
            Map<String, String> methodNameMappings) {
        Objects.requireNonNull(classNameMappings, "classNameMappings");
        Objects.requireNonNull(fieldNameMappings, "fieldNameMappings");
        Objects.requireNonNull(methodNameMappings, "methodNameMappings");
        if (mergedEntries.isEmpty()) {
            return;
        }
        if (classNameMappings.isEmpty() && fieldNameMappings.isEmpty() && methodNameMappings.isEmpty()) {
            return;
        }
        for (Map.Entry<String, byte[]> e : new ArrayList<>(mergedEntries.entrySet())) {
            String path = e.getKey();
            if (!path.endsWith(".class")) {
                continue;
            }
            byte[] updated = MergedClasspathBytecodeRemapper.remap(
                    e.getValue(), classNameMappings, fieldNameMappings, methodNameMappings);
            mergedEntries.put(path, updated);
        }
    }

    /**
     * Renames {@code META-INF/services/&lt;fully.qualified.Interface&gt;} when the service interface type was renamed.
     */
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

    /**
     * Updates {@code META-INF/services/*} implementation lines for renamed classes.
     */
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
    
    /**
     * Adds a program class to the mapping.
     * @param clazz the ProgramClass to add
     */
    public void addClass(ProgramClass clazz) {
        Objects.requireNonNull(clazz, "clazz");
        programClasses.put(clazz.getName(), clazz);
    }
    
    /**
     * Adds a library class to the mapping.
     * @param clazz the LibraryClass to add
     */
    public void addLibraryClass(LibraryClass clazz) {
        Objects.requireNonNull(clazz, "clazz");
        libraryClasses.put(clazz.getName(), clazz);
    }
    
    /**
     * Adds a resource to the mapping.
     * @param name the resource name
     * @param data the resource data
     */
    public void addResource(String name, byte[] data) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(data, "data");
        resources.put(name, data);
    }

    /**
     * Adds a module descriptor ({@code module-info.class}) to the mapping.
     *
     * @param jarEntryName JAR path (e.g. {@code module-info.class} or {@code META-INF/versions/9/module-info.class})
     */
    public void addModuleInfo(String jarEntryName, ModuleInfoClass moduleInfo) {
        Objects.requireNonNull(jarEntryName, "jarEntryName");
        Objects.requireNonNull(moduleInfo, "moduleInfo");
        moduleInfos.put(jarEntryName, moduleInfo);
    }

    /**
     * Adds a {@code package-info.class} entry to the mapping.
     *
     * @param jarEntryName JAR path (e.g. {@code com/example/package-info.class})
     */
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
     * Retrieves a program class by name.
     * @param name the class name
     * @return the ProgramClass, or null if not found
     */
    public ProgramClass getProgramClass(String name) {
        return programClasses.get(name);
    }
    
    /**
     * Retrieves a library class by name.
     * @param name the class name
     * @return the LibraryClass, or null if not found
     */
    public LibraryClass getLibraryClass(String name) {
        return libraryClasses.get(name);
    }
    
    /**
     * Retrieves a resource by name.
     * @param name the resource name
     * @return the resource data, or null if not found
     */
    public byte[] getResource(String name) {
        return resources.get(name);
    }
    
    /**
     * Returns all program classes in this mapping.
     * @return an unmodifiable collection of ProgramClass objects
     */
    public Collection<ProgramClass> getProgramClasses() {
        return Collections.unmodifiableCollection(programClasses.values());
    }
    
    /**
     * Returns all library classes in this mapping.
     * @return an unmodifiable collection of LibraryClass objects
     */
    public Collection<LibraryClass> getLibraryClasses() {
        return Collections.unmodifiableCollection(libraryClasses.values());
    }
    
    /**
     * Returns all resource names in this mapping.
     * @return an unmodifiable set of resource names
     */
    public Set<String> getResourceNames() {
        return Collections.unmodifiableSet(resources.keySet());
    }
    
    /**
     * Removes a class (program or library) by name.
     * @param name the class name
     */
    public void removeClass(String name) {
        programClasses.remove(name);
        libraryClasses.remove(name);
        for (var it = packageInfos.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (name.equals(e.getValue().getInternalName())) {
                it.remove();
            }
        }
    }
    
    /**
     * Removes a resource by name.
     * @param name the resource name
     */
    public void removeResource(String name) {
        resources.remove(name);
    }
    
    /**
     * Renames a class (program or library) in the mapping.
     * @param oldName the old class name
     * @param newName the new class name
     */
    public void renameClass(String oldName, String newName) {
        Objects.requireNonNull(oldName, "oldName");
        Objects.requireNonNull(newName, "newName");
        ProgramClass programClass = programClasses.remove(oldName);
        if (programClass != null) {
            programClass.setName(newName);
            programClasses.put(newName, programClass);
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
    
    /**
     * Writes the contents of this mapping to a JAR file at the specified output path.
     * @param outputPath the output JAR file path
     * @throws IOException if writing fails
     */
    public void writeToJar(String outputPath) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath");
        JarWriter.write(this, new File(outputPath));
    }

    /**
     * Writes this mapping to a JAR at the given path.
     *
     * @param outputPath destination path
     * @throws IOException if writing fails
     */
    public void writeToJar(Path outputPath) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath");
        JarWriter.write(this, outputPath);
    }
    
    /**
     * Returns the path to the JAR file represented by this mapping.
     * @return the JAR path
     */
    public String getJarPath() {
        return jarPath;
    }
    
    /**
     * Returns program classes, library classes, module descriptors, package-info entries, and merged dependency classes.
     */
    public int getTotalClassCount() {
        return programClasses.size()
                + libraryClasses.size()
                + moduleInfos.size()
                + packageInfos.size()
                + mergedEntries.size();
    }

    public int getModuleInfoCount() {
        return moduleInfos.size();
    }

    public int getPackageInfoCount() {
        return packageInfos.size();
    }
    
    /**
     * Returns the number of resources in this mapping.
     * @return the resource count
     */
    public int getResourceCount() {
        return resources.size();
    }
    
    /**
     * Checks if a class (program or library) exists in this mapping.
     * @param name the class name
     * @return true if the class exists, false otherwise
     */
    public boolean containsClass(String name) {
        return programClasses.containsKey(name) || libraryClasses.containsKey(name);
    }
    
    /**
     * Returns a list of all class names (program and library) in this mapping.
     * @return a list of class names
     */
    public List<String> getAllClassNames() {
        List<String> names = new ArrayList<>();
        names.addAll(programClasses.keySet());
        names.addAll(libraryClasses.keySet());
        return names;
    }
}