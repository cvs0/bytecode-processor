package io.github.cvs0.bytecode.transform;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramMethod;
import io.github.cvs0.bytecode.transform.transformer.ClassTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassTransformerExtendedTest {

    @Test
    void renamePackageMovesInternalHierarchy() {
        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass("com/old/A"));
        m.addClass(new ProgramClass("com/old/nested/B"));
        m.addClass(new ProgramClass("com/other/C"));

        ClassTransformer t = new ClassTransformer(m);
        t.renamePackage("com.old", "com.new");
        t.applyTransformations();

        assertNull(m.getProgramClass("com/old/A"));
        assertNotNull(m.getProgramClass("com/new/A"));
        assertNotNull(m.getProgramClass("com/new/nested/B"));
        assertNotNull(m.getProgramClass("com/other/C"));
    }

    @Test
    void renamePackageMovesPackageInfoAndModuleExportPackages() {
        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass("com/old/App"));

        ClassNode pkgCn = new ClassNode();
        pkgCn.version = Opcodes.V17;
        pkgCn.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        pkgCn.name = "com/old/package-info";
        pkgCn.superName = "java/lang/Object";
        m.addPackageInfo("com/old/package-info.class", new PackageInfoClass("com/old/package-info.class", pkgCn, Opcodes.V17));

        ClassNode modCn = new ClassNode();
        modCn.version = Opcodes.V9;
        modCn.access = Opcodes.ACC_MODULE;
        modCn.name = "module-info";
        ModuleNode module = new ModuleNode("m", 0, null);
        modCn.module = module;
        module.exports = new ArrayList<>();
        module.exports.add(new ModuleExportNode("com/old", 0, null));
        m.addModuleInfo("module-info.class", new ModuleInfoClass("module-info.class", modCn, Opcodes.V9));

        ClassTransformer t = new ClassTransformer(m);
        t.renamePackage("com.old", "com.ren");
        t.applyTransformations();

        assertNull(m.getPackageInfo("com/old/package-info.class"));
        PackageInfoClass pi = m.getPackageInfo("com/ren/package-info.class");
        assertNotNull(pi);
        assertEquals("com/ren/package-info", pi.getInternalName());

        assertEquals("com/ren", modCn.module.exports.getFirst().packaze);
    }

    @Test
    void renameClassUpdatesModuleMainClassUsesAndProvides() {
        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass("com/old/App"));
        m.addClass(new ProgramClass("com/old/Impl"));

        ClassNode modCn = new ClassNode();
        modCn.version = Opcodes.V9;
        modCn.access = Opcodes.ACC_MODULE;
        modCn.name = "module-info";
        ModuleNode module = new ModuleNode("m", 0, null);
        modCn.module = module;
        module.mainClass = "com/old/App";
        module.uses = new ArrayList<>(List.of("com/old/Svc"));
        module.provides = new ArrayList<>();
        module.provides.add(new ModuleProvideNode("com/old/Svc", new ArrayList<>(List.of("com/old/Impl"))));
        m.addModuleInfo("module-info.class", new ModuleInfoClass("module-info.class", modCn, Opcodes.V9));

        ClassTransformer t = new ClassTransformer(m);
        t.renameClass("com/old/App", "com/new/App");
        t.renameClass("com/old/Svc", "com/new/Svc");
        t.renameClass("com/old/Impl", "com/new/Impl");
        t.applyTransformations();

        ModuleNode out = modCn.module;
        assertEquals("com/new/App", out.mainClass);
        assertEquals("com/new/Svc", out.uses.getFirst());
        assertEquals("com/new/Svc", out.provides.getFirst().service);
        assertEquals("com/new/Impl", out.provides.getFirst().providers.getFirst());
    }

    @Test
    void stripDebugClearsSourceFileAndLineNumbers() {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = "x/D";
        cn.superName = "java/lang/Object";
        cn.sourceFile = "D.java";

        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        LabelNode label = new LabelNode();
        mn.instructions.add(label);
        mn.instructions.add(new LineNumberNode(7, label));
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(mn);

        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass(cn));

        ClassTransformer t = new ClassTransformer(m);
        t.stripDebugEverywhere(StripDebugMode.SOURCE_FILE, StripDebugMode.LINE_NUMBERS);
        t.applyTransformations();

        ProgramClass pc = m.getProgramClass("x/D");
        assertNotNull(pc);
        assertNull(pc.getSourceFile());
        MethodNode out = pc.getClassNode().methods.getFirst();
        for (var insn = out.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            assertFalse(insn instanceof LineNumberNode);
        }
    }

    @Test
    void transformStringConstantsAfterApply() {
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        mn.instructions.add(new LdcInsnNode("hello"));
        mn.instructions.add(new InsnNode(Opcodes.RETURN));

        ProgramClass c = new ProgramClass("p/C");
        c.addMethod(new ProgramMethod(mn));

        JarMapping m = new JarMapping("x.jar");
        m.addClass(c);

        ClassTransformer t = new ClassTransformer(m);
        t.transformStringConstants((cl, me) -> true, s -> s.replace("hello", "hi"));
        t.applyTransformations();

        LdcInsnNode ldc = (LdcInsnNode) c.getMethod("m", "()V").getMethodNode().instructions.getFirst();
        assertEquals("hi", ldc.cst);
    }

    @Test
    void renameResourceMovesBytes() {
        JarMapping m = new JarMapping("x.jar");
        m.addResource("META-INF/old.txt", new byte[] {1, 2, 3});

        ClassTransformer t = new ClassTransformer(m);
        t.renameResource("META-INF/old.txt", "META-INF/new.txt");
        t.applyTransformations();

        assertNull(m.getResource("META-INF/old.txt"));
        assertArrayEquals(new byte[] {1, 2, 3}, m.getResource("META-INF/new.txt"));
    }

    @Test
    void renameClassesMatching() {
        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass("pkg/FooService"));
        m.addClass(new ProgramClass("pkg/Other"));

        ClassTransformer t = new ClassTransformer(m);
        t.renameClassesMatching(n -> n.endsWith("Service"), n -> n.replace("Service", "Svc"));
        t.applyTransformations();

        assertNull(m.getProgramClass("pkg/FooService"));
        assertNotNull(m.getProgramClass("pkg/FooSvc"));
        assertNotNull(m.getProgramClass("pkg/Other"));
    }

    @Test
    void hasPendingWorkAndClearMappings() {
        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass("a/B"));
        ClassTransformer t = new ClassTransformer(m);
        assertFalse(t.hasPendingWork());

        t.setClassAccess("a/B", Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);
        assertTrue(t.hasPendingWork());
        t.applyTransformations();
        assertFalse(t.hasPendingWork());

        t.renameClass("a/B", "a/C");
        assertTrue(t.hasPendingWork());
        t.clearMappings();
        assertFalse(t.hasPendingWork());
    }

    @Test
    void reconcilePrunesModuleExportWhenPackageEmptiedByRename() {
        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass("com/stale/App"));

        ClassNode modCn = new ClassNode();
        modCn.version = Opcodes.V9;
        modCn.access = Opcodes.ACC_MODULE;
        modCn.name = "module-info";
        ModuleNode module = new ModuleNode("m", 0, null);
        modCn.module = module;
        module.exports = new ArrayList<>();
        module.exports.add(new ModuleExportNode("com/stale", 0, null));
        m.addModuleInfo("module-info.class", new ModuleInfoClass("module-info.class", modCn, Opcodes.V9));

        ClassTransformer t = new ClassTransformer(m);
        t.renameClass("com/stale/App", "Main");
        t.applyTransformations();

        assertTrue(module.exports.isEmpty());
    }

    @Test
    void reconcileRemovesOrphanPackageInfoAfterRename() {
        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass("com/foo/App"));

        ClassNode pkgCn = new ClassNode();
        pkgCn.version = Opcodes.V17;
        pkgCn.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        pkgCn.name = "com/foo/package-info";
        pkgCn.superName = "java/lang/Object";
        m.addPackageInfo("com/foo/package-info.class", new PackageInfoClass("com/foo/package-info.class", pkgCn, Opcodes.V17));

        ClassTransformer t = new ClassTransformer(m);
        t.renameClass("com/foo/App", "Z");
        t.applyTransformations();

        assertNull(m.getPackageInfo("com/foo/package-info.class"));
    }
}
