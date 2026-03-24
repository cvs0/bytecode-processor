package io.github.cvs0.bytecode.transform;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Counts how many distinct program classes reference each type as a field/method owner. Types with very high fan-in
 * behave like shared libraries (many call sites); renaming their members risks breaking reflection-based APIs, so the
 * transformer can skip member renames for them without hardcoding package names.
 */
public final class ProgramTypeReferenceFanIn {

    /** Types referenced from at least this many other program classes skip field/method renames. */
    public static final int MEMBER_RENAME_FAN_IN_THRESHOLD = 28;

    private ProgramTypeReferenceFanIn() {}

    public static Map<String, Integer> distinctReferrerCountPerOwner(JarMapping mapping) {
        Set<String> inJar = new HashSet<>();
        for (ProgramClass pc : mapping.getProgramClasses()) {
            inJar.add(pc.getName());
        }
        Map<String, Set<String>> ownerToReferrers = new HashMap<>();
        for (ProgramClass fromPc : mapping.getProgramClasses()) {
            ClassNode cn = fromPc.getClassNode();
            if (cn == null || cn.methods == null) {
                continue;
            }
            String from = fromPc.getName();
            for (MethodNode mn : cn.methods) {
                if (mn.instructions == null) {
                    continue;
                }
                for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof FieldInsnNode fin) {
                        addEdge(inJar, ownerToReferrers, from, fin.owner);
                    } else if (insn instanceof MethodInsnNode min) {
                        addEdge(inJar, ownerToReferrers, from, min.owner);
                    } else if (insn instanceof InvokeDynamicInsnNode indy) {
                        Handle bsm = indy.bsm;
                        if (bsm != null) {
                            addEdge(inJar, ownerToReferrers, from, bsm.getOwner());
                        }
                        if (indy.bsmArgs != null) {
                            for (Object arg : indy.bsmArgs) {
                                if (arg instanceof Handle h) {
                                    addEdge(inJar, ownerToReferrers, from, h.getOwner());
                                }
                            }
                        }
                    }
                }
            }
        }
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : ownerToReferrers.entrySet()) {
            out.put(e.getKey(), e.getValue().size());
        }
        return out;
    }

    private static void addEdge(
            Set<String> inJar, Map<String, Set<String>> ownerToReferrers, String from, String owner) {
        if (owner == null || !inJar.contains(owner) || owner.equals(from)) {
            return;
        }
        ownerToReferrers.computeIfAbsent(owner, k -> new HashSet<>()).add(from);
    }
}
