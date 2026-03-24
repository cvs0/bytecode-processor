package io.github.cvs0.bytecode.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
class ManifestPatcherTest {

    @Test
    void nullManifest_returnsNull() {
        assertNull(ManifestPatcher.remapLaunchClassAttributes(null, Map.of("a", "b")));
    }

    @Test
    void emptyManifest_returnsNull() {
        assertNull(ManifestPatcher.remapLaunchClassAttributes(new byte[0], Map.of("a", "b")));
    }

    @Test
    void nullMap_returnsNull() {
        assertNull(ManifestPatcher.remapLaunchClassAttributes("Manifest-Version: 1.0\n\n".getBytes(), null));
    }

    @Test
    void emptyMap_returnsNull() {
        assertNull(
                ManifestPatcher.remapLaunchClassAttributes(
                        "Manifest-Version: 1.0\nMain-Class: a.B\n\n".getBytes(), Map.of()));
    }

    @Test
    void identityRename_returnsNull() {
        assertNull(
                ManifestPatcher.remapLaunchClassAttributes(
                        "Manifest-Version: 1.0\nMain-Class: same.Name\n\n".getBytes(),
                        Map.of("same/Name", "same/Name")));
    }

    @Test
    void premainClassRemapped() throws Exception {
        byte[] raw = "Manifest-Version: 1.0\nPremain-Class: agent.Old\n\n".getBytes(StandardCharsets.UTF_8);
        byte[] out = ManifestPatcher.remapLaunchClassAttributes(raw, Map.of("agent/Old", "agent/New"));
        assertNotNull(out);
        Manifest mf = new Manifest(new ByteArrayInputStream(out));
        assertEquals("agent.New", mf.getMainAttributes().getValue("Premain-Class"));
    }

    @Test
    void agentClassRemapped() throws Exception {
        byte[] raw = "Manifest-Version: 1.0\nAgent-Class: a.Probe\n\n".getBytes(StandardCharsets.UTF_8);
        byte[] out = ManifestPatcher.remapLaunchClassAttributes(raw, Map.of("a/Probe", "a/P"));
        assertNotNull(out);
        Manifest mf = new Manifest(new ByteArrayInputStream(out));
        assertEquals("a.P", mf.getMainAttributes().getValue("Agent-Class"));
    }

    @Test
    void severalLaunchAttributesUpdatedInOnePass() throws Exception {
        String in =
                """
                        Manifest-Version: 1.0
                        Main-Class: boot.Launcher
                        Start-Class: app.Entry
                        Custom-Key: keep-me

                        """;
        byte[] out =
                ManifestPatcher.remapLaunchClassAttributes(
                        in.getBytes(StandardCharsets.UTF_8),
                        Map.of("boot/Launcher", "b/L", "app/Entry", "a/E"));
        assertNotNull(out);
        Manifest mf = new Manifest(new ByteArrayInputStream(out));
        Attributes a = mf.getMainAttributes();
        assertEquals("b.L", a.getValue(Attributes.Name.MAIN_CLASS));
        assertEquals("a.E", a.getValue("Start-Class"));
        assertEquals("keep-me", a.getValue("Custom-Key"));
    }

}
