package net.cvs0.bytecode.clazz;

import net.cvs0.bytecode.attribute.Attribute;
import net.cvs0.bytecode.member.LibraryField;
import net.cvs0.bytecode.member.LibraryMethod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LibraryClass {
    private String name;
    private String superName;
    private List<String> interfaces;
    private int access;
    private String signature;
    
    private final Map<String, LibraryField> fields = new ConcurrentHashMap<>();
    private final Map<String, LibraryMethod> methods = new ConcurrentHashMap<>();
    private final List<Attribute> attributes = new ArrayList<>();
    
    public LibraryClass(String name) {
        this.name = name;
        this.interfaces = new ArrayList<>();
    }
    
    public void addField(LibraryField field) {
        fields.put(field.getName(), field);
        field.setOwner(this);
    }
    
    public void addMethod(LibraryMethod method) {
        String key = method.getName() + method.getDescriptor();
        methods.put(key, method);
        method.setOwner(this);
    }
    
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }
    
    public LibraryField getField(String name) {
        return fields.get(name);
    }
    
    public LibraryMethod getMethod(String name, String descriptor) {
        return methods.get(name + descriptor);
    }
    
    public Collection<LibraryField> getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }
    
    public Collection<LibraryMethod> getMethods() {
        return Collections.unmodifiableCollection(methods.values());
    }
    
    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSuperName() {
        return superName;
    }
    
    public void setSuperName(String superName) {
        this.superName = superName;
    }
    
    public List<String> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }
    
    public void setInterfaces(List<String> interfaces) {
        this.interfaces = new ArrayList<>(interfaces);
    }
    
    public void addInterface(String interfaceName) {
        if (!interfaces.contains(interfaceName)) {
            interfaces.add(interfaceName);
        }
    }
    
    public void removeInterface(String interfaceName) {
        interfaces.remove(interfaceName);
    }
    
    public int getAccess() {
        return access;
    }
    
    public void setAccess(int access) {
        this.access = access;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public boolean isInterface() {
        return (access & 0x0200) != 0;
    }
    
    public boolean isAbstract() {
        return (access & 0x0400) != 0;
    }
    
    public boolean isFinal() {
        return (access & 0x0010) != 0;
    }
    
    public boolean isPublic() {
        return (access & 0x0001) != 0;
    }
    
    public boolean isEnum() {
        return (access & 0x4000) != 0;
    }
    
    public boolean isAnnotation() {
        return (access & 0x2000) != 0;
    }
    
    public String getSimpleName() {
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }
    
    public String getPackageName() {
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(0, lastSlash).replace('/', '.') : "";
    }
}