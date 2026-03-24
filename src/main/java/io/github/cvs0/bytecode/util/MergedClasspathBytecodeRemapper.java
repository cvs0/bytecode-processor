package io.github.cvs0.bytecode.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.util.Map;
import java.util.Objects;

/**
 * Rewrites merged dependency {@code .class} files so references match renamed program classes, fields, and methods.
 */
public final class MergedClasspathBytecodeRemapper {

    private MergedClasspathBytecodeRemapper() {}

    /**
     * Applies the same rename mappings used for program classes to raw class bytes (e.g. merged classpath JARs).
     *
     * @param classBytes       original bytecode
     * @param classNameMappings old internal name → new internal name
     * @param fieldNameMappings key {@code owner.field} (old internal owner)
     * @param methodNameMappings key {@code owner.name+descriptor} (old internal owner)
     * @return remapped bytecode (same array if nothing to apply)
     */
    public static byte[] remap(
            byte[] classBytes,
            Map<String, String> classNameMappings,
            Map<String, String> fieldNameMappings,
            Map<String, String> methodNameMappings) {
        Objects.requireNonNull(classBytes, "classBytes");
        Objects.requireNonNull(classNameMappings, "classNameMappings");
        Objects.requireNonNull(fieldNameMappings, "fieldNameMappings");
        Objects.requireNonNull(methodNameMappings, "methodNameMappings");
        if (classNameMappings.isEmpty() && fieldNameMappings.isEmpty() && methodNameMappings.isEmpty()) {
            return classBytes;
        }
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (Throwable ignored) {
                    return "java/lang/Object";
                }
            }
        };
        Remapper remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                return classNameMappings.getOrDefault(internalName, internalName);
            }

            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                String mapped = fieldNameMappings.get(owner + "." + name);
                return mapped != null ? mapped : super.mapFieldName(owner, name, descriptor);
            }

            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                String key = owner + "." + name + descriptor;
                String mapped = methodNameMappings.get(key);
                return mapped != null ? mapped : super.mapMethodName(owner, name, descriptor);
            }
        };
        cr.accept(new ClassRemapper(cw, remapper), ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
}
