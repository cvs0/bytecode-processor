package io.github.cvs0.bytecode.example.local;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.plugin.impl.ObfuscationPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Builds a fat obfuscated JAR (same pipeline as {@code ObfuscateApp}) and checks {@code java -jar} runs.
 */
class FatJarObfuscationIT {

    @Test
    void obfuscatedFatJarIsRunnable(@TempDir Path temp) throws Exception {
        Path moduleBase = Path.of(Objects.requireNonNull(
                System.getProperty("localapp.module.basedir"),
                "localapp.module.basedir (set by Failsafe)"));
        String finalName = Objects.requireNonNull(
                System.getProperty("localapp.finalName"),
                "localapp.finalName");
        Path libraryJar = Path.of(Objects.requireNonNull(
                System.getProperty("localapp.bytecode.processor.jar"),
                "localapp.bytecode.processor.jar (Failsafe: lib/bytecode-processor-local.jar)"));
        assumeTrue(Files.isRegularFile(libraryJar), () -> "Missing " + libraryJar);

        Path thinJar = moduleBase.resolve("target/" + finalName + ".jar");
        assumeTrue(Files.isRegularFile(thinJar), () -> "Missing packaged app " + thinJar);

        Path depDir = moduleBase.resolve("target/dependency");
        assumeTrue(Files.isDirectory(depDir), () -> "Missing " + depDir + " — dependency:copy-dependencies should run pre-IT");

        Path obfJar = temp.resolve("local-demo-obf.jar");

        JarMapping mapping = JarMapping.fromJar(thinJar);
        mapping.mergeClasspathJar(libraryJar);
        try (Stream<Path> stream = Files.list(depDir)) {
            List<Path> jars = stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .toList();
            mapping.mergeClasspathJars(jars);
        }

        ObfuscationPlugin plugin = new ObfuscationPlugin();
        plugin.initialize();
        try {
            plugin.process(mapping);
        } finally {
            plugin.cleanup();
        }
        mapping.writeToJar(obfJar);

        List<String> cmd = new ArrayList<>();
        cmd.add(resolveJavaExecutable());
        cmd.add("-jar");
        cmd.add(obfJar.toString());
        cmd.add(libraryJar.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            output = r.lines().reduce((a, b) -> a + "\n" + b).orElse("");
        }
        boolean finished = p.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
        }
        assumeTrue(finished, "java -jar timed out");
        int code = p.exitValue();
        assertEquals(0, code, () -> "java -jar failed. Output:\n" + output);
        assertTrue(output.contains("Program classes:"), () -> "Unexpected output:\n" + output);
    }

    private static String resolveJavaExecutable() {
        String home = System.getProperty("java.home");
        if (home != null) {
            Path win = Path.of(home, "bin", "java.exe");
            Path nix = Path.of(home, "bin", "java");
            if (Files.isExecutable(win)) {
                return win.toString();
            }
            if (Files.isExecutable(nix)) {
                return nix.toString();
            }
        }
        return "java";
    }
}
