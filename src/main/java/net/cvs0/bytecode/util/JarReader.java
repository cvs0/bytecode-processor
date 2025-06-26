package net.cvs0.bytecode.util;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarReader {
    
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
                    processClassEntry(jar, entry, mapping);
                } else {
                    processResourceEntry(jar, entry, mapping);
                }
            }
        }
    }
    
    private static void processClassEntry(JarFile jar, JarEntry entry, JarMapping mapping) throws IOException {
        try (InputStream inputStream = jar.getInputStream(entry)) {
            byte[] classBytes = inputStream.readAllBytes();
            
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
            
            mapping.addClass(programClass);
        }
    }
    
    private static void processResourceEntry(JarFile jar, JarEntry entry, JarMapping mapping) throws IOException {
        try (InputStream inputStream = jar.getInputStream(entry)) {
            byte[] resourceBytes = inputStream.readAllBytes();
            mapping.addResource(entry.getName(), resourceBytes);
        }
    }
    
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
    
    public static byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }
}