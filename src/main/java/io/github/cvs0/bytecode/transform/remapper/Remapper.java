package io.github.cvs0.bytecode.transform.remapper;

/**
 * Contract for remapping JVM class names, member names, descriptors, and signatures.
 *
 * <p>Every transformer and plugin that needs to rewrite bytecode references works through this interface,
 * keeping the mapping strategy (rename maps, pattern rules, etc.) separate from the actual rewriting.</p>
 *
 * <p>Implementations should be stateless per-invocation or document their thread-safety.</p>
 *
 * @see MappingRemapper
 */
public interface Remapper {

    /**
     * Remaps a class internal name (e.g. {@code com/foo/Bar → com/foo/Baz}).
     *
     * @param internalName original internal name (slash form)
     * @return the remapped name, or the original if no mapping exists
     */
    String remapClass(String internalName);

    /**
     * Remaps a method name given its owner, original name, and descriptor.
     *
     * @param owner      internal name of the declaring class (pre-remap)
     * @param name       original method name
     * @param descriptor original method descriptor
     * @return the remapped method name, or the original if no mapping exists
     */
    String remapMethod(String owner, String name, String descriptor);

    /**
     * Remaps a field name given its owner, original name, and descriptor.
     *
     * @param owner      internal name of the declaring class (pre-remap)
     * @param name       original field name
     * @param descriptor original field descriptor
     * @return the remapped field name, or the original if no mapping exists
     */
    String remapField(String owner, String name, String descriptor);

    /**
     * Remaps a JVM field or method descriptor (e.g. {@code (Lcom/foo/Bar;)V → (Lcom/foo/Baz;)V}).
     *
     * @param descriptor the original descriptor
     * @return the remapped descriptor
     */
    String remapDescriptor(String descriptor);

    /**
     * Remaps a JVM generic signature (e.g. class, method, or field signature with type parameters).
     *
     * @param signature the original signature (may be {@code null})
     * @return the remapped signature, or {@code null} if input was null
     */
    String remapSignature(String signature);

    /**
     * Returns an ASM-compatible {@link org.objectweb.asm.commons.Remapper} backed by this Remapper.
     * Used with {@link org.objectweb.asm.commons.ClassRemapper} for full ClassNode remapping.
     *
     * @return an ASM Remapper bridge
     */
    org.objectweb.asm.commons.Remapper toAsm();
}
