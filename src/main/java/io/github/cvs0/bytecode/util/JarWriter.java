package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
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
 * Writes {@link JarMapping} to a JAR: module descriptors, package-info, program classes, merged classes, then resources.
 * The manifest resource is supplied to {@link JarOutputStream} and is not written again as a resource entry.
 */
public class JarWriter {

    public static void write(JarMapping mapping, Path outputPath) throws IOException {
        write(mapping, outputPath.toFile());
    }

    /**
     * Uses the mapping's {@link JarLayout#MANIFEST} resource when present and parseable; otherwise a minimal default.
     */
    public static void write(JarMapping mapping, File outputFile) throws IOException {
        write(mapping, outputFile, resolveManifest(mapping));
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

    /**
     * Writes with the given manifest (full control over {@code Main-Class} and other main attributes).
     */
    public static void write(JarMapping mapping, File outputFile, Manifest manifest) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        try (FileOutputStream fos = new FileOutputStream(outputFile);
                JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            writeClassLikeEntries(mapping, jos);
            writeResources(mapping, jos);
        }
    }

    private static void writeResources(JarMapping mapping, JarOutputStream jos) throws IOException {
        List<String> names = new ArrayList<>(mapping.getResourceNames());
        Collections.sort(names);
        for (String resourceName : names) {
            if (JarLayout.MANIFEST.equals(resourceName)) {
                continue;
            }
            byte[] data = mapping.getResource(resourceName);
            if (data == null) {
                continue;
            }
            writeResourceEntry(jos, resourceName, data);
        }
    }

    private static void writeClassLikeEntries(JarMapping mapping, JarOutputStream jos) throws IOException {
        Set<String> writtenPaths = new HashSet<>();
        writeModuleDescriptors(mapping, jos, writtenPaths);
        writePackageInfos(mapping, jos, writtenPaths);
        writeProgramClasses(mapping, jos, writtenPaths);
        writeMergedClasses(mapping, jos, writtenPaths);
    }

    private static void writeModuleDescriptors(JarMapping mapping, JarOutputStream jos, Set<String> writtenPaths)
            throws IOException {
        List<String> modulePaths = new ArrayList<>(mapping.getModuleInfoEntryNames());
        modulePaths.sort(Comparator.naturalOrder());
        for (String path : modulePaths) {
            ModuleInfoClass mi = mapping.getModuleInfo(path);
            if (mi != null && mi.getClassNode() != null) {
                String jarPath = mi.getJarEntryName();
                writeRawClassEntry(jos, jarPath, classBytesFromNode(mi.getClassNode()));
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
                writeRawClassEntry(jos, jarPath, classBytesFromNode(pi.getClassNode()));
                writtenPaths.add(jarPath);
            }
        }
    }

    private static void writeProgramClasses(JarMapping mapping, JarOutputStream jos, Set<String> writtenPaths)
            throws IOException {
        List<ProgramClass> classes = new ArrayList<>(mapping.getProgramClasses());
        classes.sort(Comparator.comparing(ProgramClass::getName));
        for (ProgramClass programClass : classes) {
            String jarPath = programClass.getName() + ".class";
            writeClassEntry(jos, programClass);
            writtenPaths.add(jarPath);
        }
    }

    private static void writeMergedClasses(JarMapping mapping, JarOutputStream jos, Set<String> writtenPaths)
            throws IOException {
        for (String mergedPath : mapping.getMergedEntryPaths()) {
            if (writtenPaths.contains(mergedPath)) {
                continue;
            }
            byte[] data = mapping.getMergedEntry(mergedPath);
            if (data != null) {
                writeRawClassEntry(jos, mergedPath, data);
                writtenPaths.add(mergedPath);
            }
        }
    }

    private static void writeRawClassEntry(JarOutputStream jos, String jarPath, byte[] classBytes) throws IOException {
        JarEntry entry = new JarEntry(jarPath);
        jos.putNextEntry(entry);
        jos.write(classBytes);
        jos.closeEntry();
    }

    static byte[] classBytesFromNode(ClassNode classNode) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private static void writeClassEntry(JarOutputStream jos, ProgramClass programClass) throws IOException {
        String className = programClass.getName() + ".class";
        JarEntry entry = new JarEntry(className);
        jos.putNextEntry(entry);
        jos.write(generateClassBytes(programClass));
        jos.closeEntry();
    }

    private static void writeResourceEntry(JarOutputStream jos, String resourceName, byte[] resourceData)
            throws IOException {
        JarEntry entry = new JarEntry(resourceName);
        jos.putNextEntry(entry);
        jos.write(resourceData);
        jos.closeEntry();
    }

    private static byte[] generateClassBytes(ProgramClass programClass) {
        if (programClass.getClassNode() != null) {
            return classBytesFromNode(programClass.getClassNode());
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
        byte[] classBytes = generateClassBytes(programClass);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(classBytes);
        }
    }

    public static byte[] getClassBytes(ProgramClass programClass) {
        return generateClassBytes(programClass);
    }

    public static void writeResource(String resourceName, byte[] resourceData, File outputDir) throws IOException {
        File outputFile = new File(outputDir, resourceName);
        outputFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(resourceData);
        }
    }
}
