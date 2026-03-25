package io.github.cvs0.bytecode.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single place for JVM internal vs binary naming and related path rules. Used by the CLI, dependency analysis,
 * {@link io.github.cvs0.bytecode.util.JarGraphMetadataReconciler}, and anywhere else internal names appear.
 */
public final class BytecodeNames {

    private BytecodeNames() {
    }

    /**
     * All package prefixes (dot-delimited) exported or contained in boot-layer modules.
     * Built once on first access from {@link ModuleLayer#boot()}.
     */
    private static volatile Set<String> bootPackages;


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
     * Returns {@code true} when the internal name belongs to a package provided by the JVM boot layer
     * ({@code java.*}, {@code jdk.*}, {@code sun.*}, etc.). These types resolve from the bootstrap/platform
     * class loaders and must never be renamed.
     *
     * <p>Detection uses {@link ModuleLayer#boot()} — every package exported or contained by a boot module
     * is collected on first call, so the check is accurate for whatever JDK is running.</p>
     */
    public static boolean isJvmRuntimeType(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return false;
        }
        String binaryPkg = internalNameToPackage(internalName).replace('/', '.');
        if (binaryPkg.isEmpty()) {
            return false;
        }
        return getBootPackages().contains(binaryPkg);
    }

    private static Set<String> getBootPackages() {
        Set<String> cached = bootPackages;
        if (cached != null) {
            return cached;
        }
        Set<String> pkgs = ConcurrentHashMap.newKeySet();
        for (Module m : ModuleLayer.boot().modules()) {
            pkgs.addAll(m.getPackages());
        }
        bootPackages = pkgs;
        return pkgs;
    }

    /**
     * Returns {@code true} if the type belongs to the JVM runtime and must never be renamed.
     * Third-party library detection is handled at a higher level by
     * {@link io.github.cvs0.bytecode.clazz.ProgramClass#isApplicationClass()} (set by the {@link JarReader}).
     */
    public static boolean isUnsafeToRename(String internalName) {
        return isJvmRuntimeType(internalName);
    }
}
