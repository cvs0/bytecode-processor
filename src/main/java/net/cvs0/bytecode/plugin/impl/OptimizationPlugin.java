package net.cvs0.bytecode.plugin.impl;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.analysis.UnusedCodeAnalyzer;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramMethod;
import net.cvs0.bytecode.plugin.AbstractPlugin;
import net.cvs0.bytecode.transform.InstructionTransformer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

import java.util.Set;

public class OptimizationPlugin extends AbstractPlugin {
    
    public OptimizationPlugin() {
        super("Optimization Plugin", "1.0.0", "Performs various bytecode optimizations");
    }
    
    @Override
    public void process(JarMapping mapping) {
        boolean removeUnusedMethods = getBooleanConfig("removeUnusedMethods", false);
        boolean removeUnusedFields = getBooleanConfig("removeUnusedFields", false);
        boolean removeNops = getBooleanConfig("removeNops", true);
        boolean optimizeConstants = getBooleanConfig("optimizeConstants", true);
        
        if (removeUnusedMethods) {
            removeUnusedMethods(mapping);
        }
        
        if (removeUnusedFields) {
            removeUnusedFields(mapping);
        }
        
        if (removeNops || optimizeConstants) {
            optimizeInstructions(mapping, removeNops, optimizeConstants);
        }
    }
    
    private void removeUnusedMethods(JarMapping mapping) {
        Set<String> unusedMethods = UnusedCodeAnalyzer.findUnusedMethods(mapping);
        
        for (String methodKey : unusedMethods) {
            String[] parts = methodKey.split("\\.");
            if (parts.length >= 2) {
                String className = parts[0];
                String methodPart = parts[1];
                
                int parenIndex = methodPart.indexOf('(');
                if (parenIndex > 0) {
                    String methodName = methodPart.substring(0, parenIndex);
                    String descriptor = methodPart.substring(parenIndex);
                    
                    ProgramClass clazz = mapping.getProgramClass(className);
                    if (clazz != null) {
                        clazz.removeMethod(methodName, descriptor);
                    }
                }
            }
        }
    }
    
    private void removeUnusedFields(JarMapping mapping) {
        Set<String> unusedFields = UnusedCodeAnalyzer.findUnusedFields(mapping);
        
        for (String fieldKey : unusedFields) {
            String[] parts = fieldKey.split("\\.");
            if (parts.length >= 2) {
                String className = parts[0];
                String fieldName = parts[1];
                
                ProgramClass clazz = mapping.getProgramClass(className);
                if (clazz != null) {
                    clazz.removeField(fieldName);
                }
            }
        }
    }
    
    private void optimizeInstructions(JarMapping mapping, boolean removeNops, boolean optimizeConstants) {
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramMethod method : clazz.getMethods()) {
                InstructionTransformer transformer = new InstructionTransformer(method);
                
                if (removeNops) {
                    transformer.removeInstructions(insn -> insn.getOpcode() == 0);
                }
                
                if (optimizeConstants) {
                    optimizeConstantLoading(transformer);
                }
            }
        }
    }
    
    private void optimizeConstantLoading(InstructionTransformer transformer) {
        transformer.replaceInstructions(
            insn -> insn.getOpcode() == 16 && 
                   ((org.objectweb.asm.tree.IntInsnNode) insn).operand >= -1 && 
                   ((org.objectweb.asm.tree.IntInsnNode) insn).operand <= 5,
            insn -> {
                int value = ((org.objectweb.asm.tree.IntInsnNode) insn).operand;
                return new InsnNode(3 + value);
            }
        );
        
        transformer.replaceInstructions(
            insn -> insn.getOpcode() == 17 && 
                   ((org.objectweb.asm.tree.IntInsnNode) insn).operand >= -1 && 
                   ((org.objectweb.asm.tree.IntInsnNode) insn).operand <= 5,
            insn -> {
                int value = ((org.objectweb.asm.tree.IntInsnNode) insn).operand;
                return new InsnNode(3 + value);
            }
        );
    }
    
    @Override
    public int getPriority() {
        return 50;
    }
}