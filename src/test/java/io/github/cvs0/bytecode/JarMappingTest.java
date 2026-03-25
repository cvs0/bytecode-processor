package io.github.cvs0.bytecode;

import io.github.cvs0.bytecode.clazz.LibraryClass;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JarMappingTest {

    private JarMapping jarMapping;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jarMapping = new JarMapping("test.jar");
    }

    @Test
    void rejectsNullJarPath() {
        assertThrows(NullPointerException.class, () -> new JarMapping(null));
    }

    @Test
    void rejectsNullAddArguments() {
        assertThrows(NullPointerException.class, () -> jarMapping.addClass(null));
        assertThrows(NullPointerException.class, () -> jarMapping.addLibraryClass(null));
        assertThrows(NullPointerException.class, () -> jarMapping.addResource(null, new byte[0]));
        assertThrows(NullPointerException.class, () -> jarMapping.addResource("a", null));
    }

    @Test
    void testAddAndGetProgramClass() {
        ProgramClass clazz = new ProgramClass("com/example/TestClass");
        jarMapping.addClass(clazz);

        assertEquals(clazz, jarMapping.getProgramClass("com/example/TestClass"));
        assertEquals(1, jarMapping.getProgramClasses().size());
        assertTrue(jarMapping.containsClass("com/example/TestClass"));
    }

    @Test
    void testAddAndGetLibraryClass() {
        LibraryClass clazz = new LibraryClass("java/lang/String");
        jarMapping.addLibraryClass(clazz);

        assertEquals(clazz, jarMapping.getLibraryClass("java/lang/String"));
        assertEquals(1, jarMapping.getLibraryClasses().size());
        assertTrue(jarMapping.containsClass("java/lang/String"));
    }

    @Test
    void testAddAndGetResource() {
        byte[] data = "test resource content".getBytes();
        jarMapping.addResource("META-INF/MANIFEST.MF", data);

        assertArrayEquals(data, jarMapping.getResource("META-INF/MANIFEST.MF"));
        assertEquals(1, jarMapping.getResourceCount());
        assertTrue(jarMapping.getResourceNames().contains("META-INF/MANIFEST.MF"));
    }

    @Test
    void testRemoveClass() {
        ProgramClass clazz = new ProgramClass("com/example/TestClass");
        jarMapping.addClass(clazz);

        assertTrue(jarMapping.containsClass("com/example/TestClass"));

        jarMapping.removeClass("com/example/TestClass");

        assertFalse(jarMapping.containsClass("com/example/TestClass"));
        assertNull(jarMapping.getProgramClass("com/example/TestClass"));
    }

    @Test
    void testRenameClass() {
        ProgramClass clazz = new ProgramClass("com/example/OldName");
        jarMapping.addClass(clazz);

        jarMapping.renameClass("com/example/OldName", "com/example/NewName");

        assertNull(jarMapping.getProgramClass("com/example/OldName"));
        assertNotNull(jarMapping.getProgramClass("com/example/NewName"));
        assertEquals("com/example/NewName", jarMapping.getProgramClass("com/example/NewName").getName());
    }

    @Test
    void testGetAllClassNames() {
        jarMapping.addClass(new ProgramClass("com/example/Class1"));
        jarMapping.addClass(new ProgramClass("com/example/Class2"));
        jarMapping.addLibraryClass(new LibraryClass("java/lang/String"));

        assertEquals(3, jarMapping.getAllClassNames().size());
        assertTrue(jarMapping.getAllClassNames().contains("com/example/Class1"));
        assertTrue(jarMapping.getAllClassNames().contains("com/example/Class2"));
        assertTrue(jarMapping.getAllClassNames().contains("java/lang/String"));
    }

    @Test
    void testGetTotalClassCount() {
        jarMapping.addClass(new ProgramClass("com/example/Class1"));
        jarMapping.addClass(new ProgramClass("com/example/Class2"));
        jarMapping.addLibraryClass(new LibraryClass("java/lang/String"));

        assertEquals(3, jarMapping.getTotalClassCount());
    }

    @Test
    void moduleAndPackageInfoIncreaseTotalClassCount() {
        ClassNode mod = new ClassNode();
        mod.version = Opcodes.V9;
        mod.access = Opcodes.ACC_MODULE;
        mod.name = "module-info";
        mod.module = new ModuleNode("demo", 0, null);
        jarMapping.addModuleInfo("module-info.class", new ModuleInfoClass("module-info.class", mod, Opcodes.V9));

        ClassNode pkg = new ClassNode();
        pkg.version = Opcodes.V17;
        pkg.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        pkg.name = "com/example/package-info";
        pkg.superName = "java/lang/Object";
        jarMapping.addPackageInfo("com/example/package-info.class", new PackageInfoClass("com/example/package-info.class", pkg, Opcodes.V17));

        assertEquals(1, jarMapping.getModuleInfoCount());
        assertEquals(1, jarMapping.getPackageInfoCount());
        assertEquals(2, jarMapping.getTotalClassCount());
        assertNotNull(jarMapping.getModuleInfo("module-info.class"));
        assertNotNull(jarMapping.getPackageInfo("com/example/package-info.class"));
    }

    @Test
    void testJarPath() {
        assertEquals("test.jar", jarMapping.getJarPath());
    }
}
