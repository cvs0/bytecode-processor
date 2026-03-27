package io.github.cvs0.bytecode.member;

import io.github.cvs0.bytecode.attribute.Attribute;
import io.github.cvs0.bytecode.clazz.LibraryClass;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibraryMethod {
    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String descriptor;

    @Getter
    @Setter
    private String signature;

    @Getter
    @Setter
    private int access;

    private String[] exceptions;

    @Getter
    @Setter
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

    public String[] getExceptions() {
        return exceptions != null ? exceptions.clone() : new String[0];
    }

    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions != null ? exceptions.clone() : new String[0];
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

    public boolean isAbstract() {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isSynchronized() {
        return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
    }

    public boolean isNative() {
        return (access & Opcodes.ACC_NATIVE) != 0;
    }

    public boolean isSynthetic() {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
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
