package net.cvs0.bytecode.transform;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescriptorRemapperTest {

    @Test
    void remapFieldDescriptor() {
        assertEquals("Lcom/new/Foo;", DescriptorRemapper.remap("Lcom/old/Foo;", Map.of("com/old/Foo", "com/new/Foo")));
    }

    @Test
    void remapMethodDescriptor() {
        assertEquals("(Lcom/new/A;)Lcom/new/B;",
                DescriptorRemapper.remap("(Lcom/old/A;)Lcom/old/B;", Map.of("com/old/A", "com/new/A", "com/old/B", "com/new/B")));
    }

    @Test
    void longestKeyWins() {
        Map<String, String> m = Map.of("com/a", "com/x", "com/a$B", "com/y");
        assertEquals("Lcom/y;", DescriptorRemapper.remap("Lcom/a$B;", m));
    }
}
