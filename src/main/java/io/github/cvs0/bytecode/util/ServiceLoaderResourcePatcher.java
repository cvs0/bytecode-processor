package io.github.cvs0.bytecode.util;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Rewrites {@code META-INF/services/*} lines when a service implementation class was renamed.
 *
 * @see io.github.cvs0.bytecode.JarMapping#remapServiceLoaderImplementations(java.util.Map)
 */
public final class ServiceLoaderResourcePatcher {

    private ServiceLoaderResourcePatcher() {}

    /**
     * @param content UTF-8 text (typical for service files)
     * @param internalOldToNew old internal name → new internal name
     * @return updated bytes, or {@code null} if unchanged
     */
    public static byte[] remapImplementations(byte[] content, Map<String, String> internalOldToNew) {
        Objects.requireNonNull(internalOldToNew, "internalOldToNew");
        if (content == null || content.length == 0 || internalOldToNew.isEmpty()) {
            return null;
        }
        String text = new String(content, StandardCharsets.UTF_8);
        String[] lines = text.split("\\R", -1);
        StringBuilder out = new StringBuilder(text.length() + 16);
        boolean changed = false;
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                out.append(line);
                continue;
            }
            String internal = trimmed.replace('.', '/');
            String nw = internalOldToNew.get(internal);
            if (nw == null) {
                out.append(line);
                continue;
            }
            int pos = line.indexOf(trimmed);
            if (pos < 0) {
                out.append(line);
                continue;
            }
            out.append(line, 0, pos).append(nw.replace('/', '.')).append(line.substring(pos + trimmed.length()));
            changed = true;
        }
        return changed ? out.toString().getBytes(StandardCharsets.UTF_8) : null;
    }
}
