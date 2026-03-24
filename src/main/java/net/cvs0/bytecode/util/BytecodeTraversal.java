package net.cvs0.bytecode.util;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.tree.MethodNode;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Walks {@linkplain net.cvs0.bytecode.JarMapping#getProgramClasses() program classes} and their methods, including
 * classes that only have instructions on the backing {@link org.objectweb.asm.tree.ClassNode} when the
 * {@link net.cvs0.bytecode.member.ProgramMethod} map is empty.
 */
public final class BytecodeTraversal {

    private BytecodeTraversal() {
    }

    /**
     * Invokes the consumer for each method-like body. Prefers {@link ProgramClass#getMethods()}; if empty but a
     * {@link org.objectweb.asm.tree.ClassNode} exists, iterates its {@link MethodNode} list using temporary
     * {@link ProgramMethod} wrappers that share the same {@link MethodNode} reference.
     */
    public static void forEachMethod(JarMapping mapping, BiConsumer<ProgramClass, ProgramMethod> consumer) {
        Objects.requireNonNull(mapping, "mapping");
        Objects.requireNonNull(consumer, "consumer");
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            forEachMethod(clazz, consumer);
        }
    }

    /**
     * Same as {@link #forEachMethod(JarMapping, BiConsumer)} for a single class.
     */
    public static void forEachMethod(ProgramClass clazz, BiConsumer<ProgramClass, ProgramMethod> consumer) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(consumer, "consumer");
        if (!clazz.getMethods().isEmpty()) {
            for (ProgramMethod m : clazz.getMethods()) {
                consumer.accept(clazz, m);
            }
        } else if (clazz.getClassNode() != null && clazz.getClassNode().methods != null) {
            for (MethodNode mn : clazz.getClassNode().methods) {
                ProgramMethod pm = new ProgramMethod(mn);
                pm.setOwner(clazz);
                consumer.accept(clazz, pm);
            }
        }
    }
}
