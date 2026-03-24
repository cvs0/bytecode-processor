package io.github.cvs0.bytecode.attribute;

import io.github.cvs0.bytecode.member.LocalVariable;
import io.github.cvs0.bytecode.member.LineNumber;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.InsnList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class for creating and managing bytecode attributes.
 * Provides convenient static methods to create various attribute types from ASM nodes and other sources.
 * Supports classification of attributes as debug, runtime, or structural.
 */
public class AttributeFactory {
    /**
     * Creates a LocalVariableTableAttribute from a list of ASM LocalVariableNode objects.
     * Each node is converted to a LocalVariable and added to the attribute.
     *
     * This implementation prefers instruction-index based PCs (instruction list index)
     * which is safe even when Label offsets haven't been resolved. If the parent
     * MethodNode or its instruction list is not available, it falls back to using
     * label offsets when possible.
     *
     * @param localVariableNodes the list of ASM LocalVariableNode objects
     * @param parentMethod the parent MethodNode (may be null)
     * @return a LocalVariableTableAttribute containing all local variables
     */
    public static LocalVariableTableAttribute createLocalVariableTable(List<LocalVariableNode> localVariableNodes, MethodNode parentMethod) {
        LocalVariableTableAttribute attribute = new LocalVariableTableAttribute();
        if (localVariableNodes == null || localVariableNodes.isEmpty()) {
            return attribute;
        }

        InsnList insnList = parentMethod != null ? parentMethod.instructions : null;

        for (LocalVariableNode node : localVariableNodes) {
            int startPc = resolveInstructionPc(node.start, insnList);
            int endPc = resolveInstructionPc(node.end, insnList);
            int length = Math.max(0, endPc - startPc);

            LocalVariable localVar = new LocalVariable(
                node.name,
                node.desc,
                node.signature,
                startPc,
                length,
                node.index
            );
            attribute.addLocalVariable(localVar);
        }

        return attribute;
    }

    /**
     * Resolve a stable instruction "PC" for a LabelNode / AbstractInsnNode.
     * Primary strategy: use instruction index within the InsnList.
     * Fallback strategy: use LabelNode#getLabel().getOffset() if available (may throw if not resolved).
     * If neither approach yields a value, returns 0.
     */
    private static int resolveInstructionPc(AbstractInsnNode anchor, InsnList insnList) {
        if (anchor == null) return 0;
        if (insnList != null) {
            try {
                int idx = insnList.indexOf(anchor);
                if (idx >= 0) return idx;
            } catch (Throwable ignored) {
                // ignore and fall back
            }
        }
        try {
            if (anchor instanceof LabelNode labelNode) {
                return labelNode.getLabel().getOffset();
            }
        } catch (Throwable ignored) {
            // ignore
        }
        return 0;
    }

    /**
     * Creates a LineNumberTableAttribute from a list of ASM LineNumberNode objects.
     * Each node is converted to a LineNumber and added to the attribute.
     * @param lineNumberNodes the list of ASM LineNumberNode objects
     * @return a LineNumberTableAttribute containing all line numbers
     */
    public static LineNumberTableAttribute createLineNumberTable(List<LineNumberNode> lineNumberNodes, MethodNode parentMethod) {
        LineNumberTableAttribute attribute = new LineNumberTableAttribute();
        if (lineNumberNodes != null && !lineNumberNodes.isEmpty()) {
            InsnList insnList = parentMethod != null ? parentMethod.instructions : null;
            for (int i = 0; i < lineNumberNodes.size(); i++) {
                LineNumberNode node = lineNumberNodes.get(i);
                int startPc = resolveInstructionPc(node.start, insnList);
                int endPc = -1;
                if (i + 1 < lineNumberNodes.size()) {
                    endPc = resolveInstructionPc(lineNumberNodes.get(i + 1).start, insnList);
                }

                boolean synthetic = (parentMethod != null && (parentMethod.access & 0x1000) != 0) || node.line == 0;

                String originalSourceLine = String.valueOf(node.line);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("index", i);
                metadata.put("methodName", parentMethod != null ? parentMethod.name : null);
                metadata.put("desc", parentMethod != null ? parentMethod.desc : null);

                LineNumber lineNumber = new LineNumber(
                    startPc,
                    endPc,
                    node.line,
                    null,
                    -1,
                    synthetic,
                    originalSourceLine,
                    metadata,
                    parentMethod
                );
                attribute.addLineNumber(lineNumber);
            }
        }
        return attribute;
    }

    /**
     * Creates a CodeAttribute from an ASM MethodNode.
     * Populates stack size, local variable count, code bytes, exception handlers, and local variable table.
     *
     * @param methodNode the ASM MethodNode
     * @return a CodeAttribute representing the method's code, or null if methodNode is null
     */
    public static CodeAttribute createCodeAttribute(MethodNode methodNode) {
        if (methodNode == null) {
            return null;
        }

        CodeAttribute codeAttribute = new CodeAttribute();
        codeAttribute.setMaxStack(methodNode.maxStack);
        codeAttribute.setMaxLocals(methodNode.maxLocals);

        if (methodNode.instructions != null) {
            byte[] codeBytes = new byte[methodNode.instructions.size() * 4];
            codeAttribute.setCode(codeBytes);
        }

        if (methodNode.tryCatchBlocks != null && methodNode.instructions != null) {
            InsnList il = methodNode.instructions;
            for (var tcb : methodNode.tryCatchBlocks) {
                int start = resolveInstructionPc(tcb.start, il);
                int end = resolveInstructionPc(tcb.end, il);
                int handlerOffset = resolveInstructionPc(tcb.handler, il);
                codeAttribute.addExceptionHandler(
                        new CodeAttribute.ExceptionHandler(start, end, handlerOffset, tcb.type));
            }
        }

        if (methodNode.localVariables != null) {
            LocalVariableTableAttribute lvtAttribute = createLocalVariableTable(methodNode.localVariables, methodNode);
            codeAttribute.addCodeAttribute(lvtAttribute);
        }

        if (methodNode.instructions != null) {
            List<LineNumberNode> lineNumberNodes = new ArrayList<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof LineNumberNode lnn) {
                    lineNumberNodes.add(lnn);
                }
            }
            if (!lineNumberNodes.isEmpty()) {
                LineNumberTableAttribute lnTable = createLineNumberTable(lineNumberNodes, methodNode);
                codeAttribute.addCodeAttribute(lnTable);
            }
        }
        return codeAttribute;
    }

    /**
     * Creates an ExceptionsAttribute from a list of exception type names.
     *
     * @param exceptions the list of exception type names
     * @return an ExceptionsAttribute containing the exception types
     */
    public static ExceptionsAttribute createExceptionsAttribute(List<String> exceptions) {
        return new ExceptionsAttribute(exceptions);
    }

    /**
     * Creates a SourceFileAttribute for the given source file name.
     *
     * @param sourceFile the source file name
     * @return a SourceFileAttribute for the given file
     */
    public static SourceFileAttribute createSourceFileAttribute(String sourceFile) {
        return new SourceFileAttribute(sourceFile);
    }

    /**
     * Creates a SignatureAttribute for the given generic signature string.
     *
     * @param signature the generic signature string
     * @return a SignatureAttribute for the given signature
     */
    public static SignatureAttribute createSignatureAttribute(String signature) {
        return new SignatureAttribute(signature);
    }

    /**
     * Creates an empty InnerClassesAttribute.
     *
     * @return a new InnerClassesAttribute
     */
    public static InnerClassesAttribute createInnerClassesAttribute() {
        return new InnerClassesAttribute();
    }

    /**
     * Creates an empty BootstrapMethodsAttribute.
     *
     * @return a new BootstrapMethodsAttribute
     */
    public static BootstrapMethodsAttribute createBootstrapMethodsAttribute() {
        return new BootstrapMethodsAttribute();
    }

    /**
     * Creates an empty MethodParametersAttribute.
     *
     * @return a new MethodParametersAttribute
     */
    public static MethodParametersAttribute createMethodParametersAttribute() {
        return new MethodParametersAttribute();
    }

    /**
     * Creates an empty LocalVariableTypeTableAttribute.
     *
     * @return a new LocalVariableTypeTableAttribute
     */
    public static LocalVariableTypeTableAttribute createLocalVariableTypeTableAttribute() {
        return new LocalVariableTypeTableAttribute();
    }

    /**
     * Creates a synthetic marker attribute (with no data).
     *
     * @return a new Attribute named "Synthetic"
     */
    public static Attribute createSyntheticAttribute() {
        return new Attribute("Synthetic");
    }

    /**
     * Creates a deprecated marker attribute (with no data).
     *
     * @return a new Attribute named "Deprecated"
     */
    public static Attribute createDeprecatedAttribute() {
        return new Attribute("Deprecated");
    }

    /**
     * Creates an attribute from its name and raw data.
     * Returns a specialized attribute type if the name matches a known attribute, otherwise a generic Attribute.
     *
     * @param name the attribute name
     * @param data the raw attribute data
     * @return an Attribute instance for the given name and data
     */
    public static Attribute createAttribute(String name, byte[] data) {
        return switch (name) {
            case "SourceFile" -> {
                if (data != null && data.length > 0) {
                    String sourceFile = new String(data);
                    yield new SourceFileAttribute(sourceFile);
                }
                yield new SourceFileAttribute(null);
            }
            case "Signature" -> {
                if (data != null && data.length > 0) {
                    String signature = new String(data);
                    yield new SignatureAttribute(signature);
                }
                yield new SignatureAttribute(null);
            }
            case "LocalVariableTable" -> new LocalVariableTableAttribute();
            case "LocalVariableTypeTable" -> new LocalVariableTypeTableAttribute();
            case "LineNumberTable" -> new LineNumberTableAttribute();
            case "Code" -> new CodeAttribute();
            case "Exceptions" -> new ExceptionsAttribute();
            case "InnerClasses" -> new InnerClassesAttribute();
            case "BootstrapMethods" -> new BootstrapMethodsAttribute();
            case "MethodParameters" -> new MethodParametersAttribute();
            case "Synthetic", "Deprecated" -> new Attribute(name);
            default -> new Attribute(name, data);
        };
    }

    /**
     * Determines if an attribute is a debug attribute (e.g., line numbers, local variables, source file).
     *
     * @param attribute the attribute to check
     * @return true if the attribute is a debug attribute
     */
    public static boolean isDebugAttribute(Attribute attribute) {
        return attribute.isLineNumberTable() || 
               attribute.isLocalVariableTable() || 
               attribute.isLocalVariableTypeTable() ||
               attribute.isSourceFile() ||
               attribute.isSourceDebug();
    }

    /**
     * Determines if an attribute is a runtime attribute (e.g., annotations, signature, inner classes).
     *
     * @param attribute the attribute to check
     * @return true if the attribute is a runtime attribute
     */
    public static boolean isRuntimeAttribute(Attribute attribute) {
        return attribute.isAnnotation() ||
               attribute.isSignature() ||
               attribute.isInnerClasses() ||
               attribute.isBootstrapMethods() ||
               attribute.isMethodParameters();
    }

    /**
     * Determines if an attribute is a structural attribute (e.g., code, exceptions, synthetic, deprecated).
     *
     * @param attribute the attribute to check
     * @return true if the attribute is a structural attribute
     */
    public static boolean isStructuralAttribute(Attribute attribute) {
        return attribute.isCode() ||
               attribute.isExceptions() ||
               attribute.isSynthetic() ||
               attribute.isDeprecated();
    }
}
