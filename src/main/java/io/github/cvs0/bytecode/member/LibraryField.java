package io.github.cvs0.bytecode.member;

import io.github.cvs0.bytecode.attribute.Attribute;
import io.github.cvs0.bytecode.clazz.LibraryClass;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibraryField {
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

    @Getter
    @Setter
    private Object value;

    @Getter
    @Setter
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
