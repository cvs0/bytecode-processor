package io.github.cvs0.bytecode.plugin.impl;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptimizationPluginSmokeTest {

    @Test
    void removeNopsStripsOpcodeZero() {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = "opt/NopClass";
        cn.superName = "java/lang/Object";
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        mn.instructions.add(new InsnNode(Opcodes.NOP));
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(mn);

        JarMapping m = new JarMapping("o.jar");
        m.addClass(new ProgramClass(cn));

        OptimizationPlugin p = new OptimizationPlugin();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(OptimizationPlugin.CFG_REMOVE_NOPS, true);
        cfg.put(OptimizationPlugin.CFG_OPTIMIZE_CONSTANTS, false);
        p.configure(cfg);
        p.process(m);

        MethodNode out = m.getProgramClass("opt/NopClass").getClassNode().methods.getFirst();
        boolean hasNop = false;
        boolean hasReturn = false;
        for (AbstractInsnNode i = out.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i.getOpcode() == Opcodes.NOP) {
                hasNop = true;
            }
            if (i.getOpcode() == Opcodes.RETURN) {
                hasReturn = true;
            }
        }
        assertFalse(hasNop, "NOP should be removed");
        assertTrue(hasReturn, "RETURN must remain (InsnList must not be wiped)");
    }
}
