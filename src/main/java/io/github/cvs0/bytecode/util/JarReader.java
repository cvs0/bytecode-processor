package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramField;
import io.github.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Populates a {@link JarMapping} from a JAR on disk. After loading every {@code .class} entry,
 * the reader resolves the full class hierarchy (parent/child/interface links between
 * {@link ProgramClass} instances), marks external-contract method overrides on
 * {@link ProgramMethod#setOverridesExternal(boolean)}, and classifies application
 * vs embedded-library classes using the JAR manifest.
 *
 * <p>This "read-time enrichment" means downstream code (transformers, plugins, analysis)
 * never needs to infer relationships or build separate graphs —
 * the model is complete when {@link #read} returns.</p>
 *
 * @see JarWriter
 * @see JarMapping#fromJar(java.nio.file.Path)
 */
public class JarReader {

    /**
     * Reads all entries from a JAR file, builds hierarchy links, resolves external overrides,
     * and classifies application classes.
     */
    public static void read(File jarFile, JarMapping mapping) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    if (entryName.endsWith("module-info.class")) {
                        processModuleInfoEntry(jar, entry, entryName, mapping);
                    } else if (entryName.endsWith("package-info.class")) {
                        processPackageInfoEntry(jar, entry, entryName, mapping);
                    } else {
                        processClassEntry(jar, entry, mapping);
                    }
                } else {
                    processResourceEntry(jar, entry, mapping);
                }
            }

            // ---- post-load enrichment ----
            resolveHierarchyAndOverrides(mapping);
            classifyApplicationClasses(jar, mapping);
        }
    }

    /**
     * Processes a module-info.class entry and adds it to the mapping.
     * @param jar the JarFile
     * @param entry the module-info entry
     * @param mapping the JarMapping
     * @throws IOException if reading fails
     */
    private static void processModuleInfoEntry(JarFile jar, JarEntry entry, String entryName, JarMapping mapping) throws IOException {
        try (InputStream inputStream = jar.getInputStream(entry)) {
            byte[] classBytes = inputStream.readAllBytes();
            ClassReader classReader = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            int classVersion = classReader.readShort(6) & 0xFFFF;
            mapping.addModuleInfo(entryName, new ModuleInfoClass(entryName, classNode, classVersion));
        } catch (Exception e) {
            throw new IOException("Failed to load module descriptor: " + entryName, e);
        }
    }

    /**
     * Processes a package-info.class entry and adds it to the mapping.
     * @param jar the JarFile
     * @param entry the package-info entry
     * @param mapping the JarMapping
     * @throws IOException if reading fails
     */
    private static void processPackageInfoEntry(JarFile jar, JarEntry entry, String entryName, JarMapping mapping) throws IOException {
        try (InputStream inputStream = jar.getInputStream(entry)) {
            byte[] classBytes = inputStream.readAllBytes();
            ClassReader classReader = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            int classVersion = classReader.readShort(6) & 0xFFFF;
            mapping.addPackageInfo(entryName, new PackageInfoClass(entryName, classNode, classVersion));
        } catch (Exception e) {
            throw new IOException("Failed to load package-info: " + entryName, e);
        }
    }

    /**
     * Processes a class entry from a JAR and adds it to the mapping.
     * @param jar the JarFile
     * @param entry the class entry
     * @param mapping the JarMapping
     * @throws IOException if reading fails
     */
    private static void processClassEntry(JarFile jar, JarEntry entry, JarMapping mapping) throws IOException {
        try (InputStream inputStream = jar.getInputStream(entry)) {
            byte[] classBytes = inputStream.readAllBytes();
            ClassReader classReader = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            if (classNode.name == null || classNode.name.isEmpty()) {
                return;
            }

            ProgramClass programClass = new ProgramClass(classNode);
            programClass.setJarEntryName(entry.getName());
            int classVersion = classReader.readShort(6);
            programClass.setClassVersion(classVersion);
            if (classNode.recordComponents != null) {
                for (RecordComponentNode rc : classNode.recordComponents) {
                    programClass.addRecordComponent(rc);
                }
            }
            if (classNode.nestHostClass != null) {
                programClass.setNestHostClass(classNode.nestHostClass);
            }
            if (classNode.nestMembers != null) {
                for (String member : classNode.nestMembers) {
                    programClass.addNestMember(member);
                }
            }
            if (classNode.permittedSubclasses != null) {
                for (String subclass : classNode.permittedSubclasses) {
                    programClass.addPermittedSubclass(subclass);
                }
            }
            if (classNode.fields != null) {
                for (FieldNode fieldNode : classNode.fields) {
                    ProgramField field = new ProgramField(fieldNode);
                    programClass.addField(field);
                }
            }
            if (classNode.methods != null) {
                for (MethodNode methodNode : classNode.methods) {
                    ProgramMethod method = new ProgramMethod(methodNode);
                    programClass.addMethod(method);
                }
            }
            mapping.addClass(programClass);
        } catch (Exception e) {
            throw new IOException("Failed to load class entry: " + entry.getName(), e);
        }
    }

    /**
     * Processes a resource entry from a JAR and adds it to the mapping.
     * @param jar the JarFile
     * @param entry the resource entry
     * @param mapping the JarMapping
     * @throws IOException if reading fails
     */
    private static void processResourceEntry(JarFile jar, JarEntry entry, JarMapping mapping) throws IOException {
        try (InputStream inputStream = jar.getInputStream(entry)) {
            byte[] resourceBytes = inputStream.readAllBytes();
            mapping.addResource(entry.getName(), resourceBytes);
        }
    }

    /**
     * Reads a single class from a .class file.
     * @param classFile the class file
     * @return the ProgramClass instance
     * @throws IOException if reading fails
     */
    public static ProgramClass readClass(File classFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(classFile)) {
            byte[] classBytes = fis.readAllBytes();
            ClassReader classReader = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            ProgramClass programClass = new ProgramClass(classNode);
            if (classNode.fields != null) {
                for (FieldNode fieldNode : classNode.fields) {
                    ProgramField field = new ProgramField(fieldNode);
                    programClass.addField(field);
                }
            }
            if (classNode.methods != null) {
                for (MethodNode methodNode : classNode.methods) {
                    ProgramMethod method = new ProgramMethod(methodNode);
                    programClass.addMethod(method);
                }
            }
            return programClass;
        }
    }

    /**
     * Reads a single class from a byte array.
     * @param classBytes the class bytes
     * @return the ProgramClass instance
     * @throws IOException if reading fails
     */
    public static ProgramClass readClass(byte[] classBytes) throws IOException {
        ClassReader classReader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        ProgramClass programClass = new ProgramClass(classNode);
        if (classNode.fields != null) {
            for (FieldNode fieldNode : classNode.fields) {
                ProgramField field = new ProgramField(fieldNode);
                programClass.addField(field);
            }
        }
        if (classNode.methods != null) {
            for (MethodNode methodNode : classNode.methods) {
                ProgramMethod method = new ProgramMethod(methodNode);
                programClass.addMethod(method);
            }
        }
        return programClass;
    }

    /**
     * Reads all bytes from a file.
     * @param file the file to read
     * @return the file bytes
     * @throws IOException if reading fails
     */
    public static byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    // ========================================================================
    //  Post-load enrichment (hierarchy, overrides, app classification)
    // ========================================================================

    /**
     * Links every ProgramClass to its parent, children, and resolved interfaces,
     * then marks methods that override external (non-ProgramClass) contracts.
     */
    static void resolveHierarchyAndOverrides(JarMapping mapping) {
        // Phase 1: wire parent / child / interface links
        for (ProgramClass pc : mapping.getProgramClasses()) {
            String superName = pc.getSuperName();
            if (superName != null) {
                ProgramClass parent = mapping.getProgramClass(superName);
                if (parent != null) {
                    pc.setParentProgramClass(parent);
                    parent.addChildProgramClass(pc);
                } else {
                    pc.addUnresolvedSuperType(superName);
                }
            }
            for (String iface : pc.getInterfaces()) {
                ProgramClass resolved = mapping.getProgramClass(iface);
                if (resolved != null) {
                    pc.addResolvedInterface(resolved);
                    resolved.addChildProgramClass(pc);
                } else {
                    pc.addUnresolvedSuperType(iface);
                }
            }
        }

        Map<String, Set<String>> externalMethodCache = new HashMap<>();

        for (ProgramClass pc : mapping.getProgramClasses()) {
            // Walk upward through already-linked parents to collect all external supertypes
            Set<String> externalTypes = new LinkedHashSet<>();
            Deque<ProgramClass> stack = new ArrayDeque<>();
            Set<ProgramClass> visited = new HashSet<>();
            stack.push(pc);
            while (!stack.isEmpty()) {
                ProgramClass cur = stack.pop();
                if (cur == null || !visited.add(cur)) {
                    continue;
                }
                externalTypes.addAll(cur.getUnresolvedSuperTypes());
                if (cur.getParentProgramClass() != null) {
                    stack.push(cur.getParentProgramClass());
                }
                for (ProgramClass iface : cur.getResolvedInterfaces()) {
                    stack.push(iface);
                }
            }

            if (externalTypes.isEmpty()) {
                continue;
            }

            boolean anyUnresolvable = false;
            Set<String> externalSignatures = new HashSet<>();
            for (String extType : externalTypes) {
                Set<String> sigs = externalMethodCache.computeIfAbsent(extType, JarReader::resolveExternalMethods);
                if (sigs == null) {
                    anyUnresolvable = true;
                } else {
                    externalSignatures.addAll(sigs);
                }
            }

            for (ProgramMethod pm : pc.getMethods()) {
                if (pm.isConstructor() || pm.isStaticInitializer()
                        || pm.isPrivate() || pm.isStatic()) {
                    continue;
                }
                if (anyUnresolvable) {
                    pm.setOverridesExternal(true);
                } else {
                    String sig = pm.getName() + "(" + countDescriptorParams(pm.getDescriptor()) + ")";
                    if (externalSignatures.contains(sig)) {
                        pm.setOverridesExternal(true);
                    }
                }
            }
        }
    }

    /**
     * Tries to load an external type via {@code Class.forName()} and extract its non-private,
     * non-static method signatures. Returns {@code null} if the type can't be loaded
     * (signals conservative locking).
     */
    private static Set<String> resolveExternalMethods(String internalName) {
        if ("java/lang/Object".equals(internalName)) {
            return Set.of(); // Object methods are always safe to leave unlocked
        }
        try {
            Class<?> clazz = Class.forName(internalName.replace('/', '.'), false,
                    JarReader.class.getClassLoader());
            Set<String> sigs = new HashSet<>();
            for (Method m : clazz.getMethods()) {
                if (Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                // Skip Object base methods — they're universally available
                if (m.getDeclaringClass() == Object.class) {
                    continue;
                }
                sigs.add(m.getName() + "(" + m.getParameterCount() + ")");
            }
            return sigs;
        } catch (Throwable t) {
            return null; // unresolvable → conservative
        }
    }

    /** Counts the number of parameter slots in a JVM method descriptor like {@code (ILFoo;D)V → 3}. */
    private static int countDescriptorParams(String descriptor) {
        int count = 0;
        int i = 1; // skip '('
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            char c = descriptor.charAt(i);
            if (c == 'L') {
                count++;
                i = descriptor.indexOf(';', i) + 1;
            } else if (c == '[') {
                i++;
            } else {
                count++;
                i++;
            }
        }
        return count;
    }

    /**
     * Classifies application vs embedded-library classes based on the JAR manifest
     * {@code Main-Class} (or {@code Start-Class} for Spring Boot).
     *
     * <p>If a manifest launch class is found, its root package (parent of the main class's package,
     * covering all sibling packages) is treated as application code.
     * Everything outside that package tree is marked as an embedded library.
     * If no manifest entry exists, <strong>all classes are application classes</strong> (safe default).</p>
     */
    private static void classifyApplicationClasses(JarFile jar, JarMapping mapping) throws IOException {
        String appRoot = resolveApplicationRoot(jar, mapping);
        if (appRoot == null) {
            // No manifest hint → treat everything as application (safe default)
            return;
        }
        for (ProgramClass pc : mapping.getProgramClasses()) {
            String name = pc.getName();
            boolean isApp = name.startsWith(appRoot + "/") || name.equals(appRoot);
            pc.setApplicationClass(isApp);
        }
    }

    /**
     * Extracts the application root package from manifest Main-Class / Start-Class,
     * or from module-info mainClass. Returns the root package prefix (internal slash form)
     * by going up one level from the main class's immediate package, so all sibling
     * packages are included. Returns {@code null} if none found.
     */
    private static String resolveApplicationRoot(JarFile jar, JarMapping mapping) throws IOException {
        // 1. Try manifest
        Manifest manifest = jar.getManifest();
        if (manifest != null) {
            Attributes main = manifest.getMainAttributes();
            if (main != null) {
                String mainClass = firstNonNull(
                        main.getValue("Main-Class"),
                        main.getValue("Start-Class"));
                if (mainClass != null && !mainClass.isBlank()) {
                    String internal = mainClass.trim().replace('.', '/');
                    return parentPackageOf(internal);
                }
            }
        }
        // 2. Try module-info mainClass
        for (ModuleInfoClass mic : mapping.getModuleInfos()) {
            ClassNode cn = mic.getClassNode();
            if (cn != null && cn.module != null) {
                ModuleNode mod = cn.module;
                if (mod.mainClass != null && !mod.mainClass.isBlank()) {
                    return parentPackageOf(mod.mainClass);
                }
            }
        }
        return null;
    }

    /**
     * Returns the parent of the class's package (i.e. two levels up from the class name).
     * For {@code a/b/c/d/Foo} this returns {@code a/b/c} so all sibling packages
     * ({@code a/b/c/d}, {@code a/b/c/e}, etc.) are covered.
     * Falls back to the immediate package if there is no grandparent.
     */
    private static String parentPackageOf(String internalName) {
        String pkg = packageOf(internalName);
        if (pkg == null) {
            return null;
        }
        String parent = packageOf(pkg);
        return parent != null ? parent : pkg;
    }

    private static String packageOf(String internalName) {
        int last = internalName.lastIndexOf('/');
        return last > 0 ? internalName.substring(0, last) : null;
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
