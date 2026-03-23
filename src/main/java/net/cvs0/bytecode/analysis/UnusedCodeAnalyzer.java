package net.cvs0.bytecode.analysis;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Utility class for analyzing unused code in a JarMapping.
 * Provides methods to find unused methods, fields, dead code, and method complexity.
 */
public class UnusedCodeAnalyzer {
    /**
     * Finds all unused methods in the given JarMapping.
     * @param mapping the JarMapping to analyze
     * @return set of unused method keys
     */
    public static Set<String> findUnusedMethods(JarMapping mapping) {
        Set<String> allMethods = new HashSet<>();
        Set<String> referencedMethods = new HashSet<>();

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramMethod method : clazz.getMethods()) {
                String methodKey = clazz.getName() + "." + method.getName() + method.getDescriptor();
                allMethods.add(methodKey);

                if (isEntryPoint(method)) {
                    referencedMethods.add(methodKey);
                }
            }
        }

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramMethod method : clazz.getMethods()) {
                referencedMethods.addAll(findMethodReferences(method));
            }
        }

        Set<String> unusedMethods = new HashSet<>(allMethods);
        unusedMethods.removeAll(referencedMethods);

        return unusedMethods;
    }
    /**
     * Finds all unused fields in the given JarMapping.
     * @param mapping the JarMapping to analyze
     * @return set of unused field keys
     */
    public static Set<String> findUnusedFields(JarMapping mapping) {
        Set<String> allFields = new HashSet<>();
        Set<String> referencedFields = new HashSet<>();

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramField field : clazz.getFields()) {
                String fieldKey = clazz.getName() + "." + field.getName();
                allFields.add(fieldKey);
            }
        }

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramMethod method : clazz.getMethods()) {
                referencedFields.addAll(findFieldReferences(method));
            }
        }

        Set<String> unusedFields = new HashSet<>(allFields);
        unusedFields.removeAll(referencedFields);

        return unusedFields;
    }
    /**
     * Returns true if the method is an entry point (public main, constructor, or getter/setter).
     * @param method the ProgramMethod
     * @return true if entry point
     */
    private static boolean isEntryPoint(ProgramMethod method) {
        return method.isPublic() && 
               ("main".equals(method.getName()) || 
                method.isConstructor() ||
                method.getName().startsWith("get") ||
                method.getName().startsWith("set") ||
                method.getName().startsWith("is"));
    }
    /**
     * Finds all method references in the given method's bytecode.
     * @param method the ProgramMethod
     * @return set of referenced method keys
     */
    private static Set<String> findMethodReferences(ProgramMethod method) {
        Set<String> references = new HashSet<>();

        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            for (AbstractInsnNode insn : method.getMethodNode().instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    String methodKey = methodInsn.owner + "." + methodInsn.name + methodInsn.desc;
                    references.add(methodKey);
                }
            }
        }

        return references;
    }
    /**
     * Finds all field references in the given method's bytecode.
     * @param method the ProgramMethod
     * @return set of referenced field keys
     */
    private static Set<String> findFieldReferences(ProgramMethod method) {
        Set<String> references = new HashSet<>();

        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            for (AbstractInsnNode insn : method.getMethodNode().instructions) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    String fieldKey = fieldInsn.owner + "." + fieldInsn.name;
                    references.add(fieldKey);
                }
            }
        }

        return references;
    }
    /**
     * Returns a map of method keys to their calculated complexity.
     * @param mapping the JarMapping
     * @return map of method keys to complexity values
     */
    public static Map<String, Integer> getMethodComplexity(JarMapping mapping) {
        Map<String, Integer> complexity = new HashMap<>();

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramMethod method : clazz.getMethods()) {
                String methodKey = clazz.getName() + "." + method.getName() + method.getDescriptor();
                complexity.put(methodKey, calculateMethodComplexity(method));
            }
        }

        return complexity;
    }
    /**
     * Calculates the cyclomatic complexity of a method.
     * @param method the ProgramMethod
     * @return the complexity value
     */
    private static int calculateMethodComplexity(ProgramMethod method) {
        if (method.getMethodNode() == null || method.getMethodNode().instructions == null) {
            return 0;
        }

        int complexity = 1;

        for (AbstractInsnNode insn : method.getMethodNode().instructions) {
            switch (insn.getOpcode()) {
                case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE,
                        Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE,
                        Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE,
                        Opcodes.GOTO, Opcodes.JSR, Opcodes.IFNULL, Opcodes.IFNONNULL -> complexity++;
                case Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH -> complexity++;
                default -> { }
            }
        }

        return complexity;
    }
    /**
     * Finds all methods with unreachable code (dead code) in the mapping.
     * @param mapping the JarMapping
     * @return set of method keys with dead code
     */
    public static Set<String> findDeadCode(JarMapping mapping) {
        Set<String> deadCode = new HashSet<>();

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramMethod method : clazz.getMethods()) {
                if (hasUnreachableCode(method)) {
                    String methodKey = clazz.getName() + "." + method.getName() + method.getDescriptor();
                    deadCode.add(methodKey);
                }
            }
        }

        return deadCode;
    }
    /**
     * Returns true if the method contains unreachable code.
     * @param method the ProgramMethod
     * @return true if unreachable code exists
     */
    private static boolean hasUnreachableCode(ProgramMethod method) {
        if (method.getMethodNode() == null || method.getMethodNode().instructions == null) {
            return false;
        }

        Set<AbstractInsnNode> reachable = new HashSet<>();
        Queue<AbstractInsnNode> queue = new LinkedList<>();

        AbstractInsnNode first = method.getMethodNode().instructions.getFirst();
        if (first != null) {
            queue.add(first);
            reachable.add(first);
        }

        while (!queue.isEmpty()) {
            AbstractInsnNode current = queue.poll();
            AbstractInsnNode next = current.getNext();

            if (next != null && !reachable.contains(next)) {
                reachable.add(next);
                queue.add(next);
            }

            if (current instanceof JumpInsnNode) {
                JumpInsnNode jumpInsn = (JumpInsnNode) current;
                if (!reachable.contains(jumpInsn.label)) {
                    reachable.add(jumpInsn.label);
                    queue.add(jumpInsn.label);
                }
            }
        }

        return reachable.size() < method.getMethodNode().instructions.size();
    }
    /**
     * Returns a list of the largest methods by instruction count.
     * @param mapping the JarMapping
     * @param limit the maximum number of methods to return
     * @return list of method keys
     */
    public static List<String> getLargestMethods(JarMapping mapping, int limit) {
        Map<String, Integer> methodSizes = new HashMap<>();

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramMethod method : clazz.getMethods()) {
                String methodKey = clazz.getName() + "." + method.getName() + method.getDescriptor();
                methodSizes.put(methodKey, method.getInstructionCount());
            }
        }

        return methodSizes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}