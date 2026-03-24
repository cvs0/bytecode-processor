package io.github.cvs0.bytecode.clazz;

import org.objectweb.asm.tree.ClassNode;

import java.util.Objects;

/**
 * A {@code package-info.class} entry: package-level annotations and {@code package.html} metadata, not a normal program class.
 */
public final class PackageInfoClass {
    private String jarEntryName;
    private final ClassNode classNode;
    private int classVersion;

    public PackageInfoClass(String jarEntryName, ClassNode classNode, int classVersion) {
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

    /** Internal name (e.g. {@code com/example/package-info}). */
    public String getInternalName() {
        return classNode.name != null ? classNode.name : internalNameFromJarEntry(jarEntryName);
    }

    public int getClassVersion() {
        return classVersion;
    }

    public void setClassVersion(int classVersion) {
        this.classVersion = classVersion;
    }

    private static String internalNameFromJarEntry(String entryName) {
        if (entryName.endsWith(".class")) {
            return entryName.substring(0, entryName.length() - ".class".length());
        }
        return entryName;
    }
}
