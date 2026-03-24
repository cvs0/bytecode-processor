package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Utility class for writing classes and resources to JAR files.
 * Supports writing entire JarMapping contents, individual classes, and resources.
 */
public class JarWriter {

    /** Written implicitly by {@link JarOutputStream#JarOutputStream(OutputStream, Manifest)} — must not be duplicated as a resource. */
    private static final String MANIFEST_ENTRY = "META-INF/MANIFEST.MF";
    /**
     * Writes all classes and resources from the mapping to the specified output JAR file.
     * Uses a default manifest.
     * @param mapping the JarMapping to write
     * @param outputFile the output JAR file
     * @throws IOException if writing fails
     */
    /**
     * Writes the mapping to a JAR file at the given path.
     */
    public static void write(JarMapping mapping, Path outputPath) throws IOException {
        write(mapping, outputPath.toFile());
    }

    public static void write(JarMapping mapping, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             JarOutputStream jos = new JarOutputStream(fos, createDefaultManifest())) {
            writeClassLikeEntries(mapping, jos);
            for (String resourceName : mapping.getResourceNames()) {
                if (MANIFEST_ENTRY.equals(resourceName)) {
                    continue;
                }
                writeResourceEntry(jos, resourceName, mapping.getResource(resourceName));
            }
        }
    }
    
    /**
     * Writes all classes and resources from the mapping to the specified output JAR file, using a custom manifest.
     * @param mapping the JarMapping to write
     * @param outputFile the output JAR file
     * @param manifest the manifest to use
     * @throws IOException if writing fails
     */
    public static void write(JarMapping mapping, File outputFile, Manifest manifest) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            writeClassLikeEntries(mapping, jos);
            for (String resourceName : mapping.getResourceNames()) {
                if (MANIFEST_ENTRY.equals(resourceName)) {
                    continue;
                }
                writeResourceEntry(jos, resourceName, mapping.getResource(resourceName));
            }
        }
    }

    private static void writeClassLikeEntries(JarMapping mapping, JarOutputStream jos) throws IOException {
        List<String> modulePaths = new ArrayList<>(mapping.getModuleInfoEntryNames());
        modulePaths.sort(Comparator.naturalOrder());
        for (String path : modulePaths) {
            ModuleInfoClass mi = mapping.getModuleInfo(path);
            if (mi != null && mi.getClassNode() != null) {
                writeRawClassEntry(jos, mi.getJarEntryName(), classBytesFromNode(mi.getClassNode()));
            }
        }
        List<String> packagePaths = new ArrayList<>(mapping.getPackageInfoEntryNames());
        packagePaths.sort(Comparator.naturalOrder());
        for (String path : packagePaths) {
            PackageInfoClass pi = mapping.getPackageInfo(path);
            if (pi != null && pi.getClassNode() != null) {
                writeRawClassEntry(jos, pi.getJarEntryName(), classBytesFromNode(pi.getClassNode()));
            }
        }
        for (ProgramClass programClass : mapping.getProgramClasses()) {
            writeClassEntry(jos, programClass);
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
    
    /**
     * Writes a single class entry to the JAR output stream.
     * @param jos the JarOutputStream
     * @param programClass the ProgramClass to write
     * @throws IOException if writing fails
     */
    private static void writeClassEntry(JarOutputStream jos, ProgramClass programClass) throws IOException {
        String className = programClass.getName() + ".class";
        JarEntry entry = new JarEntry(className);
        jos.putNextEntry(entry);
        
        byte[] classBytes = generateClassBytes(programClass);
        jos.write(classBytes);
        jos.closeEntry();
    }
    
    /**
     * Writes a single resource entry to the JAR output stream.
     * @param jos the JarOutputStream
     * @param resourceName the resource name
     * @param resourceData the resource data
     * @throws IOException if writing fails
     */
    private static void writeResourceEntry(JarOutputStream jos, String resourceName, byte[] resourceData) throws IOException {
        JarEntry entry = new JarEntry(resourceName);
        jos.putNextEntry(entry);
        jos.write(resourceData);
        jos.closeEntry();
    }
    
    /**
     * Generates the bytecode for a ProgramClass.
     * @param programClass the ProgramClass
     * @return the byte array of class bytes
     */
    private static byte[] generateClassBytes(ProgramClass programClass) {
        if (programClass.getClassNode() != null) {
            return classBytesFromNode(programClass.getClassNode());
        }
        throw new IllegalStateException("Cannot generate bytes for class without ClassNode: " + programClass.getName());
    }
    
    /**
     * Creates a default manifest for JAR files.
     * @return a Manifest instance
     */
    private static Manifest createDefaultManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Created-By", "Bytecode Processor Library");
        return manifest;
    }
    
    /**
     * Writes a single class to a .class file.
     * @param programClass the ProgramClass to write
     * @param outputFile the output file
     * @throws IOException if writing fails
     */
    public static void writeClass(ProgramClass programClass, File outputFile) throws IOException {
        byte[] classBytes = generateClassBytes(programClass);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(classBytes);
        }
    }
    
    /**
     * Returns the byte array for a ProgramClass.
     * @param programClass the ProgramClass
     * @return the class bytes
     */
    public static byte[] getClassBytes(ProgramClass programClass) {
        return generateClassBytes(programClass);
    }
    
    /**
     * Writes a resource to the specified output directory.
     * @param resourceName the resource name
     * @param resourceData the resource data
     * @param outputDir the output directory
     * @throws IOException if writing fails
     */
    public static void writeResource(String resourceName, byte[] resourceData, File outputDir) throws IOException {
        File outputFile = new File(outputDir, resourceName);
        outputFile.getParentFile().mkdirs();
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(resourceData);
        }
    }
}