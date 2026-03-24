package io.github.cvs0.bytecode;

import io.github.cvs0.bytecode.plugin.impl.ObfuscationPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared helpers for building tiny JARs with {@code javac}, running {@link ObfuscationPlugin}, and asserting
 * {@code java -jar} output. Subprocess I/O is read without merging streams so tests can require a clean stderr.
 */
public final class ObfuscationJarHarness {

    public static final int JAVAC_TIMEOUT_SEC = 60;
    public static final int JAVA_TIMEOUT_SEC = 120;

    /**
     * @param stdout standard output (decoded UTF-8)
     * @param stderr standard error (decoded UTF-8)
     * @param exitCode process exit value
     */
    public record ProcessResult(String stdout, String stderr, int exitCode) {}

    private ObfuscationJarHarness() {}

    /** Fails fast when {@code java.home} does not expose {@code bin/java} / {@code bin/javac}. */
    public static void assumeJavaHomeToolsPresent() {
        Path home = Path.of(System.getProperty("java.home", ""));
        assertTrue(Files.isDirectory(home), () -> "java.home is not a directory: " + home);
        Path java = Files.isExecutable(home.resolve("bin/java.exe"))
                ? home.resolve("bin/java.exe")
                : home.resolve("bin/java");
        Path javac = Files.isExecutable(home.resolve("bin/javac.exe"))
                ? home.resolve("bin/javac.exe")
                : home.resolve("bin/javac");
        assertTrue(Files.isExecutable(java), () -> "Missing executable: " + java);
        assertTrue(Files.isExecutable(javac), () -> "Missing executable (JDK required for tests): " + javac);
    }

    public static String javacExe() {
        String home = System.getProperty("java.home");
        if (home != null) {
            Path win = Path.of(home, "bin", "javac.exe");
            Path nix = Path.of(home, "bin", "javac");
            if (Files.isExecutable(win)) {
                return win.toString();
            }
            if (Files.isExecutable(nix)) {
                return nix.toString();
            }
        }
        return "javac";
    }

    public static String javaExe() {
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

    /**
     * Compiles sources into {@code outputDir} with {@code --release 21}. Optional non-null {@code classpath} is
     * {@code -classpath}.
     */
    public static void javac(Path outputDir, Path classpath, Path... sources) throws Exception {
        assumeJavaHomeToolsPresent();
        Files.createDirectories(outputDir);
        List<String> cmd = new ArrayList<>();
        cmd.add(javacExe());
        cmd.add("-encoding");
        cmd.add("UTF-8");
        cmd.add("--release");
        cmd.add("21");
        cmd.add("-d");
        cmd.add(outputDir.toString());
        if (classpath != null) {
            cmd.add("-classpath");
            cmd.add(classpath.toString());
        }
        for (Path s : sources) {
            assertTrue(Files.isRegularFile(s), () -> "missing source: " + s);
            cmd.add(s.toString());
        }
        assertProcessMerged(cmd, JAVAC_TIMEOUT_SEC);
    }

    public static void assertProcessMerged(List<String> command, int timeoutSec) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String out = readStreamUtf8(proc.getInputStream());
        assertTrue(proc.waitFor(timeoutSec, TimeUnit.SECONDS), "process timed out: " + command);
        assertEquals(0, proc.exitValue(), () -> "command failed: " + command + "\n" + out);
    }

    public static ProcessResult runJavaJarSeparated(Path jar, int timeoutSec) throws Exception {
        assumeJavaHomeToolsPresent();
        assertTrue(Files.isRegularFile(jar), () -> "not a file: " + jar);
        assertTrue(Files.size(jar) > 0, () -> "empty jar: " + jar);
        List<String> cmd = List.of(javaExe(), "-jar", jar.toAbsolutePath().normalize().toString());
        return runProcessSeparated(cmd, timeoutSec);
    }

    /**
     * Runs {@code java -jar}; requires exit code {@code 0}, blank stderr, and exactly one non-empty stdout line
     * containing {@code markerSubstring}.
     */
    public static void assertJarSingleMarkerLine(Path jar, String markerSubstring) throws Exception {
        ProcessResult r = runJavaJarSeparated(jar, JAVA_TIMEOUT_SEC);
        assertEquals(0, r.exitCode(), () -> combinedFailureMessage(r));
        assertStderrClean(r);
        List<String> nonBlank =
                r.stdout().lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
        long hits = nonBlank.stream().filter(line -> line.contains(markerSubstring)).count();
        assertEquals(
                1,
                hits,
                () -> "expected exactly one non-blank stdout line containing '"
                        + markerSubstring
                        + "', lines="
                        + nonBlank
                        + combinedFailureMessage(r));
    }

    /**
     * Exit 0, blank stderr, and at least one stdout line containing {@code substring}.
     */
    public static void assertJarOutputCleanStderr(Path jar, String substring) throws Exception {
        ProcessResult r = runJavaJarSeparated(jar, JAVA_TIMEOUT_SEC);
        assertEquals(0, r.exitCode(), () -> combinedFailureMessage(r));
        assertStderrClean(r);
        assertTrue(r.stdout().contains(substring), () -> "stdout missing expected text. " + combinedFailureMessage(r));
    }

    private static void assertStderrClean(ProcessResult r) {
        String err = r.stderr();
        assertTrue(
                err.isBlank(),
                () -> "expected empty stderr, got:\n" + err + combinedFailureMessage(r));
    }

    private static String combinedFailureMessage(ProcessResult r) {
        return "\n--- stdout ---\n"
                + r.stdout()
                + "\n--- stderr ---\n"
                + r.stderr()
                + "\nexit="
                + r.exitCode();
    }

    private static ProcessResult runProcessSeparated(List<String> command, int timeoutSec) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        Process proc = pb.start();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> outF = ex.submit(() -> readStreamUtf8(proc.getInputStream()));
            Future<String> errF = ex.submit(() -> readStreamUtf8(proc.getErrorStream()));
            boolean finished = proc.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
            }
            assertTrue(finished, () -> "timed out: " + command);
            return new ProcessResult(outF.get(), errF.get(), proc.exitValue());
        }
    }

    private static String readStreamUtf8(InputStream in) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return r.lines().reduce((a, b) -> a + "\n" + b).orElse("");
        }
    }

    /** @deprecated Prefer {@link #assertJarSingleMarkerLine} or {@link #assertJarOutputCleanStderr}. */
    @Deprecated
    public static String runJavaJar(Path jar, int timeoutSec) throws Exception {
        ProcessResult r = runJavaJarSeparated(jar, timeoutSec);
        assertEquals(0, r.exitCode(), combinedFailureMessage(r));
        return r.stdout();
    }

    /** @deprecated Prefer {@link #assertJarSingleMarkerLine} or {@link #assertJarOutputCleanStderr}. */
    @Deprecated
    public static void assertJarOutputContains(Path jar, String substring) throws Exception {
        assertJarOutputCleanStderr(jar, substring);
    }

    public static void writeJarWithMain(Path jar, Path classesDir, String mainClass, String... classEntries)
            throws Exception {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
        writeJarFromClasses(jar, mf, classesDir, classEntries);
    }

    /**
     * Manifest with {@code Main-Class} plus extra main attributes (e.g. {@code Start-Class}).
     */
    public static void writeJarWithManifest(Path jar, Path classesDir, Map<String, String> mainAttributes, String... classEntries)
            throws Exception {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        for (Map.Entry<String, String> e : mainAttributes.entrySet()) {
            attrs.put(new Attributes.Name(e.getKey()), e.getValue());
        }
        writeJarFromClasses(jar, mf, classesDir, classEntries);
    }

    public static void writeJarClassesOnly(Path jar, Path classesDir, String... classEntries) throws Exception {
        writeJarFromClasses(jar, null, classesDir, classEntries);
    }

    public static void writeJarWithResource(
            Path jar, Manifest mf, Path classesDir, String[] classEntries, String resourcePath, byte[] resourceBytes)
            throws Exception {
        try (JarOutputStream jos =
                mf != null
                        ? new JarOutputStream(Files.newOutputStream(jar), mf)
                        : new JarOutputStream(Files.newOutputStream(jar))) {
            for (String rel : classEntries) {
                putClassEntry(jos, classesDir, rel);
            }
            jos.putNextEntry(new ZipEntry(resourcePath));
            jos.write(resourceBytes);
            jos.closeEntry();
        }
    }

    private static void writeJarFromClasses(Path jar, Manifest mf, Path classesDir, String... classEntries)
            throws Exception {
        try (JarOutputStream jos =
                mf != null
                        ? new JarOutputStream(Files.newOutputStream(jar), mf)
                        : new JarOutputStream(Files.newOutputStream(jar))) {
            for (String rel : classEntries) {
                putClassEntry(jos, classesDir, rel);
            }
        }
    }

    private static void putClassEntry(JarOutputStream jos, Path classesDir, String rel) throws Exception {
        Path f = classesDir.resolve(rel);
        assertTrue(Files.isRegularFile(f), () -> "missing class file for JAR entry " + rel + ": " + f);
        jos.putNextEntry(new ZipEntry(rel));
        jos.write(Files.readAllBytes(f));
        jos.closeEntry();
    }

    public static void obfuscateAndWrite(JarMapping mapping, Path outputJar, Map<String, Object> pluginConfig)
            throws Exception {
        ObfuscationPlugin plugin = new ObfuscationPlugin();
        if (pluginConfig != null && !pluginConfig.isEmpty()) {
            plugin.configure(pluginConfig);
        }
        plugin.initialize();
        try {
            plugin.process(mapping);
        } finally {
            plugin.cleanup();
        }
        mapping.writeToJar(outputJar);
        assertTrue(Files.isRegularFile(outputJar), () -> "obfuscated jar not written: " + outputJar);
        assertTrue(Files.size(outputJar) > 0, () -> "obfuscated jar is empty: " + outputJar);
    }

    public static void obfuscateAndWrite(JarMapping mapping, Path outputJar) throws Exception {
        obfuscateAndWrite(mapping, outputJar, Map.of());
    }

    /** Non-blank stdout lines containing {@code marker}, in order. */
    public static List<String> stdoutLinesContaining(ProcessResult r, String marker) {
        return r.stdout().lines().map(String::strip).filter(s -> !s.isEmpty()).filter(l -> l.contains(marker)).toList();
    }

    /** All {@code META-INF/services/*} entry names in a JAR (excluding directory-only keys). */
    public static List<String> listServiceLoaderPaths(Path jar) throws Exception {
        try (var zf = new java.util.zip.ZipFile(jar.toFile())) {
            return zf.stream()
                    .map(java.util.zip.ZipEntry::getName)
                    .filter(n -> n.startsWith("META-INF/services/") && !n.endsWith("/"))
                    .sorted()
                    .toList();
        }
    }
}
