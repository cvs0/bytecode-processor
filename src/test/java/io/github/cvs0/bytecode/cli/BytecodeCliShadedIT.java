package io.github.cvs0.bytecode.cli;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Subprocess exercises of the shaded uber-JAR (real {@code java -jar}) plus the thin library JAR where useful.
 */
class BytecodeCliShadedIT {

    @Test
    void shadedJarHelpExitsZero() throws Exception {
        Path shaded = shadedJar();
        Result r = run(javaExe(), "-jar", shaded.toString(), "--help");
        assertEquals(0, r.exit, r::toString);
        assertTrue(r.out.contains("analyze") || r.out.contains("USAGE"), () -> "unexpected help:\n" + r.out);
    }

    @Test
    void shadedJarVersionExitsZero() throws Exception {
        Path shaded = shadedJar();
        Result r = run(javaExe(), "-jar", shaded.toString(), "--version");
        assertEquals(0, r.exit, r::toString);
        assertTrue(r.out.contains("Bytecode Processor"), () -> "unexpected version output:\n" + r.out);
    }

    @Test
    void statsHumanReadable_onShadedJar() throws Exception {
        Path shaded = shadedJar();
        Result r = run(javaExe(), "-jar", shaded.toString(), "stats", shaded.toString());
        assertEquals(0, r.exit, r::toString);
        assertTrue(r.out.contains("Program classes:"), () -> r.out);
        assertTrue(r.out.contains("Methods:"), () -> r.out);
    }

    @Test
    void statsJson_onShadedJar() throws Exception {
        Path shaded = shadedJar();
        Result r = run(javaExe(), "-jar", shaded.toString(), "stats", shaded.toString(), "--json");
        assertEquals(0, r.exit, r::toString);
        assertTrue(r.out.trim().startsWith("{") && r.out.contains("\"programClasses\""), () -> r.out);
    }

    @Test
    void depsPrintsGraphSummary_onShadedJar() throws Exception {
        Path shaded = shadedJar();
        Result r = run(javaExe(), "-jar", shaded.toString(), "deps", shaded.toString());
        assertEquals(0, r.exit, r::toString);
        assertTrue(r.out.contains("Dependency nodes"), () -> r.out);
        assertTrue(r.out.contains("cycles"), () -> r.out);
    }

    @Test
    void depsDotWritten_andDependentsWithDottedClassName(@TempDir Path temp) throws Exception {
        Path shaded = shadedJar();
        Path dot = temp.resolve("g.dot");
        Result r =
                run(
                        javaExe(),
                        "-jar",
                        shaded.toString(),
                        "deps",
                        shaded.toString(),
                        "--dot",
                        dot.toString(),
                        "--class",
                        "java.lang.Object");
        assertEquals(0, r.exit, r::toString);
        assertTrue(Files.isRegularFile(dot) && Files.size(dot) > 20, "DOT file missing or empty");
        String dotText = Files.readString(dot);
        assertTrue(dotText.contains("digraph"), () -> dotText.substring(0, Math.min(200, dotText.length())));
        assertTrue(r.out.contains("Dependents of java/lang/Object"), () -> r.out);
    }

    @Test
    void statsMissingJar_exits2() throws Exception {
        Path shaded = shadedJar();
        Path missing = shaded.getParent().resolve("definitely-missing-" + System.nanoTime() + ".jar");
        Result r = run(javaExe(), "-jar", shaded.toString(), "stats", missing.toString());
        assertEquals(2, r.exit, r::toString);
        assertTrue(r.out.contains("JAR not found") || r.err.contains("JAR not found"), () -> r.toString());
    }

    @Test
    void analyze_onThinLibraryJar_completes() throws Exception {
        Path shaded = shadedJar();
        Path thin = libraryJar();
        Result r = run(javaExe(), "-jar", shaded.toString(), "analyze", thin.toString());
        assertEquals(0, r.exit, r::toString);
        assertTrue(
                r.out.contains("Analysis completed") || r.out.contains("BYTECODE PROCESSOR"),
                () -> "unexpected analyze output (first 500 chars):\n" + r.out.substring(0, Math.min(500, r.out.length())));
    }

    private static Path shadedJar() {
        String jarProp = System.getProperty("integrationTest.shadedJar");
        Assumptions.assumeTrue(jarProp != null && !jarProp.isBlank(), "integrationTest.shadedJar not set");
        Path jar = Paths.get(jarProp);
        Assumptions.assumeTrue(Files.isRegularFile(jar), "Shaded JAR not built: " + jar);
        return jar;
    }

    private static Path libraryJar() {
        String jarProp = System.getProperty("integrationTest.libraryJar");
        Assumptions.assumeTrue(jarProp != null && !jarProp.isBlank(), "integrationTest.libraryJar not set");
        Path jar = Paths.get(jarProp);
        Assumptions.assumeTrue(Files.isRegularFile(jar), "library JAR not built: " + jar);
        return jar;
    }

    private static String javaExe() {
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

    private record Result(int exit, String out, String err) {
        @Override
        public String toString() {
            return "exit=" + exit + "\n--- stdout ---\n" + out + "\n--- stderr ---\n" + err;
        }
    }

    private static Result run(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process p = pb.start();
        try (var ex = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var outF = ex.submit(() -> new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            var errF = ex.submit(() -> new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
            assertTrue(p.waitFor(120, TimeUnit.SECONDS), "timeout: " + String.join(" ", cmd));
            return new Result(p.exitValue(), outF.get(), errF.get());
        }
    }
}
