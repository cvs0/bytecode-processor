package io.github.cvs0.bytecode.example;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.plugin.impl.ObfuscationPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a JAR, applies {@link ObfuscationPlugin} (class / method / field renames), writes the result.
 *
 * <p>From the repository root: {@code mvn install -DskipTests}, then from {@code examples/obfuscate-app}:
 * {@code mvn exec:java -Dexec.args="in.jar out.jar"}.</p>
 *
 * <p>Only classes inside the input JAR are rewritten. If the app needs libraries at runtime, ship them on the classpath
 * or merge them (e.g. shaded uber-JAR) the same way you would for an unobfuscated build.</p>
 */
public final class ObfuscateApp {

    public static void main(String[] args) throws Exception {
        ParsedArgs parsed = ParsedArgs.parse(args);
        if (parsed == null) {
            System.err.println(
                    "Usage: ObfuscateApp <input.jar> <output.jar> [--prefix <string>] [--no-classes] [--no-methods] [--no-fields]");
            System.exit(2);
        }

        Path input = parsed.input;
        Path output = parsed.output;
        if (!Files.isRegularFile(input)) {
            System.err.println("Not a file: " + input);
            System.exit(2);
        }
        if (input.normalize().equals(output.normalize())) {
            System.err.println("Output path must differ from input.");
            System.exit(2);
        }

        JarMapping mapping = JarMapping.fromJar(input);

        ObfuscationPlugin plugin = new ObfuscationPlugin();
        plugin.configure(parsed.config);
        plugin.initialize();
        try {
            plugin.process(mapping);
        } finally {
            plugin.cleanup();
        }

        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        mapping.writeToJar(output);
        System.out.println("Wrote obfuscated JAR: " + output);
    }

    private ObfuscateApp() {}

    private static final class ParsedArgs {
        final Path input;
        final Path output;
        final Map<String, Object> config;

        ParsedArgs(Path input, Path output, Map<String, Object> config) {
            this.input = input;
            this.output = output;
            this.config = config;
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
                    default -> {
                        return null;
                    }
                }
            }
            return new ParsedArgs(in, out, cfg);
        }
    }
}
