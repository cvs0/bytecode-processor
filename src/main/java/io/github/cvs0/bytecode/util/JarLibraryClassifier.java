package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Splits JAR classes into application vs embedded library using launch anchors ({@code Main-Class},
 * {@code Start-Class}, modular {@code main}) and package layout. No hardcoded vendor package lists.
 *
 * <p>Heuristic: take the longest common prefix of all anchor <em>packages</em>, then walk up to parent packages while
 * the parent does not add “too many” extra classes (typical sign of jumping into another top-level dependency tree
 * under e.g. {@code io/}). If anchors disagree (no common prefix), every class stays application.</p>
 */
public final class JarLibraryClassifier {

    /**
     * If expanding to the parent package would add more than this fraction of the JAR's classes, stop (parent likely
     * pulls in unrelated libraries).
     */
    public static final double PARENT_EXTRA_FRACTION_THRESHOLD = 0.12;

    private JarLibraryClassifier() {}

    /**
     * Sets {@link ProgramClass#setEmbeddedLibrary(boolean)} on every loaded class. Safe to call multiple times.
     */
    public static void classify(JarMapping mapping) {
        Set<String> allOuterNames = new LinkedHashSet<>();
        for (ProgramClass pc : mapping.getProgramClasses()) {
            allOuterNames.add(outerInternalName(pc.getName()));
        }
        int total = allOuterNames.size();
        Set<String> anchorPackages = collectAnchorPackages(mapping);
        if (anchorPackages.isEmpty()) {
            clearEmbedded(mapping);
            return;
        }
        String lcp = longestCommonPackagePrefix(anchorPackages);
        if (lcp == null || lcp.isEmpty()) {
            clearEmbedded(mapping);
            return;
        }
        String appRoot = expandApplicationRoot(lcp, allOuterNames, total);
        for (ProgramClass pc : mapping.getProgramClasses()) {
            String outer = outerInternalName(pc.getName());
            pc.setEmbeddedLibrary(!isUnderApplicationPackageRoot(outer, appRoot));
        }
    }

    private static void clearEmbedded(JarMapping mapping) {
        for (ProgramClass pc : mapping.getProgramClasses()) {
            pc.setEmbeddedLibrary(false);
        }
    }

    static Set<String> collectAnchorPackages(JarMapping mapping) {
        Set<String> out = new LinkedHashSet<>();
        for (String internal : ManifestLaunchClassParser.launchInternalNames(mapping.getResource(JarLayout.MANIFEST))) {
            out.add(packageOfInternalClass(internal));
        }
        for (ModuleInfoClass mic : mapping.getModuleInfos()) {
            ClassNode cn = mic.getClassNode();
            if (cn != null && cn.module != null && cn.module.mainClass != null) {
                out.add(packageOfInternalClass(cn.module.mainClass));
            }
        }
        out.remove("");
        return out;
    }

    static String packageOfInternalClass(String internalClassName) {
        if (internalClassName == null || internalClassName.isEmpty()) {
            return "";
        }
        String outer = outerInternalName(internalClassName);
        int slash = outer.lastIndexOf('/');
        return slash < 0 ? "" : outer.substring(0, slash);
    }

    static String outerInternalName(String internalName) {
        if (internalName == null) {
            return "";
        }
        int d = internalName.indexOf('$');
        return d < 0 ? internalName : internalName.substring(0, d);
    }

    static boolean isUnderApplicationPackageRoot(String outerInternalName, String appRootPackage) {
        if (appRootPackage == null || appRootPackage.isEmpty()) {
            return true;
        }
        return outerInternalName.startsWith(appRootPackage + '/') || outerInternalName.equals(appRootPackage);
    }

    static String longestCommonPackagePrefix(Collection<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return null;
        }
        List<String[]> parts = new ArrayList<>();
        for (String p : packages) {
            if (p == null || p.isEmpty()) {
                return null;
            }
            parts.add(p.split("/", -1));
        }
        String[] first = parts.get(0);
        int minLen = parts.stream().mapToInt(a -> a.length).min().orElse(0);
        int prefixSegCount = 0;
        outer:
        for (int i = 0; i < minLen; i++) {
            String seg = first[i];
            for (String[] arr : parts) {
                if (!seg.equals(arr[i])) {
                    break outer;
                }
            }
            prefixSegCount = i + 1;
        }
        if (prefixSegCount == 0) {
            return null;
        }
        return String.join("/", Arrays.copyOfRange(first, 0, prefixSegCount));
    }

    static String expandApplicationRoot(String startPackage, Set<String> allOuterClassNames, int totalClasses) {
        String current = startPackage;
        while (true) {
            String parent = parentPackage(current);
            if (parent == null || parent.isEmpty()) {
                break;
            }
            int curCount = countClassesUnderPackageRoot(allOuterClassNames, current);
            int parCount = countClassesUnderPackageRoot(allOuterClassNames, parent);
            int extra = parCount - curCount;
            if (totalClasses > 0 && (double) extra / totalClasses > PARENT_EXTRA_FRACTION_THRESHOLD) {
                break;
            }
            current = parent;
        }
        return current;
    }

    static String parentPackage(String packageSlash) {
        if (packageSlash == null || packageSlash.isEmpty()) {
            return null;
        }
        int i = packageSlash.lastIndexOf('/');
        return i < 0 ? null : packageSlash.substring(0, i);
    }

    static int countClassesUnderPackageRoot(Set<String> outerInternalNames, String rootPackage) {
        int n = 0;
        for (String outer : outerInternalNames) {
            if (isUnderApplicationPackageRoot(outer, rootPackage)) {
                n++;
            }
        }
        return n;
    }
}
