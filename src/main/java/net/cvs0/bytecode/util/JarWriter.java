package net.cvs0.bytecode.util;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarWriter {
    
    public static void write(JarMapping mapping, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             JarOutputStream jos = new JarOutputStream(fos, createDefaultManifest())) {
            
            for (ProgramClass programClass : mapping.getProgramClasses()) {
                writeClassEntry(jos, programClass);
            }
            
            for (String resourceName : mapping.getResourceNames()) {
                writeResourceEntry(jos, resourceName, mapping.getResource(resourceName));
            }
        }
    }
    
    public static void write(JarMapping mapping, File outputFile, Manifest manifest) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            
            for (ProgramClass programClass : mapping.getProgramClasses()) {
                writeClassEntry(jos, programClass);
            }
            
            for (String resourceName : mapping.getResourceNames()) {
                writeResourceEntry(jos, resourceName, mapping.getResource(resourceName));
            }
        }
    }
    
    private static void writeClassEntry(JarOutputStream jos, ProgramClass programClass) throws IOException {
        String className = programClass.getName() + ".class";
        JarEntry entry = new JarEntry(className);
        jos.putNextEntry(entry);
        
        byte[] classBytes = generateClassBytes(programClass);
        jos.write(classBytes);
        jos.closeEntry();
    }
    
    private static void writeResourceEntry(JarOutputStream jos, String resourceName, byte[] resourceData) throws IOException {
        JarEntry entry = new JarEntry(resourceName);
        jos.putNextEntry(entry);
        jos.write(resourceData);
        jos.closeEntry();
    }
    
    private static byte[] generateClassBytes(ProgramClass programClass) {
        if (programClass.getClassNode() != null) {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            programClass.getClassNode().accept(classWriter);
            return classWriter.toByteArray();
        } else {
            throw new IllegalStateException("Cannot generate bytes for class without ClassNode: " + programClass.getName());
        }
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