package io.github.cvs0.bytecode.member;

import io.github.cvs0.bytecode.attribute.Attribute;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Opcodes;
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
    @Getter
    private String name;
    @Getter
    private String descriptor;
    @Getter
    private String signature;
    @Getter
    private int access;
    @Getter
    private Object value;

    @Getter
    @Setter
    private ProgramClass owner;
    private final List<Attribute> attributes = new ArrayList<>();
    @Getter
    @Setter
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

    public void setName(String name) {
        this.name = name;
        if (fieldNode != null) {
            fieldNode.name = name;
        }
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
        if (fieldNode != null) {
            fieldNode.desc = descriptor;
        }
    }

    public void setSignature(String signature) {
        this.signature = signature;
        if (fieldNode != null) {
            fieldNode.signature = signature;
        }
    }

    public void setAccess(int access) {
        this.access = access;
        if (fieldNode != null) {
            fieldNode.access = access;
        }
    }

    public void setValue(Object value) {
        this.value = value;
        if (fieldNode != null) {
            fieldNode.value = value;
        }
    }

    public boolean isStatic() {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isFinal() {
        return (access & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isPublic() {
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isPrivate() {
        return (access & Opcodes.ACC_PRIVATE) != 0;
    }

    public boolean isProtected() {
        return (access & Opcodes.ACC_PROTECTED) != 0;
    }

    public boolean isVolatile() {
        return (access & Opcodes.ACC_VOLATILE) != 0;
    }

    public boolean isTransient() {
        return (access & Opcodes.ACC_TRANSIENT) != 0;
    }

    public boolean isSynthetic() {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public boolean isEnum() {
        return (access & Opcodes.ACC_ENUM) != 0;
    }

    public String getType() {
        return descriptor;
    }

    public String getFullName() {
        return owner != null ? owner.getName() + "." + name : name;
    }
}
