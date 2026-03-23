package net.cvs0.bytecode.cli;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodeCliShadedIT {

    @Test
    void shadedJarHelpExitsZero() throws IOException, InterruptedException {
        String jarProp = System.getProperty("integrationTest.shadedJar");
        Assumptions.assumeTrue(jarProp != null && !jarProp.isBlank(), "integrationTest.shadedJar not set (use mvn verify)");

        Path jar = Paths.get(jarProp);
        Assumptions.assumeTrue(Files.isRegularFile(jar), "Shaded JAR not built yet: " + jar);

        String java = javaExecutable();
        ProcessBuilder pb = new ProcessBuilder(java, "-jar", jar.toAbsolutePath().toString(), "--help");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(30, TimeUnit.SECONDS), "java -jar timed out");
        assertEquals(0, p.exitValue(), () -> "stderr+stdout:\n" + out);
        assertTrue(out.contains("analyze") || out.contains("USAGE"), () -> "unexpected help output:\n" + out);
    }

    private static String javaExecutable() {
        String home = System.getProperty("java.home");
        if (home == null || home.isBlank()) {
            return "java";
        }
        Path bin = Paths.get(home, "bin", isWindows() ? "java.exe" : "java");
        return Files.isExecutable(bin) ? bin.toString() : "java";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
