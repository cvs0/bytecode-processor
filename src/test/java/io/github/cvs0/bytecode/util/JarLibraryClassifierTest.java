package io.github.cvs0.bytecode.util;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarLibraryClassifierTest {

    @Test
    void noManifest_everythingStaysApplication() {
        JarMapping m = new JarMapping("t.jar");
        m.addClass(new ProgramClass("org/other/Lib"));
        m.addClass(new ProgramClass("app/Main"));
        JarLibraryClassifier.classify(m);
        assertFalse(m.getProgramClass("org/other/Lib").isEmbeddedLibrary());
        assertFalse(m.getProgramClass("app/Main").isEmbeddedLibrary());
    }

    @Test
    void manifestMainClass_marksOtherPackagesAsEmbedded() {
        JarMapping m = new JarMapping("t.jar");
        m.addResource(
                JarLayout.MANIFEST,
                "Manifest-Version: 1.0\nMain-Class: app.Main\n\n".getBytes(StandardCharsets.UTF_8));
        m.addClass(new ProgramClass("app/Main"));
        m.addClass(new ProgramClass("org/shaded/Lib"));
        JarLibraryClassifier.classify(m);
        assertFalse(m.getProgramClass("app/Main").isEmbeddedLibrary());
        assertTrue(m.getProgramClass("org/shaded/Lib").isEmbeddedLibrary());
    }

    @Test
    void longestCommonPrefix_twoMainsSameTree() {
        assertEquals(
                "io/app",
                JarLibraryClassifier.longestCommonPackagePrefix(
                        java.util.Set.of("io/app/cli", "io/app/service")));
    }

    @Test
    void longestCommonPrefix_disjoint_returnsNull() {
        assertEquals(
                null,
                JarLibraryClassifier.longestCommonPackagePrefix(java.util.Set.of("com/a", "org/b")));
    }

    @Test
    void getApplicationClasses_filtersEmbedded() {
        JarMapping m = new JarMapping("t.jar");
        ProgramClass a = new ProgramClass("a/A");
        ProgramClass b = new ProgramClass("b/B");
        b.setEmbeddedLibrary(true);
        m.addClass(a);
        m.addClass(b);
        assertEquals(1, m.getApplicationClasses().size());
        assertEquals(1, m.getEmbeddedLibraryProgramClasses().size());
    }
}
