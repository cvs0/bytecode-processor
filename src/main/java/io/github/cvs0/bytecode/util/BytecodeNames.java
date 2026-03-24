package io.github.cvs0.bytecode.util;

/**
 * Single place for JVM internal vs binary naming and related path rules. Used by the CLI, dependency analysis,
 * {@link io.github.cvs0.bytecode.util.JarGraphMetadataReconciler}, and anywhere else internal names appear.
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

    /**
     * Containing JVM package in internal form (slashes, e.g. {@code com/foo} for {@code com/foo/Bar}).
     * The default package is {@code ""}.
     */
    public static String internalNameToPackage(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return "";
        }
        int i = internalName.lastIndexOf('/');
        return i < 0 ? "" : internalName.substring(0, i);
    }

    /**
     * Internal names that normally resolve from the JDK / bootstrap loaders. Renaming such a type if it appears as
     * bytecode in a JAR would desync from what the JVM actually loads, so class rewrites skip them.
     */
    public static boolean isJvmRuntimeType(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return false;
        }
        return internalName.startsWith("java/")
                || internalName.startsWith("javax/")
                || internalName.startsWith("jdk/")
                || internalName.startsWith("sun/")
                || internalName.startsWith("com/sun/")
                || internalName.startsWith("org/w3c/")
                || internalName.startsWith("org/xml/");
    }
}
