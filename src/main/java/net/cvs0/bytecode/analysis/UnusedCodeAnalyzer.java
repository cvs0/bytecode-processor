package net.cvs0.bytecode.analysis;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.tree.*;

import java.util.*;

public class UnusedCodeAnalyzer {
    
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
    
    private static boolean isEntryPoint(ProgramMethod method) {
        return method.isPublic() && 
               ("main".equals(method.getName()) || 
                method.isConstructor() ||
                method.getName().startsWith("get") ||
                method.getName().startsWith("set") ||
                method.getName().startsWith("is"));
    }
    
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
    
    private static int calculateMethodComplexity(ProgramMethod method) {
        if (method.getMethodNode() == null || method.getMethodNode().instructions == null) {
            return 0;
        }
        
        int complexity = 1;
        
        for (AbstractInsnNode insn : method.getMethodNode().instructions) {
            switch (insn.getOpcode()) {
                case 153: case 154: case 155: case 156: case 157: case 158:
                case 159: case 160: case 161: case 162: case 163: case 164:
                case 165: case 166: case 167: case 168:
                case 170: case 171:
                case 198: case 199:
                    complexity++;
                    break;
            }
        }
        
        return complexity;
    }
    
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