package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.io.JarLayout;
import io.github.cvs0.bytecode.transform.patcher.ManifestPatcher;
import io.github.cvs0.bytecode.transform.patcher.ServiceLoaderResourcePatcher;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Manifest and {@code META-INF/services} patching on {@link JarMapping}. */
class JarMappingResourcePatchTest {

    @Test
    void remapServiceLoaderResourcePaths_requiresNonNullMap() {
        JarMapping m = new JarMapping("x.jar");
        assertThrows(NullPointerException.class, () -> m.remapServiceLoaderResourcePaths(null));
    }

    @Test
    void remapManifestMainClassRewritesLaunchClass() throws Exception {
        JarMapping m = new JarMapping("x.jar");
        m.addResource(
                JarLayout.MANIFEST,
                "Manifest-Version: 1.0\nMain-Class: com.example.App\n\n".getBytes());
        m.remapManifestMainClass(Map.of("com/example/App", "obf/a0"));

        byte[] raw = m.getResource(JarLayout.MANIFEST);
        assertNotNull(raw);
        Manifest mf = new Manifest(new java.io.ByteArrayInputStream(raw));
        assertEquals("obf.a0", mf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS));
    }

    @Test
    void manifestPatcherNoOpWhenNoMainClassOrNoRename() {
        assertNull(ManifestPatcher.remapMainClass(null, Map.of("a", "b")));
        assertNull(ManifestPatcher.remapMainClass("Manifest-Version: 1.0\n\n".getBytes(), Map.of("x/y", "z")));
    }

    @Test
    void remapManifestRewritesStartClass() throws Exception {
        JarMapping m = new JarMapping("x.jar");
        m.addResource(
                JarLayout.MANIFEST,
                "Manifest-Version: 1.0\nStart-Class: com.example.Boot\n\n".getBytes());
        m.remapManifestMainClass(Map.of("com/example/Boot", "boot/X"));
        byte[] raw = m.getResource(JarLayout.MANIFEST);
        assertNotNull(raw);
        Manifest mf = new Manifest(new java.io.ByteArrayInputStream(raw));
        assertEquals("boot.X", mf.getMainAttributes().getValue("Start-Class"));
    }

    @Test
    void serviceLoaderResourcePatcherRewritesImplementationLine() {
        byte[] raw = "  com.foo.Impl  \n".getBytes(StandardCharsets.UTF_8);
        byte[] out = ServiceLoaderResourcePatcher.remapImplementations(raw, Map.of("com/foo/Impl", "x/Y"));
        assertEquals("  x.Y  \n", new String(out, StandardCharsets.UTF_8));
    }

    @Test
    void remapServiceLoaderResourcePathsRenamesProviderDescriptorFile() {
        JarMapping m = new JarMapping("x.jar");
        m.addResource("META-INF/services/com.example.Svc", "com.example.Impl\n".getBytes(StandardCharsets.UTF_8));
        m.remapServiceLoaderResourcePaths(Map.of("com/example/Svc", "a/B"));
        assertNull(m.getResource("META-INF/services/com.example.Svc"));
        byte[] moved = m.getResource("META-INF/services/a.B");
        assertNotNull(moved);
        assertEquals("com.example.Impl\n", new String(moved, StandardCharsets.UTF_8));
    }
}
