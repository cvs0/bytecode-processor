package io.github.cvs0.bytecode.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BytecodeCliTest {

    @Test
    void normalizeInternalNameForDependencyQuery_acceptsDotsAndSlashes() {
        assertEquals("java/lang/String", BytecodeCli.normalizeInternalNameForDependencyQuery("java.lang.String"));
        assertEquals("java/lang/String", BytecodeCli.normalizeInternalNameForDependencyQuery("java/lang/String"));
        assertEquals("com/foo/Bar", BytecodeCli.normalizeInternalNameForDependencyQuery("  com.foo.Bar  "));
    }
}
