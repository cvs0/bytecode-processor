package net.cvs0.bytecode.transform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rewrites JVM descriptors and signatures when internal class names change.
 * Replaces object types as {@code Linternal/name;} segments (longest keys first to avoid partial matches).
 */
public final class DescriptorRemapper {

    private DescriptorRemapper() {
    }

    /**
     * Remaps every {@code Lold;} object reference in a field or method descriptor or generic signature.
     *
     * @param descriptorOrSignature a field descriptor, method descriptor, or JVM signature string
     * @param classNameMappings     internal name → internal name (e.g. {@code com/foo/A} → {@code com/foo/B})
     * @return the rewritten string, or the original if null or empty mappings
     */
    public static String remap(String descriptorOrSignature, Map<String, String> classNameMappings) {
        if (descriptorOrSignature == null || descriptorOrSignature.isEmpty() || classNameMappings.isEmpty()) {
            return descriptorOrSignature;
        }
        List<Map.Entry<String, String>> entries = new ArrayList<>(classNameMappings.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());
        String result = descriptorOrSignature;
        for (Map.Entry<String, String> e : entries) {
            String token = "L" + e.getKey() + ";";
            String replacement = "L" + e.getValue() + ";";
            result = result.replace(token, replacement);
        }
        return result;
    }
}
