package net.cvs0.bytecode.integration;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ModuleInfoClass;
import net.cvs0.bytecode.clazz.PackageInfoClass;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.transform.ClassTransformer;
import net.cvs0.bytecode.util.JarReader;
import net.cvs0.bytecode.util.JarWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ensures {@link JarWriter} output is readable by {@link JarReader} and class bytes round-trip.
 */
class JarReadWriteRoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    void writeThenReadProgramClassWithClassNode() throws IOException {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = "roundtrip/Hello";
        cn.superName = "java/lang/Object";
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(mn);

        JarMapping out = new JarMapping("source.jar");
        out.addClass(new ProgramClass(cn));
        out.addResource("META-INF/extra.txt", "hello".getBytes());

        File jarFile = tempDir.resolve("out.jar").toFile();
        JarWriter.write(out, jarFile);

        JarMapping in = new JarMapping(jarFile.getAbsolutePath());
        JarReader.read(jarFile, in);

        assertEquals(1, in.getProgramClasses().size());
        ProgramClass loaded = in.getProgramClass("roundtrip/Hello");
        assertNotNull(loaded);
        assertNotNull(loaded.getClassNode());
        assertEquals("roundtrip/Hello", loaded.getClassNode().name);
        assertEquals(1, loaded.getClassNode().methods.size());
        assertArrayEquals("hello".getBytes(), in.getResource("META-INF/extra.txt"));
    }

    @Test
    void fromJarPathRoundTrip() throws IOException {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V21;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = "p/Minimal";
        cn.superName = "java/lang/Object";
        MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(clinit);

        JarMapping first = new JarMapping("a.jar");
        first.addClass(new ProgramClass(cn));
        Path jarPath = tempDir.resolve("minimal.jar");
        JarWriter.write(first, jarPath.toFile());

        JarMapping second = JarMapping.fromJar(jarPath);
        assertNotNull(second.getProgramClass("p/Minimal"));
    }

    @Test
    void readTransformRenameWriteReadPreservesBytecode() throws IOException {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = "rt/Original";
        cn.superName = "java/lang/Object";
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "go", "()V", null, null);
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(mn);

        File jar1 = tempDir.resolve("step1.jar").toFile();
        JarMapping a = new JarMapping("a.jar");
        a.addClass(new ProgramClass(cn));
        JarWriter.write(a, jar1);

        JarMapping b = new JarMapping(jar1.getAbsolutePath());
        JarReader.read(jar1, b);
        ClassTransformer tr = new ClassTransformer(b);
        tr.renameClass("rt/Original", "rt/Renamed");
        tr.applyTransformations();

        File jar2 = tempDir.resolve("step2.jar").toFile();
        JarWriter.write(b, jar2);

        JarMapping c = new JarMapping(jar2.getAbsolutePath());
        JarReader.read(jar2, c);
        assertNull(c.getProgramClass("rt/Original"));
        assertNotNull(c.getProgramClass("rt/Renamed"));
        assertEquals(1, c.getProgramClass("rt/Renamed").getClassNode().methods.size());
    }

    @Test
    void writeThenReadModuleInfoAndPackageInfo() throws IOException {
        ClassNode modCn = new ClassNode();
        modCn.version = Opcodes.V9;
        modCn.access = Opcodes.ACC_MODULE;
        modCn.name = "module-info";
        modCn.module = new ModuleNode("roundtrip.demo", 0, null);

        ClassNode pkgCn = new ClassNode();
        pkgCn.version = Opcodes.V17;
        pkgCn.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        pkgCn.name = "roundtrip/demo/package-info";
        pkgCn.superName = "java/lang/Object";

        JarMapping out = new JarMapping("source.jar");
        out.addModuleInfo("module-info.class", new ModuleInfoClass("module-info.class", modCn, Opcodes.V9));
        out.addPackageInfo(
                "roundtrip/demo/package-info.class",
                new PackageInfoClass("roundtrip/demo/package-info.class", pkgCn, Opcodes.V17));

        File jarFile = tempDir.resolve("modpkg.jar").toFile();
        JarWriter.write(out, jarFile);

        JarMapping in = new JarMapping(jarFile.getAbsolutePath());
        JarReader.read(jarFile, in);

        assertEquals(0, in.getProgramClasses().size());
        assertEquals(1, in.getModuleInfoCount());
        assertEquals(1, in.getPackageInfoCount());
        assertNotNull(in.getModuleInfo("module-info.class"));
        assertNotNull(in.getPackageInfo("roundtrip/demo/package-info.class"));
        assertEquals("module-info", in.getModuleInfo("module-info.class").getClassNode().name);
        assertEquals(
                "roundtrip/demo/package-info",
                in.getPackageInfo("roundtrip/demo/package-info.class").getClassNode().name);
    }
}
