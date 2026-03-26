package io.github.cvs0.bytecode.io;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.ClassWriter;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ClassWriter} that resolves {@link #getCommonSuperClass} using the {@link JarMapping} class hierarchy
 * when the types are not loadable from the writer's class loader (e.g. renamed internal names after obfuscation).
 */
public final class SafeClassWriter extends ClassWriter {

    private final JarMapping mapping;

    public SafeClassWriter(int flags, JarMapping mapping) {
        super(flags);
        this.mapping = mapping;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (mapping != null) {
            String result = resolveFromMapping(type1, type2);
            if (result != null) {
                return result;
            }
        }
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Throwable ignored) {
            return "java/lang/Object";
        }
    }

    private String resolveFromMapping(String type1, String type2) {
        if (type1.equals(type2)) return type1;

        Set<String> ancestors1 = collectAncestors(type1);
        if (ancestors1.contains(type2)) return type2;

        String current = type2;
        while (current != null && !current.equals("java/lang/Object")) {
            if (ancestors1.contains(current)) return current;
            ProgramClass pc = mapping.getProgramClass(current);
            if (pc == null) return null;
            current = pc.getSuperName();
        }
        return "java/lang/Object";
    }

    private Set<String> collectAncestors(String type) {
        Set<String> ancestors = new HashSet<>();
        String current = type;
        while (current != null && !current.equals("java/lang/Object")) {
            ancestors.add(current);
            ProgramClass pc = mapping.getProgramClass(current);
            if (pc == null) break;
            current = pc.getSuperName();
        }
        ancestors.add("java/lang/Object");
        return ancestors;
    }
}
