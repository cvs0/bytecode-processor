package io.github.cvs0.bytecode.clazz;

import io.github.cvs0.bytecode.attribute.Attribute;
import io.github.cvs0.bytecode.member.LibraryField;
import io.github.cvs0.bytecode.member.LibraryMethod;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a library (external) class, including its name, superclass, interfaces, fields, methods, and attributes.
 * Provides methods for querying and modifying class structure and metadata.
 */
public class LibraryClass {
    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String superName;

    private List<String> interfaces;

    @Getter
    @Setter
    private int access;

    @Getter
    @Setter
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

    public boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isAbstract() {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isFinal() {
        return (access & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isPublic() {
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isEnum() {
        return (access & Opcodes.ACC_ENUM) != 0;
    }

    public boolean isAnnotation() {
        return (access & Opcodes.ACC_ANNOTATION) != 0;
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
