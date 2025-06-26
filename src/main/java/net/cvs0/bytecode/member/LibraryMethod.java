package net.cvs0.bytecode.member;

import net.cvs0.bytecode.attribute.Attribute;
import net.cvs0.bytecode.clazz.LibraryClass;

import java.util.*;

public class LibraryMethod {
    private String name;
    private String descriptor;
    private String signature;
    private int access;
    private String[] exceptions;
    
    private LibraryClass owner;
    private final List<Attribute> attributes = new ArrayList<>();
    
    public LibraryMethod(String name, String descriptor, int access) {
        this.name = name;
        this.descriptor = descriptor;
        this.access = access;
    }
    
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
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
    
    public String getDescriptor() {
        return descriptor;
    }
    
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public int getAccess() {
        return access;
    }
    
    public void setAccess(int access) {
        this.access = access;
    }
    
    public String[] getExceptions() {
        return exceptions != null ? exceptions.clone() : new String[0];
    }
    
    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions != null ? exceptions.clone() : new String[0];
    }
    
    public LibraryClass getOwner() {
        return owner;
    }
    
    public void setOwner(LibraryClass owner) {
        this.owner = owner;
    }
    
    public boolean isStatic() {
        return (access & 0x0008) != 0;
    }
    
    public boolean isFinal() {
        return (access & 0x0010) != 0;
    }
    
    public boolean isPublic() {
        return (access & 0x0001) != 0;
    }
    
    public boolean isPrivate() {
        return (access & 0x0002) != 0;
    }
    
    public boolean isProtected() {
        return (access & 0x0004) != 0;
    }
    
    public boolean isAbstract() {
        return (access & 0x0400) != 0;
    }
    
    public boolean isSynchronized() {
        return (access & 0x0020) != 0;
    }
    
    public boolean isNative() {
        return (access & 0x0100) != 0;
    }
    
    public boolean isSynthetic() {
        return (access & 0x1000) != 0;
    }
    
    public boolean isConstructor() {
        return "<init>".equals(name);
    }
    
    public boolean isStaticInitializer() {
        return "<clinit>".equals(name);
    }
    
    public String getFullName() {
        return owner != null ? owner.getName() + "." + name + descriptor : name + descriptor;
    }
}