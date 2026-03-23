package net.cvs0.bytecode.analysis;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UnusedCodeAnalyzerTest {

    private JarMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new JarMapping("u.jar");
        ProgramClass a = new ProgramClass("u/A");
        a.addMethod(new ProgramMethod("entry", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
        a.addMethod(new ProgramMethod("orphan", "()V", Opcodes.ACC_PRIVATE));
        mapping.addClass(a);
    }

    @Test
    void findUnusedMethodsContainsPrivateNonReferenced() {
        Set<String> unused = UnusedCodeAnalyzer.findUnusedMethods(mapping);
        assertTrue(unused.stream().anyMatch(k -> k.contains("orphan")));
    }

    @Test
    void getMethodComplexityNonNegative() {
        Map<String, Integer> cx = UnusedCodeAnalyzer.getMethodComplexity(mapping);
        assertTrue(cx.values().stream().allMatch(v -> v >= 0));
        assertTrue(cx.containsKey("u/A.entry()V"));
    }

    @Test
    void getLargestMethodsRespectsLimit() {
        List<String> top = UnusedCodeAnalyzer.getLargestMethods(mapping, 1);
        assertEquals(1, top.size());
    }

    @Test
    void findUnusedFieldsAndDeadCodeDoNotThrow() {
        ProgramClass b = new ProgramClass("u/B");
        b.addMethod(new ProgramMethod("m", "()V", 0x0001));
        mapping.addClass(b);
        assertDoesNotThrow(() -> UnusedCodeAnalyzer.findUnusedFields(mapping));
        assertDoesNotThrow(() -> UnusedCodeAnalyzer.findDeadCode(mapping));
    }
}
