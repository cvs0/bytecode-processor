package io.github.cvs0.bytecode.example;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.log.BPLogger;
import io.github.cvs0.bytecode.plugin.impl.ObfuscationPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a JAR, applies {@link ObfuscationPlugin}, writes the transformed JAR.
 *
 * <p>Pack every runtime dependency into the input JAR (uber/fat JAR or shaded build) so all {@code .class} files and
 * resources are loaded together; the processor then rewrites the archive from its full in-memory model.</p>
 *
 * <p>Maven: {@code mvn exec:java "-Dexec.args=in.jar out.jar"} after installing the library.
 * Runnable obfuscated JAR scenarios are covered by {@code io.github.cvs0.bytecode.ObfuscationRunnableJarIT}.</p>
 */
public final class ObfuscateApp {

    private static final BPLogger LOG = BPLogger.of(ObfuscateApp.class);

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && isHelp(args[0])) {
            printUsage();
            return;
        }
        ParsedArgs parsed = ParsedArgs.parse(args);
        if (parsed == null) {
            printUsage();
            System.exit(2);
        }

        if (!Files.isRegularFile(parsed.input)) {
            LOG.error("Not a file: %s", parsed.input);
            System.exit(2);
        }
        if (parsed.input.normalize().equals(parsed.output.normalize())) {
            LOG.error("Output path must differ from input.");
            System.exit(2);
        }

        JarMapping mapping = JarMapping.fromJar(parsed.input);

        ObfuscationPlugin plugin = new ObfuscationPlugin();
        plugin.configure(parsed.pluginConfig);
        plugin.initialize();
        try {
            plugin.process(mapping);
        } finally {
            plugin.cleanup();
        }

        Path parent = parsed.output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        mapping.writeToJar(parsed.output);
        LOG.info("Wrote obfuscated JAR: %s", parsed.output.toAbsolutePath().normalize());
        LOG.info("  (program class entries: %d, resources: %d)",
                mapping.getProgramClassEntryCount(),
                mapping.getResourceCount());
    }

    private static boolean isHelp(String a) {
        return "-h".equals(a) || "--help".equalsIgnoreCase(a) || "-?".equals(a);
    }

    private static void printUsage() {
        LOG.error(
                """
                        Usage: ObfuscateApp <input.jar> <output.jar> [options]

                          input.jar   JAR to obfuscate (include dependencies in this JAR if you need a self-contained result)
                          output.jar  Must differ from input; parent directories are created if needed

                          --prefix <string>      Prefix for generated obfuscated names (default: a)
                          --no-classes           Do not rename program class names
                          --no-methods           Do not rename methods (constructors / main / accessors skipped)
                          --no-fields            Do not rename fields (static final skipped)

                        Manifest Main-Class / Start-Class and META-INF/services lines are updated when possible.

                          -h, --help             Show this text
                        """);
    }

    private ObfuscateApp() {}

    private static final class ParsedArgs {
        final Path input;
        final Path output;
        final Map<String, Object> pluginConfig;

        ParsedArgs(Path input, Path output, Map<String, Object> pluginConfig) {
            this.input = input;
            this.output = output;
            this.pluginConfig = pluginConfig;
        }

        static ParsedArgs parse(String[] args) {
            if (args.length < 2) {
                return null;
            }
            Path in = Path.of(args[0]).toAbsolutePath().normalize();
            Path out = Path.of(args[1]).toAbsolutePath().normalize();
            Map<String, Object> cfg = new HashMap<>();

            for (int i = 2; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "--prefix" -> {
                        if (i + 1 >= args.length) {
                            return null;
                        }
                        cfg.put(ObfuscationPlugin.CFG_NAME_PREFIX, args[++i]);
                    }
                    case "--no-classes" -> cfg.put(ObfuscationPlugin.CFG_OBFUSCATE_CLASSES, Boolean.FALSE);
                    case "--no-methods" -> cfg.put(ObfuscationPlugin.CFG_OBFUSCATE_METHODS, Boolean.FALSE);
                    case "--no-fields" -> cfg.put(ObfuscationPlugin.CFG_OBFUSCATE_FIELDS, Boolean.FALSE);
                    case "-h", "--help", "-?" -> {
                        return null;
                    }
                    default -> {
                        return null;
                    }
                }
            }
            return new ParsedArgs(in, out, cfg);
        }
    }
}
