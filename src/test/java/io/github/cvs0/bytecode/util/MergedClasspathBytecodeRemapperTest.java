package io.github.cvs0.bytecode.util;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MergedClasspathBytecodeRemapperTest {

    @Test
    void noMappings_returnsSameBytes() {
        byte[] b = new byte[] {0, -1, 2};
        assertSame(b, MergedClasspathBytecodeRemapper.remap(b, Map.of(), Map.of(), Map.of()));
    }

    @Test
    void classRename_updatesMethodInsnOwnerAndTypeInsn() {
        ClassNode lib = new ClassNode();
        lib.version = Opcodes.V21;
        lib.access = Opcodes.ACC_PUBLIC;
        lib.name = "lib/Lib";
        lib.superName = "java/lang/Object";
        MethodNode touch = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "touch", "()V", null, null);
        InsnList ins = new InsnList();
        ins.add(new TypeInsnNode(Opcodes.NEW, "app/App"));
        ins.add(new InsnNode(Opcodes.DUP));
        ins.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "app/App", "<init>", "()V", false));
        ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "app/App", "work", "()V", false));
        ins.add(new InsnNode(Opcodes.RETURN));
        touch.instructions = ins;
        lib.methods.add(touch);

        byte[] raw = JarWriter.classBytesFromNode(lib);
        byte[] out = MergedClasspathBytecodeRemapper.remap(raw, Map.of("app/App", "x/Z"), Map.of(), Map.of());

        ClassNode outLib = new ClassNode();
        new ClassReader(out).accept(outLib, 0);
        MethodNode m = outLib.methods.stream().filter(n -> "touch".equals(n.name)).findFirst().orElseThrow();
        MethodInsnNode virt = null;
        MethodInsnNode spec = null;
        for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min) {
                if ("<init>".equals(min.name)) {
                    spec = min;
                } else {
                    virt = min;
                }
            }
        }
        assertNotNull(spec);
        assertNotNull(virt);
        assertEquals("x/Z", spec.owner);
        assertEquals("x/Z", virt.owner);
        TypeInsnNode tnew = null;
        for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof TypeInsnNode tn) {
                tnew = tn;
                break;
            }
        }
        assertNotNull(tnew);
        assertEquals("x/Z", tnew.desc);
    }

    @Test
    void fieldRename_updatesGetFieldName() {
        ClassNode lib = new ClassNode();
        lib.version = Opcodes.V21;
        lib.access = Opcodes.ACC_PUBLIC;
        lib.name = "lib/Lib";
        lib.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "read", "(Lapp/App;)V", null, null);
        InsnList ins = new InsnList();
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new FieldInsnNode(Opcodes.GETFIELD, "app/App", "counter", "I"));
        ins.add(new InsnNode(Opcodes.POP));
        ins.add(new InsnNode(Opcodes.RETURN));
        m.instructions = ins;
        lib.methods.add(m);

        byte[] raw = JarWriter.classBytesFromNode(lib);
        byte[] out = MergedClasspathBytecodeRemapper.remap(
                raw,
                Map.of(),
                Map.of("app/App.counter", "f0"),
                Map.of());

        ClassNode outLib = new ClassNode();
        new ClassReader(out).accept(outLib, 0);
        MethodNode outM = outLib.methods.getFirst();
        FieldInsnNode gf = null;
        for (var insn = outM.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode fin) {
                gf = fin;
                break;
            }
        }
        assertNotNull(gf);
        assertEquals("f0", gf.name);
    }

    @Test
    void methodRename_updatesInvokeVirtualName() {
        ClassNode lib = new ClassNode();
        lib.version = Opcodes.V21;
        lib.access = Opcodes.ACC_PUBLIC;
        lib.name = "lib/Lib";
        lib.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "call", "()V", null, null);
        InsnList ins = new InsnList();
        ins.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "app/App", "factory", "()Lapp/App;", false));
        ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "app/App", "work", "()V", false));
        ins.add(new InsnNode(Opcodes.RETURN));
        m.instructions = ins;
        lib.methods.add(m);

        byte[] raw = JarWriter.classBytesFromNode(lib);
        byte[] out =
                MergedClasspathBytecodeRemapper.remap(
                        raw,
                        Map.of(),
                        Map.of(),
                        Map.of("app/App.work()V", "m0"));

        ClassNode outLib = new ClassNode();
        new ClassReader(out).accept(outLib, 0);
        MethodNode outM = outLib.methods.getFirst();
        MethodInsnNode virt = null;
        for (var insn = outM.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min && "app/App".equals(min.owner) && "()V".equals(min.desc) && !"<init>".equals(min.name)) {
                virt = min;
                break;
            }
        }
        assertNotNull(virt);
        assertEquals("m0", virt.name);
    }

    @Test
    void classAndMethodRename_bothApplied() {
        ClassNode lib = new ClassNode();
        lib.version = Opcodes.V21;
        lib.access = Opcodes.ACC_PUBLIC;
        lib.name = "lib/Lib";
        lib.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "go", "()V", null, null);
        InsnList ins = new InsnList();
        ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "app/App", "run", "()V", false));
        ins.add(new InsnNode(Opcodes.RETURN));
        m.instructions = ins;
        lib.methods.add(m);

        byte[] raw = JarWriter.classBytesFromNode(lib);
        byte[] out =
                MergedClasspathBytecodeRemapper.remap(
                        raw,
                        Map.of("app/App", "z/Z"),
                        Map.of(),
                        Map.of("app/App.run()V", "r0"));

        ClassNode outLib = new ClassNode();
        new ClassReader(out).accept(outLib, 0);
        MethodInsnNode virt = null;
        for (var insn = outLib.methods.getFirst().instructions.getFirst();
                insn != null;
                insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min && "()V".equals(min.desc) && !"<init>".equals(min.name)) {
                virt = min;
                break;
            }
        }
        assertNotNull(virt);
        assertEquals("z/Z", virt.owner);
        assertEquals("r0", virt.name);
    }
}
