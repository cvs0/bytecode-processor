package net.cvs0.bytecode.transform;

import net.cvs0.bytecode.instruction.Instruction;
import net.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class InstructionTransformer {
    private final ProgramMethod method;
    
    public InstructionTransformer(ProgramMethod method) {
        this.method = method;
    }
    
    public void replaceInstruction(int index, AbstractInsnNode newInstruction) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList instructions = method.getMethodNode().instructions;
            if (index >= 0 && index < instructions.size()) {
                AbstractInsnNode oldInstruction = instructions.get(index);
                instructions.set(oldInstruction, newInstruction);
                
                method.replaceInstruction(index, new Instruction(newInstruction));
            }
        }
    }
    
    public void insertBefore(int index, AbstractInsnNode newInstruction) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList instructions = method.getMethodNode().instructions;
            if (index >= 0 && index < instructions.size()) {
                AbstractInsnNode target = instructions.get(index);
                instructions.insertBefore(target, newInstruction);
                
                method.insertInstruction(index, new Instruction(newInstruction));
            }
        }
    }
    
    public void insertAfter(int index, AbstractInsnNode newInstruction) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList instructions = method.getMethodNode().instructions;
            if (index >= 0 && index < instructions.size()) {
                AbstractInsnNode target = instructions.get(index);
                instructions.insert(target, newInstruction);
                
                method.insertInstruction(index + 1, new Instruction(newInstruction));
            }
        }
    }
    
    public void removeInstruction(int index) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList instructions = method.getMethodNode().instructions;
            if (index >= 0 && index < instructions.size()) {
                AbstractInsnNode target = instructions.get(index);
                instructions.remove(target);
                
                method.removeInstruction(index);
            }
        }
    }
    
    public void replaceInstructions(Predicate<AbstractInsnNode> matcher, 
                                   Function<AbstractInsnNode, AbstractInsnNode> replacer) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList instructions = method.getMethodNode().instructions;
            List<AbstractInsnNode> toReplace = new ArrayList<>();
            
            for (AbstractInsnNode insn : instructions) {
                if (matcher.test(insn)) {
                    toReplace.add(insn);
                }
            }
            
            for (AbstractInsnNode insn : toReplace) {
                AbstractInsnNode replacement = replacer.apply(insn);
                if (replacement != null) {
                    instructions.set(insn, replacement);
                } else {
                    instructions.remove(insn);
                }
            }
            
            rebuildInstructionList();
        }
    }
    
    public void removeInstructions(Predicate<AbstractInsnNode> matcher) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList instructions = method.getMethodNode().instructions;
            List<AbstractInsnNode> toRemove = new ArrayList<>();
            
            for (AbstractInsnNode insn : instructions) {
                if (matcher.test(insn)) {
                    toRemove.add(insn);
                }
            }
            
            for (AbstractInsnNode insn : toRemove) {
                instructions.remove(insn);
            }
            
            rebuildInstructionList();
        }
    }
    
    public void insertAtBeginning(AbstractInsnNode... instructions) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList methodInstructions = method.getMethodNode().instructions;
            
            for (int i = instructions.length - 1; i >= 0; i--) {
                if (methodInstructions.size() > 0) {
                    methodInstructions.insertBefore(methodInstructions.getFirst(), instructions[i]);
                } else {
                    methodInstructions.add(instructions[i]);
                }
            }
            
            rebuildInstructionList();
        }
    }
    
    public void insertAtEnd(AbstractInsnNode... instructions) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList methodInstructions = method.getMethodNode().instructions;
            
            for (AbstractInsnNode instruction : instructions) {
                methodInstructions.add(instruction);
            }
            
            rebuildInstructionList();
        }
    }
    
    public void insertBeforeReturn(AbstractInsnNode... instructions) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList methodInstructions = method.getMethodNode().instructions;
            List<AbstractInsnNode> returnInstructions = new ArrayList<>();
            
            for (AbstractInsnNode insn : methodInstructions) {
                if (isReturnInstruction(insn)) {
                    returnInstructions.add(insn);
                }
            }
            
            for (AbstractInsnNode returnInsn : returnInstructions) {
                for (AbstractInsnNode instruction : instructions) {
                    methodInstructions.insertBefore(returnInsn, instruction);
                }
            }
            
            rebuildInstructionList();
        }
    }
    
    private boolean isReturnInstruction(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return opcode >= 172 && opcode <= 177;
    }
    
    public void wrapWithTryCatch(String exceptionType, AbstractInsnNode... catchInstructions) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList instructions = method.getMethodNode().instructions;
            
            LabelNode startLabel = new LabelNode();
            LabelNode endLabel = new LabelNode();
            LabelNode handlerLabel = new LabelNode();
            
            instructions.insertBefore(instructions.getFirst(), startLabel);
            instructions.add(endLabel);
            instructions.add(handlerLabel);
            
            for (AbstractInsnNode catchInstruction : catchInstructions) {
                instructions.add(catchInstruction);
            }
            
            if (method.getMethodNode().tryCatchBlocks == null) {
                method.getMethodNode().tryCatchBlocks = new ArrayList<>();
            }
            
            method.getMethodNode().tryCatchBlocks.add(
                new TryCatchBlockNode(startLabel, endLabel, handlerLabel, exceptionType)
            );
            
            rebuildInstructionList();
        }
    }
    
    public int findInstruction(Predicate<AbstractInsnNode> matcher) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList instructions = method.getMethodNode().instructions;
            
            for (int i = 0; i < instructions.size(); i++) {
                if (matcher.test(instructions.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    public List<Integer> findAllInstructions(Predicate<AbstractInsnNode> matcher) {
        List<Integer> indices = new ArrayList<>();
        
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            InsnList instructions = method.getMethodNode().instructions;
            
            for (int i = 0; i < instructions.size(); i++) {
                if (matcher.test(instructions.get(i))) {
                    indices.add(i);
                }
            }
        }
        
        return indices;
    }
    
    private void rebuildInstructionList() {
        method.clearInstructions();
        
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            for (AbstractInsnNode insn : method.getMethodNode().instructions) {
                method.addInstruction(new Instruction(insn));
            }
        }
    }
    
    public static InstructionTransformer forMethod(ProgramMethod method) {
        return new InstructionTransformer(method);
    }
}