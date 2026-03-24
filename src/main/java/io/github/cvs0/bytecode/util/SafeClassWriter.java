package io.github.cvs0.bytecode.util;

import org.objectweb.asm.ClassWriter;

/**
 * {@link ClassWriter} that does not fail {@link #getCommonSuperClass} when types are not loadable from the
 * writer's class loader (e.g. renamed internal names during JAR emission).
 */
public final class SafeClassWriter extends ClassWriter {

    public SafeClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Throwable ignored) {
            return "java/lang/Object";
        }
    }
}
