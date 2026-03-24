package io.github.cvs0.bytecode.clazz;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Objects;

/**
 * A {@code package-info.class} entry from a JAR: package-level annotations and {@code package.html} metadata, not a normal program class.
 */
@Getter
@Setter
public final class PackageInfoClass {
    private String jarEntryName;
    private final ClassNode classNode;
    private int classVersion;

    public PackageInfoClass(String jarEntryName, ClassNode classNode, int classVersion) {
        this.jarEntryName = Objects.requireNonNull(jarEntryName, "jarEntryName");
        this.classNode = Objects.requireNonNull(classNode, "classNode");
        this.classVersion = classVersion;
    }

    /** Internal name (e.g. {@code com/example/package-info}). */
    public String getInternalName() {
        return classNode.name != null ? classNode.name : internalNameFromJarEntry(jarEntryName);
    }

    private static String internalNameFromJarEntry(String entryName) {
        if (entryName.endsWith(".class")) {
            return entryName.substring(0, entryName.length() - ".class".length());
        }
        return entryName;
    }
}
