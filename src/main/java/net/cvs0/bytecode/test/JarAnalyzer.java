package net.cvs0.bytecode.test;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.analysis.DependencyAnalyzer;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramMethod;

import java.io.File;
import java.util.*;

/**
 * Standalone utility for testing the bytecode processor on real JAR files.
 * Run this class with a JAR file path as argument to analyze it.
 */
public class JarAnalyzer {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java JarAnalyzer <jar-file-path>");
            System.out.println("Example: java JarAnalyzer myapp.jar");
            return;
        }
        
        String jarPath = args[0];
        File jarFile = new File(jarPath);
        
        if (!jarFile.exists()) {
            System.err.println("JAR file not found: " + jarPath);
            return;
        }
        
        System.out.println("=".repeat(60));
        System.out.println("BYTECODE PROCESSOR - JAR ANALYSIS");
        System.out.println("=".repeat(60));
        System.out.println("Analyzing: " + jarFile.getAbsolutePath());
        System.out.println();
        
        try {
            analyzeJar(jarPath);
        } catch (Exception e) {
            System.err.println("Error analyzing JAR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void analyzeJar(String jarPath) {
        long startTime = System.currentTimeMillis();
        
        // Load the JAR
        System.out.println("📦 Loading JAR file...");
        JarMapping mapping = new JarMapping(jarPath);
        
        Collection<ProgramClass> classes = mapping.getProgramClasses();
        System.out.println("✅ Loaded " + classes.size() + " classes");
        System.out.println();
        
        // Basic statistics
        printBasicStatistics(mapping);
        
        // Dependency analysis
        printDependencyAnalysis(mapping);
        
        // Class details
        printClassDetails(mapping);
        
        // Performance info
        long endTime = System.currentTimeMillis();
        System.out.println("⏱️  Analysis completed in " + (endTime - startTime) + "ms");
    }
    
    private static void printBasicStatistics(JarMapping mapping) {
        System.out.println("📊 BASIC STATISTICS");
        System.out.println("-".repeat(40));
        
        Collection<ProgramClass> classes = mapping.getProgramClasses();
        
        int totalClasses = classes.size();
        int interfaces = 0;
        int abstractClasses = 0;
        int finalClasses = 0;
        int publicClasses = 0;
        int totalMethods = 0;
        int totalFields = 0;
        
        for (ProgramClass clazz : classes) {
            if (clazz.isInterface()) interfaces++;
            if (clazz.isAbstract()) abstractClasses++;
            if (clazz.isFinal()) finalClasses++;
            if (clazz.isPublic()) publicClasses++;
            
            totalMethods += clazz.getMethods().size();
            totalFields += clazz.getFields().size();
        }
        
        System.out.println("Total Classes: " + totalClasses);
        System.out.println("  - Interfaces: " + interfaces);
        System.out.println("  - Abstract: " + abstractClasses);
        System.out.println("  - Final: " + finalClasses);
        System.out.println("  - Public: " + publicClasses);
        System.out.println("Total Methods: " + totalMethods);
        System.out.println("Total Fields: " + totalFields);
        System.out.println();
    }
    
    private static void printDependencyAnalysis(JarMapping mapping) {
        System.out.println("🔗 DEPENDENCY ANALYSIS");
        System.out.println("-".repeat(40));
        
        try {
            // Build dependency graph
            Map<String, Set<String>> dependencies = DependencyAnalyzer.buildDependencyGraph(mapping);
            System.out.println("✅ Built dependency graph with " + dependencies.size() + " nodes");
            
            // Find circular dependencies
            Set<String> circularDeps = DependencyAnalyzer.findCircularDependencies(mapping);
            if (circularDeps.isEmpty()) {
                System.out.println("✅ No circular dependencies found");
            } else {
                System.out.println("⚠️  Found " + circularDeps.size() + " classes in circular dependencies:");
                circularDeps.stream().limit(10).forEach(dep -> System.out.println("   - " + dep));
                if (circularDeps.size() > 10) {
                    System.out.println("   ... and " + (circularDeps.size() - 10) + " more");
                }
            }
            
            // Get topological order
            List<String> topologicalOrder = DependencyAnalyzer.getTopologicalOrder(mapping);
            System.out.println("✅ Computed topological order for " + topologicalOrder.size() + " classes");
            
            // Find unused classes
            Set<String> unusedClasses = DependencyAnalyzer.findUnusedClasses(mapping);
            if (unusedClasses.isEmpty()) {
                System.out.println("✅ No unused classes found");
            } else {
                System.out.println("📋 Found " + unusedClasses.size() + " potentially unused classes:");
                unusedClasses.stream().limit(5).forEach(unused -> System.out.println("   - " + unused));
                if (unusedClasses.size() > 5) {
                    System.out.println("   ... and " + (unusedClasses.size() - 5) + " more");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error during dependency analysis: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void printClassDetails(JarMapping mapping) {
        System.out.println("🏗️  CLASS DETAILS");
        System.out.println("-".repeat(40));
        
        Collection<ProgramClass> classes = mapping.getProgramClasses();
        
        // Find most complex classes (by method count)
        List<ProgramClass> sortedByMethods = classes.stream()
                .sorted((a, b) -> Integer.compare(b.getMethods().size(), a.getMethods().size()))
                .limit(5)
                .toList();
        
        System.out.println("Top 5 classes by method count:");
        for (int i = 0; i < sortedByMethods.size(); i++) {
            ProgramClass clazz = sortedByMethods.get(i);
            System.out.println("  " + (i + 1) + ". " + clazz.getSimpleName() + 
                             " (" + clazz.getMethods().size() + " methods, " + 
                             clazz.getFields().size() + " fields)");
        }
        System.out.println();
        
        // Find classes with most dependencies
        System.out.println("Sample class analysis:");
        ProgramClass sampleClass = classes.stream()
                .filter(c -> !c.getName().startsWith("java/"))
                .findFirst()
                .orElse(classes.iterator().next());
        
        if (sampleClass != null) {
            System.out.println("Class: " + sampleClass.getName());
            System.out.println("  Package: " + sampleClass.getPackageName());
            System.out.println("  Simple Name: " + sampleClass.getSimpleName());
            System.out.println("  Super Class: " + sampleClass.getSuperName());
            System.out.println("  Interfaces: " + sampleClass.getInterfaces().size());
            System.out.println("  Access Flags: " + getAccessFlagsString(sampleClass));
            System.out.println("  Methods: " + sampleClass.getMethods().size());
            System.out.println("  Fields: " + sampleClass.getFields().size());
            
            // Show some methods
            if (!sampleClass.getMethods().isEmpty()) {
                System.out.println("  Sample methods:");
                sampleClass.getMethods().stream()
                        .limit(3)
                        .forEach(method -> {
                            System.out.println("    - " + method.getName() + method.getDescriptor() + 
                                             " (" + method.getInstructionCount() + " instructions)");
                        });
            }
            
            // Try to find dependencies for this class
            try {
                Set<String> classDeps = DependencyAnalyzer.findClassDependencies(sampleClass);
                System.out.println("  Dependencies: " + classDeps.size());
                if (!classDeps.isEmpty()) {
                    System.out.println("    Sample dependencies:");
                    classDeps.stream().limit(3).forEach(dep -> System.out.println("      - " + dep));
                }
            } catch (Exception e) {
                System.out.println("  Dependencies: Error analyzing (" + e.getMessage() + ")");
            }
        }
        
        System.out.println();
    }
    
    private static String getAccessFlagsString(ProgramClass clazz) {
        List<String> flags = new ArrayList<>();
        if (clazz.isPublic()) flags.add("public");
        if (clazz.isPrivate()) flags.add("private");
        if (clazz.isProtected()) flags.add("protected");
        if (clazz.isStatic()) flags.add("static");
        if (clazz.isFinal()) flags.add("final");
        if (clazz.isAbstract()) flags.add("abstract");
        if (clazz.isInterface()) flags.add("interface");
        if (clazz.isEnum()) flags.add("enum");
        if (clazz.isAnnotation()) flags.add("annotation");
        
        return flags.isEmpty() ? "package-private" : String.join(", ", flags);
    }
}