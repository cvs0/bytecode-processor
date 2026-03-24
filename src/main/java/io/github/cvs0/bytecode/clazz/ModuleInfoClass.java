package io.github.cvs0.bytecode.clazz;

import org.objectweb.asm.tree.ClassNode;

import java.util.Objects;

/**
 * A {@code module-info.class} entry from a JAR (possibly under a multi-release path).
 * Kept separate from {@link ProgramClass} because the file is a module descriptor, not a normal type.
 */
public final class ModuleInfoClass {
    private String jarEntryName;
    private final ClassNode classNode;
    private int classVersion;

    public ModuleInfoClass(String jarEntryName, ClassNode classNode, int classVersion) {
        this.jarEntryName = Objects.requireNonNull(jarEntryName, "jarEntryName");
        this.classNode = Objects.requireNonNull(classNode, "classNode");
        this.classVersion = classVersion;
    }

    public String getJarEntryName() {
        return jarEntryName;
    }

    public void setJarEntryName(String jarEntryName) {
        this.jarEntryName = Objects.requireNonNull(jarEntryName, "jarEntryName");
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    /** Internal name of the class file ({@code module-info}). */
    public String getInternalName() {
        return classNode.name != null ? classNode.name : "module-info";
    }

    public int getClassVersion() {
        return classVersion;
    }

    public void setClassVersion(int classVersion) {
        this.classVersion = classVersion;
    }
}
