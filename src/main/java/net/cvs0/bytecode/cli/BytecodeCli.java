package net.cvs0.bytecode.cli;

import net.cvs0.bytecode.analysis.DependencyAnalyzer;
import net.cvs0.bytecode.analysis.JarStatistics;
import net.cvs0.bytecode.test.JarAnalyzer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Command-line entry point for JAR analysis (stats, dependencies, full report).
 */
@Command(
        name = "bytecode-processor",
        mixinStandardHelpOptions = true,
        version = "Bytecode Processor 1.1",
        description = "Analyze Java JAR files (dependencies, statistics, Graphviz export).",
        subcommands = {
                BytecodeCli.AnalyzeCommand.class,
                BytecodeCli.StatsCommand.class,
                BytecodeCli.DepsCommand.class
        })
public class BytecodeCli implements Runnable {

    @Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        int code = new CommandLine(new BytecodeCli()).execute(args);
        System.exit(code);
    }

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    @Command(name = "analyze", description = "Print a full analysis report (same as JarAnalyzer).")
    static class AnalyzeCommand implements Callable<Integer> {
        @Parameters(paramLabel = "JAR", description = "Path to the JAR file", arity = "1")
        Path jar;

        @Override
        public Integer call() {
            if (!Files.isRegularFile(jar)) {
                System.err.println("JAR not found: " + jar);
                return 2;
            }
            JarAnalyzer.analyzeJar(jar);
            return 0;
        }
    }

    @Command(name = "stats", description = "Print aggregate class/member/resource counts.")
    static class StatsCommand implements Callable<Integer> {
        @Parameters(paramLabel = "JAR", arity = "1")
        Path jar;

        @Option(names = "--json", description = "Print as a single line of JSON")
        boolean json;

        @Override
        public Integer call() throws Exception {
            if (!Files.isRegularFile(jar)) {
                System.err.println("JAR not found: " + jar);
                return 2;
            }
            var mapping = net.cvs0.bytecode.JarMapping.fromJar(jar);
            JarStatistics s = JarStatistics.from(mapping);
            if (json) {
                System.out.printf(
                        "{\"programClasses\":%d,\"libraryClasses\":%d,\"resources\":%d,\"interfaces\":%d,\"abstractClasses\":%d,\"finalClasses\":%d,\"publicClasses\":%d,\"methods\":%d,\"fields\":%d}%n",
                        s.getProgramClassCount(),
                        s.getLibraryClassCount(),
                        s.getResourceCount(),
                        s.getInterfaceCount(),
                        s.getAbstractClassCount(),
                        s.getFinalClassCount(),
                        s.getPublicClassCount(),
                        s.getTotalMethods(),
                        s.getTotalFields());
            } else {
                System.out.println("Program classes: " + s.getProgramClassCount());
                System.out.println("Library classes: " + s.getLibraryClassCount());
                System.out.println("Resources: " + s.getResourceCount());
                System.out.println("Interfaces: " + s.getInterfaceCount());
                System.out.println("Abstract classes: " + s.getAbstractClassCount());
                System.out.println("Final classes: " + s.getFinalClassCount());
                System.out.println("Public classes: " + s.getPublicClassCount());
                System.out.println("Methods: " + s.getTotalMethods());
                System.out.println("Fields: " + s.getTotalFields());
            }
            return 0;
        }
    }

    @Command(name = "deps", description = "Dependency summary and optional Graphviz DOT export.")
    static class DepsCommand implements Callable<Integer> {
        @Parameters(paramLabel = "JAR", arity = "1")
        Path jar;

        @Option(names = "--dot", description = "Write forward dependency graph as Graphviz DOT")
        Path dotOut;

        @Option(names = "--class", description = "List program classes that depend on this internal name (e.g. com/foo/Bar)")
        String className;

        @Override
        public Integer call() throws Exception {
            if (!Files.isRegularFile(jar)) {
                System.err.println("JAR not found: " + jar);
                return 2;
            }
            var mapping = net.cvs0.bytecode.JarMapping.fromJar(jar);
            Map<String, Set<String>> graph = DependencyAnalyzer.buildDependencyGraph(mapping);
            System.out.println("Dependency nodes (program classes): " + graph.size());
            Set<String> cycles = DependencyAnalyzer.findCircularDependencies(mapping);
            System.out.println("Classes involved in cycles: " + cycles.size());
            if (className != null && !className.isBlank()) {
                Set<String> dependents = DependencyAnalyzer.findDependents(mapping, className);
                System.out.println("Dependents of " + className + ": " + dependents.size());
                dependents.stream().sorted().limit(50).forEach(d -> System.out.println("  " + d));
                if (dependents.size() > 50) {
                    System.out.println("  ... (" + (dependents.size() - 50) + " more)");
                }
            }
            if (dotOut != null) {
                String dot = DependencyAnalyzer.toDotFormat(graph, "jar");
                Files.writeString(dotOut, dot);
                System.out.println("Wrote DOT to " + dotOut.toAbsolutePath());
            }
            return 0;
        }
    }
}
