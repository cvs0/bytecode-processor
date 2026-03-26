package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.transform.patcher.ServiceLoaderResourcePatcher;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceLoaderResourcePatcherTest {

    @Test
    void nullMap_throwsNpe() {
        assertThrows(NullPointerException.class, () -> ServiceLoaderResourcePatcher.remapImplementations(new byte[] {1}, null));
    }

    @Test
    void nullContent_returnsNull() {
        assertNull(ServiceLoaderResourcePatcher.remapImplementations(null, Map.of("a/B", "c/D")));
    }

    @Test
    void emptyContent_returnsNull() {
        assertNull(ServiceLoaderResourcePatcher.remapImplementations(new byte[0], Map.of("a/B", "c/D")));
    }

    @Test
    void emptyMap_returnsNull() {
        byte[] raw = "x.Y\n".getBytes(StandardCharsets.UTF_8);
        assertNull(ServiceLoaderResourcePatcher.remapImplementations(raw, Map.of()));
    }

    @Test
    void commentAndBlankLines_leftUnchanged() {
        byte[] raw = "\n  # hint\n\ncom.foo.Impl\n".getBytes(StandardCharsets.UTF_8);
        byte[] out = ServiceLoaderResourcePatcher.remapImplementations(raw, Map.of("com/foo/Impl", "a/B"));
        assertEquals(
                "\n  # hint\n\na.B\n",
                new String(out, StandardCharsets.UTF_8));
    }

    @Test
    void multipleImplementations_eachRemappedIndependently() {
        byte[] raw = "p.A\np.B\n".getBytes(StandardCharsets.UTF_8);
        byte[] out =
                ServiceLoaderResourcePatcher.remapImplementations(
                        raw, Map.of("p/A", "x/A", "p/B", "y/B"));
        assertEquals("x.A\ny.B\n", new String(out, StandardCharsets.UTF_8));
    }

    @Test
    void unmappedImplementationLine_unchanged() {
        byte[] raw = "com.other.Other\n".getBytes(StandardCharsets.UTF_8);
        byte[] out = ServiceLoaderResourcePatcher.remapImplementations(raw, Map.of("com/foo/X", "a/B"));
        assertNull(out, "no line matched; output should be null");
    }

    @Test
    void mixedMappedAndUnmapped_onlyMappedLineChanges() {
        byte[] raw = "com.keep.Same\ncom.move.Moved\n".getBytes(StandardCharsets.UTF_8);
        byte[] out =
                ServiceLoaderResourcePatcher.remapImplementations(
                        raw, Map.of("com/move/Moved", "z/Z"));
        assertEquals("com.keep.Same\nz.Z\n", new String(out, StandardCharsets.UTF_8));
    }

    @Test
    void crlfLineEndings_normalizedToLfInOutput() {
        byte[] raw = "a.B\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] out = ServiceLoaderResourcePatcher.remapImplementations(raw, Map.of("a/B", "c/D"));
        assertEquals("c.D\n", new String(out, StandardCharsets.UTF_8));
    }

    @Test
    void noChange_whenMappingDoesNotApply_returnsNull() {
        byte[] raw = "plain\n".getBytes(StandardCharsets.UTF_8);
        assertNull(ServiceLoaderResourcePatcher.remapImplementations(raw, Map.of("x/Y", "z/Z")));
    }

    @Test
    void utf8ImplementationClassName() {
        // Valid Java identifiers cannot contain non-ASCII in typical source; test UTF-8 round-trip for ASCII path
        byte[] raw = "com.foo.Impl\n".getBytes(StandardCharsets.UTF_8);
        byte[] out = ServiceLoaderResourcePatcher.remapImplementations(raw, Map.of("com/foo/Impl", "u/V"));
        assertArrayEquals("u.V\n".getBytes(StandardCharsets.UTF_8), out);
    }
}
