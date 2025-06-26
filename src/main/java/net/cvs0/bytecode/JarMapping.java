package net.cvs0.bytecode;

import net.cvs0.bytecode.clazz.LibraryClass;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.util.JarReader;
import net.cvs0.bytecode.util.JarWriter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JarMapping {
    private final Map<String, ProgramClass> programClasses = new ConcurrentHashMap<>();
    private final Map<String, LibraryClass> libraryClasses = new ConcurrentHashMap<>();
    private final Map<String, byte[]> resources = new ConcurrentHashMap<>();
    private final String jarPath;
    
    public JarMapping(String jarPath) {
        this.jarPath = jarPath;
    }
    
    public static JarMapping fromJar(String jarPath) throws IOException {
        JarMapping mapping = new JarMapping(jarPath);
        JarReader.read(new File(jarPath), mapping);
        return mapping;
    }
    
    public void addClass(ProgramClass clazz) {
        programClasses.put(clazz.getName(), clazz);
    }
    
    public void addLibraryClass(LibraryClass clazz) {
        libraryClasses.put(clazz.getName(), clazz);
    }
    
    public void addResource(String name, byte[] data) {
        resources.put(name, data);
    }
    
    public ProgramClass getProgramClass(String name) {
        return programClasses.get(name);
    }
    
    public LibraryClass getLibraryClass(String name) {
        return libraryClasses.get(name);
    }
    
    public byte[] getResource(String name) {
        return resources.get(name);
    }
    
    public Collection<ProgramClass> getProgramClasses() {
        return Collections.unmodifiableCollection(programClasses.values());
    }
    
    public Collection<LibraryClass> getLibraryClasses() {
        return Collections.unmodifiableCollection(libraryClasses.values());
    }
    
    public Set<String> getResourceNames() {
        return Collections.unmodifiableSet(resources.keySet());
    }
    
    public void removeClass(String name) {
        programClasses.remove(name);
        libraryClasses.remove(name);
    }
    
    public void removeResource(String name) {
        resources.remove(name);
    }
    
    public void renameClass(String oldName, String newName) {
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
    }
    
    public void writeToJar(String outputPath) throws IOException {
        JarWriter.write(this, new File(outputPath));
    }
    
    public String getJarPath() {
        return jarPath;
    }
    
    public int getTotalClassCount() {
        return programClasses.size() + libraryClasses.size();
    }
    
    public int getResourceCount() {
        return resources.size();
    }
    
    public boolean containsClass(String name) {
        return programClasses.containsKey(name) || libraryClasses.containsKey(name);
    }
    
    public List<String> getAllClassNames() {
        List<String> names = new ArrayList<>();
        names.addAll(programClasses.keySet());
        names.addAll(libraryClasses.keySet());
        return names;
    }
}