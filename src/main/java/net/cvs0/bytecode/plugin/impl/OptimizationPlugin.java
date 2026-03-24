package net.cvs0.bytecode.plugin.impl;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.analysis.UnusedCodeAnalyzer;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.plugin.AbstractPlugin;
import net.cvs0.bytecode.transform.InstructionTransformer;
import net.cvs0.bytecode.util.BytecodeTraversal;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;

/**
 * <p>Example plugin: optional dead-code removal (unused methods/fields per {@link UnusedCodeAnalyzer})
 * and peephole instruction cleanup (NOP removal, small-integer {@code iconst_*} folding).</p>
 *
 * <p><b>Configuration keys</b></p>
 * <table border="1" summary="OptimizationPlugin configuration">
 *   <tr><th>Key</th><th>Type</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>{@value OptimizationPlugin#CFG_REMOVE_UNUSED_METHODS}</td><td>boolean</td><td>{@code false}</td><td>Remove methods reported unused (unsafe if reflection/JNI).</td></tr>
 *   <tr><td>{@value OptimizationPlugin#CFG_REMOVE_UNUSED_FIELDS}</td><td>boolean</td><td>{@code false}</td><td>Remove fields reported unused.</td></tr>
 *   <tr><td>{@value OptimizationPlugin#CFG_REMOVE_NOPS}</td><td>boolean</td><td>{@code true}</td><td>Strip {@link Opcodes#NOP} instructions.</td></tr>
 *   <tr><td>{@value OptimizationPlugin#CFG_OPTIMIZE_CONSTANTS}</td><td>boolean</td><td>{@code true}</td><td>Replace {@code bipush}/{@code sipush} of -1…5 with {@code iconst_*}.</td></tr>
 * </table>
 */
public class OptimizationPlugin extends AbstractPlugin {

    public static final String CFG_REMOVE_UNUSED_METHODS = "removeUnusedMethods";
    public static final String CFG_REMOVE_UNUSED_FIELDS = "removeUnusedFields";
    public static final String CFG_REMOVE_NOPS = "removeNops";
    public static final String CFG_OPTIMIZE_CONSTANTS = "optimizeConstants";

    public OptimizationPlugin() {
        super(
                "OptimizationPlugin",
                "2.0.0",
                "Example: NOP removal, small-int peepholes, optional unused member removal");
    }

    @Override
    public void process(JarMapping mapping) {
        if (getBooleanConfig(CFG_REMOVE_UNUSED_METHODS, false)) {
            removeUnusedMethods(mapping);
        }
        if (getBooleanConfig(CFG_REMOVE_UNUSED_FIELDS, false)) {
            removeUnusedFields(mapping);
        }
        boolean removeNops = getBooleanConfig(CFG_REMOVE_NOPS, true);
        boolean optimizeConstants = getBooleanConfig(CFG_OPTIMIZE_CONSTANTS, true);
        if (removeNops || optimizeConstants) {
            optimizeInstructions(mapping, removeNops, optimizeConstants);
        }
    }

    private static void removeUnusedMethods(JarMapping mapping) {
        for (String key : UnusedCodeAnalyzer.findUnusedMethods(mapping)) {
            ParsedMethod m = ParsedMethod.parse(key);
            if (m == null) {
                continue;
            }
            ProgramClass clazz = mapping.getProgramClass(m.owner);
            if (clazz != null) {
                clazz.removeMethod(m.name, m.descriptor);
            }
        }
    }

    private static void removeUnusedFields(JarMapping mapping) {
        for (String key : UnusedCodeAnalyzer.findUnusedFields(mapping)) {
            ParsedField f = ParsedField.parse(key);
            if (f == null) {
                continue;
            }
            ProgramClass clazz = mapping.getProgramClass(f.owner);
            if (clazz != null) {
                clazz.removeField(f.name);
            }
        }
    }

    private static void optimizeInstructions(JarMapping mapping, boolean removeNops, boolean optimizeConstants) {
        BytecodeTraversal.forEachMethod(mapping, (clazz, method) -> {
            InstructionTransformer tx = new InstructionTransformer(method);
            if (removeNops) {
                tx.removeInstructions(insn -> insn.getOpcode() == Opcodes.NOP);
            }
            if (optimizeConstants) {
                foldSmallIntPush(tx, Opcodes.BIPUSH);
                foldSmallIntPush(tx, Opcodes.SIPUSH);
            }
        });
    }

    /**
     * Replaces BIPUSH/SIPUSH of values in {@code -1…5} with the corresponding {@code iconst_*} opcode.
     */
    private static void foldSmallIntPush(InstructionTransformer tx, int pushOpcode) {
        tx.replaceInstructions(
                insn -> insn.getOpcode() == pushOpcode && insn instanceof IntInsnNode i && inIconstRange(i.operand),
                insn -> new InsnNode(iconstOpcode(((IntInsnNode) insn).operand)));
    }

    private static boolean inIconstRange(int v) {
        return v >= -1 && v <= 5;
    }

    private static int iconstOpcode(int value) {
        if (value == -1) {
            return Opcodes.ICONST_M1;
        }
        return Opcodes.ICONST_0 + value;
    }

    private record ParsedField(String owner, String name) {
        static ParsedField parse(String key) {
            int d = key.indexOf('.');
            if (d <= 0 || d == key.length() - 1) {
                return null;
            }
            return new ParsedField(key.substring(0, d), key.substring(d + 1));
        }
    }

    private record ParsedMethod(String owner, String name, String descriptor) {
        static ParsedMethod parse(String key) {
            int d = key.indexOf('.');
            if (d <= 0 || d >= key.length() - 1) {
                return null;
            }
            String owner = key.substring(0, d);
            String rest = key.substring(d + 1);
            int p = rest.indexOf('(');
            if (p <= 0) {
                return null;
            }
            return new ParsedMethod(owner, rest.substring(0, p), rest.substring(p));
        }
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
