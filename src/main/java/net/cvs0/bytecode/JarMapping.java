package net.cvs0.bytecode;

import net.cvs0.bytecode.clazz.LibraryClass;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.util.JarReader;
import net.cvs0.bytecode.util.JarWriter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a mapping of classes and resources within a JAR file.
 * Provides methods to add, remove, and retrieve program and library classes, as well as resources.
 * Supports reading from and writing to JAR files.
 */
public class JarMapping {
    /** Map of program class names to their ProgramClass representations. */
    private final Map<String, ProgramClass> programClasses = new ConcurrentHashMap<>();
    /** Map of library class names to their LibraryClass representations. */
    private final Map<String, LibraryClass> libraryClasses = new ConcurrentHashMap<>();
    /** Map of resource names to their byte array data. */
    private final Map<String, byte[]> resources = new ConcurrentHashMap<>();
    /** Path to the JAR file represented by this mapping. */
    private final String jarPath;
    
    /**
     * Constructs a new JarMapping for the specified JAR path.
     * @param jarPath the path to the JAR file
     */
    public JarMapping(String jarPath) {
        this.jarPath = jarPath;
    }
    
    /**
     * Loads a JarMapping from the specified JAR file.
     * @param jarPath the path to the JAR file
     * @return a new JarMapping instance
     * @throws IOException if the JAR cannot be read
     */
    public static JarMapping fromJar(String jarPath) throws IOException {
        JarMapping mapping = new JarMapping(jarPath);
        JarReader.read(new File(jarPath), mapping);
        return mapping;
    }
    
    /**
     * Adds a program class to the mapping.
     * @param clazz the ProgramClass to add
     */
    public void addClass(ProgramClass clazz) {
        programClasses.put(clazz.getName(), clazz);
    }
    
    /**
     * Adds a library class to the mapping.
     * @param clazz the LibraryClass to add
     */
    public void addLibraryClass(LibraryClass clazz) {
        libraryClasses.put(clazz.getName(), clazz);
    }
    
    /**
     * Adds a resource to the mapping.
     * @param name the resource name
     * @param data the resource data
     */
    public void addResource(String name, byte[] data) {
        resources.put(name, data);
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
    
    /**
     * Writes the contents of this mapping to a JAR file at the specified output path.
     * @param outputPath the output JAR file path
     * @throws IOException if writing fails
     */
    public void writeToJar(String outputPath) throws IOException {
        JarWriter.write(this, new File(outputPath));
    }
    
    /**
     * Returns the path to the JAR file represented by this mapping.
     * @return the JAR path
     */
    public String getJarPath() {
        return jarPath;
    }
    
    /**
     * Returns the total number of classes (program + library) in this mapping.
     * @return the total class count
     */
    public int getTotalClassCount() {
        return programClasses.size() + libraryClasses.size();
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