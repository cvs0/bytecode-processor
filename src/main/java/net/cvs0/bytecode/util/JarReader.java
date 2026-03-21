package net.cvs0.bytecode.util;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class for reading classes and resources from JAR files and class files.
 * Supports loading classes into JarMapping and reading raw bytes.
 */
public class JarReader {
    /**
     * Reads all entries from a JAR file and populates the given JarMapping with classes and resources.
     * @param jarFile the JAR file to read
     * @param mapping the JarMapping to populate
     * @throws IOException if reading fails
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
                    if (entryName.equals("module-info.class")) {
                        processModuleInfoEntry(jar, entry, mapping);
                    } else if (entryName.endsWith("package-info.class")) {
                        processPackageInfoEntry(jar, entry, mapping);
                    } else {
                        processClassEntry(jar, entry, mapping);
                    }
                } else {
                    processResourceEntry(jar, entry, mapping);
                }
            }
        }
    }

    /**
     * Processes a module-info.class entry and adds it to the mapping.
     * @param jar the JarFile
     * @param entry the module-info entry
     * @param mapping the JarMapping
     * @throws IOException if reading fails
     */
    private static void processModuleInfoEntry(JarFile jar, JarEntry entry, JarMapping mapping) throws IOException {
        try (InputStream inputStream = jar.getInputStream(entry)) {
            byte[] classBytes = inputStream.readAllBytes();
            ClassReader classReader = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            // Store module info as a special ProgramClass or ModuleInfo model
            if (classNode.module != null) {
                // Optionally, create a ModuleInfo model and add to mapping
                // mapping.addModuleInfo(new ModuleInfo(classNode.module));
            }
            // Optionally, store as a ProgramClass for uniformity
            ProgramClass programClass = new ProgramClass(classNode);
            mapping.addClass(programClass);
        }
    }

    /**
     * Processes a package-info.class entry and adds it to the mapping.
     * @param jar the JarFile
     * @param entry the package-info entry
     * @param mapping the JarMapping
     * @throws IOException if reading fails
     */
    private static void processPackageInfoEntry(JarFile jar, JarEntry entry, JarMapping mapping) throws IOException {
        try (InputStream inputStream = jar.getInputStream(entry)) {
            byte[] classBytes = inputStream.readAllBytes();
            ClassReader classReader = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            // Optionally, create a PackageInfo model and add to mapping
            // mapping.addPackageInfo(new PackageInfo(classNode));
            // Or store as a ProgramClass
            ProgramClass programClass = new ProgramClass(classNode);
            mapping.addClass(programClass);
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
                System.err.println("[DEBUG] Skipping class with null/empty name: " + entry.getName());
                return;
            }
            System.out.println("[DEBUG] Loaded class: " + classNode.name + " from entry: " + entry.getName());
            try {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(cw);
            } catch (Throwable t) {
                System.err.println("[DEBUG] ClassWriter failed to compute labels for " + classNode.name + " - falling back to per-method accept: " + t);
                if (classNode.methods != null) {
                    for (MethodNode methodNode : classNode.methods) {
                        methodNode.accept(new MethodVisitor(Opcodes.ASM9) {});
                    }
                }
            }

            ProgramClass programClass = new ProgramClass(classNode);
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
            System.err.println("[DEBUG] Error processing class entry: " + entry.getName() + " - " + e);
            e.printStackTrace();
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
}