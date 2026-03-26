package io.github.cvs0.bytecode.io;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JarWriterManifestTest {

    @Test
    void writeDoesNotFailWhenMappingIncludesManifestResource(@TempDir Path temp) throws IOException {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = "a/B";
        cn.superName = "java/lang/Object";
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(mn);

        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass(cn));
        m.addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n\n".getBytes());
        File out = temp.resolve("out.jar").toFile();
        assertDoesNotThrow(() -> JarWriter.write(m, out));
    }
}
