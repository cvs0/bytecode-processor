package io.github.cvs0.bytecode.io;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.FieldKey;
import io.github.cvs0.bytecode.MethodKey;
import io.github.cvs0.bytecode.analysis.DependencyAnalyzer;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.transform.MappingRemapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the streaming / incremental class loading APIs:
 * <ul>
 *   <li>{@link JarMapping#addClassFromBytes(byte[])}</li>
 *   <li>{@link JarMapping#resolveHierarchy()}</li>
 *   <li>{@link JarWriter#remapClassBytes(ProgramClass, MappingRemapper, JarMapping)}</li>
 *   <li>{@link JarWriter#getClassBytes(ProgramClass, JarMapping)}</li>
 *   <li>{@link JarReader#readClass(byte[])}</li>
 * </ul>
 */
class StreamingApiTest {

    private JarMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new JarMapping();
    }

    // ========================================================================
    //  Helper — generate minimal class bytes via ASM
    // ========================================================================

    private static byte[] generateClassBytes(String internalName, String superName, String... interfaces) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null, superName, interfaces);
        // Add a default constructor
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

    private static byte[] generateClassWithField(String internalName, String superName,
                                                   String fieldName, String fieldDesc) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null, superName, null);
        cw.visitField(Opcodes.ACC_PRIVATE, fieldName, fieldDesc, null, null).visitEnd();
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

    private static byte[] generateClassWithMethod(String internalName, String superName,
                                                    String methodName, String methodDesc) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null, superName, null);
        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        var m = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, methodDesc, null, null);
        m.visitCode();
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0, 1);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    // ========================================================================
    //  JarMapping() no-arg constructor
    // ========================================================================

    @Test
    void noArgConstructorCreatesEmptyMapping() {
        JarMapping m = new JarMapping();
        assertEquals("<in-memory>", m.getJarPath());
        assertTrue(m.getProgramClasses().isEmpty());
    }

    // ========================================================================
    //  readClass(byte[]) enrichment completeness
    // ========================================================================

    @Test
    void readClassFromBytesPopulatesAllFields() throws IOException {
        byte[] bytes = generateClassBytes("com/example/Foo", "java/lang/Object");
        ProgramClass pc = JarReader.readClass(bytes);

        assertEquals("com/example/Foo", pc.getName());
        assertEquals("java/lang/Object", pc.getSuperName());
        assertEquals("com/example/Foo.class", pc.getJarEntryName());
        assertTrue(pc.getClassVersion() > 0, "classVersion should be set");
        assertNotNull(pc.getClassNode(), "ClassNode should be present");
        assertFalse(pc.getMethods().isEmpty(), "should have at least <init>");
    }

    @Test
    void readClassFromBytesRejectsEmptyName() {
        // An empty class file (just the magic/version header) should fail
        assertThrows(Exception.class, () -> JarReader.readClass(new byte[]{
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0, 0, 0, 65, 0, 0
        }));
    }

    // ========================================================================
    //  addClassFromBytes
    // ========================================================================

    @Test
    void addClassFromBytesParsesAndIndexes() throws IOException {
        byte[] bytes = generateClassBytes("com/example/Bar", "java/lang/Object");
        ProgramClass pc = mapping.addClassFromBytes(bytes);

        assertEquals("com/example/Bar", pc.getName());
        assertSame(pc, mapping.getProgramClass("com/example/Bar"));
        assertEquals(1, mapping.getProgramClasses().size());
    }

    @Test
    void addMultipleClassesFromBytes() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/A", "java/lang/Object"));
        mapping.addClassFromBytes(generateClassBytes("com/a/B", "java/lang/Object"));
        mapping.addClassFromBytes(generateClassBytes("com/a/C", "com/a/A"));

        assertEquals(3, mapping.getProgramClasses().size());
        assertNotNull(mapping.getProgramClass("com/a/A"));
        assertNotNull(mapping.getProgramClass("com/a/B"));
        assertNotNull(mapping.getProgramClass("com/a/C"));
    }

    // ========================================================================
    //  resolveHierarchy
    // ========================================================================

    @Test
    void resolveHierarchyLinksParentAndChild() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/Base", "java/lang/Object"));
        mapping.addClassFromBytes(generateClassBytes("com/a/Child", "com/a/Base"));
        mapping.resolveHierarchy();

        ProgramClass base = mapping.getProgramClass("com/a/Base");
        ProgramClass child = mapping.getProgramClass("com/a/Child");

        assertSame(base, child.getParentProgramClass());
        assertTrue(base.getChildProgramClasses().contains(child));
    }

    @Test
    void resolveHierarchyLinksInterfaces() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/MyIface", "java/lang/Object"));
        mapping.addClassFromBytes(generateClassBytes("com/a/Impl", "java/lang/Object", "com/a/MyIface"));
        mapping.resolveHierarchy();

        ProgramClass iface = mapping.getProgramClass("com/a/MyIface");
        ProgramClass impl = mapping.getProgramClass("com/a/Impl");

        assertTrue(impl.getResolvedInterfaces().contains(iface));
        assertTrue(iface.getChildProgramClasses().contains(impl));
    }

    @Test
    void resolveHierarchyTracksUnresolvedExternalTypes() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/Foo", "java/lang/Object", "java/io/Serializable"));
        mapping.resolveHierarchy();

        ProgramClass foo = mapping.getProgramClass("com/a/Foo");
        // java/lang/Object and java/io/Serializable are external
        assertTrue(foo.getUnresolvedSuperTypes().contains("java/lang/Object"));
        assertTrue(foo.getUnresolvedSuperTypes().contains("java/io/Serializable"));
    }

    @Test
    void resolveHierarchySafeToCallMultipleTimes() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/A", "java/lang/Object"));
        mapping.addClassFromBytes(generateClassBytes("com/a/B", "com/a/A"));

        mapping.resolveHierarchy();
        mapping.resolveHierarchy(); // second call should not duplicate links

        ProgramClass a = mapping.getProgramClass("com/a/A");
        assertEquals(1, a.getChildProgramClasses().size(), "child list should not have duplicates");
    }

    @Test
    void resolveHierarchyAfterIncrementalAdd() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/A", "java/lang/Object"));
        mapping.resolveHierarchy();

        // Add another class later
        mapping.addClassFromBytes(generateClassBytes("com/a/B", "com/a/A"));
        mapping.resolveHierarchy();

        ProgramClass a = mapping.getProgramClass("com/a/A");
        ProgramClass b = mapping.getProgramClass("com/a/B");
        assertSame(a, b.getParentProgramClass());
        assertEquals(1, a.getChildProgramClasses().size());
    }

    // ========================================================================
    //  DependencyAnalyzer on incrementally-built mapping
    // ========================================================================

    @Test
    void topologicalOrderWorksOnIncrementalMapping() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/Base", "java/lang/Object"));
        mapping.addClassFromBytes(generateClassBytes("com/a/Mid", "com/a/Base"));
        mapping.addClassFromBytes(generateClassBytes("com/a/Leaf", "com/a/Mid"));
        mapping.resolveHierarchy();

        List<String> order = DependencyAnalyzer.getTopologicalOrder(mapping);
        assertFalse(order.isEmpty());

        int baseIdx = order.indexOf("com/a/Base");
        int midIdx = order.indexOf("com/a/Mid");
        int leafIdx = order.indexOf("com/a/Leaf");
        assertTrue(baseIdx < midIdx, "Base should come before Mid");
        assertTrue(midIdx < leafIdx, "Mid should come before Leaf");
    }

    // ========================================================================
    //  getClassBytes with JarMapping
    // ========================================================================

    @Test
    void getClassBytesWithMappingProducesValidBytecode() throws IOException {
        byte[] original = generateClassBytes("com/a/Foo", "java/lang/Object");
        ProgramClass pc = mapping.addClassFromBytes(original);
        mapping.resolveHierarchy();

        byte[] output = JarWriter.getClassBytes(pc, mapping);
        assertNotNull(output);
        assertTrue(output.length > 0);

        // Verify the output is valid by parsing it
        ClassReader cr = new ClassReader(output);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        assertEquals("com/a/Foo", cn.name);
    }

    // ========================================================================
    //  remapClassBytes — single-class remap
    // ========================================================================

    @Test
    void remapClassBytesRenamesClass() throws IOException {
        byte[] original = generateClassBytes("com/a/Original", "java/lang/Object");
        ProgramClass pc = mapping.addClassFromBytes(original);
        mapping.resolveHierarchy();

        MappingRemapper remapper = new MappingRemapper(
                Map.of("com/a/Original", "com/b/Renamed"),
                Map.of(),
                Map.of()
        );

        byte[] remapped = JarWriter.remapClassBytes(pc, remapper, mapping);
        ClassReader cr = new ClassReader(remapped);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        assertEquals("com/b/Renamed", cn.name);
    }

    @Test
    void remapClassBytesRenamesFieldAndMethod() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/a/Foo", null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PRIVATE, "myField", "I", null, null).visitEnd();
        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        var m = cw.visitMethod(Opcodes.ACC_PUBLIC, "doWork", "()V", null, null);
        m.visitCode();
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0, 1);
        m.visitEnd();
        cw.visitEnd();

        ProgramClass pc = mapping.addClassFromBytes(cw.toByteArray());
        mapping.resolveHierarchy();

        MappingRemapper remapper = new MappingRemapper(
                Map.of("com/a/Foo", "com/b/Bar"),
                Map.of(FieldKey.of("com/a/Foo", "myField"), "a"),
                Map.of(MethodKey.of("com/a/Foo", "doWork", "()V"), "b")
        );

        byte[] remapped = JarWriter.remapClassBytes(pc, remapper, mapping);
        ClassReader cr = new ClassReader(remapped);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        assertEquals("com/b/Bar", cn.name);
        assertTrue(cn.fields.stream().anyMatch(f -> "a".equals(f.name)));
        assertTrue(cn.methods.stream().anyMatch(m2 -> "b".equals(m2.name)));
    }

    @Test
    void remapClassBytesRemapsTypeReferences() throws IOException {
        // Create a class that references another class in a field type
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/a/User", null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PRIVATE, "dep", "Lcom/a/Dep;", null, null).visitEnd();
        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();

        ProgramClass pc = mapping.addClassFromBytes(cw.toByteArray());
        mapping.addClassFromBytes(generateClassBytes("com/a/Dep", "java/lang/Object"));
        mapping.resolveHierarchy();

        MappingRemapper remapper = new MappingRemapper(
                Map.of("com/a/Dep", "com/b/RenamedDep", "com/a/User", "com/a/User"),
                Map.of(),
                Map.of()
        );

        byte[] remapped = JarWriter.remapClassBytes(pc, remapper, mapping);
        ClassReader cr = new ClassReader(remapped);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // The field descriptor should reference the renamed class
        assertTrue(cn.fields.stream().anyMatch(f -> "Lcom/b/RenamedDep;".equals(f.desc)),
                "Field descriptor should reference renamed class");
    }

    @Test
    void remapClassBytesRejectsNullClassNode() {
        ProgramClass pc = new ProgramClass("com/a/NoNode");
        MappingRemapper remapper = new MappingRemapper(Map.of(), Map.of(), Map.of());
        assertThrows(IllegalStateException.class, () -> JarWriter.remapClassBytes(pc, remapper, null));
    }

    // ========================================================================
    //  clearHierarchyLinks
    // ========================================================================

    @Test
    void clearHierarchyLinksResetsAllLinks() throws IOException {
        mapping.addClassFromBytes(generateClassBytes("com/a/A", "java/lang/Object"));
        mapping.addClassFromBytes(generateClassBytes("com/a/B", "com/a/A"));
        mapping.resolveHierarchy();

        ProgramClass a = mapping.getProgramClass("com/a/A");
        ProgramClass b = mapping.getProgramClass("com/a/B");

        // verify links exist
        assertNotNull(b.getParentProgramClass());
        assertFalse(a.getChildProgramClasses().isEmpty());

        // clear
        a.clearHierarchyLinks();
        b.clearHierarchyLinks();

        assertNull(b.getParentProgramClass());
        assertTrue(a.getChildProgramClasses().isEmpty());
        assertTrue(b.getResolvedInterfaces().isEmpty());
        assertTrue(a.getUnresolvedSuperTypes().isEmpty());
    }
}
