package io.github.cvs0.bytecode.analysis;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Analyzes dependencies between classes in a bytecode program.
 *
 * <p>This utility class provides comprehensive dependency analysis capabilities including:
 * <ul>
 *   <li>Class-level dependency detection</li>
 *   <li>Method-level dependency analysis</li>
 *   <li>Dependency graph construction</li>
 *   <li>Topological sorting for build order</li>
 *   <li>Circular dependency detection</li>
 *   <li>Unused class identification</li>
 * </ul>
 *
 * <p>Dependencies are extracted from various sources including inheritance relationships,
 * interface implementations, method signatures, and bytecode instructions.
 */
public class DependencyAnalyzer {
    /**
     * Finds all dependencies for a given class.
     *
     * <p>Analyzes the class to identify all types it depends on, including:
     * <ul>
     *   <li>Superclass (if any)</li>
     *   <li>Implemented interfaces</li>
     *   <li>Types referenced in method bytecode</li>
     * </ul>
     *
     * @param clazz the class to analyze
     * @return set of class names that this class depends on
     */
    public static Set<String> findClassDependencies(ProgramClass clazz) {
        Set<String> dependencies = new HashSet<>();

        if (clazz.getSuperName() != null) {
            dependencies.add(clazz.getSuperName());
        }

        dependencies.addAll(clazz.getInterfaces());

        for (ProgramMethod method : clazz.getMethods()) {
            dependencies.addAll(findMethodDependencies(method));
        }

        return dependencies;
    }

    /**
     * Extracts dependencies from a method's bytecode instructions.
     *
     * <p>Analyzes all instructions in the method to find references to other classes
     * through field access, method calls, type operations, and constant loading.
     *
     * @param method the method to analyze
     * @return set of class names referenced by this method
     */
    public static Set<String> findMethodDependencies(ProgramMethod method) {
        Set<String> dependencies = new HashSet<>();

        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            for (AbstractInsnNode insn : method.getMethodNode().instructions) {
                dependencies.addAll(extractDependenciesFromInstruction(insn));
            }
        }

        return dependencies;
    }

    /**
     * Extracts class dependencies from a single bytecode instruction.
     *
     * @param insn the instruction to analyze
     * @return set of class names referenced by this instruction
     */
    private static Set<String> extractDependenciesFromInstruction(AbstractInsnNode insn) {
        Set<String> dependencies = new HashSet<>();

        switch (insn.getType()) {
            case AbstractInsnNode.TYPE_INSN:
                TypeInsnNode typeInsn = (TypeInsnNode) insn;
                dependencies.add(typeInsn.desc);
                break;

            case AbstractInsnNode.FIELD_INSN:
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                dependencies.add(fieldInsn.owner);
                break;

            case AbstractInsnNode.METHOD_INSN:
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                dependencies.add(methodInsn.owner);
                break;

            case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                InvokeDynamicInsnNode invokeDynamicInsn = (InvokeDynamicInsnNode) insn;
                dependencies.addAll(extractTypesFromDescriptor(invokeDynamicInsn.desc));
                break;

            case AbstractInsnNode.LDC_INSN:
                LdcInsnNode ldcInsn = (LdcInsnNode) insn;
                if (ldcInsn.cst instanceof Type type) {
                    dependencies.add(type.getInternalName());
                }
                break;
                
            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                MultiANewArrayInsnNode multiArrayInsn = (MultiANewArrayInsnNode) insn;
                dependencies.add(multiArrayInsn.desc);
                break;
        }

        return dependencies;
    }

    /**
     * Extracts class names from a method descriptor.
     *
     * @param descriptor the method descriptor (e.g., "(Ljava/lang/String;)V")
     * @return set of class names found in the descriptor
     */
    private static Set<String> extractTypesFromDescriptor(String descriptor) {
        Set<String> types = new HashSet<>();
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        Type returnType = Type.getReturnType(descriptor);

        for (Type type : argumentTypes) {
            if (type.getSort() == Type.OBJECT) {
                types.add(type.getInternalName());
            }
        }

        if (returnType.getSort() == Type.OBJECT) {
            types.add(returnType.getInternalName());
        }

        return types;
    }

    /**
     * Builds a complete dependency graph for all classes in the mapping.
     *
     * <p>Creates a map where each class name is associated with the set of classes
     * it depends on. This graph can be used for various analyses including
     * topological sorting and circular dependency detection.
     *
     * @param mapping the jar mapping containing all classes to analyze
     * @return map from class names to their dependencies
     */
    public static Map<String, Set<String>> buildDependencyGraph(JarMapping mapping) {
        Map<String, Set<String>> dependencyGraph = new HashMap<>();

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            Set<String> dependencies = findClassDependencies(clazz);
            dependencyGraph.put(clazz.getName(), dependencies);
        }

        return dependencyGraph;
    }

    /**
     * Identifies classes that are not referenced by any other class.
     *
     * <p>Finds classes that have no incoming dependencies, making them potential
     * candidates for removal during dead code elimination. Note that entry points
     * and classes loaded via reflection may appear unused but still be necessary.
     *
     * @param mapping the jar mapping to analyze
     * @return set of class names that are not referenced by other classes
     */
    public static Set<String> findUnusedClasses(JarMapping mapping) {
        Map<String, Set<String>> dependencyGraph = buildDependencyGraph(mapping);
        Set<String> allClasses = new HashSet<>();
        Set<String> referencedClasses = new HashSet<>();

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            allClasses.add(clazz.getName());
        }

        for (Set<String> dependencies : dependencyGraph.values()) {
            referencedClasses.addAll(dependencies);
        }

        Set<String> unusedClasses = new HashSet<>(allClasses);
        unusedClasses.removeAll(referencedClasses);

        return unusedClasses;
    }

    /**
     * Detects circular dependencies between classes.
     *
     * <p>Uses depth-first search to identify cycles in the dependency graph.
     * Circular dependencies can cause issues during class loading and should
     * generally be avoided in well-designed systems.
     *
     * @param mapping the jar mapping to analyze
     * @return set of class names involved in circular dependencies
     */
    public static Set<String> findCircularDependencies(JarMapping mapping) {
        Map<String, Set<String>> dependencyGraph = buildDependencyGraph(mapping);
        Set<String> circularDependencies = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String className : dependencyGraph.keySet()) {
            if (!visited.contains(className)) {
                findCircularDependenciesRecursive(className, dependencyGraph, visited, recursionStack, circularDependencies);
            }
        }

        return circularDependencies;
    }

    /**
     * Recursive helper method for circular dependency detection.
     *
     * @param className current class being analyzed
     * @param dependencyGraph the complete dependency graph
     * @param visited set of already visited classes
     * @param recursionStack current path in the DFS traversal
     * @param circularDependencies accumulator for classes in cycles
     */
    private static void findCircularDependenciesRecursive(String className,
                                                         Map<String, Set<String>> dependencyGraph,
                                                         Set<String> visited,
                                                         Set<String> recursionStack,
                                                         Set<String> circularDependencies) {
        visited.add(className);
        recursionStack.add(className);

        Set<String> dependencies = dependencyGraph.get(className);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                if (!visited.contains(dependency)) {
                    findCircularDependenciesRecursive(dependency, dependencyGraph, visited, recursionStack, circularDependencies);
                } else if (recursionStack.contains(dependency)) {
                    circularDependencies.add(className);
                    circularDependencies.add(dependency);
                }
            }
        }

        recursionStack.remove(className);
    }

    /**
     * Computes a topological ordering of classes based on their dependencies.
     *
     * <p>Returns classes in an order such that dependencies come before the classes
     * that depend on them. This is useful for determining build order, initialization
     * sequence, or processing order where dependencies must be handled first.
     *
     * <p>Uses Kahn's algorithm for topological sorting, which handles cycles gracefully
     * by excluding them from the final ordering.
     *
     * @param mapping the jar mapping containing classes to order
     * @return list of class names in topological order (dependencies first)
     */
    public static List<String> getTopologicalOrder(JarMapping mapping) {
        Map<String, Set<String>> dependencyGraph = buildDependencyGraph(mapping);

        Set<String> internalClasses = new HashSet<>();
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            internalClasses.add(clazz.getName());
        }

        Map<String, Integer> inDegree = new HashMap<>();
        for (String className : internalClasses) {
            inDegree.put(className, 0);
        }

        for (String className : internalClasses) {
            Set<String> dependencies = dependencyGraph.get(className);
            if (dependencies != null) {
                for (String dependency : dependencies) {
                    if (internalClasses.contains(dependency)) {
                        inDegree.put(className, inDegree.get(className) + 1);
                    }
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (String className : internalClasses) {
            if (inDegree.get(className) == 0) {
                queue.offer(className);
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);

            for (String className : internalClasses) {
                Set<String> dependencies = dependencyGraph.get(className);
                if (dependencies != null && dependencies.contains(current)) {
                    inDegree.put(className, inDegree.get(className) - 1);
                    if (inDegree.get(className) == 0) {
                        queue.offer(className);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Builds a reverse dependency graph: for each class, the set of program classes that depend on it.
     *
     * @param mapping jar mapping (only {@link ProgramClass} names appear as keys)
     * @return map from depended-on class name to set of dependent internal class names
     */
    public static Map<String, Set<String>> buildReverseDependencyGraph(JarMapping mapping) {
        Map<String, Set<String>> forward = buildDependencyGraph(mapping);
        Map<String, Set<String>> reverse = new HashMap<>();
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            reverse.putIfAbsent(clazz.getName(), new HashSet<>());
        }
        for (Map.Entry<String, Set<String>> entry : forward.entrySet()) {
            String dependent = entry.getKey();
            for (String dependency : entry.getValue()) {
                reverse.computeIfAbsent(dependency, k -> new HashSet<>()).add(dependent);
            }
        }
        return reverse;
    }

    /**
     * Returns program classes that directly depend on the given internal class name.
     *
     * @param mapping   jar mapping
     * @param className internal name of the depended-on type
     * @return dependents that appear as program classes in the mapping (may be empty)
     */
    public static Set<String> findDependents(JarMapping mapping, String className) {
        Set<String> dependents = new HashSet<>();
        Map<String, Set<String>> graph = buildDependencyGraph(mapping);
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getValue().contains(className)) {
                dependents.add(entry.getKey());
            }
        }
        return dependents;
    }

    /**
     * Exports a forward dependency graph as Graphviz DOT (digraph). Node IDs are quoted and backslash-escaped.
     *
     * @param dependencyGraph map from class name to dependencies (e.g. from {@link #buildDependencyGraph})
     * @param graphId         identifier for the digraph statement
     * @return DOT source text
     */
    public static String toDotFormat(Map<String, Set<String>> dependencyGraph, String graphId) {
        String id = graphId == null || graphId.isBlank() ? "dependencies" : graphId.replaceAll("[^a-zA-Z0-9_]", "_");
        StringBuilder sb = new StringBuilder();
        sb.append("digraph ").append(id).append(" {\n");
        sb.append("  rankdir=LR;\n");
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            String from = dotEscape(entry.getKey());
            for (String to : entry.getValue()) {
                sb.append("  \"").append(from).append("\" -> \"").append(dotEscape(to)).append("\";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String dotEscape(String internalName) {
        return internalName.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
