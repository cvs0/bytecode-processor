package net.cvs0.bytecode.plugin.impl;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptimizationPluginTest {

    @Test
    void bipushSmallIntFoldsToIconst() {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = "opt/Const";
        cn.superName = "java/lang/Object";
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        mn.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 3));
        mn.instructions.add(new InsnNode(Opcodes.POP));
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(mn);

        JarMapping m = new JarMapping("o.jar");
        m.addClass(new ProgramClass(cn));

        OptimizationPlugin p = new OptimizationPlugin();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(OptimizationPlugin.CFG_REMOVE_NOPS, false);
        cfg.put(OptimizationPlugin.CFG_OPTIMIZE_CONSTANTS, true);
        p.configure(cfg);
        p.process(m);

        MethodNode out = m.getProgramClass("opt/Const").getClassNode().methods.getFirst();
        AbstractInsnNode first = out.instructions.getFirst();
        assertEquals(Opcodes.ICONST_3, first.getOpcode());
    }

    @Test
    void sipushNegativeOneFoldsToIconstM1() {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = "opt/Neg";
        cn.superName = "java/lang/Object";
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        mn.instructions.add(new IntInsnNode(Opcodes.SIPUSH, -1));
        mn.instructions.add(new InsnNode(Opcodes.POP));
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(mn);

        JarMapping m = new JarMapping("o.jar");
        m.addClass(new ProgramClass(cn));

        OptimizationPlugin p = new OptimizationPlugin();
        p.configure(Map.of(
                OptimizationPlugin.CFG_REMOVE_NOPS, false,
                OptimizationPlugin.CFG_OPTIMIZE_CONSTANTS, true));
        p.process(m);

        MethodNode out = m.getProgramClass("opt/Neg").getClassNode().methods.getFirst();
        assertEquals(Opcodes.ICONST_M1, out.instructions.getFirst().getOpcode());
    }

    @Test
    void removeUnusedMethod_dropsUnreachableMethod() {
        MethodNode main = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        main.instructions.add(new InsnNode(Opcodes.RETURN));
        MethodNode orphan = new MethodNode(Opcodes.ACC_PUBLIC, "orphan", "()V", null, null);
        orphan.instructions.add(new InsnNode(Opcodes.RETURN));

        ProgramClass app = new ProgramClass("u/App");
        app.setSuperName("java/lang/Object");
        app.addMethod(new ProgramMethod(main));
        app.addMethod(new ProgramMethod(orphan));

        JarMapping m = new JarMapping("o.jar");
        m.addClass(app);

        OptimizationPlugin p = new OptimizationPlugin();
        p.configure(Map.of(
                OptimizationPlugin.CFG_REMOVE_UNUSED_METHODS, true,
                OptimizationPlugin.CFG_REMOVE_NOPS, false,
                OptimizationPlugin.CFG_OPTIMIZE_CONSTANTS, false));
        p.process(m);

        ProgramClass after = m.getProgramClass("u/App");
        assertNotNull(after.getMethod("main", "([Ljava/lang/String;)V"));
        assertNull(after.getMethod("orphan", "()V"), "orphan should be removed as unused");
    }

    @Test
    void removeUnusedField_dropsUnreferencedField() {
        MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.instructions.add(new InsnNode(Opcodes.RETURN));
        MethodNode main = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        main.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "u/Fields", "used", "I"));
        main.instructions.add(new InsnNode(Opcodes.POP));
        main.instructions.add(new InsnNode(Opcodes.RETURN));

        ProgramClass pc = new ProgramClass("u/Fields");
        pc.setSuperName("java/lang/Object");
        pc.addField(new ProgramField("used", "I", Opcodes.ACC_PUBLIC));
        pc.addField(new ProgramField("dead", "I", Opcodes.ACC_PUBLIC));
        pc.addMethod(new ProgramMethod(clinit));
        pc.addMethod(new ProgramMethod(main));

        JarMapping m = new JarMapping("o.jar");
        m.addClass(pc);

        OptimizationPlugin p = new OptimizationPlugin();
        p.configure(Map.of(
                OptimizationPlugin.CFG_REMOVE_UNUSED_FIELDS, true,
                OptimizationPlugin.CFG_REMOVE_NOPS, false,
                OptimizationPlugin.CFG_OPTIMIZE_CONSTANTS, false));
        p.process(m);

        ProgramClass afterFields = m.getProgramClass("u/Fields");
        assertNotNull(afterFields.getField("used"));
        assertNull(afterFields.getField("dead"), "dead field never referenced in bytecode");
    }

    @Test
    void metadataAndPriority() {
        OptimizationPlugin p = new OptimizationPlugin();
        assertEquals("OptimizationPlugin", p.getName());
        assertEquals("2.0.0", p.getVersion());
        assertEquals(50, p.getPriority());
    }
}
