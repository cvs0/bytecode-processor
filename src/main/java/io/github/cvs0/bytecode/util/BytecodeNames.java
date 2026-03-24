package io.github.cvs0.bytecode.util;

/**
 * Conventions for JVM internal names vs dotted/binary names and JAR entry paths.
 */
public final class BytecodeNames {

    private BytecodeNames() {
    }

    /** {@code com.foo.Bar} → {@code com/foo/Bar} */
    public static String binaryToInternal(String binaryClassName) {
        if (binaryClassName == null) {
            return null;
        }
        return binaryClassName.replace('.', '/');
    }

    /** {@code com/foo/Bar} → {@code com.foo.Bar} */
    public static String internalToBinary(String internalName) {
        if (internalName == null) {
            return null;
        }
        return internalName.replace('/', '.');
    }

    /** Internal name → JAR entry path for a class file. */
    public static String internalToClassFilePath(String internalName) {
        return internalName + ".class";
    }

    /** JAR entry path → internal name, or {@code null} if not a {@code .class} entry. */
    public static String classFilePathToInternal(String entryName) {
        if (entryName == null || !entryName.endsWith(".class")) {
            return null;
        }
        return entryName.substring(0, entryName.length() - ".class".length());
    }
}
