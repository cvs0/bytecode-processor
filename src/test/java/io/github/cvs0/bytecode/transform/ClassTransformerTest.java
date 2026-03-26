package io.github.cvs0.bytecode.transform;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramField;
import io.github.cvs0.bytecode.member.ProgramMethod;
import io.github.cvs0.bytecode.transform.transformer.ClassTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.*;

class ClassTransformerTest {

    private JarMapping jarMapping;
    private ClassTransformer transformer;
    private ProgramClass testClass;

    @BeforeEach
    void setUp() {
        jarMapping = new JarMapping("test.jar");
        testClass = new ProgramClass("com/example/TestClass");
        jarMapping.addClass(testClass);
        transformer = new ClassTransformer(jarMapping);
    }

    @Test
    void testRenameClass() {
        transformer.renameClass("com/example/TestClass", "com/example/NewTestClass");
        transformer.applyTransformations();

        assertNull(jarMapping.getProgramClass("com/example/TestClass"));
        assertNotNull(jarMapping.getProgramClass("com/example/NewTestClass"));
        assertEquals("com/example/NewTestClass", jarMapping.getProgramClass("com/example/NewTestClass").getName());
    }

    @Test
    void testRenameClassUpdatesFieldInsnDescriptor() {
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        mn.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "com/example/Other", "x", "Lcom/example/Other;"));
        testClass.addMethod(new ProgramMethod(mn));

        transformer.renameClass("com/example/Other", "com/example/RenOther");
        transformer.applyTransformations();

        ProgramMethod run = testClass.getMethod("run", "()V");
        FieldInsnNode fin = (FieldInsnNode) run.getMethodNode().instructions.getFirst();
        assertEquals("com/example/RenOther", fin.owner);
        assertEquals("Lcom/example/RenOther;", fin.desc);
    }

    @Test
    void testRenameField() {
        ProgramField field = new ProgramField("oldFieldName", "I", 0x0001);
        testClass.addField(field);

        transformer.renameField("com/example/TestClass", "oldFieldName", "newFieldName");
        transformer.applyTransformations();

        assertNull(testClass.getField("oldFieldName"));
        assertNotNull(testClass.getField("newFieldName"));
        assertEquals("newFieldName", testClass.getField("newFieldName").getName());
    }

    @Test
    void testRenameMethod() {
        ProgramMethod method = new ProgramMethod("oldMethodName", "()V", 0x0001);
        testClass.addMethod(method);

        transformer.renameMethod("com/example/TestClass", "oldMethodName", "()V", "newMethodName");
        transformer.applyTransformations();

        assertNull(testClass.getMethod("oldMethodName", "()V"));
        assertNotNull(testClass.getMethod("newMethodName", "()V"));
        assertEquals("newMethodName", testClass.getMethod("newMethodName", "()V").getName());
    }

    @Test
    void testTransformClasses() {
        transformer.transformClasses(clazz -> {
            clazz.setAccess(0x0001);
            return clazz;
        });

        assertEquals(0x0001, testClass.getAccess());
    }

    @Test
    void testTransformMethods() {
        ProgramMethod method = new ProgramMethod("testMethod", "()V", 0x0000);
        testClass.addMethod(method);

        transformer.transformMethods(m -> {
            m.setAccess(0x0001);
            return m;
        });

        assertEquals(0x0001, testClass.getMethod("testMethod", "()V").getAccess());
    }

    @Test
    void testTransformFields() {
        ProgramField field = new ProgramField("testField", "I", 0x0000);
        testClass.addField(field);

        transformer.transformFields(f -> {
            f.setAccess(0x0001);
            return f;
        });

        assertEquals(0x0001, testClass.getField("testField").getAccess());
    }

    @Test
    void testGetMappings() {
        transformer.renameClass("oldClass", "newClass");
        transformer.renameField("className", "oldField", "newField");
        transformer.renameMethod("className", "oldMethod", "()V", "newMethod");

        assertEquals(1, transformer.getClassNameMappings().size());
        assertEquals(1, transformer.getFieldNameMappings().size());
        assertEquals(1, transformer.getMethodNameMappings().size());

        assertEquals("newClass", transformer.getClassNameMappings().get("oldClass"));
        assertEquals("newField", transformer.getFieldNameMappings().get("className.oldField"));
        assertEquals("newMethod", transformer.getMethodNameMappings().get("className.oldMethod()V"));
    }

    @Test
    void testClearMappings() {
        transformer.renameClass("oldClass", "newClass");
        transformer.renameField("className", "oldField", "newField");
        transformer.renameMethod("className", "oldMethod", "()V", "newMethod");

        assertFalse(transformer.getClassNameMappings().isEmpty());
        assertFalse(transformer.getFieldNameMappings().isEmpty());
        assertFalse(transformer.getMethodNameMappings().isEmpty());

        transformer.clearMappings();

        assertTrue(transformer.getClassNameMappings().isEmpty());
        assertTrue(transformer.getFieldNameMappings().isEmpty());
        assertTrue(transformer.getMethodNameMappings().isEmpty());
    }
}
