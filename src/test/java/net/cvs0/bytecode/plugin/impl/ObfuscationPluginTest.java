package net.cvs0.bytecode.plugin.impl;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObfuscationPluginTest {

    @Test
    void renamesClassInstanceFieldAndMethod_skipsMainStaticFinalAndEntryNames() {
        JarMapping m = new JarMapping("t.jar");
        ProgramClass c = new ProgramClass("app/Demo");
        c.addField(new ProgramField("counter", "I", Opcodes.ACC_PUBLIC));
        c.addField(new ProgramField("C", "I", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL));
        c.addMethod(new ProgramMethod("main", "([Ljava/lang/String;)V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
        c.addMethod(new ProgramMethod("run", "()V", Opcodes.ACC_PUBLIC));
        m.addClass(c);

        ObfuscationPlugin p = new ObfuscationPlugin();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ObfuscationPlugin.CFG_NAME_PREFIX, "t");
        p.configure(cfg);
        p.initialize();
        p.process(m);

        assertNull(m.getProgramClass("app/Demo"));
        ProgramClass obf = m.getProgramClass("t0");
        assertNotNull(obf);
        assertNotNull(obf.getField("C"), "static final field name preserved");
        assertNotNull(obf.getField("t1"), "instance field obfuscated");
        assertNotNull(obf.getMethod("main", "([Ljava/lang/String;)V"));
        assertNotNull(obf.getMethod("t2", "()V"), "run -> t2");
    }

    @Test
    void skipsRenamingClassNamedMain() {
        JarMapping m = new JarMapping("t.jar");
        ProgramClass c = new ProgramClass("com/example/Main");
        c.addMethod(new ProgramMethod("run", "()V", Opcodes.ACC_PUBLIC));
        m.addClass(c);

        ObfuscationPlugin p = new ObfuscationPlugin();
        p.configure(Map.of(ObfuscationPlugin.CFG_NAME_PREFIX, "z"));
        p.initialize();
        p.process(m);

        assertNotNull(m.getProgramClass("com/example/Main"));
    }

    @Test
    void obfuscateClassesFalse_keepsInternalClassName() {
        JarMapping m = new JarMapping("t.jar");
        ProgramClass c = new ProgramClass("app/KeepName");
        c.addMethod(new ProgramMethod("m", "()V", Opcodes.ACC_PUBLIC));
        m.addClass(c);

        ObfuscationPlugin p = new ObfuscationPlugin();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ObfuscationPlugin.CFG_OBFUSCATE_CLASSES, false);
        cfg.put(ObfuscationPlugin.CFG_NAME_PREFIX, "q");
        p.configure(cfg);
        p.initialize();
        p.process(m);

        assertNotNull(m.getProgramClass("app/KeepName"));
        assertNotNull(m.getProgramClass("app/KeepName").getMethod("q0", "()V"), "method still obfuscated");
    }

    @Test
    void metadataAndPriority() {
        ObfuscationPlugin p = new ObfuscationPlugin();
        assertEquals("ObfuscationPlugin", p.getName());
        assertEquals("2.0.0", p.getVersion());
        assertEquals(100, p.getPriority());
    }

    @Test
    void obfuscationRewritesMethodInsnOwnerAndNameAfterRenames() {
        ClassNode callee = new ClassNode();
        callee.version = Opcodes.V17;
        callee.access = Opcodes.ACC_PUBLIC;
        callee.name = "app/ObfCallee";
        callee.superName = "java/lang/Object";
        MethodNode calleeRun = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        calleeRun.instructions.add(new InsnNode(Opcodes.RETURN));
        callee.methods.add(calleeRun);

        ClassNode caller = new ClassNode();
        caller.version = Opcodes.V17;
        caller.access = Opcodes.ACC_PUBLIC;
        caller.name = "app/ObfCaller";
        caller.superName = "java/lang/Object";
        MethodNode main = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        main.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "app/ObfCallee", "run", "()V", false));
        main.instructions.add(new InsnNode(Opcodes.RETURN));
        caller.methods.add(main);

        ProgramClass pcCallee = new ProgramClass(callee);
        pcCallee.addMethod(new ProgramMethod(calleeRun));
        ProgramClass pcCaller = new ProgramClass(caller);
        pcCaller.addMethod(new ProgramMethod(main));

        JarMapping m = new JarMapping("t.jar");
        m.addClass(pcCallee);
        m.addClass(pcCaller);

        ObfuscationPlugin p = new ObfuscationPlugin();
        p.configure(Map.of(ObfuscationPlugin.CFG_NAME_PREFIX, "x"));
        p.initialize();
        p.process(m);

        ProgramClass outCaller = null;
        for (ProgramClass c : m.getProgramClasses()) {
            if (c.getMethod("main", "([Ljava/lang/String;)V") != null) {
                outCaller = c;
                break;
            }
        }
        assertNotNull(outCaller);
        MethodNode mainOut = outCaller.getMethod("main", "([Ljava/lang/String;)V").getMethodNode();
        assertNotNull(mainOut);
        MethodInsnNode invoke = null;
        for (var insn = mainOut.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min) {
                invoke = min;
                break;
            }
        }
        assertNotNull(invoke);

        ProgramClass calleeAfter = m.getProgramClasses().stream()
                .filter(c -> c.getMethod("main", "([Ljava/lang/String;)V") == null)
                .findFirst()
                .orElse(null);
        assertNotNull(calleeAfter);
        assertEquals(1, calleeAfter.getMethods().size());
        String expectedCalleeName = calleeAfter.getName();
        String expectedRun = calleeAfter.getMethods().iterator().next().getName();
        assertEquals(expectedCalleeName, invoke.owner);
        assertEquals(expectedRun, invoke.name);
        assertEquals("()V", invoke.desc);
    }
}
