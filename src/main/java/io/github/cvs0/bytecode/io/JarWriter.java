package io.github.cvs0.bytecode.io;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.transform.MappingRemapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Writes a {@link JarMapping} to a JAR: module descriptors, package-info, all program classes (each at its JAR path),
 * then every resource except {@link JarLayout#MANIFEST} (passed to {@link JarOutputStream}).
 *
 * @see JarReader
 * @see io.github.cvs0.bytecode.JarMapping#writeToJar(java.nio.file.Path)
 */
public class JarWriter {

    public static void write(JarMapping mapping, Path outputPath) throws IOException {
        write(mapping, outputPath.toFile());
    }

    public static void write(JarMapping mapping, File outputFile) throws IOException {
        write(mapping, outputFile, resolveManifest(mapping));
    }

    /**
     * Writes the given {@link JarMapping} to an in-memory byte array instead of a file on disk.
     * Useful for classloader injection, network transfer, or byte-backed URL schemes.
     *
     * @param mapping the mapping to serialize
     * @return the complete JAR file as a byte array
     * @throws IOException if serialization fails
     */
    public static byte[] writeToBytes(JarMapping mapping) throws IOException {
        Manifest manifest = resolveManifest(mapping);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             JarOutputStream jos = new JarOutputStream(baos, manifest)) {
            Set<String> writtenPaths = new HashSet<>();
            writeClassLikeEntries(mapping, jos, writtenPaths);
            writeResources(mapping, jos, writtenPaths);
            jos.finish();
            return baos.toByteArray();
        }
    }

    private static Manifest resolveManifest(JarMapping mapping) {
        byte[] raw = mapping.getResource(JarLayout.MANIFEST);
        if (raw != null && raw.length > 0) {
            try {
                return new Manifest(new ByteArrayInputStream(raw));
            } catch (IOException ignored) {
                // Corrupt manifest: fall back so output JAR is still produced
            }
        }
        return createDefaultManifest();
    }

    public static void write(JarMapping mapping, File outputFile, Manifest manifest) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        try (FileOutputStream fos = new FileOutputStream(outputFile);
                JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            Set<String> writtenPaths = new HashSet<>();
            writeClassLikeEntries(mapping, jos, writtenPaths);
            writeResources(mapping, jos, writtenPaths);
        }
    }

    private static void writeResources(JarMapping mapping, JarOutputStream jos, Set<String> writtenPaths)
            throws IOException {
        List<String> names = new ArrayList<>(mapping.getResourceNames());
        Collections.sort(names);
        for (String resourceName : names) {
            if (JarLayout.MANIFEST.equals(resourceName)) {
                continue;
            }
            if (writtenPaths.contains(resourceName)) {
                continue;
            }
            byte[] data = mapping.getResource(resourceName);
            if (data == null) {
                continue;
            }
            writeResourceEntry(jos, resourceName, data);
        }
    }

    private static void writeClassLikeEntries(JarMapping mapping, JarOutputStream jos, Set<String> writtenPaths)
            throws IOException {
        writeModuleDescriptors(mapping, jos, writtenPaths);
        writePackageInfos(mapping, jos, writtenPaths);
        writeProgramClasses(mapping, jos, writtenPaths);
    }

    private static void writeModuleDescriptors(JarMapping mapping, JarOutputStream jos, Set<String> writtenPaths)
            throws IOException {
        List<String> modulePaths = new ArrayList<>(mapping.getModuleInfoEntryNames());
        modulePaths.sort(Comparator.naturalOrder());
        for (String path : modulePaths) {
            ModuleInfoClass mi = mapping.getModuleInfo(path);
            if (mi != null && mi.getClassNode() != null) {
                String jarPath = mi.getJarEntryName();
                writeRawClassEntry(jos, jarPath, classBytesFromNode(mi.getClassNode(), mapping));
                writtenPaths.add(jarPath);
            }
        }
    }

    private static void writePackageInfos(JarMapping mapping, JarOutputStream jos, Set<String> writtenPaths)
            throws IOException {
        List<String> packagePaths = new ArrayList<>(mapping.getPackageInfoEntryNames());
        packagePaths.sort(Comparator.naturalOrder());
        for (String path : packagePaths) {
            PackageInfoClass pi = mapping.getPackageInfo(path);
            if (pi != null && pi.getClassNode() != null) {
                String jarPath = pi.getJarEntryName();
                writeRawClassEntry(jos, jarPath, classBytesFromNode(pi.getClassNode(), mapping));
                writtenPaths.add(jarPath);
            }
        }
    }

    private static void writeProgramClasses(JarMapping mapping, JarOutputStream jos, Set<String> writtenPaths)
            throws IOException {
        List<ProgramClass> classes = new ArrayList<>(mapping.getProgramClasses());
        classes.sort(Comparator.comparing(ProgramClass::getJarEntryName));
        for (ProgramClass programClass : classes) {
            String jarPath = programClass.getJarEntryName();
            if (!writtenPaths.add(jarPath)) {
                continue;
            }
            writeClassEntry(jos, programClass, jarPath, mapping);
        }
    }

    private static void writeRawClassEntry(JarOutputStream jos, String jarPath, byte[] classBytes) throws IOException {
        JarEntry entry = new JarEntry(jarPath);
        jos.putNextEntry(entry);
        jos.write(classBytes);
        jos.closeEntry();
    }

    static byte[] classBytesFromNode(ClassNode classNode, JarMapping mapping) {
        ClassWriter classWriter = new SafeClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, mapping);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private static void writeClassEntry(JarOutputStream jos, ProgramClass programClass, String jarPath, JarMapping mapping) throws IOException {
        JarEntry entry = new JarEntry(jarPath);
        jos.putNextEntry(entry);
        jos.write(generateClassBytes(programClass, mapping));
        jos.closeEntry();
    }

    private static void writeResourceEntry(JarOutputStream jos, String resourceName, byte[] resourceData)
            throws IOException {
        JarEntry entry = new JarEntry(resourceName);
        jos.putNextEntry(entry);
        jos.write(resourceData);
        jos.closeEntry();
    }

    private static byte[] generateClassBytes(ProgramClass programClass, JarMapping mapping) {
        if (programClass.getClassNode() != null) {
            return classBytesFromNode(programClass.getClassNode(), mapping);
        }
        throw new IllegalStateException("Cannot generate bytes for class without ClassNode: " + programClass.getName());
    }

    private static Manifest createDefaultManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Created-By", "Bytecode Processor Library");
        return manifest;
    }

    public static void writeClass(ProgramClass programClass, File outputFile) throws IOException {
        byte[] classBytes = generateClassBytes(programClass, null);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(classBytes);
        }
    }

    public static byte[] getClassBytes(ProgramClass programClass) {
        return generateClassBytes(programClass, null);
    }

    /**
     * Returns the bytecode for a {@link ProgramClass}, using the given {@link JarMapping}
     * for hierarchy-aware frame computation via {@link SafeClassWriter}.
     *
     * @param programClass the class to serialize
     * @param mapping      the JarMapping for hierarchy resolution (may be {@code null})
     * @return the class bytes
     */
    public static byte[] getClassBytes(ProgramClass programClass, JarMapping mapping) {
        return generateClassBytes(programClass, mapping);
    }

    /**
     * Remaps a single {@link ProgramClass} through the given {@link MappingRemapper} and returns
     * the resulting bytecode. The {@code mapping} is used by {@link SafeClassWriter} to resolve
     * common supertypes during frame computation.
     *
     * <p>This is the streaming-friendly counterpart to
     * {@link io.github.cvs0.bytecode.transform.transformer.ClassTransformer#applyTransformations()} — it
     * transforms one class at a time without touching the rest of the mapping.</p>
     *
     * @param programClass the class to remap
     * @param remapper     the rename mappings to apply
     * @param mapping      the JarMapping for hierarchy-aware frame computation (may be {@code null}
     *                     if frame computation is not needed)
     * @return the remapped class bytes
     */
    public static byte[] remapClassBytes(ProgramClass programClass, MappingRemapper remapper, JarMapping mapping) {
        Objects.requireNonNull(programClass, "programClass");
        Objects.requireNonNull(remapper, "remapper");
        ClassNode original = programClass.getClassNode();
        if (original == null) {
            throw new IllegalStateException("Cannot remap class without ClassNode: " + programClass.getName());
        }
        ClassNode remapped = new ClassNode();
        ClassRemapper cr = new ClassRemapper(remapped, remapper.toAsm());
        original.accept(cr);
        return classBytesFromNode(remapped, mapping);
    }

    public static void writeResource(String resourceName, byte[] resourceData, File outputDir) throws IOException {
        File outputFile = new File(outputDir, resourceName);
        outputFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(resourceData);
        }
    }
}
