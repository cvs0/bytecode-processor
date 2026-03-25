package io.github.cvs0.bytecode.member;

import io.github.cvs0.bytecode.attribute.Attribute;
import io.github.cvs0.bytecode.clazz.LibraryClass;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

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
