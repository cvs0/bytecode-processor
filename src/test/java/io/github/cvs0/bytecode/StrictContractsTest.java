package io.github.cvs0.bytecode;

import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramMethod;
import io.github.cvs0.bytecode.plugin.PluginManager;
import io.github.cvs0.bytecode.transform.ClassTransformer;
import io.github.cvs0.bytecode.util.BytecodeTraversal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Null-safety and constructor contracts for core entry points.
 */
class StrictContractsTest {

    @Test
    void jarMappingFromJarRejectsNullString() {
        assertThrows(NullPointerException.class, () -> JarMapping.fromJar((String) null));
    }

    @Test
    void jarMappingFromJarRejectsNullPath() {
        assertThrows(NullPointerException.class, () -> JarMapping.fromJar((java.nio.file.Path) null));
    }

    @Test
    void classTransformerRejectsNullMapping() {
        assertThrows(NullPointerException.class, () -> new ClassTransformer(null));
    }

    @Test
    void pluginManagerProcessRejectsNullMapping() {
        PluginManager pm = new PluginManager();
        assertThrows(NullPointerException.class, () -> pm.processWithPlugins(null));
    }

    @Test
    void bytecodeTraversalRejectsNullMapping() {
        assertThrows(
                NullPointerException.class,
                () -> BytecodeTraversal.forEachMethod(
                        (JarMapping) null,
                        (ProgramClass c, ProgramMethod m) -> { }));
    }

    @Test
    void bytecodeTraversalRejectsNullConsumer() {
        JarMapping m = new JarMapping("x.jar");
        assertThrows(NullPointerException.class, () -> BytecodeTraversal.forEachMethod(m, null));
    }
}
