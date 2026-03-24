package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JarMappingClasspathMergeTest {

    @Test
    void mergeClasspathJarAddsDependencyClasses(@TempDir Path temp) throws IOException {
        ClassNode depCn = new ClassNode();
        depCn.version = Opcodes.V17;
        depCn.access = Opcodes.ACC_PUBLIC;
        depCn.name = "dep/Lib";
        depCn.superName = "java/lang/Object";
        MethodNode depM = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        depM.instructions.add(new InsnNode(Opcodes.RETURN));
        depCn.methods.add(depM);

        ClassNode appCn = new ClassNode();
        appCn.version = Opcodes.V17;
        appCn.access = Opcodes.ACC_PUBLIC;
        appCn.name = "app/App";
        appCn.superName = "java/lang/Object";
        MethodNode appM = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        appM.instructions.add(new InsnNode(Opcodes.RETURN));
        appCn.methods.add(appM);

        Path depJar = temp.resolve("dep.jar");
        Path appJar = temp.resolve("app.jar");
        JarMapping depMap = new JarMapping("dep.jar");
        depMap.addClass(new ProgramClass(depCn));
        JarWriter.write(depMap, depJar.toFile());
        JarMapping appMap = new JarMapping("app.jar");
        appMap.addClass(new ProgramClass(appCn));
        JarWriter.write(appMap, appJar.toFile());

        JarMapping merged = JarMapping.fromJar(appJar);
        merged.mergeClasspathJar(depJar);

        assertEquals(1, merged.getProgramClasses().size());
        assertEquals(1, merged.getMergedEntryCount());
        assertNotNull(merged.getMergedEntry("dep/Lib.class"));

        Path out = temp.resolve("out.jar");
        JarWriter.write(merged, out.toFile());
        try (JarFile jf = new JarFile(out.toFile())) {
            assertNotNull(jf.getEntry("app/App.class"));
            assertNotNull(jf.getEntry("dep/Lib.class"));
        }
    }

    @Test
    void mergeClasspathJar_programClassWinsOverSameNameInDependency(@TempDir Path temp) throws IOException {
        ClassNode depCn = new ClassNode();
        depCn.version = Opcodes.V17;
        depCn.access = Opcodes.ACC_PUBLIC;
        depCn.name = "app/App";
        depCn.superName = "java/lang/Object";
        MethodNode depInit = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        depInit.instructions.add(new InsnNode(Opcodes.RETURN));
        depCn.methods.add(depInit);

        ClassNode appCn = new ClassNode();
        appCn.version = Opcodes.V17;
        appCn.access = Opcodes.ACC_PUBLIC;
        appCn.name = "app/App";
        appCn.superName = "java/lang/Object";
        MethodNode appInit = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        appInit.instructions.add(new InsnNode(Opcodes.RETURN));
        appCn.methods.add(appInit);

        Path depJar = temp.resolve("dep.jar");
        Path appJar = temp.resolve("app.jar");
        JarMapping depMap = new JarMapping("dep.jar");
        depMap.addClass(new ProgramClass(depCn));
        JarWriter.write(depMap, depJar.toFile());
        JarMapping appMap = new JarMapping("app.jar");
        appMap.addClass(new ProgramClass(appCn));
        JarWriter.write(appMap, appJar.toFile());

        JarMapping merged = JarMapping.fromJar(appJar);
        merged.mergeClasspathJar(depJar);

        assertEquals(1, merged.getProgramClasses().size());
        assertEquals(0, merged.getMergedEntryCount(), "dependency class with same internal name as program class must be skipped");
    }

    @Test
    void mergeClasspathJar_twiceSameFile_doesNotDuplicateMergedEntries(@TempDir Path temp) throws IOException {
        ClassNode depCn = new ClassNode();
        depCn.version = Opcodes.V17;
        depCn.access = Opcodes.ACC_PUBLIC;
        depCn.name = "dep/Lib";
        depCn.superName = "java/lang/Object";
        MethodNode depM = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        depM.instructions.add(new InsnNode(Opcodes.RETURN));
        depCn.methods.add(depM);

        ClassNode appCn = new ClassNode();
        appCn.version = Opcodes.V17;
        appCn.access = Opcodes.ACC_PUBLIC;
        appCn.name = "app/App";
        appCn.superName = "java/lang/Object";
        MethodNode appM = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        appM.instructions.add(new InsnNode(Opcodes.RETURN));
        appCn.methods.add(appM);

        Path depJar = temp.resolve("dep.jar");
        Path appJar = temp.resolve("app.jar");
        JarMapping depMap = new JarMapping("dep.jar");
        depMap.addClass(new ProgramClass(depCn));
        JarWriter.write(depMap, depJar.toFile());
        JarMapping appMap = new JarMapping("app.jar");
        appMap.addClass(new ProgramClass(appCn));
        JarWriter.write(appMap, appJar.toFile());

        JarMapping merged = JarMapping.fromJar(appJar);
        merged.mergeClasspathJar(depJar);
        int n = merged.getMergedEntryCount();
        merged.mergeClasspathJar(depJar);
        assertEquals(n, merged.getMergedEntryCount());
    }

    @Test
    void remapServiceLoaderResourcePaths_requiresNonNullMap() {
        JarMapping m = new JarMapping("x.jar");
        assertThrows(NullPointerException.class, () -> m.remapServiceLoaderResourcePaths(null));
    }

    @Test
    void remapManifestMainClassRewritesLaunchClass() throws Exception {
        JarMapping m = new JarMapping("x.jar");
        m.addResource(
                JarLayout.MANIFEST,
                "Manifest-Version: 1.0\nMain-Class: com.example.App\n\n".getBytes());
        m.remapManifestMainClass(Map.of("com/example/App", "obf/a0"));

        byte[] raw = m.getResource(JarLayout.MANIFEST);
        assertNotNull(raw);
        Manifest mf = new Manifest(new java.io.ByteArrayInputStream(raw));
        assertEquals("obf.a0", mf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS));
    }

    @Test
    void manifestPatcherNoOpWhenNoMainClassOrNoRename() {
        assertNull(ManifestPatcher.remapMainClass(null, Map.of("a", "b")));
        assertNull(ManifestPatcher.remapMainClass("Manifest-Version: 1.0\n\n".getBytes(), Map.of("x/y", "z")));
    }

    @Test
    void remapManifestRewritesStartClass() throws Exception {
        JarMapping m = new JarMapping("x.jar");
        m.addResource(
                JarLayout.MANIFEST,
                "Manifest-Version: 1.0\nStart-Class: com.example.Boot\n\n".getBytes());
        m.remapManifestMainClass(Map.of("com/example/Boot", "boot/X"));
        byte[] raw = m.getResource(JarLayout.MANIFEST);
        assertNotNull(raw);
        Manifest mf = new Manifest(new java.io.ByteArrayInputStream(raw));
        assertEquals("boot.X", mf.getMainAttributes().getValue("Start-Class"));
    }

    @Test
    void serviceLoaderResourcePatcherRewritesImplementationLine() {
        byte[] raw = "  com.foo.Impl  \n".getBytes(StandardCharsets.UTF_8);
        byte[] out = ServiceLoaderResourcePatcher.remapImplementations(raw, Map.of("com/foo/Impl", "x/Y"));
        assertEquals("  x.Y  \n", new String(out, StandardCharsets.UTF_8));
    }

    @Test
    void remapServiceLoaderResourcePathsRenamesProviderDescriptorFile() {
        JarMapping m = new JarMapping("x.jar");
        m.addResource("META-INF/services/com.example.Svc", "com.example.Impl\n".getBytes(StandardCharsets.UTF_8));
        m.remapServiceLoaderResourcePaths(Map.of("com/example/Svc", "a/B"));
        assertNull(m.getResource("META-INF/services/com.example.Svc"));
        byte[] moved = m.getResource("META-INF/services/a.B");
        assertNotNull(moved);
        assertEquals("com.example.Impl\n", new String(moved, StandardCharsets.UTF_8));
    }
}
