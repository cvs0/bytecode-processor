package io.github.cvs0.bytecode.member;

import io.github.cvs0.bytecode.attribute.Attribute;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a field in a program class, including its name, descriptor, signature, access flags, value, and attributes.
 * Provides methods for manipulating field structure, metadata, and ASM FieldNode integration.
 * 
 * This class serves as a bridge between the bytecode representation of a field and its corresponding ASM FieldNode.
 * It provides a convenient API for accessing and modifying field metadata, as well as integrating with ASM for bytecode manipulation.
 */
public class ProgramField {
    private String name;
    private String descriptor;
    private String signature;
    private int access;
    private Object value;
    
    private ProgramClass owner;
    private final List<Attribute> attributes = new ArrayList<>();
    private FieldNode fieldNode;
    
    public ProgramField(String name, String descriptor, int access) {
        this.name = Objects.requireNonNull(name, "Field name cannot be null");
        this.descriptor = Objects.requireNonNull(descriptor, "Field descriptor cannot be null");
        this.access = access;
    }
    
    public ProgramField(FieldNode fieldNode) {
        this.fieldNode = Objects.requireNonNull(fieldNode, "FieldNode cannot be null");
        this.name = this.fieldNode.name;
        this.descriptor = this.fieldNode.desc;
        this.signature = this.fieldNode.signature;
        this.access = this.fieldNode.access;
        this.value = this.fieldNode.value;
    }
    
    public void addAttribute(Attribute attribute) {
        Objects.requireNonNull(attribute, "Attribute cannot be null");
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
        if (fieldNode != null) {
            fieldNode.name = name;
        }
    }
    
    public String getDescriptor() {
        return descriptor;
    }
    
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
        if (fieldNode != null) {
            fieldNode.desc = descriptor;
        }
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
        if (fieldNode != null) {
            fieldNode.signature = signature;
        }
    }
    
    public int getAccess() {
        return access;
    }
    
    public void setAccess(int access) {
        this.access = access;
        if (fieldNode != null) {
            fieldNode.access = access;
        }
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
        if (fieldNode != null) {
            fieldNode.value = value;
        }
    }
    
    public ProgramClass getOwner() {
        return owner;
    }
    
    public void setOwner(ProgramClass owner) {
        this.owner = owner;
    }
    
    public FieldNode getFieldNode() {
        return fieldNode;
    }
    
    public void setFieldNode(FieldNode fieldNode) {
        this.fieldNode = fieldNode;
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