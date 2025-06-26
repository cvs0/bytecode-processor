package net.cvs0.bytecode.member;

import net.cvs0.bytecode.attribute.Attribute;
import net.cvs0.bytecode.clazz.LibraryClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibraryField {
    private String name;
    private String descriptor;
    private String signature;
    private int access;
    private Object value;
    
    private LibraryClass owner;
    private final List<Attribute> attributes = new ArrayList<>();
    
    public LibraryField(String name, String descriptor, int access) {
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
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
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
    
    public boolean isVolatile() {
        return (access & 0x0040) != 0;
    }
    
    public boolean isTransient() {
        return (access & 0x0080) != 0;
    }
    
    public boolean isSynthetic() {
        return (access & 0x1000) != 0;
    }
    
    public boolean isEnum() {
        return (access & 0x4000) != 0;
    }
    
    public String getType() {
        return descriptor;
    }
    
    public String getFullName() {
        return owner != null ? owner.getName() + "." + name : name;
    }
}