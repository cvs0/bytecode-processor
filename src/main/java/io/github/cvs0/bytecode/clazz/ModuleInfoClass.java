package io.github.cvs0.bytecode.clazz;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Objects;

/**
 * A {@code module-info.class} entry from a JAR (possibly under a multi-release path).
 * Kept separate from {@link ProgramClass} because the file is a module descriptor, not a normal type.
 */
@Getter
@Setter
public final class ModuleInfoClass {
    private String jarEntryName;
    private ClassNode classNode;
    private int classVersion;

    public ModuleInfoClass(String jarEntryName, ClassNode classNode, int classVersion) {
        this.jarEntryName = Objects.requireNonNull(jarEntryName, "jarEntryName");
        this.classNode = Objects.requireNonNull(classNode, "classNode");
        this.classVersion = classVersion;
    }

    /** Internal name of the class file ({@code module-info}). */
    public String getInternalName() {
        return classNode.name != null ? classNode.name : "module-info";
    }
}
