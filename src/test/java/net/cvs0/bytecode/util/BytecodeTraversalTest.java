package net.cvs0.bytecode.util;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BytecodeTraversalTest {

    @Test
    void visitsMethodsOnClassNodeWhenProgramMapEmpty() {
        ClassNode cn = new ClassNode();
        cn.name = "only/Node";
        cn.version = Opcodes.V17;
        cn.superName = "java/lang/Object";
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(mn);

        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass(cn));

        AtomicInteger count = new AtomicInteger();
        BytecodeTraversal.forEachMethod(m, (c, pm) -> count.incrementAndGet());
        assertEquals(1, count.get());
    }
}
