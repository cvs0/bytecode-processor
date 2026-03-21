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

/**
 * Plugin that performs bytecode optimizations on a JarMapping.
 * Supports removing unused methods/fields, removing NOPs, and optimizing constant loading.
 */
public class OptimizationPlugin extends AbstractPlugin {
    /**
     * Constructs an OptimizationPlugin with default metadata.
     */
    public OptimizationPlugin() {
        super("Optimization Plugin", "1.0.0", "Performs various bytecode optimizations");
    }

    /**
     * Processes the JarMapping and applies optimizations as configured.
     *
     * @param mapping the JarMapping to process
     */
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

    /**
     * Removes unused methods from the JarMapping.
     *
     * @param mapping the JarMapping
     */
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

    /**
     * Removes unused fields from the JarMapping.
     *
     * @param mapping the JarMapping
     */
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

    /**
     * Optimizes instructions in all methods of all classes in the JarMapping.
     *
     * @param mapping         the JarMapping
     * @param removeNops      true to remove NOP instructions
     * @param optimizeConstants true to optimize constant loading
     */
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

    /**
     * Optimizes constant loading instructions in a method.
     *
     * @param transformer the InstructionTransformer
     */
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

    /**
     * Returns the plugin priority (higher runs first).
     *
     * @return the plugin priority
     */
    @Override
    public int getPriority() {
        return 50;
    }
}