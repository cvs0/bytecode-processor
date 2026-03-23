package net.cvs0.bytecode.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeNamesTest {

    @Test
    void roundTripBinaryInternal() {
        assertEquals("com/foo/Bar", BytecodeNames.binaryToInternal("com.foo.Bar"));
        assertEquals("com.foo.Bar", BytecodeNames.internalToBinary("com/foo/Bar"));
    }

    @Test
    void classFilePaths() {
        assertEquals("a/b/C.class", BytecodeNames.internalToClassFilePath("a/b/C"));
        assertEquals("m/X", BytecodeNames.classFilePathToInternal("m/X.class"));
        assertNull(BytecodeNames.classFilePathToInternal("readme.txt"));
    }
}
