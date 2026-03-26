package io.github.cvs0.bytecode.runtime;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.runtime.clazz.ClassBytesSource;
import io.github.cvs0.bytecode.runtime.clazz.ClassLoaderInjector;
import io.github.cvs0.bytecode.runtime.resource.ResourceBytesSource;
import io.github.cvs0.bytecode.runtime.url.ByteBackedUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the runtime/loader API features:
 * <ul>
 *   <li>{@link ClassBytesSource} / {@link ResourceBytesSource} interfaces</li>
 *   <li>{@link JarMapping} as a class/resource bytes source</li>
 *   <li>{@link JarMapping#merge(JarMapping)}</li>
 *   <li>{@link JarMapping#toClassBytesMap()}</li>
 *   <li>{@link JarMapping#writeToBytes()}</li>
 *   <li>{@link JarWriter#writeToBytes(JarMapping)}</li>
 *   <li>{@link ByteBackedUrl}</li>
 * </ul>
 */
class RuntimeLoaderApiTest {

    private JarMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new JarMapping();
    }

    private static byte[] generateClassBytes(String internalName, String superName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null, superName, null);
        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    // ========================================================================
    //  ClassBytesSource
    // ========================================================================

    @Test
    void classBytesSourceFromMapResolves() throws IOException {
        byte[] bytes = generateClassBytes("com/a/Foo", "java/lang/Object");
        ClassBytesSource source = ClassBytesSource.fromMap(Map.of("com/a/Foo", bytes));
        assertArrayEquals(bytes, source.getClassBytes("com/a/Foo"));
        assertNull(source.getClassBytes("com/a/Missing"));
    }

    @Test
    void classBytesSourceOrElseChainsCorrectly() throws IOException {
        byte[] primary = new byte[]{1, 2, 3};
        byte[] fallback = new byte[]{4, 5, 6};

        ClassBytesSource first = name -> "hit".equals(name) ? primary : null;
        ClassBytesSource second = name -> "miss".equals(name) ? fallback : null;
        ClassBytesSource chain = first.orElse(second);

        assertArrayEquals(primary, chain.getClassBytes("hit"));
        assertArrayEquals(fallback, chain.getClassBytes("miss"));
        assertNull(chain.getClassBytes("neither"));
    }

    @Test
    void classBytesSourcePrimaryWinsOverFallback() throws IOException {
        byte[] primary = new byte[]{1};
        byte[] fallback = new byte[]{2};

        ClassBytesSource chain = ((ClassBytesSource) n -> primary).orElse(n -> fallback);
        assertArrayEquals(primary, chain.getClassBytes("any"));
    }

    // ========================================================================
    //  ResourceBytesSource
    // ========================================================================

    @Test
    void resourceBytesSourceFromMapResolves() throws IOException {
        byte[] data = "service=impl".getBytes();
        ResourceBytesSource source = ResourceBytesSource.fromMap(
                Map.of("META-INF/services/com.foo.SPI", data));
        assertArrayEquals(data, source.getResourceBytes("META-INF/services/com.foo.SPI"));
        assertNull(source.getResourceBytes("missing"));
    }

    @Test
    void resourceBytesSourceOrElseChainsCorrectly() throws IOException {
        byte[] native_ = "native".getBytes();
        byte[] disk = "disk".getBytes();

        ResourceBytesSource chain = ((ResourceBytesSource) p -> "a".equals(p) ? native_ : null)
                .orElse(p -> "b".equals(p) ? disk : null);

        assertArrayEquals(native_, chain.getResourceBytes("a"));
        assertArrayEquals(disk, chain.getResourceBytes("b"));
        assertNull(chain.getResourceBytes("c"));
    }

    // ========================================================================
    //  JarMapping as ClassBytesSource / ResourceBytesSource
    // ========================================================================

    @Test
    void jarMappingProvidesClassBytes() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/Foo", "java/lang/Object"));
        mapping.resolveHierarchy();

        byte[] resolved = mapping.getClassBytes("com/a/Foo");
        assertNotNull(resolved);
        assertTrue(resolved.length > 0);

        // Verify round-trip: parse the bytes and check the name
        ClassReader cr = new ClassReader(resolved);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        assertEquals("com/a/Foo", cn.name);
    }

    @Test
    void jarMappingReturnsNullForMissingClass() throws IOException {
        assertNull(mapping.getClassBytes("com/a/NotHere"));
    }

    @Test
    void jarMappingProvidesResourceBytes() {
        byte[] data = "hello".getBytes();
        mapping.addResource("config.txt", data);
        assertArrayEquals(data, mapping.getResourceBytes("config.txt"));
        assertNull(mapping.getResourceBytes("missing.txt"));
    }

    @Test
    void jarMappingAsClassBytesSourceInChain() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/jar/A", "java/lang/Object"));
        mapping.resolveHierarchy();

        byte[] nativeBytes = new byte[]{42};
        ClassBytesSource nativeStore = name -> "com/native/B".equals(name) ? nativeBytes : null;

        // native check first, then fall back to JarMapping
        ClassBytesSource chain = nativeStore.orElse(mapping);

        assertArrayEquals(nativeBytes, chain.getClassBytes("com/native/B"));
        assertNotNull(chain.getClassBytes("com/jar/A")); // resolved from mapping
        assertNull(chain.getClassBytes("com/missing/C"));
    }

    // ========================================================================
    //  merge
    // ========================================================================

    @Test
    void mergeAddsClassesFromOther() throws IOException {
        JarMapping loader = new JarMapping();
        loader.addClassFromBytes(generateClassBytes("com/loader/Boot", "java/lang/Object"));

        JarMapping payload = new JarMapping();
        payload.addClassFromBytes(generateClassBytes("com/payload/Feature", "java/lang/Object"));
        payload.addResource("assets/icon.png", new byte[]{1, 2, 3});

        loader.merge(payload);

        assertNotNull(loader.getProgramClass("com/loader/Boot"));
        assertNotNull(loader.getProgramClass("com/payload/Feature"));
        assertNotNull(loader.getResource("assets/icon.png"));
        assertEquals(2, loader.getProgramClasses().size());
    }

    @Test
    void mergeOverwritesExistingEntries() throws IOException {
        mapping.addResource("config.txt", "old".getBytes());
        JarMapping other = new JarMapping();
        other.addResource("config.txt", "new".getBytes());

        mapping.merge(other);
        assertArrayEquals("new".getBytes(), mapping.getResource("config.txt"));
    }

    @Test
    void mergeDoesNotResolveHierarchyAutomatically() throws IOException {
        JarMapping a = new JarMapping();
        a.addClassFromBytes(generateClassBytes("com/a/Base", "java/lang/Object"));

        JarMapping b = new JarMapping();
        b.addClassFromBytes(generateClassBytes("com/a/Child", "com/a/Base"));

        a.merge(b);

        ProgramClass child = a.getProgramClass("com/a/Child");
        // Hierarchy not resolved yet — parent should be null
        assertNull(child.getParentProgramClass());

        // Now resolve
        a.resolveHierarchy();
        assertNotNull(child.getParentProgramClass());
    }

    // ========================================================================
    //  toClassBytesMap
    // ========================================================================

    @Test
    void toClassBytesMapExportsAllClasses() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/Foo", "java/lang/Object"));
        mapping.addClassFromBytes(generateClassBytes("com/a/Bar", "java/lang/Object"));
        mapping.resolveHierarchy();

        Map<String, byte[]> bytesMap = mapping.toClassBytesMap();
        assertEquals(2, bytesMap.size());
        assertTrue(bytesMap.containsKey("com/a/Foo"));
        assertTrue(bytesMap.containsKey("com/a/Bar"));

        // Verify each entry is valid bytecode
        for (Map.Entry<String, byte[]> entry : bytesMap.entrySet()) {
            ClassReader cr = new ClassReader(entry.getValue());
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            assertEquals(entry.getKey(), cn.name);
        }
    }

    @Test
    void toClassBytesMapIsUnmodifiable() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/Foo", "java/lang/Object"));
        Map<String, byte[]> bytesMap = mapping.toClassBytesMap();
        assertThrows(UnsupportedOperationException.class,
                () -> bytesMap.put("hack", new byte[0]));
    }

    @Test
    void toClassBytesMapFeedableIntoClassBytesSource() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/X", "java/lang/Object"));
        mapping.resolveHierarchy();

        Map<String, byte[]> exported = mapping.toClassBytesMap();
        ClassBytesSource source = ClassBytesSource.fromMap(exported);

        assertNotNull(source.getClassBytes("com/a/X"));
    }

    // ========================================================================
    //  writeToBytes — in-memory JAR
    // ========================================================================

    @Test
    void writeToBytesProducesValidJar() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/Foo", "java/lang/Object"));
        mapping.addResource("META-INF/services/com.foo.SPI", "impl".getBytes());
        mapping.resolveHierarchy();

        byte[] jarBytes = mapping.writeToBytes();
        assertNotNull(jarBytes);
        assertTrue(jarBytes.length > 0);

        // Parse the JAR
        try (JarInputStream jis = new JarInputStream(new java.io.ByteArrayInputStream(jarBytes))) {
            assertNotNull(jis.getManifest());
            boolean foundClass = false;
            boolean foundResource = false;
            java.util.jar.JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().equals("com/a/Foo.class")) {
                    foundClass = true;
                } else if (entry.getName().equals("META-INF/services/com.foo.SPI")) {
                    foundResource = true;
                }
            }
            assertTrue(foundClass, "JAR should contain the class entry");
            assertTrue(foundResource, "JAR should contain the resource entry");
        }
    }

    @Test
    void writeToBytesMatchesWriteToFile() throws Exception {
        mapping.addClassFromBytes(generateClassBytes("com/a/A", "java/lang/Object"));
        mapping.resolveHierarchy();

        byte[] inMemory = mapping.writeToBytes();
        assertTrue(inMemory.length > 0);

        // Both should produce parseable JARs with the same class
        try (JarInputStream jis = new JarInputStream(new java.io.ByteArrayInputStream(inMemory))) {
            java.util.jar.JarEntry entry;
            boolean found = false;
            while ((entry = jis.getNextJarEntry()) != null) {
                if ("com/a/A.class".equals(entry.getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    // ========================================================================
    //  ByteBackedUrl
    // ========================================================================

    @Test
    void byteBackedUrlServesContent() throws IOException {
        byte[] content = "hello world".getBytes();
        URL url = ByteBackedUrl.create("test.txt", content);

        assertNotNull(url);
        assertEquals("memory", url.getProtocol());

        try (InputStream is = url.openStream()) {
            byte[] read = is.readAllBytes();
            assertArrayEquals(content, read);
        }
    }

    @Test
    void byteBackedUrlReportsCorrectLength() throws IOException {
        byte[] content = new byte[42];
        URL url = ByteBackedUrl.create("data.bin", content);
        assertEquals(42, url.openConnection().getContentLength());
        assertEquals(42L, url.openConnection().getContentLengthLong());
    }

    @Test
    void byteBackedUrlIsDefensiveCopy() throws IOException {
        byte[] original = {1, 2, 3};
        URL url = ByteBackedUrl.create("data.bin", original);

        // Mutate the original
        original[0] = 99;

        try (InputStream is = url.openStream()) {
            byte[] read = is.readAllBytes();
            assertEquals(1, read[0], "URL should serve the original bytes, not the mutated array");
        }
    }

    @Test
    void byteBackedUrlWithJarBytes() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/Foo", "java/lang/Object"));
        mapping.resolveHierarchy();

        byte[] jarBytes = mapping.writeToBytes();
        URL url = ByteBackedUrl.create("payload.jar", jarBytes);

        // Verify we can read a JAR back from the URL
        try (JarInputStream jis = new JarInputStream(url.openStream())) {
            assertNotNull(jis.getManifest());
            boolean found = false;
            java.util.jar.JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if ("com/a/Foo.class".equals(entry.getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Should find the class in the JAR served from the byte-backed URL");
        }
    }

    @Test
    void byteBackedUrlRejectsNull() {
        assertThrows(NullPointerException.class, () -> ByteBackedUrl.create(null, new byte[0]));
        assertThrows(NullPointerException.class, () -> ByteBackedUrl.create("x", null));
    }

    // ========================================================================
    //  ByteBackedUrl — builder API
    // ========================================================================

    @Test
    void builderCustomProtocolAndHost() throws IOException {
        byte[] content = "data".getBytes();
        URL url = ByteBackedUrl.builder("test.dat", content)
                .protocol("bytecode")
                .host("internal")
                .port(8080)
                .build();

        assertEquals("bytecode", url.getProtocol());
        assertEquals("internal", url.getHost());
        assertEquals(8080, url.getPort());

        try (InputStream is = url.openStream()) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    @Test
    void builderDefaultsMatchCreate() throws IOException {
        byte[] content = "same".getBytes();
        URL fromCreate = ByteBackedUrl.create("a.txt", content);
        URL fromBuilder = ByteBackedUrl.builder("a.txt", content).build();

        assertEquals(fromCreate.getProtocol(), fromBuilder.getProtocol());
        assertEquals(fromCreate.getHost(), fromBuilder.getHost());

        try (InputStream is = fromBuilder.openStream()) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    // ========================================================================
    //  ClassLoaderInjector
    // ========================================================================

    /** A test classloader with a public addURL(URL) that records injected URLs. */
    private static final class RecordingClassLoader extends java.net.URLClassLoader {
        final java.util.List<URL> injected = new java.util.ArrayList<>();

        RecordingClassLoader() {
            super(new URL[0], null);
        }

        @Override
        protected void addURL(URL url) {
            injected.add(url);
        }
    }

    @Test
    void classLoaderInjectorForAddUrl() throws Exception {
        RecordingClassLoader cl = new RecordingClassLoader();
        ClassLoaderInjector injector = ClassLoaderInjector.forAddUrl(cl);

        URL url = ByteBackedUrl.create("test.jar", new byte[]{1});
        injector.inject(url);

        assertEquals(1, cl.injected.size());
        assertSame(url, cl.injected.getFirst());
    }

    @Test
    void classLoaderInjectorInjectBytes() throws Exception {
        RecordingClassLoader cl = new RecordingClassLoader();
        ClassLoaderInjector injector = ClassLoaderInjector.forAddUrl(cl);

        injector.injectBytes("payload.jar", new byte[]{42});

        assertEquals(1, cl.injected.size());
        URL injectedUrl = cl.injected.getFirst();
        assertEquals("memory", injectedUrl.getProtocol());
        try (InputStream is = injectedUrl.openStream()) {
            assertArrayEquals(new byte[]{42}, is.readAllBytes());
        }
    }

    @Test
    void classLoaderInjectorInjectMapping() throws Exception {
        mapping.addClassFromBytes(generateClassBytes("com/a/Foo", "java/lang/Object"));
        mapping.resolveHierarchy();

        RecordingClassLoader cl = new RecordingClassLoader();
        ClassLoaderInjector injector = ClassLoaderInjector.forAddUrl(cl);
        injector.injectMapping(mapping);

        assertEquals(1, cl.injected.size());

        // Verify the injected URL contains a valid JAR with our class
        try (java.util.jar.JarInputStream jis =
                     new java.util.jar.JarInputStream(cl.injected.getFirst().openStream())) {
            boolean found = false;
            java.util.jar.JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if ("com/a/Foo.class".equals(entry.getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Injected JAR should contain the class");
        }
    }

    @Test
    void classLoaderInjectorInjectClass() throws Exception {
        ProgramClass pc = mapping.addClassFromBytes(
                generateClassBytes("com/a/Single", "java/lang/Object"));
        mapping.resolveHierarchy();

        RecordingClassLoader cl = new RecordingClassLoader();
        ClassLoaderInjector injector = ClassLoaderInjector.forAddUrl(cl);
        injector.injectClass(pc, mapping);

        assertEquals(1, cl.injected.size());
        try (InputStream is = cl.injected.getFirst().openStream()) {
            byte[] bytes = is.readAllBytes();
            assertTrue(bytes.length > 0);
            // Verify it's valid class bytes
            ClassReader cr = new ClassReader(bytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            assertEquals("com/a/Single", cn.name);
        }
    }

    @Test
    void classLoaderInjectorGetHandle() throws Exception {
        RecordingClassLoader cl = new RecordingClassLoader();
        ClassLoaderInjector injector = ClassLoaderInjector.forAddUrl(cl);

        java.lang.invoke.MethodHandle handle = injector.getHandle();
        assertNotNull(handle);

        // Invoke handle directly (simulates what native code would do)
        URL url = ByteBackedUrl.create("native.jar", new byte[0]);
        try {
            handle.invoke(url);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        assertEquals(1, cl.injected.size());
    }

    @Test
    void classLoaderInjectorForMethodCustomName() throws Exception {
        // Test with a classloader that has a differently-named method
        var cl = new java.net.URLClassLoader(new URL[0], null) {
            final java.util.List<URL> received = new java.util.ArrayList<>();
            @SuppressWarnings("unused")
            void addUrlFwd(URL url) { received.add(url); }
        };

        ClassLoaderInjector injector = ClassLoaderInjector.forMethod(cl, "addUrlFwd");
        URL url = ByteBackedUrl.create("fwd.jar", new byte[0]);
        injector.inject(url);
        assertEquals(1, cl.received.size());
    }

    @Test
    void classLoaderInjectorFromHandle() throws Exception {
        RecordingClassLoader cl = new RecordingClassLoader();
        java.lang.invoke.MethodHandles.Lookup lookup =
                java.lang.invoke.MethodHandles.privateLookupIn(cl.getClass(), java.lang.invoke.MethodHandles.lookup());
        java.lang.invoke.MethodHandle handle = lookup.findVirtual(
                cl.getClass(), "addURL",
                java.lang.invoke.MethodType.methodType(void.class, URL.class)).bindTo(cl);

        ClassLoaderInjector injector = ClassLoaderInjector.fromHandle(cl, handle);
        injector.inject(ByteBackedUrl.create("x.jar", new byte[0]));
        assertEquals(1, cl.injected.size());
    }

    @Test
    void classLoaderInjectorGetClassLoader() throws Exception {
        RecordingClassLoader cl = new RecordingClassLoader();
        ClassLoaderInjector injector = ClassLoaderInjector.forAddUrl(cl);
        assertSame(cl, injector.getClassLoader());
    }

    // ========================================================================
    //  End-to-end: loader + payload pattern
    // ========================================================================

    @Test
    void endToEndLoaderPayloadMergeAndExport() throws IOException {
        // Simulate: loader has bootstrap classes
        JarMapping loader = new JarMapping();
        loader.addClassFromBytes(generateClassBytes("com/mod/Loader", "java/lang/Object"));

        // Simulate: payload arrives from network/native
        JarMapping payload = new JarMapping();
        payload.addClassFromBytes(generateClassBytes("com/mod/Feature", "java/lang/Object"));
        payload.addClassFromBytes(generateClassBytes("com/mod/Mixin", "java/lang/Object"));
        payload.addResource("assets/mod/icon.png", new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        // Merge payload into loader
        loader.merge(payload);
        loader.resolveHierarchy();

        // Verify the merged mapping has everything
        assertEquals(3, loader.getProgramClasses().size());
        assertNotNull(loader.getResource("assets/mod/icon.png"));

        // Export as class bytes map (for custom classloader)
        Map<String, byte[]> bytesMap = loader.toClassBytesMap();
        assertEquals(3, bytesMap.size());

        // Export as in-memory JAR (for URL injection)
        byte[] jarBytes = loader.writeToBytes();
        URL jarUrl = ByteBackedUrl.create("mod-payload.jar", jarBytes);

        try (JarInputStream jis = new JarInputStream(jarUrl.openStream())) {
            int classCount = 0;
            java.util.jar.JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    classCount++;
                }
            }
            assertEquals(3, classCount);
        }

        // Use as a ClassBytesSource in a chain
        ClassBytesSource nativeStore = name -> null; // native returns nothing in this test
        ClassBytesSource chain = nativeStore.orElse(loader);
        assertNotNull(chain.getClassBytes("com/mod/Feature"));
    }
}
