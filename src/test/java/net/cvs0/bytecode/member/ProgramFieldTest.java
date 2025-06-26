package net.cvs0.bytecode.member;

import net.cvs0.bytecode.attribute.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgramFieldTest {
    
    private ProgramField field;
    
    @BeforeEach
    void setUp() {
        field = new ProgramField("testField", "Ljava/lang/String;", 0x0001);
    }
    
    @Test
    void testConstructor() {
        assertEquals("testField", field.getName());
        assertEquals("Ljava/lang/String;", field.getDescriptor());
        assertEquals(0x0001, field.getAccess());
        assertTrue(field.getAttributes().isEmpty());
    }
    
    @Test
    void testSettersAndGetters() {
        field.setName("newName");
        assertEquals("newName", field.getName());
        
        field.setDescriptor("I");
        assertEquals("I", field.getDescriptor());
        
        field.setAccess(0x0008);
        assertEquals(0x0008, field.getAccess());
        
        field.setSignature("TT;");
        assertEquals("TT;", field.getSignature());
        
        field.setValue("test value");
        assertEquals("test value", field.getValue());
    }
    
    @Test
    void testAccessFlags() {
        field.setAccess(0x0001);
        assertTrue(field.isPublic());
        assertFalse(field.isPrivate());
        assertFalse(field.isProtected());
        assertFalse(field.isStatic());
        
        field.setAccess(0x0002);
        assertTrue(field.isPrivate());
        assertFalse(field.isPublic());
        
        field.setAccess(0x0004);
        assertTrue(field.isProtected());
        
        field.setAccess(0x0008);
        assertTrue(field.isStatic());
        
        field.setAccess(0x0010);
        assertTrue(field.isFinal());
        
        field.setAccess(0x0040);
        assertTrue(field.isVolatile());
        
        field.setAccess(0x0080);
        assertTrue(field.isTransient());
        
        field.setAccess(0x1000);
        assertTrue(field.isSynthetic());
        
        field.setAccess(0x4000);
        assertTrue(field.isEnum());
    }
    
    @Test
    void testAttributes() {
        Attribute attr1 = new Attribute("Signature");
        Attribute attr2 = new Attribute("RuntimeVisibleAnnotations");
        
        field.addAttribute(attr1);
        field.addAttribute(attr2);
        
        assertEquals(2, field.getAttributes().size());
        assertTrue(field.getAttributes().contains(attr1));
        assertTrue(field.getAttributes().contains(attr2));
    }
    
    @Test
    void testType() {
        assertEquals("Ljava/lang/String;", field.getType());
        
        field.setDescriptor("I");
        assertEquals("I", field.getType());
    }
    
    @Test
    void testFullName() {
        assertEquals("testField", field.getFullName());
    }
}