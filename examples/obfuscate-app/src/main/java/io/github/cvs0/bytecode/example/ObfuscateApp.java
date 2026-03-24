package io.github.cvs0.bytecode.example;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.plugin.impl.ObfuscationPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads a JAR, optionally merges dependency JARs, applies {@link ObfuscationPlugin}, writes a self-contained JAR.
 *
 * <p>Merge libraries that your application loads at runtime (e.g. {@code target/dependency}) so bytecode in those JARs
 * stays consistent with renamed application classes. {@code Main-Class} and {@code Start-Class} in the manifest are
 * updated when the launch class is renamed.</p>
 *
 * <p>Maven: {@code mvn exec:java "-Dexec.args=in.jar out.jar --libDir path\\to\\deps"} after installing the library.
 * Fat-JAR obfuscation smoke test: {@code mvn verify} in {@code examples/local-app/} (after {@code mvn package} at repo
 * root).</p>
 */
public final class ObfuscateApp {

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
            System.err.println("Not a file: " + parsed.input);
            System.exit(2);
        }
        if (parsed.input.normalize().equals(parsed.output.normalize())) {
            System.err.println("Output path must differ from input.");
            System.exit(2);
        }

        for (Path lib : parsed.libs) {
            if (!Files.isRegularFile(lib)) {
                System.err.println("Not a file: " + lib);
                System.exit(2);
            }
        }
        for (Path dir : parsed.libDirs) {
            if (!Files.isDirectory(dir)) {
                System.err.println("Not a directory: " + dir);
                System.exit(2);
            }
        }

        JarMapping mapping = JarMapping.fromJar(parsed.input);
        mapping.mergeClasspathJars(listDependencyJars(parsed));

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
        System.out.println("Wrote obfuscated JAR: " + parsed.output.toAbsolutePath().normalize());
        System.out.println(
                "  (program classes: "
                        + mapping.getProgramClasses().size()
                        + ", merged classpath entries: "
                        + mapping.getMergedEntryCount()
                        + ")");
    }

    private static boolean isHelp(String a) {
        return "-h".equals(a) || "--help".equalsIgnoreCase(a) || "-?".equals(a);
    }

    private static void printUsage() {
        System.err.println(
                """
                        Usage: ObfuscateApp <input.jar> <output.jar> [options]

                          input.jar   Application JAR (usually has Main-Class in META-INF/MANIFEST.MF)
                          output.jar  Must differ from input; parent directories are created if needed

                          --lib <path.jar>       Merge a dependency JAR (repeatable; order matters)
                          --libDir <dir>         Merge every *.jar in the directory (sorted by file name)
                          --prefix <string>      Prefix for generated obfuscated names (default: a)
                          --no-classes           Do not rename program class names
                          --no-methods           Do not rename methods (constructors / main / accessors skipped)
                          --no-fields            Do not rename fields (static final skipped)

                        Merge every runtime dependency so renamed app classes stay linkable from library bytecode.
                        Manifest Main-Class / Start-Class and META-INF/services lines are updated when possible.

                          -h, --help             Show this text
                        """);
    }

    /** Explicit {@code --lib} jars first, then each {@code --libDir} in order (jars sorted by file name). */
    private static List<Path> listDependencyJars(ParsedArgs parsed) throws java.io.IOException {
        List<Path> jars = new ArrayList<>(parsed.libs);
        for (Path dir : parsed.libDirs) {
            try (Stream<Path> stream = Files.list(dir)) {
                List<Path> fromDir = stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .toList();
                jars.addAll(fromDir);
            }
        }
        return jars;
    }

    private ObfuscateApp() {}

    private static final class ParsedArgs {
        final Path input;
        final Path output;
        final Map<String, Object> pluginConfig;
        final List<Path> libs;
        final List<Path> libDirs;

        ParsedArgs(
                Path input,
                Path output,
                Map<String, Object> pluginConfig,
                List<Path> libs,
                List<Path> libDirs) {
            this.input = input;
            this.output = output;
            this.pluginConfig = pluginConfig;
            this.libs = libs;
            this.libDirs = libDirs;
        }

        static ParsedArgs parse(String[] args) {
            if (args.length < 2) {
                return null;
            }
            Path in = Path.of(args[0]).toAbsolutePath().normalize();
            Path out = Path.of(args[1]).toAbsolutePath().normalize();
            Map<String, Object> cfg = new HashMap<>();
            List<Path> libs = new ArrayList<>();
            List<Path> libDirs = new ArrayList<>();

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
                    case "--lib" -> {
                        if (i + 1 >= args.length) {
                            return null;
                        }
                        libs.add(Path.of(args[++i]).toAbsolutePath().normalize());
                    }
                    case "--libDir" -> {
                        if (i + 1 >= args.length) {
                            return null;
                        }
                        libDirs.add(Path.of(args[++i]).toAbsolutePath().normalize());
                    }
                    case "-h", "--help", "-?" -> {
                        return null;
                    }
                    default -> {
                        return null;
                    }
                }
            }
            return new ParsedArgs(in, out, cfg, libs, libDirs);
        }
    }
}
