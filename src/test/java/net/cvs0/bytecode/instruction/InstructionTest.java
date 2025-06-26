package net.cvs0.bytecode.instruction;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

import static org.junit.jupiter.api.Assertions.*;

class InstructionTest {
    
    @Test
    void testConstructorWithOpcode() {
        Instruction instruction = new Instruction(1);
        assertEquals(1, instruction.getOpcode());
    }
    
    @Test
    void testConstructorWithInstructionNode() {
        InsnNode insnNode = new InsnNode(177);
        Instruction instruction = new Instruction(insnNode);
        
        assertEquals(177, instruction.getOpcode());
        assertEquals(AbstractInsnNode.INSN, instruction.getType());
        assertEquals(insnNode, instruction.getInstructionNode());
    }
    
    @Test
    void testSettersAndGetters() {
        Instruction instruction = new Instruction(1);
        
        instruction.setOpcode(177);
        assertEquals(177, instruction.getOpcode());
        
        instruction.setType(AbstractInsnNode.INSN);
        assertEquals(AbstractInsnNode.INSN, instruction.getType());
        
        InsnNode newNode = new InsnNode(2);
        instruction.setInstructionNode(newNode);
        assertEquals(newNode, instruction.getInstructionNode());
        assertEquals(2, instruction.getOpcode());
    }
    
    @Test
    void testInstructionTypeChecks() {
        Instruction instruction = new Instruction(1);
        instruction.setType(AbstractInsnNode.INSN);
        assertTrue(instruction.isInsn());
        assertFalse(instruction.isMethodInsn());
        
        instruction.setType(AbstractInsnNode.METHOD_INSN);
        assertTrue(instruction.isMethodInsn());
        assertFalse(instruction.isInsn());
        
        instruction.setType(AbstractInsnNode.FIELD_INSN);
        assertTrue(instruction.isFieldInsn());
        
        instruction.setType(AbstractInsnNode.VAR_INSN);
        assertTrue(instruction.isVarInsn());
        
        instruction.setType(AbstractInsnNode.TYPE_INSN);
        assertTrue(instruction.isTypeInsn());
        
        instruction.setType(AbstractInsnNode.INT_INSN);
        assertTrue(instruction.isIntInsn());
        
        instruction.setType(AbstractInsnNode.LDC_INSN);
        assertTrue(instruction.isLdcInsn());
        
        instruction.setType(AbstractInsnNode.JUMP_INSN);
        assertTrue(instruction.isJumpInsn());
        
        instruction.setType(AbstractInsnNode.LABEL);
        assertTrue(instruction.isLabelInsn());
        
        instruction.setType(AbstractInsnNode.IINC_INSN);
        assertTrue(instruction.isIincInsn());
        
        instruction.setType(AbstractInsnNode.TABLESWITCH_INSN);
        assertTrue(instruction.isTableSwitchInsn());
        
        instruction.setType(AbstractInsnNode.LOOKUPSWITCH_INSN);
        assertTrue(instruction.isLookupSwitchInsn());
        
        instruction.setType(AbstractInsnNode.MULTIANEWARRAY_INSN);
        assertTrue(instruction.isMultiANewArrayInsn());
        
        instruction.setType(AbstractInsnNode.INVOKE_DYNAMIC_INSN);
        assertTrue(instruction.isInvokeDynamicInsn());
        
        instruction.setType(AbstractInsnNode.FRAME);
        assertTrue(instruction.isFrameInsn());
        
        instruction.setType(AbstractInsnNode.LINE);
        assertTrue(instruction.isLineNumberInsn());
    }
    
    @Test
    void testOpcodeNames() {
        assertEquals("ACONST_NULL", Instruction.getOpcodeName(1));
        assertEquals("ICONST_0", Instruction.getOpcodeName(3));
        assertEquals("RETURN", Instruction.getOpcodeName(177));
        assertEquals("INVOKEVIRTUAL", Instruction.getOpcodeName(182));
        assertEquals("NEW", Instruction.getOpcodeName(187));
        assertEquals("UNKNOWN", Instruction.getOpcodeName(-1));
        assertEquals("UNKNOWN_999", Instruction.getOpcodeName(999));
        
        Instruction instruction = new Instruction(177);
        assertEquals("RETURN", instruction.getOpcodeName());
    }
    
    @Test
    void testToString() {
        Instruction instruction = new Instruction(177);
        assertEquals("RETURN (177)", instruction.toString());
        
        instruction.setOpcode(1);
        assertEquals("ACONST_NULL (1)", instruction.toString());
    }
}