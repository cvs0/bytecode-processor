package net.cvs0.bytecode.attribute;

import net.cvs0.bytecode.member.LocalVariable;
import net.cvs0.bytecode.member.LineNumber;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LineNumberNode;

import java.util.List;

/**
 * Factory class for creating and managing bytecode attributes.
 * Provides convenient methods to create attributes from ASM nodes and other sources.
 */
public class AttributeFactory {
    
    /**
     * Creates a LocalVariableTableAttribute from ASM LocalVariableNode list.
     */
    public static LocalVariableTableAttribute createLocalVariableTable(List<LocalVariableNode> localVariableNodes) {
        LocalVariableTableAttribute attribute = new LocalVariableTableAttribute();
        
        if (localVariableNodes != null) {
            for (LocalVariableNode node : localVariableNodes) {
                LocalVariable localVar = new LocalVariable(
                    node.name,
                    node.desc,
                    node.signature,
                    node.start.getLabel().getOffset(),
                    node.end.getLabel().getOffset() - node.start.getLabel().getOffset(),
                    node.index
                );
                attribute.addLocalVariable(localVar);
            }
        }
        
        return attribute;
    }
    
    /**
     * Creates a LineNumberTableAttribute from ASM LineNumberNode list.
     */
    public static LineNumberTableAttribute createLineNumberTable(List<LineNumberNode> lineNumberNodes) {
        LineNumberTableAttribute attribute = new LineNumberTableAttribute();
        
        if (lineNumberNodes != null) {
            for (LineNumberNode node : lineNumberNodes) {
                LineNumber lineNumber = new LineNumber(
                    node.start.getLabel().getOffset(),
                    node.line
                );
                attribute.addLineNumber(lineNumber);
            }
        }
        
        return attribute;
    }
    
    /**
     * Creates a CodeAttribute from a MethodNode.
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

        if (methodNode.tryCatchBlocks != null) {
            methodNode.tryCatchBlocks.forEach(tcb -> {
                try {
                    int start = tcb.start.getLabel().getOffset();
                    int end = tcb.end.getLabel().getOffset();
                    int handlerOffset = tcb.handler.getLabel().getOffset();

                    CodeAttribute.ExceptionHandler handler = new CodeAttribute.ExceptionHandler(
                            start, end, handlerOffset, tcb.type
                    );
                    codeAttribute.addExceptionHandler(handler);
                } catch (IllegalStateException e) {

                }
            });
        }

        if (methodNode.localVariables != null) {
            LocalVariableTableAttribute lvtAttribute = createLocalVariableTable(methodNode.localVariables);
            codeAttribute.addCodeAttribute(lvtAttribute);
        }

        return codeAttribute;
    }

    /**
     * Creates an ExceptionsAttribute from a list of exception types.
     */
    public static ExceptionsAttribute createExceptionsAttribute(List<String> exceptions) {
        return new ExceptionsAttribute(exceptions);
    }
    
    /**
     * Creates a SourceFileAttribute.
     */
    public static SourceFileAttribute createSourceFileAttribute(String sourceFile) {
        return new SourceFileAttribute(sourceFile);
    }
    
    /**
     * Creates a SignatureAttribute.
     */
    public static SignatureAttribute createSignatureAttribute(String signature) {
        return new SignatureAttribute(signature);
    }
    
    /**
     * Creates an InnerClassesAttribute.
     */
    public static InnerClassesAttribute createInnerClassesAttribute() {
        return new InnerClassesAttribute();
    }
    
    /**
     * Creates a BootstrapMethodsAttribute.
     */
    public static BootstrapMethodsAttribute createBootstrapMethodsAttribute() {
        return new BootstrapMethodsAttribute();
    }
    
    /**
     * Creates a MethodParametersAttribute.
     */
    public static MethodParametersAttribute createMethodParametersAttribute() {
        return new MethodParametersAttribute();
    }
    
    /**
     * Creates a LocalVariableTypeTableAttribute.
     */
    public static LocalVariableTypeTableAttribute createLocalVariableTypeTableAttribute() {
        return new LocalVariableTypeTableAttribute();
    }
    
    /**
     * Creates a synthetic attribute (marker attribute with no data).
     */
    public static Attribute createSyntheticAttribute() {
        return new Attribute("Synthetic");
    }
    
    /**
     * Creates a deprecated attribute (marker attribute with no data).
     */
    public static Attribute createDeprecatedAttribute() {
        return new Attribute("Deprecated");
    }
    
    /**
     * Creates an attribute from its name and raw data.
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
     * Determines if an attribute is a debug attribute.
     */
    public static boolean isDebugAttribute(Attribute attribute) {
        return attribute.isLineNumberTable() || 
               attribute.isLocalVariableTable() || 
               attribute.isLocalVariableTypeTable() ||
               attribute.isSourceFile() ||
               attribute.isSourceDebug();
    }
    
    /**
     * Determines if an attribute is a runtime attribute.
     */
    public static boolean isRuntimeAttribute(Attribute attribute) {
        return attribute.isAnnotation() ||
               attribute.isSignature() ||
               attribute.isInnerClasses() ||
               attribute.isBootstrapMethods() ||
               attribute.isMethodParameters();
    }
    
    /**
     * Determines if an attribute is a structural attribute.
     */
    public static boolean isStructuralAttribute(Attribute attribute) {
        return attribute.isCode() ||
               attribute.isExceptions() ||
               attribute.isSynthetic() ||
               attribute.isDeprecated();
    }
}