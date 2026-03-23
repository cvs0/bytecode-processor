package net.cvs0.bytecode.transform;

/**
 * What to remove when stripping debug metadata from classes.
 */
public enum StripDebugMode {
    /** Clears {@link org.objectweb.asm.tree.ClassNode#sourceFile} and {@code sourceDebug}. */
    SOURCE_FILE,
    /** Removes {@link org.objectweb.asm.tree.LineNumberNode} from code. */
    LINE_NUMBERS,
    /** Clears {@link org.objectweb.asm.tree.MethodNode#localVariables}. */
    LOCAL_VARIABLES,
    /** Clears {@link org.objectweb.asm.tree.MethodNode#parameters} (MethodParameters attribute source). */
    METHOD_PARAMETERS
}
