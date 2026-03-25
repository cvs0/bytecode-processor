package io.github.cvs0.bytecode.transform;

import java.util.Map;
import java.util.Objects;

/**
 * {@link Remapper} backed by explicit old→new rename maps for classes, methods, and fields.
 *
 * <p>Descriptor and signature remapping is delegated to ASM's built-in
 * {@link org.objectweb.asm.commons.Remapper} so we get correct handling of
 * {@code L…;} tokens, generic type arguments, array types, and primitives for free —
 * no hand-rolled {@code StringBuilder} parsing needed.</p>
 *
 * <p>The {@link #toAsm()} bridge allows this remapper to be passed directly to
 * {@link org.objectweb.asm.commons.ClassRemapper} for full ClassNode remapping.</p>
 *
 * @see Remapper
 * @see org.objectweb.asm.commons.ClassRemapper
 */
public final class MappingRemapper implements Remapper {

    private final Map<String, String> classMap;
    private final Map<String, String> fieldMap;
    private final Map<String, String> methodMap;
    private final org.objectweb.asm.commons.Remapper asmRemapper;

    /**
     * @param classMap  internal-name → internal-name (e.g. {@code com/foo/Bar → com/foo/Baz})
     * @param fieldMap  {@code owner.fieldName → newFieldName}
     * @param methodMap {@code owner.methodName(descriptor) → newName}
     */
    public MappingRemapper(Map<String, String> classMap,
                           Map<String, String> fieldMap,
                           Map<String, String> methodMap) {
        this.classMap = Objects.requireNonNull(classMap, "classMap");
        this.fieldMap = Objects.requireNonNull(fieldMap, "fieldMap");
        this.methodMap = Objects.requireNonNull(methodMap, "methodMap");
        this.asmRemapper = new AsmBridge();
    }

    @Override
    public String remapClass(String internalName) {
        if (internalName == null) {
            return null;
        }
        return classMap.getOrDefault(internalName, internalName);
    }

    @Override
    public String remapMethod(String owner, String name, String descriptor) {
        return methodMap.getOrDefault(owner + "." + name + descriptor, name);
    }

    @Override
    public String remapField(String owner, String name, String descriptor) {
        return fieldMap.getOrDefault(owner + "." + name, name);
    }

    @Override
    public String remapDescriptor(String descriptor) {
        if (descriptor == null) {
            return null;
        }
        return asmRemapper.mapDesc(descriptor);
    }

    @Override
    public String remapSignature(String signature) {
        if (signature == null) {
            return null;
        }
        return asmRemapper.mapSignature(signature, false);
    }

    @Override
    public org.objectweb.asm.commons.Remapper toAsm() {
        return asmRemapper;
    }

    /**
     * Whether any rename mappings are present.
     */
    public boolean isEmpty() {
        return classMap.isEmpty() && fieldMap.isEmpty() && methodMap.isEmpty();
    }

    // ------------------------------------------------------------------
    // ASM bridge — delegates back to MappingRemapper for name resolution
    // ------------------------------------------------------------------

    private final class AsmBridge extends org.objectweb.asm.commons.Remapper {

        @Override
        public String map(String internalName) {
            return remapClass(internalName);
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            return remapMethod(owner, name, descriptor);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            return remapField(owner, name, descriptor);
        }
    }
}
