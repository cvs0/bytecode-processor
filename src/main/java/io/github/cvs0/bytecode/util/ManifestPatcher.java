package io.github.cvs0.bytecode.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Rewrites {@code META-INF/MANIFEST.MF} entries when launch-related classes were renamed (e.g. after obfuscation).
 */
public final class ManifestPatcher {

    private static final List<String> LAUNCH_ATTRIBUTES = List.of(
            "Main-Class",
            "Start-Class",
            "Premain-Class",
            "Agent-Class");

    private ManifestPatcher() {}

    /**
     * If {@code Main-Class} maps to a key in {@code internalOldToNew}, returns updated manifest bytes; otherwise {@code null}.
     *
     * @param manifestBytes raw manifest; may be {@code null} or empty
     * @param internalOldToNew old internal name (slashes) → new internal name
     * @return new UTF-8 manifest bytes, or {@code null} if nothing changed
     */
    public static byte[] remapMainClass(byte[] manifestBytes, Map<String, String> internalOldToNew) {
        return remapLaunchClassAttributes(manifestBytes, internalOldToNew);
    }

    /**
     * Updates manifest attributes that hold a single binary class name ({@code Main-Class}, {@code Start-Class}, etc.)
     * when that class was renamed.
     *
     * @return new manifest bytes, or {@code null} if nothing changed
     */
    public static byte[] remapLaunchClassAttributes(byte[] manifestBytes, Map<String, String> internalOldToNew) {
        if (manifestBytes == null
                || manifestBytes.length == 0
                || internalOldToNew == null
                || internalOldToNew.isEmpty()) {
            return null;
        }
        try {
            Manifest mf = new Manifest(new ByteArrayInputStream(manifestBytes));
            Attributes mainAttrs = mf.getMainAttributes();
            boolean changed = false;
            for (String key : LAUNCH_ATTRIBUTES) {
                String mc = mainAttrs.getValue(key);
                if (mc == null || mc.isEmpty()) {
                    continue;
                }
                String internal = mc.replace('.', '/');
                String nw = internalOldToNew.get(internal);
                if (nw == null) {
                    continue;
                }
                String newMain = nw.replace('/', '.');
                if (newMain.equals(mc)) {
                    continue;
                }
                mainAttrs.putValue(key, newMain);
                changed = true;
            }
            if (!changed) {
                return null;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            mf.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Invalid META-INF/MANIFEST.MF in mapping", e);
        }
    }
}
