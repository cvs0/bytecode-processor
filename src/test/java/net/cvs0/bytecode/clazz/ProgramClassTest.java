package net.cvs0.bytecode.clazz;

import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import net.cvs0.bytecode.attribute.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ProgramClassTest {
    
    private ProgramClass programClass;
    
    @BeforeEach
    void setUp() {
        programClass = new ProgramClass("com/example/TestClass");
    }
    
    @Test
    void testConstructorWithName() {
        assertEquals("com/example/TestClass", programClass.getName());
        assertNotNull(programClass.getInterfaces());
        assertTrue(programClass.getInterfaces().isEmpty());
    }
    
    @Test
    void testAddAndGetField() {
        ProgramField field = new ProgramField("testField", "Ljava/lang/String;", 0x0001);
        programClass.addField(field);
        
        assertEquals(field, programClass.getField("testField"));
        assertEquals(1, programClass.getFields().size());
        assertEquals(programClass, field.getOwner());
    }
    
    @Test
    void testAddAndGetMethod() {
        ProgramMethod method = new ProgramMethod("testMethod", "()V", 0x0001);
        programClass.addMethod(method);
        
        assertEquals(method, programClass.getMethod("testMethod", "()V"));
        assertEquals(1, programClass.getMethods().size());
        assertEquals(programClass, method.getOwner());
    }
    
    @Test
    void testRemoveField() {
        ProgramField field = new ProgramField("testField", "I", 0x0001);
        programClass.addField(field);
        
        assertEquals(field, programClass.getField("testField"));
        
        programClass.removeField("testField");
        
        assertNull(programClass.getField("testField"));
        assertTrue(programClass.getFields().isEmpty());
        assertNull(field.getOwner());
    }
    
    @Test
    void testRemoveMethod() {
        ProgramMethod method = new ProgramMethod("testMethod", "()V", 0x0001);
        programClass.addMethod(method);
        
        assertEquals(method, programClass.getMethod("testMethod", "()V"));
        
        programClass.removeMethod("testMethod", "()V");
        
        assertNull(programClass.getMethod("testMethod", "()V"));
        assertTrue(programClass.getMethods().isEmpty());
        assertNull(method.getOwner());
    }
    
    @Test
    void testRenameField() {
        ProgramField field = new ProgramField("oldName", "I", 0x0001);
        programClass.addField(field);
        
        programClass.renameField("oldName", "newName");
        
        assertNull(programClass.getField("oldName"));
        assertEquals(field, programClass.getField("newName"));
        assertEquals("newName", field.getName());
    }
    
    @Test
    void testRenameMethod() {
        ProgramMethod method = new ProgramMethod("oldName", "()V", 0x0001);
        programClass.addMethod(method);
        
        programClass.renameMethod("oldName", "()V", "newName");
        
        assertNull(programClass.getMethod("oldName", "()V"));
        assertEquals(method, programClass.getMethod("newName", "()V"));
        assertEquals("newName", method.getName());
    }
    
    @Test
    void testSettersAndGetters() {
        programClass.setSuperName("java/lang/Object");
        assertEquals("java/lang/Object", programClass.getSuperName());
        
        programClass.setAccess(0x0021);
        assertEquals(0x0021, programClass.getAccess());
        
        programClass.setSignature("<T:Ljava/lang/Object;>Ljava/lang/Object;");
        assertEquals("<T:Ljava/lang/Object;>Ljava/lang/Object;", programClass.getSignature());
        
        programClass.setSourceFile("TestClass.java");
        assertEquals("TestClass.java", programClass.getSourceFile());
    }
    
    @Test
    void testInterfaces() {
        programClass.addInterface("java/io/Serializable");
        programClass.addInterface("java/lang/Cloneable");
        
        assertEquals(2, programClass.getInterfaces().size());
        assertTrue(programClass.getInterfaces().contains("java/io/Serializable"));
        assertTrue(programClass.getInterfaces().contains("java/lang/Cloneable"));
        
        programClass.addInterface("java/io/Serializable");
        assertEquals(2, programClass.getInterfaces().size());
        
        programClass.removeInterface("java/io/Serializable");
        assertEquals(1, programClass.getInterfaces().size());
        assertFalse(programClass.getInterfaces().contains("java/io/Serializable"));
    }
    
    @Test
    void testAccessFlags() {
        programClass.setAccess(0x0001);
        assertTrue(programClass.isPublic());
        assertFalse(programClass.isFinal());
        assertFalse(programClass.isAbstract());
        
        programClass.setAccess(0x0010);
        assertTrue(programClass.isFinal());
        assertFalse(programClass.isPublic());
        
        programClass.setAccess(0x0400);
        assertTrue(programClass.isAbstract());
        
        programClass.setAccess(0x0200);
        assertTrue(programClass.isInterface());
        
        programClass.setAccess(0x4000);
        assertTrue(programClass.isEnum());
        
        programClass.setAccess(0x2000);
        assertTrue(programClass.isAnnotation());
    }
    
    @Test
    void testSimpleNameAndPackage() {
        assertEquals("TestClass", programClass.getSimpleName());
        assertEquals("com.example", programClass.getPackageName());
        
        ProgramClass rootClass = new ProgramClass("RootClass");
        assertEquals("RootClass", rootClass.getSimpleName());
        assertEquals("", rootClass.getPackageName());
    }
    
    @Test
    void testAttributes() {
        Attribute attr1 = new Attribute("Signature");
        Attribute attr2 = new Attribute("SourceFile");
        
        programClass.addAttribute(attr1);
        programClass.addAttribute(attr2);
        
        assertEquals(2, programClass.getAttributes().size());
        assertTrue(programClass.getAttributes().contains(attr1));
        assertTrue(programClass.getAttributes().contains(attr2));
    }
}