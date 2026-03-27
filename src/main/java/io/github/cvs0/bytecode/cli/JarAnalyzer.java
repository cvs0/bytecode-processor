package io.github.cvs0.bytecode.cli;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.analysis.DependencyAnalyzer;
import io.github.cvs0.bytecode.analysis.JarStatistics;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.log.BPLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Standalone utility for analyzing JAR files with the bytecode processor.
 * Run this class with a JAR file path as argument to analyze it.
 */
public class JarAnalyzer {

    private static final BPLogger LOG = BPLogger.of(JarAnalyzer.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            LOG.info("Usage: java JarAnalyzer <jar-file-path>");
            LOG.info("Example: java JarAnalyzer myapp.jar");
            return;
        }

        String jarPath = args[0];
        File jarFile = new File(jarPath);

        if (!jarFile.exists()) {
            LOG.error("JAR file not found: %s", jarPath);
            return;
        }

        LOG.info("=".repeat(60));
        LOG.info("BYTECODE PROCESSOR - JAR ANALYSIS");
        LOG.info("=".repeat(60));
        LOG.info("Analyzing: %s", jarFile.getAbsolutePath());
        LOG.info("");

        try {
            analyzeJar(Path.of(jarPath));
        } catch (Exception e) {
            LOG.error("Error analyzing JAR: " + e.getMessage(), e);
        }
    }

    /**
     * Runs the full console report for a JAR (used by {@link BytecodeCli}).
     */
    public static void analyzeJar(Path jarPath) {
        long startTime = System.currentTimeMillis();
        // Load the JAR
        LOG.info("\uD83D\uDCE6 Loading JAR file...");
        JarMapping mapping;
        try {
            mapping = JarMapping.fromJar(jarPath);
        } catch (IOException e) {
            LOG.error("Failed to load JAR: " + e.getMessage(), e);
            return;
        }
        Collection<ProgramClass> classes = mapping.getProgramClasses();
        int mod = mapping.getModuleInfoCount();
        int pkg = mapping.getPackageInfoCount();
        LOG.info("✅ Loaded %d program classes%s%s",
                classes.size(),
                mod > 0 ? ", " + mod + " module descriptor(s)" : "",
                pkg > 0 ? ", " + pkg + " package-info" : "");
        LOG.info("");

        // Basic statistics
        printBasicStatistics(mapping);

        // Dependency analysis
        printDependencyAnalysis(mapping);

        // Class details
        printClassDetails(mapping);

        // Performance info
        long endTime = System.currentTimeMillis();
        LOG.info("⏱️  Analysis completed in %dms", endTime - startTime);
    }

    private static void printBasicStatistics(JarMapping mapping) {
        LOG.info("\uD83D\uDCCA BASIC STATISTICS");
        LOG.info("-".repeat(40));

        JarStatistics s = JarStatistics.from(mapping);
        LOG.info("Total class models: %d", s.getTotalModeledClassCount());
        LOG.info("  - Application: %d", s.getApplicationClassCount());
        LOG.info("  - Embedded libraries: %d", s.getEmbeddedLibraryClassCount());
        LOG.info("  - Interfaces: %d", s.getInterfaceCount());
        LOG.info("  - Abstract: %d", s.getAbstractClassCount());
        LOG.info("  - Final: %d", s.getFinalClassCount());
        LOG.info("  - Public: %d", s.getPublicClassCount());
        LOG.info("Total Methods: %d", s.getTotalMethods());
        LOG.info("Total Fields: %d", s.getTotalFields());
        if (s.getLibraryClassCount() > 0 || s.getResourceCount() > 0) {
            LOG.info("Library classes: %d", s.getLibraryClassCount());
            LOG.info("Resources: %d", s.getResourceCount());
        }
        if (s.getModuleDescriptorCount() > 0 || s.getPackageInfoCount() > 0) {
            LOG.info("Module descriptors: %d", s.getModuleDescriptorCount());
            LOG.info("Package infos: %d", s.getPackageInfoCount());
        }
        LOG.info("");
    }

    private static void printDependencyAnalysis(JarMapping mapping) {
        LOG.info("\uD83D\uDD17 DEPENDENCY ANALYSIS");
        LOG.info("-".repeat(40));

        try {
            // Build dependency graph
            Map<String, Set<String>> dependencies = DependencyAnalyzer.buildDependencyGraph(mapping);
            LOG.info("✅ Built dependency graph with %d nodes", dependencies.size());

            // Find circular dependencies
            Set<String> circularDeps = DependencyAnalyzer.findCircularDependencies(mapping);
            if (circularDeps.isEmpty()) {
                LOG.info("✅ No circular dependencies found");
            } else {
                LOG.warn("⚠️  Found %d classes in circular dependencies:", circularDeps.size());
                circularDeps.stream().limit(10).forEach(dep -> LOG.warn("   - %s", dep));
                if (circularDeps.size() > 10) {
                    LOG.warn("   ... and %d more", circularDeps.size() - 10);
                }
            }

            // Get topological order
            List<String> topologicalOrder = DependencyAnalyzer.getTopologicalOrder(mapping);
            LOG.info("✅ Computed topological order for %d classes", topologicalOrder.size());

            // Find unused classes
            Set<String> unusedClasses = DependencyAnalyzer.findUnusedClasses(mapping);
            if (unusedClasses.isEmpty()) {
                LOG.info("✅ No unused classes found");
            } else {
                LOG.info("\uD83D\uDCCB Found %d potentially unused classes:", unusedClasses.size());
                unusedClasses.stream().limit(5).forEach(unused -> LOG.info("   - %s", unused));
                if (unusedClasses.size() > 5) {
                    LOG.info("   ... and %d more", unusedClasses.size() - 5);
                }
            }

        } catch (Exception e) {
            LOG.error("Error during dependency analysis: %s", e.getMessage());
        }

        LOG.info("");
    }

    private static void printClassDetails(JarMapping mapping) {
        LOG.info("\uD83C\uDFD7️  CLASS DETAILS");
        LOG.info("-".repeat(40));

        Collection<ProgramClass> classes = mapping.getProgramClasses();

        List<ProgramClass> sortedByMethods = classes.stream()
                .sorted((a, b) -> Integer.compare(b.getMethods().size(), a.getMethods().size()))
                .limit(5)
                .toList();

        LOG.info("Top 5 classes by method count:");
        for (int i = 0; i < sortedByMethods.size(); i++) {
            ProgramClass clazz = sortedByMethods.get(i);
            LOG.info("  %d. %s (%d methods, %d fields)",
                    i + 1, clazz.getSimpleName(),
                    clazz.getMethods().size(), clazz.getFields().size());
        }
        LOG.info("");

        if (classes.isEmpty()) {
            LOG.info("No classes found in the JAR. Skipping sample class analysis.");
            LOG.info("");
            return;
        }

        LOG.info("Sample class analysis:");
        ProgramClass sampleClass = classes.stream()
                .filter(c -> !c.getName().startsWith("java/"))
                .findFirst()
                .orElse(classes.iterator().next());
        if (sampleClass != null) {
            LOG.info("Class: %s", sampleClass.getName());
            LOG.info("  Package: %s", sampleClass.getPackageName());
            LOG.info("  Simple Name: %s", sampleClass.getSimpleName());
            LOG.info("  Super Class: %s", sampleClass.getSuperName());
            LOG.info("  Interfaces: %d", sampleClass.getInterfaces().size());
            LOG.info("  Access Flags: %s", getAccessFlagsString(sampleClass));
            LOG.info("  Methods: %d", sampleClass.getMethods().size());
            LOG.info("  Fields: %d", sampleClass.getFields().size());

            if (!sampleClass.getMethods().isEmpty()) {
                LOG.info("  Sample methods:");
                sampleClass.getMethods().stream()
                        .limit(3)
                        .forEach(method -> {
                            LOG.info("    - %s%s (%d instructions)",
                                    method.getName(), method.getDescriptor(),
                                    method.getInstructionCount());
                        });
            }

            try {
                Set<String> classDeps = DependencyAnalyzer.findClassDependencies(sampleClass);
                LOG.info("  Dependencies: %d", classDeps.size());
                if (!classDeps.isEmpty()) {
                    LOG.info("    Sample dependencies:");
                    classDeps.stream().limit(3).forEach(dep -> LOG.info("      - %s", dep));
                }
            } catch (Exception e) {
                LOG.info("  Dependencies: Error analyzing (%s)", e.getMessage());
            }
        }

        LOG.info("");
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
