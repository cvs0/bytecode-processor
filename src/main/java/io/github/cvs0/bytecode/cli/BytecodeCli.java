package io.github.cvs0.bytecode.cli;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.analysis.DependencyAnalyzer;
import io.github.cvs0.bytecode.analysis.JarStatistics;
import io.github.cvs0.bytecode.log.BPLogger;
import io.github.cvs0.bytecode.plugin.ConfigurablePlugin;
import io.github.cvs0.bytecode.plugin.Plugin;
import io.github.cvs0.bytecode.util.BytecodeNames;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Command-line entry point for JAR analysis (stats, dependencies, full report).
 */
@Command(
        name = "bytecode-processor",
        mixinStandardHelpOptions = true,
        versionProvider = BytecodeCli.CliVersionProvider.class,
        description = "Analyze and transform Java JAR files (dependencies, statistics, plugins, Graphviz export).",
        subcommands = {
                BytecodeCli.AnalyzeCommand.class,
                BytecodeCli.StatsCommand.class,
                BytecodeCli.DepsCommand.class,
                BytecodeCli.TransformCommand.class
        })
public class BytecodeCli implements Runnable {

    private static final BPLogger LOG = BPLogger.of(BytecodeCli.class);

    /**
     * Resolves {@code Implementation-Version} from the JAR manifest (set by Maven for packaged builds).
     */
    static final class CliVersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            Package p = BytecodeCli.class.getPackage();
            String v = p != null ? p.getImplementationVersion() : null;
            return new String[] {"Bytecode Processor " + (v != null ? v : "dev-SNAPSHOT")};
        }
    }

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
                LOG.error("JAR not found: %s", jar);
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
                LOG.error("JAR not found: %s", jar);
                return 2;
            }
            var mapping = io.github.cvs0.bytecode.JarMapping.fromJar(jar);
            JarStatistics s = JarStatistics.from(mapping);
            if (json) {
                LOG.info("{\"applicationClasses\":%d,\"embeddedLibraryClasses\":%d,\"totalClasses\":%d,\"libraryClasses\":%d,\"moduleDescriptors\":%d,\"packageInfos\":%d,\"resources\":%d,\"interfaces\":%d,\"abstractClasses\":%d,\"finalClasses\":%d,\"publicClasses\":%d,\"methods\":%d,\"fields\":%d}",
                        s.getApplicationClassCount(),
                        s.getEmbeddedLibraryClassCount(),
                        s.getTotalModeledClassCount(),
                        s.getLibraryClassCount(),
                        s.getModuleDescriptorCount(),
                        s.getPackageInfoCount(),
                        s.getResourceCount(),
                        s.getInterfaceCount(),
                        s.getAbstractClassCount(),
                        s.getFinalClassCount(),
                        s.getPublicClassCount(),
                        s.getTotalMethods(),
                        s.getTotalFields());
            } else {
                LOG.info("Application classes: %d", s.getApplicationClassCount());
                LOG.info("Embedded library classes: %d", s.getEmbeddedLibraryClassCount());
                LOG.info("Total class models: %d", s.getTotalModeledClassCount());
                LOG.info("Module descriptors: %d", s.getModuleDescriptorCount());
                LOG.info("Package infos: %d", s.getPackageInfoCount());
                LOG.info("Resources: %d", s.getResourceCount());
                LOG.info("Interfaces: %d", s.getInterfaceCount());
                LOG.info("Abstract classes: %d", s.getAbstractClassCount());
                LOG.info("Final classes: %d", s.getFinalClassCount());
                LOG.info("Public classes: %d", s.getPublicClassCount());
                LOG.info("Methods: %d", s.getTotalMethods());
                LOG.info("Fields: %d", s.getTotalFields());
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

        @Option(
                names = "--class",
                description =
                        "List program classes that depend on this type (internal form e.g. com/foo/Bar, or binary name com.foo.Bar)")
        String className;

        @Override
        public Integer call() throws Exception {
            if (!Files.isRegularFile(jar)) {
                LOG.error("JAR not found: %s", jar);
                return 2;
            }
            var mapping = io.github.cvs0.bytecode.JarMapping.fromJar(jar);
            Map<String, Set<String>> graph = DependencyAnalyzer.buildDependencyGraph(mapping);
            LOG.info("Dependency nodes (classes in JAR): %d", graph.size());
            Set<String> cycles = DependencyAnalyzer.findCircularDependencies(mapping);
            LOG.info("Classes involved in cycles: %d", cycles.size());
            if (className != null && !className.isBlank()) {
                String internalQuery = normalizeInternalNameForDependencyQuery(className);
                Set<String> dependents = DependencyAnalyzer.findDependents(mapping, internalQuery);
                LOG.info("Dependents of %s: %d", internalQuery, dependents.size());
                dependents.stream().sorted().limit(50).forEach(d -> LOG.info("  %s", d));
                if (dependents.size() > 50) {
                    LOG.info("  ... (%d more)", dependents.size() - 50);
                }
            }
            if (dotOut != null) {
                String dot = DependencyAnalyzer.toDotFormat(graph, "jar");
                Files.writeString(dotOut, dot);
                LOG.info("Wrote DOT to %s", dotOut.toAbsolutePath());
            }
            return 0;
        }
    }

    /**
     * Dependency graphs use slash-separated internal names; accept dotted binary names from the CLI as well.
     *
     * @see BytecodeNames#binaryToInternal(String)
     */
    static String normalizeInternalNameForDependencyQuery(String raw) {
        String s = raw.trim();
        if (s.contains("/")) {
            return s;
        }
        return BytecodeNames.binaryToInternal(s);
    }

    @Command(
            name = "transform",
            description = "Load a JAR, run one or more plugins in order, and write a new JAR from the full in-memory model.")
    static class TransformCommand implements Callable<Integer> {
        @Option(
                names = {"-i", "--input"},
                required = true,
                description = "Input JAR path")
        Path input;

        @Option(
                names = {"-o", "--output"},
                required = true,
                description = "Output JAR path (must differ from input)")
        Path output;

        @Option(
                names = {"-p", "--plugin"},
                required = true,
                description =
                        "Plugin id (repeatable, order preserved): obfuscation, obfuscate, optimization, optimize")
        List<String> plugins = new ArrayList<>();

        @Option(
                names = "--plugin-opt",
                description = "Plugin configuration key=value (repeatable; applied to each plugin that supports it)")
        List<String> pluginOpts = new ArrayList<>();

        @Override
        public Integer call() throws Exception {
            if (!Files.isRegularFile(input)) {
                LOG.error("Input JAR not found: %s", input);
                return 2;
            }
            if (input.normalize().equals(output.normalize())) {
                LOG.error("Output path must differ from input.");
                return 2;
            }

            JarMapping mapping = JarMapping.fromJar(input);

            Map<String, Object> config = parsePluginOpts(pluginOpts);
            for (String pluginId : plugins) {
                Plugin plugin = CliPluginRegistry.create(pluginId);
                if (plugin instanceof ConfigurablePlugin cp && !config.isEmpty()) {
                    cp.configure(config);
                }
                plugin.initialize();
                try {
                    plugin.process(mapping);
                } finally {
                    plugin.cleanup();
                }
            }

            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapping.writeToJar(output);
            LOG.info("Wrote: %s", output.toAbsolutePath().normalize());
            return 0;
        }

        static Map<String, Object> parsePluginOpts(List<String> pluginOpts) {
            Map<String, Object> m = new HashMap<>();
            for (String raw : pluginOpts) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                int eq = raw.indexOf('=');
                if (eq <= 0 || eq == raw.length() - 1) {
                    throw new IllegalArgumentException("Expected key=value, got: " + raw);
                }
                String key = raw.substring(0, eq).trim();
                String value = raw.substring(eq + 1).trim();
                m.put(key, parseConfigScalar(value));
            }
            return m;
        }

        static Object parseConfigScalar(String value) {
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                // fall through
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // fall through
            }
            return value;
        }
    }
}
