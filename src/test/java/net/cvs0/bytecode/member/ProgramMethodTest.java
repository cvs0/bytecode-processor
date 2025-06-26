package net.cvs0.bytecode.member;

import net.cvs0.bytecode.instruction.Instruction;
import net.cvs0.bytecode.attribute.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgramMethodTest {
    
    private ProgramMethod method;
    
    @BeforeEach
    void setUp() {
        method = new ProgramMethod("testMethod", "(I)V", 0x0001);
    }
    
    @Test
    void testConstructor() {
        assertEquals("testMethod", method.getName());
        assertEquals("(I)V", method.getDescriptor());
        assertEquals(0x0001, method.getAccess());
        assertTrue(method.getInstructions().isEmpty());
        assertTrue(method.getAttributes().isEmpty());
    }
    
    @Test
    void testSettersAndGetters() {
        method.setName("newName");
        assertEquals("newName", method.getName());
        
        method.setDescriptor("(Ljava/lang/String;)I");
        assertEquals("(Ljava/lang/String;)I", method.getDescriptor());
        
        method.setAccess(0x0008);
        assertEquals(0x0008, method.getAccess());
        
        method.setSignature("<T:Ljava/lang/Object;>(TT;)V");
        assertEquals("<T:Ljava/lang/Object;>(TT;)V", method.getSignature());
        
        method.setMaxStack(10);
        assertEquals(10, method.getMaxStack());
        
        method.setMaxLocals(5);
        assertEquals(5, method.getMaxLocals());
        
        String[] exceptions = {"java/io/IOException", "java/lang/RuntimeException"};
        method.setExceptions(exceptions);
        assertArrayEquals(exceptions, method.getExceptions());
    }
    
    @Test
    void testInstructions() {
        Instruction insn1 = new Instruction(1);
        Instruction insn2 = new Instruction(177);
        
        method.addInstruction(insn1);
        method.addInstruction(insn2);
        
        assertEquals(2, method.getInstructionCount());
        assertTrue(method.hasInstructions());
        assertEquals(insn1, method.getInstructions().get(0));
        assertEquals(insn2, method.getInstructions().get(1));
        
        Instruction insn3 = new Instruction(16);
        method.insertInstruction(1, insn3);
        
        assertEquals(3, method.getInstructionCount());
        assertEquals(insn3, method.getInstructions().get(1));
        
        method.removeInstruction(1);
        assertEquals(2, method.getInstructionCount());
        
        Instruction replacement = new Instruction(2);
        method.replaceInstruction(0, replacement);
        assertEquals(replacement, method.getInstructions().get(0));
        
        method.clearInstructions();
        assertTrue(method.getInstructions().isEmpty());
        assertFalse(method.hasInstructions());
    }
    
    @Test
    void testAccessFlags() {
        method.setAccess(0x0001);
        assertTrue(method.isPublic());
        assertFalse(method.isPrivate());
        assertFalse(method.isProtected());
        assertFalse(method.isStatic());
        
        method.setAccess(0x0002);
        assertTrue(method.isPrivate());
        assertFalse(method.isPublic());
        
        method.setAccess(0x0004);
        assertTrue(method.isProtected());
        
        method.setAccess(0x0008);
        assertTrue(method.isStatic());
        
        method.setAccess(0x0010);
        assertTrue(method.isFinal());
        
        method.setAccess(0x0020);
        assertTrue(method.isSynchronized());
        
        method.setAccess(0x0100);
        assertTrue(method.isNative());
        
        method.setAccess(0x0400);
        assertTrue(method.isAbstract());
        
        method.setAccess(0x1000);
        assertTrue(method.isSynthetic());
    }
    
    @Test
    void testSpecialMethods() {
        ProgramMethod constructor = new ProgramMethod("<init>", "()V", 0x0001);
        assertTrue(constructor.isConstructor());
        assertFalse(constructor.isStaticInitializer());
        
        ProgramMethod staticInit = new ProgramMethod("<clinit>", "()V", 0x0008);
        assertTrue(staticInit.isStaticInitializer());
        assertFalse(staticInit.isConstructor());
        
        assertFalse(method.isConstructor());
        assertFalse(method.isStaticInitializer());
    }
    
    @Test
    void testAttributes() {
        Attribute attr1 = new Attribute("Code");
        Attribute attr2 = new Attribute("Exceptions");
        
        method.addAttribute(attr1);
        method.addAttribute(attr2);
        
        assertEquals(2, method.getAttributes().size());
        assertTrue(method.getAttributes().contains(attr1));
        assertTrue(method.getAttributes().contains(attr2));
    }
    
    @Test
    void testFullName() {
        assertEquals("testMethod(I)V", method.getFullName());
        
        method.setName("newMethod");
        method.setDescriptor("(Ljava/lang/String;)I");
        assertEquals("newMethod(Ljava/lang/String;)I", method.getFullName());
    }
}