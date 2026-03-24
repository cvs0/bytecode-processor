package io.github.cvs0.bytecode;

import io.github.cvs0.bytecode.plugin.impl.ObfuscationPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static io.github.cvs0.bytecode.ObfuscationJarHarness.assertJarSingleMarkerLine;
import static io.github.cvs0.bytecode.ObfuscationJarHarness.javac;
import static io.github.cvs0.bytecode.ObfuscationJarHarness.listServiceLoaderPaths;
import static io.github.cvs0.bytecode.ObfuscationJarHarness.obfuscateAndWrite;
import static io.github.cvs0.bytecode.ObfuscationJarHarness.writeJarWithMain;
import static io.github.cvs0.bytecode.ObfuscationJarHarness.writeJarWithManifest;
import static io.github.cvs0.bytecode.ObfuscationJarHarness.writeJarWithResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end checks that {@link ObfuscationPlugin} produces {@code java -jar}-runnable outputs for common layouts:
 * thin JARs, single JARs containing application plus library classes, {@link java.util.ServiceLoader}, {@code Start-Class},
 * and plugin configuration variants.
 *
 * <p>Service-loader scenarios use {@link ObfuscationPlugin#CFG_OBFUSCATE_METHODS} {@code false}: the example plugin
 * renames methods per declaring class, which would desynchronize interface abstract methods from implementors.</p>
 */
class ObfuscationRunnableJarIT {

    @BeforeAll
    static void requireJdkWithJavac() {
        ObfuscationJarHarness.assumeJavaHomeToolsPresent();
    }

    @Test
    void thinJar_singlePrimaryJar_runsAfterObfuscation(@TempDir Path temp) throws Exception {
        Path app = temp.resolve("App.java");
        Files.writeString(
                app,
                """
                        package thin;

                        public class App {
                            public static void main(String[] args) {
                                System.out.println("THIN_OK");
                            }
                        }
                        """);
        Path out = temp.resolve("classes");
        javac(out, null, app);
        Path inJar = temp.resolve("in.jar");
        writeJarWithMain(inJar, out, "thin.App", "thin/App.class");

        JarMapping mapping = JarMapping.fromJar(inJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(mapping, obf);

        assertJarSingleMarkerLine(obf, "THIN_OK");
    }

    @Test
    void fatJar_mergedLibraryInvokesRenamedAppInstanceMethod_runs(@TempDir Path temp) throws Exception {
        Path appJava = temp.resolve("App.java");
        Path libJava = temp.resolve("Lib.java");
        Files.writeString(
                appJava,
                """
                        package fatm;

                        public class App {
                            public static void main(String[] args) {
                                Lib.touch();
                                System.out.println("FAT_METHOD_OK");
                            }

                            public void work() {}
                        }
                        """);
        Files.writeString(
                libJava,
                """
                        package fatm;

                        public class Lib {
                            public static void touch() {
                                new App().work();
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, appJava, libJava);
        Path appJar = temp.resolve("app.jar");
        writeJarWithMain(appJar, classes, "fatm.App", "fatm/App.class", "fatm/Lib.class");

        JarMapping mapping = JarMapping.fromJar(appJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(mapping, obf);

        assertJarSingleMarkerLine(obf, "FAT_METHOD_OK");
    }

    @Test
    void fatJar_mergedLibraryReadsRenamedInstanceField_runs(@TempDir Path temp) throws Exception {
        Path appJava = temp.resolve("App.java");
        Path libJava = temp.resolve("Lib.java");
        Files.writeString(
                appJava,
                """
                        package fatf;

                        public class App {
                            public static void main(String[] args) {
                                Lib.print(new App());
                                System.out.println("FAT_FIELD_OK");
                            }

                            public int counter = 99;
                        }
                        """);
        Files.writeString(
                libJava,
                """
                        package fatf;

                        public class Lib {
                            public static void print(App a) {
                                if (a.counter != 99) {
                                    throw new IllegalStateException();
                                }
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, appJava, libJava);
        Path appJar = temp.resolve("app.jar");
        writeJarWithMain(appJar, classes, "fatf.App", "fatf/App.class", "fatf/Lib.class");

        JarMapping mapping = JarMapping.fromJar(appJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(mapping, obf);

        assertJarSingleMarkerLine(obf, "FAT_FIELD_OK");
    }

    @Test
    void fatJar_twoMergedLibrariesReferencingApp_runs(@TempDir Path temp) throws Exception {
        Path appJava = temp.resolve("App.java");
        Path lib1 = temp.resolve("Lib1.java");
        Path lib2 = temp.resolve("Lib2.java");
        Files.writeString(
                appJava,
                """
                        package twolib;

                        public class App {
                            public static void main(String[] args) {
                                Lib1.ping();
                                Lib2.pong();
                                System.out.println("TWO_LIB_OK");
                            }

                            public void mark() {}
                        }
                        """);
        Files.writeString(
                lib1,
                """
                        package twolib;

                        public class Lib1 {
                            public static void ping() {
                                new App().mark();
                            }
                        }
                        """);
        Files.writeString(
                lib2,
                """
                        package twolib;

                        public class Lib2 {
                            public static void pong() {
                                new App().mark();
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, appJava, lib1, lib2);
        Path appJar = temp.resolve("app.jar");
        writeJarWithMain(
                appJar,
                classes,
                "twolib.App",
                "twolib/App.class",
                "twolib/Lib1.class",
                "twolib/Lib2.class");

        JarMapping mapping = JarMapping.fromJar(appJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(mapping, obf);

        assertJarSingleMarkerLine(obf, "TWO_LIB_OK");
    }

    @Test
    void fatJar_singleJar_appAndLib_runs(@TempDir Path temp) throws Exception {
        Path appJava = temp.resolve("App.java");
        Path libJava = temp.resolve("Lib.java");
        Files.writeString(
                appJava,
                """
                        package dup;

                        public class App {
                            public static void main(String[] args) {
                                Lib.go();
                                System.out.println("DUP_MERGE_OK");
                            }

                            public void x() {}
                        }
                        """);
        Files.writeString(
                libJava,
                """
                        package dup;

                        public class Lib {
                            public static void go() {
                                new App().x();
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, appJava, libJava);
        Path appJar = temp.resolve("app.jar");
        writeJarWithMain(appJar, classes, "dup.App", "dup/App.class", "dup/Lib.class");

        JarMapping mapping = JarMapping.fromJar(appJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(mapping, obf);
        assertJarSingleMarkerLine(obf, "DUP_MERGE_OK");
    }

    @Test
    void mergedLibraryCallingAppClasses_stillRuns(@TempDir Path temp) throws Exception {
        Path appJava = temp.resolve("App.java");
        Path helperJava = temp.resolve("Helper.java");
        Files.writeString(
                appJava,
                """
                        package obfext;

                        public class App {
                            public static void main(String[] args) {
                                Helper.go();
                                System.out.println("RUN_OK");
                            }

                            public void used() {}
                        }
                        """);
        Files.writeString(
                helperJava,
                """
                        package obfext;

                        public class Helper {
                            public static void go() {
                                new App().used();
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, appJava, helperJava);
        Path appJar = temp.resolve("app.jar");
        writeJarWithMain(appJar, classes, "obfext.App", "obfext/App.class", "obfext/Helper.class");

        JarMapping mapping = JarMapping.fromJar(appJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(mapping, obf);

        assertJarSingleMarkerLine(obf, "RUN_OK");
    }

    @Test
    void mergedLibReferencesAppInnerClass_runs(@TempDir Path temp) throws Exception {
        Path appJava = temp.resolve("App.java");
        Path libJava = temp.resolve("Lib.java");
        Files.writeString(
                appJava,
                """
                        package inn;

                        public class App {
                            public static void main(String[] args) {
                                Lib.use(new App.Inner());
                                System.out.println("INNER_OK");
                            }

                            public static class Inner {
                                public void ping() {}
                            }
                        }
                        """);
        Files.writeString(
                libJava,
                """
                        package inn;

                        public class Lib {
                            public static void use(App.Inner i) {
                                i.ping();
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, appJava, libJava);
        Path appJar = temp.resolve("app.jar");
        writeJarWithMain(
                appJar, classes, "inn.App", "inn/App.class", "inn/App$Inner.class", "inn/Lib.class");

        JarMapping mapping = JarMapping.fromJar(appJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(mapping, obf);

        assertJarSingleMarkerLine(obf, "INNER_OK");
    }

    @Test
    void serviceLoader_findsRenamedProvider_runs(@TempDir Path temp) throws Exception {
        Path greeter = temp.resolve("Greeter.java");
        Path hello = temp.resolve("Hello.java");
        Path main = temp.resolve("Main.java");
        Files.writeString(
                greeter,
                """
                        package spi;

                        public interface Greeter {
                            void say();
                        }
                        """);
        Files.writeString(
                hello,
                """
                        package spi;

                        public class Hello implements Greeter {
                            public void say() {
                                System.out.println("SPI_OK");
                            }
                        }
                        """);
        Files.writeString(
                main,
                """
                        package spi;

                        import java.util.ServiceLoader;

                        public class Main {
                            public static void main(String[] args) {
                                Greeter g = ServiceLoader.load(Greeter.class).findFirst().orElseThrow();
                                g.say();
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, greeter, hello, main);

        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "spi.Main");
        byte[] services = "spi.Hello\n".getBytes(StandardCharsets.UTF_8);
        Path inJar = temp.resolve("in.jar");
        writeJarWithResource(
                inJar,
                mf,
                classes,
                new String[] {"spi/Greeter.class", "spi/Hello.class", "spi/Main.class"},
                "META-INF/services/spi.Greeter",
                services);

        JarMapping mapping = JarMapping.fromJar(inJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(
                mapping,
                obf,
                Map.of(ObfuscationPlugin.CFG_OBFUSCATE_METHODS, Boolean.FALSE));

        List<String> spiPaths = listServiceLoaderPaths(obf);
        assertEquals(1, spiPaths.size(), () -> "expected one SPI descriptor, got: " + spiPaths);
        assertFalse(spiPaths.getFirst().endsWith("spi.Greeter"), "descriptor file should be renamed away from spi.Greeter");

        assertJarSingleMarkerLine(obf, "SPI_OK");
    }

    @Test
    void serviceLoader_commentsBlankLinesAndTwoProviders_allLoad(@TempDir Path temp) throws Exception {
        Path greeter = temp.resolve("Greeter.java");
        Path hello = temp.resolve("Hello.java");
        Path hola = temp.resolve("Hola.java");
        Path main = temp.resolve("Main.java");
        Files.writeString(
                greeter,
                """
                        package spi2;

                        public interface Greeter {
                            void say();
                        }
                        """);
        Files.writeString(
                hello,
                """
                        package spi2;

                        public class Hello implements Greeter {
                            public void say() {
                                System.out.println("SPI2_A");
                            }
                        }
                        """);
        Files.writeString(
                hola,
                """
                        package spi2;

                        public class Hola implements Greeter {
                            public void say() {
                                System.out.println("SPI2_B");
                            }
                        }
                        """);
        Files.writeString(
                main,
                """
                        package spi2;

                        import java.util.ServiceLoader;

                        public class Main {
                            public static void main(String[] args) {
                                for (Greeter g : ServiceLoader.load(Greeter.class)) {
                                    g.say();
                                }
                                System.out.println("SPI2_DONE");
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, greeter, hello, hola, main);

        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "spi2.Main");
        String svc =
                """
                        # first provider

                        spi2.Hello

                        spi2.Hola
                        """;
        Path inJar = temp.resolve("in.jar");
        writeJarWithResource(
                inJar,
                mf,
                classes,
                new String[] {
                    "spi2/Greeter.class", "spi2/Hello.class", "spi2/Hola.class", "spi2/Main.class"
                },
                "META-INF/services/spi2.Greeter",
                svc.getBytes(StandardCharsets.UTF_8));

        JarMapping mapping = JarMapping.fromJar(inJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(
                mapping,
                obf,
                Map.of(ObfuscationPlugin.CFG_OBFUSCATE_METHODS, Boolean.FALSE));

        ObfuscationJarHarness.ProcessResult r = ObfuscationJarHarness.runJavaJarSeparated(obf, ObfuscationJarHarness.JAVA_TIMEOUT_SEC);
        assertEquals(0, r.exitCode(), r::toString);
        assertTrue(r.stderr().isBlank(), () -> "stderr: " + r.stderr());
        assertTrue(r.stdout().contains("SPI2_A"), () -> "stdout:\n" + r.stdout());
        assertTrue(r.stdout().contains("SPI2_B"), () -> "stdout:\n" + r.stdout());
        assertTrue(r.stdout().contains("SPI2_DONE"), () -> "stdout:\n" + r.stdout());
    }

    @Test
    void startClass_manifestUpdated_obfuscatedJarLaunchesViaBootstrap(@TempDir Path temp) throws Exception {
        Path launcher = temp.resolve("Launcher.java");
        Path real = temp.resolve("RealApp.java");
        Files.writeString(
                launcher,
                """
                        package sc;

                        import java.io.InputStream;
                        import java.util.jar.Manifest;

                        public class Launcher {
                            public static void main(String[] args) throws Exception {
                                try (InputStream in = Launcher.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
                                    Manifest m = new Manifest(in);
                                    String start = m.getMainAttributes().getValue("Start-Class");
                                    Class.forName(start).getMethod("main", String[].class).invoke(null, (Object) args);
                                }
                            }
                        }
                        """);
        Files.writeString(
                real,
                """
                        package sc;

                        public class RealApp {
                            public static void main(String[] args) {
                                System.out.println("START_CLASS_OK");
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, launcher, real);

        LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
        attrs.put("Main-Class", "sc.Launcher");
        attrs.put("Start-Class", "sc.RealApp");
        Path inJar = temp.resolve("in.jar");
        writeJarWithManifest(inJar, classes, attrs, "sc/Launcher.class", "sc/RealApp.class");

        JarMapping mapping = JarMapping.fromJar(inJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(mapping, obf);

        try (JarFile jf = new JarFile(obf.toFile())) {
            Manifest outMf = jf.getManifest();
            assertNotNull(outMf);
            String start = outMf.getMainAttributes().getValue("Start-Class");
            String main = outMf.getMainAttributes().getValue("Main-Class");
            assertNotNull(start);
            assertNotNull(main);
            assertNotEquals("sc.RealApp", start, "Start-Class should be rewritten when RealApp was renamed");
            assertNotEquals("sc.Launcher", main, "Main-Class should be rewritten when Launcher was renamed");
            assertNotNull(
                    jf.getEntry(main.replace('.', '/') + ".class"),
                    () -> "Main-Class entry missing: " + main);
        }

        assertJarSingleMarkerLine(obf, "START_CLASS_OK");
    }

    @Test
    void customNamePrefix_obfuscatedFatJarStillRuns(@TempDir Path temp) throws Exception {
        Path appJava = temp.resolve("App.java");
        Path libJava = temp.resolve("Lib.java");
        Files.writeString(
                appJava,
                """
                        package pref;

                        public class App {
                            public static void main(String[] args) {
                                Lib.go();
                                System.out.println("PREFIX_OK");
                            }

                            public void x() {}
                        }
                        """);
        Files.writeString(
                libJava,
                """
                        package pref;

                        public class Lib {
                            public static void go() {
                                new App().x();
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, appJava, libJava);
        Path appJar = temp.resolve("app.jar");
        writeJarWithMain(appJar, classes, "pref.App", "pref/App.class", "pref/Lib.class");

        JarMapping mapping = JarMapping.fromJar(appJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(
                mapping,
                obf,
                Map.of(ObfuscationPlugin.CFG_NAME_PREFIX, "zz"));

        assertJarSingleMarkerLine(obf, "PREFIX_OK");
    }

    @Test
    void obfuscateClassesDisabled_thinJarStillRuns(@TempDir Path temp) throws Exception {
        Path appJava = temp.resolve("App.java");
        Files.writeString(
                appJava,
                """
                        package norename;

                        public class App {
                            public static void main(String[] args) {
                                new App().x();
                                System.out.println("NO_CLASS_RENAME_OK");
                            }

                            void x() {}
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, appJava);
        Path inJar = temp.resolve("in.jar");
        writeJarWithMain(inJar, classes, "norename.App", "norename/App.class");

        JarMapping mapping = JarMapping.fromJar(inJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(
                mapping,
                obf,
                Map.of(ObfuscationPlugin.CFG_OBFUSCATE_CLASSES, Boolean.FALSE));

        assertJarSingleMarkerLine(obf, "NO_CLASS_RENAME_OK");
    }

    @Test
    void obfuscateMethodsDisabled_obfuscatedJarStillRuns(@TempDir Path temp) throws Exception {
        Path app = temp.resolve("App.java");
        Files.writeString(
                app,
                """
                        package nometh;

                        public class App {
                            public static void main(String[] args) {
                                System.out.println("NO_METHOD_RENAME_OK");
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, app);
        Path inJar = temp.resolve("in.jar");
        writeJarWithMain(inJar, classes, "nometh.App", "nometh/App.class");

        JarMapping mapping = JarMapping.fromJar(inJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(
                mapping,
                obf,
                Map.of(ObfuscationPlugin.CFG_OBFUSCATE_METHODS, Boolean.FALSE));

        assertJarSingleMarkerLine(obf, "NO_METHOD_RENAME_OK");
    }

    @Test
    void obfuscateFieldsDisabled_obfuscatedJarStillRuns(@TempDir Path temp) throws Exception {
        Path app = temp.resolve("App.java");
        Files.writeString(
                app,
                """
                        package nofield;

                        public class App {
                            int v = 7;

                            public static void main(String[] args) {
                                System.out.println(new App().v == 7 ? "NO_FIELD_RENAME_OK" : "bad");
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, app);
        Path inJar = temp.resolve("in.jar");
        writeJarWithMain(inJar, classes, "nofield.App", "nofield/App.class");

        JarMapping mapping = JarMapping.fromJar(inJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(
                mapping,
                obf,
                Map.of(ObfuscationPlugin.CFG_OBFUSCATE_FIELDS, Boolean.FALSE));

        assertJarSingleMarkerLine(obf, "NO_FIELD_RENAME_OK");
    }

    @Test
    void outputJar_manifestMainClassPointsAtRenamedEntry(@TempDir Path temp) throws Exception {
        Path app = temp.resolve("App.java");
        Files.writeString(
                app,
                """
                        package mc;

                        public class App {
                            public static void main(String[] args) {
                                System.out.println("MC_CHECK");
                            }
                        }
                        """);
        Path classes = temp.resolve("classes");
        javac(classes, null, app);
        Path inJar = temp.resolve("in.jar");
        writeJarWithMain(inJar, classes, "mc.App", "mc/App.class");

        JarMapping mapping = JarMapping.fromJar(inJar);
        Path obf = temp.resolve("obf.jar");
        obfuscateAndWrite(mapping, obf);

        try (JarFile jf = new JarFile(obf.toFile())) {
            Manifest m = jf.getManifest();
            assertNotNull(m);
            String mainClass = m.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            assertNotNull(mainClass);
            assertTrue(
                    mainClass.matches("[a-zA-Z_]\\w*(\\.[a-zA-Z_]\\w*)*"),
                    () -> "Main-Class should be a valid binary name: " + mainClass);
            String internal = mainClass.replace('.', '/') + ".class";
            assertNotNull(jf.getEntry(internal), () -> "Main-Class entry missing in JAR: " + internal);
        }

        assertJarSingleMarkerLine(obf, "MC_CHECK");
    }
}
