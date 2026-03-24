package io.github.cvs0.bytecode.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Reads {@code Main-Class} and {@code Start-Class} from raw manifest bytes and returns internal class names.
 */
public final class ManifestLaunchClassParser {

    private ManifestLaunchClassParser() {}

    public static Set<String> launchInternalNames(byte[] manifestBytes) {
        if (manifestBytes == null || manifestBytes.length == 0) {
            return Set.of();
        }
        try {
            Manifest m = new Manifest(new ByteArrayInputStream(manifestBytes));
            Attributes a = m.getMainAttributes();
            Set<String> out = new HashSet<>();
            add(out, a.getValue(Attributes.Name.MAIN_CLASS));
            add(out, a.getValue("Start-Class"));
            return out.isEmpty() ? Set.of() : Collections.unmodifiableSet(out);
        } catch (IOException e) {
            return Set.of();
        }
    }

    private static void add(Set<String> out, String binary) {
        if (binary == null || binary.isBlank()) {
            return;
        }
        out.add(BytecodeNames.binaryToInternal(binary.trim()));
    }
}
