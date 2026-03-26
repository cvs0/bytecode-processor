package io.github.cvs0.bytecode.transform;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramMethod;
import io.github.cvs0.bytecode.transform.transformer.ClassTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.*;

class ClassTransformerPostTasksTest {

    @Test
    void transformLdcConstantsRewritesIntegerBoxed() {
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        mn.instructions.add(new LdcInsnNode(42));
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        ProgramClass c = new ProgramClass("x/C");
        c.addMethod(new ProgramMethod(mn));

        JarMapping m = new JarMapping("t.jar");
        m.addClass(c);

        ClassTransformer t = new ClassTransformer(m);
        t.transformLdcConstants((cl, me) -> true, v -> v instanceof Integer i && i == 42 ? 99 : v);
        t.applyTransformations();

        LdcInsnNode ldc = (LdcInsnNode) c.getMethod("m", "()V").getMethodNode().instructions.getFirst();
        assertEquals(99, ldc.cst);
    }

    @Test
    void stripDebugStripsLocalVariablesAndMethodParameters() {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = "dbg/D";
        cn.superName = "java/lang/Object";
        cn.sourceFile = "D.java";

        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "run", "(I)V", null, null);
        mn.localVariables = new java.util.ArrayList<>();
        mn.parameters = new java.util.ArrayList<>();
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(mn);

        JarMapping m = new JarMapping("t.jar");
        m.addClass(new ProgramClass(cn));

        ClassTransformer t = new ClassTransformer(m);
        t.stripDebugEverywhere(
                StripDebugMode.SOURCE_FILE,
                StripDebugMode.LINE_NUMBERS,
                StripDebugMode.LOCAL_VARIABLES,
                StripDebugMode.METHOD_PARAMETERS);
        t.applyTransformations();

        MethodNode out = m.getProgramClass("dbg/D").getClassNode().methods.getFirst();
        assertNull(out.localVariables);
        assertNull(out.parameters);
    }
}
