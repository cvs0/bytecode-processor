package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleOpenNode;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarGraphMetadataReconcilerTest {

    @Test
    void reconcilePrunesOpensAndPackagesLists() {
        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass("keep/Alive"));

        ClassNode modCn = new ClassNode();
        modCn.version = Opcodes.V9;
        modCn.access = Opcodes.ACC_MODULE;
        modCn.name = "module-info";
        ModuleNode module = new ModuleNode("m", 0, null);
        modCn.module = module;
        module.opens = new ArrayList<>();
        module.opens.add(new ModuleOpenNode("gone/pkg", 0, null));
        module.opens.add(new ModuleOpenNode("keep", 0, null));
        module.packages = new ArrayList<>();
        module.packages.add("gone/pkg");
        module.packages.add("keep");
        m.addModuleInfo("module-info.class", new ModuleInfoClass("module-info.class", modCn, Opcodes.V9));

        JarGraphMetadataReconciler.reconcile(m);

        assertEquals(1, module.opens.size());
        assertEquals("keep", module.opens.getFirst().packaze);
        assertEquals(1, module.packages.size());
        assertEquals("keep", module.packages.getFirst());
    }

    @Test
    void reconcileKeepsExportWhenClassRemainsInPackage() {
        JarMapping m = new JarMapping("x.jar");
        m.addClass(new ProgramClass("com/app/Main"));

        ClassNode modCn = new ClassNode();
        modCn.version = Opcodes.V9;
        modCn.access = Opcodes.ACC_MODULE;
        modCn.name = "module-info";
        ModuleNode module = new ModuleNode("m", 0, null);
        modCn.module = module;
        module.exports = new ArrayList<>();
        module.exports.add(new ModuleExportNode("com/app", 0, null));
        m.addModuleInfo("module-info.class", new ModuleInfoClass("module-info.class", modCn, Opcodes.V9));

        JarGraphMetadataReconciler.reconcile(m);

        assertEquals(1, module.exports.size());
        assertEquals("com/app", module.exports.getFirst().packaze);
    }

    @Test
    void reconcileRemovesPackageInfoWithNoProgramClasses() {
        JarMapping m = new JarMapping("x.jar");
        ClassNode pkgCn = new ClassNode();
        pkgCn.version = Opcodes.V17;
        pkgCn.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        pkgCn.name = "orphan/pkg/package-info";
        pkgCn.superName = "java/lang/Object";
        m.addPackageInfo("orphan/pkg/package-info.class", new PackageInfoClass("orphan/pkg/package-info.class", pkgCn, Opcodes.V17));

        JarGraphMetadataReconciler.reconcile(m);

        assertNull(m.getPackageInfo("orphan/pkg/package-info.class"));
    }
}
