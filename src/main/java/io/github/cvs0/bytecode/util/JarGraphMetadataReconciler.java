package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleNode;
import io.github.cvs0.bytecode.transform.transformer.ClassTransformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Aligns {@code package-info} and {@code module-info} metadata with the actual set of {@link ProgramClass} packages
 * after renames (e.g. obfuscation). Drops orphan {@code package-info} entries and prunes {@code exports}, {@code opens},
 * and {@code packages} clauses that no longer contain any program type.
 *
 * <p>Invoked automatically at the end of {@link ClassTransformer#applyTransformations()}.
 * May also be called directly if you mutate a {@link JarMapping} without the transformer.</p>
 *
 * @see BytecodeNames#internalNameToPackage(String)
 */
public final class JarGraphMetadataReconciler {

    private JarGraphMetadataReconciler() {}

    /**
     * @param mapping mapping whose program classes are authoritative
     */
    public static void reconcile(JarMapping mapping) {
        Objects.requireNonNull(mapping, "mapping");
        Set<String> programPackages = collectProgramPackages(mapping);
        removeOrphanPackageInfos(mapping, programPackages);
        pruneModuleDescriptors(mapping, programPackages);
    }

    private static Set<String> collectProgramPackages(JarMapping mapping) {
        Set<String> pkgs = new HashSet<>();
        for (ProgramClass pc : mapping.getProgramClasses()) {
            pkgs.add(BytecodeNames.internalNameToPackage(pc.getName()));
        }
        return pkgs;
    }

    private static void removeOrphanPackageInfos(JarMapping mapping, Set<String> programPackages) {
        for (String jarPath : new ArrayList<>(mapping.getPackageInfoEntryNames())) {
            PackageInfoClass pi = mapping.getPackageInfo(jarPath);
            if (pi == null) {
                continue;
            }
            String pkg = BytecodeNames.internalNameToPackage(pi.getInternalName());
            if (!programPackages.contains(pkg)) {
                mapping.removePackageInfo(jarPath);
            }
        }
    }

    private static void pruneModuleDescriptors(JarMapping mapping, Set<String> programPackages) {
        for (ModuleInfoClass mic : mapping.getModuleInfos()) {
            ClassNode cn = mic.getClassNode();
            if (cn == null || cn.module == null) {
                continue;
            }
            ModuleNode mod = cn.module;
            if (mod.exports != null) {
                mod.exports.removeIf(e -> e == null || !programPackages.contains(nullToEmpty(e.packaze)));
            }
            if (mod.opens != null) {
                mod.opens.removeIf(o -> o == null || !programPackages.contains(nullToEmpty(o.packaze)));
            }
            if (mod.packages != null) {
                mod.packages.removeIf(p -> p == null || !programPackages.contains(nullToEmpty(p)));
            }
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
