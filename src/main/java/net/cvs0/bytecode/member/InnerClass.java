package net.cvs0.bytecode.member;

import lombok.Data;
import org.objectweb.asm.Opcodes;

/**
 * Represents an inner class entry (JVMS InnerClasses attribute row): internal name, outer class, simple name, access.
 */
@Data
public class InnerClass {
    private String innerClass;
    private String outerClass;
    private String innerName;
    private int access;

    public InnerClass(String innerClass, String outerClass, String innerName, int access) {
        this.innerClass = innerClass;
        this.outerClass = outerClass;
        this.innerName = innerName;
        this.access = access;
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

    public boolean isStatic() {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isFinal() {
        return (access & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isAbstract() {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isSynthetic() {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public boolean isAnnotation() {
        return (access & Opcodes.ACC_ANNOTATION) != 0;
    }

    public boolean isEnum() {
        return (access & Opcodes.ACC_ENUM) != 0;
    }

    public boolean isAnonymous() {
        return innerName == null;
    }

    public boolean isLocal() {
        return outerClass == null && innerName != null;
    }

    public boolean isMember() {
        return outerClass != null && innerName != null;
    }
}
