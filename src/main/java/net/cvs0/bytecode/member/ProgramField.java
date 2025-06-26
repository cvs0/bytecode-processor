package net.cvs0.bytecode.member;

import net.cvs0.bytecode.attribute.Attribute;
import net.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        this.name = name;
        this.descriptor = descriptor;
        this.access = access;
    }
    
    public ProgramField(FieldNode fieldNode) {
        this.fieldNode = fieldNode;
        this.name = fieldNode.name;
        this.descriptor = fieldNode.desc;
        this.signature = fieldNode.signature;
        this.access = fieldNode.access;
        this.value = fieldNode.value;
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