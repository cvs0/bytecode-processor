package io.github.cvs0.bytecode.clazz;

import io.github.cvs0.bytecode.attribute.Attribute;
import io.github.cvs0.bytecode.member.LibraryField;
import io.github.cvs0.bytecode.member.LibraryMethod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a library (external) class, including its name, superclass, interfaces, fields, methods, and attributes.
 * Provides methods for querying and modifying class structure and metadata.
 */
public class LibraryClass {
    private String name;
    private String superName;
    private List<String> interfaces;
    private int access;
    private String signature;
    
    private final Map<String, LibraryField> fields = new ConcurrentHashMap<>();
    private final Map<String, LibraryMethod> methods = new ConcurrentHashMap<>();
    private final List<Attribute> attributes = new ArrayList<>();
    
    /**
     * Constructs a LibraryClass with the given name.
     * @param name the class name
     */
    public LibraryClass(String name) {
        this.name = name;
        this.interfaces = new ArrayList<>();
    }

    /**
     * Adds a field to this class.
     * @param field the LibraryField to add
     */
    public void addField(LibraryField field) {
        fields.put(field.getName(), field);
        field.setOwner(this);
    }

    /**
     * Adds a method to this class.
     * @param method the LibraryMethod to add
     */
    public void addMethod(LibraryMethod method) {
        String key = method.getName() + method.getDescriptor();
        methods.put(key, method);
        method.setOwner(this);
    }

    /**
     * Adds an attribute to this class.
     * @param attribute the Attribute to add
     */
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }

    /**
     * Gets a field by name.
     * @param name the field name
     * @return the LibraryField or null
     */
    public LibraryField getField(String name) {
        return fields.get(name);
    }

    /**
     * Gets a method by name and descriptor.
     * @param name the method name
     * @param descriptor the method descriptor
     * @return the LibraryMethod or null
     */
    public LibraryMethod getMethod(String name, String descriptor) {
        return methods.get(name + descriptor);
    }

    /**
     * Returns all fields in this class.
     * @return unmodifiable collection of fields
     */
    public Collection<LibraryField> getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }

    /**
     * Returns all methods in this class.
     * @return unmodifiable collection of methods
     */
    public Collection<LibraryMethod> getMethods() {
        return Collections.unmodifiableCollection(methods.values());
    }

    /**
     * Returns all attributes in this class.
     * @return unmodifiable list of attributes
     */
    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    /**
     * Gets the class name.
     * @return the class name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the class name.
     * @param name the new class name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the superclass name.
     * @return the superclass name
     */
    public String getSuperName() {
        return superName;
    }

    /**
     * Sets the superclass name.
     * @param superName the new superclass name
     */
    public void setSuperName(String superName) {
        this.superName = superName;
    }

    /**
     * Gets the list of interfaces implemented by this class.
     * @return unmodifiable list of interface names
     */
    public List<String> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    /**
     * Sets the list of interfaces implemented by this class.
     * @param interfaces the new list of interface names
     */
    public void setInterfaces(List<String> interfaces) {
        this.interfaces = new ArrayList<>(interfaces);
    }

    /**
     * Adds an interface to this class.
     * @param interfaceName the interface name
     */
    public void addInterface(String interfaceName) {
        if (!interfaces.contains(interfaceName)) {
            interfaces.add(interfaceName);
        }
    }

    /**
     * Removes an interface from this class.
     * @param interfaceName the interface name
     */
    public void removeInterface(String interfaceName) {
        interfaces.remove(interfaceName);
    }

    /**
     * Gets the access flags for this class.
     * @return the access flags
     */
    public int getAccess() {
        return access;
    }

    /**
     * Sets the access flags for this class.
     * @param access the new access flags
     */
    public void setAccess(int access) {
        this.access = access;
    }

    /**
     * Gets the generic signature for this class.
     * @return the signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Sets the generic signature for this class.
     * @param signature the new signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * Returns true if this class is an interface.
     * @return true if interface
     */
    public boolean isInterface() {
        return (access & 0x0200) != 0;
    }

    /**
     * Returns true if this class is abstract.
     * @return true if abstract
     */
    public boolean isAbstract() {
        return (access & 0x0400) != 0;
    }

    /**
     * Returns true if this class is final.
     * @return true if final
     */
    public boolean isFinal() {
        return (access & 0x0010) != 0;
    }

    /**
     * Returns true if this class is public.
     * @return true if public
     */
    public boolean isPublic() {
        return (access & 0x0001) != 0;
    }

    /**
     * Returns true if this class is an enum.
     * @return true if enum
     */
    public boolean isEnum() {
        return (access & 0x4000) != 0;
    }

    /**
     * Returns true if this class is an annotation.
     * @return true if annotation
     */
    public boolean isAnnotation() {
        return (access & 0x2000) != 0;
    }

    /**
     * Gets the simple (unqualified) class name.
     * @return the simple class name
     */
    public String getSimpleName() {
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }

    /**
     * Gets the package name for this class.
     * @return the package name
     */
    public String getPackageName() {
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(0, lastSlash).replace('/', '.') : "";
    }
}