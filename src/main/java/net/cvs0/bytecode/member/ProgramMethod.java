package net.cvs0.bytecode.member;

import net.cvs0.bytecode.attribute.*;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.instruction.Instruction;
import net.cvs0.bytecode.member.LineNumber;
import net.cvs0.bytecode.member.LocalVariable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.LineNumberNode;

import java.util.*;

/**
 * Represents a method in a program class, including its name, descriptor, signature, access flags, instructions, attributes, local variables, and line numbers.
 * Provides methods for manipulating method structure, bytecode, and metadata, as well as ASM MethodNode integration.
 */
public class ProgramMethod {
    private String name;
    private String descriptor;
    private String signature;
    private int access;
    private String[] exceptions;
    private int maxStack;
    private int maxLocals;
    
    private ProgramClass owner;
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<Instruction> instructions = new ArrayList<>();
    private final List<LocalVariable> localVariables = new ArrayList<>();
    private final List<LineNumber> lineNumbers = new ArrayList<>();
    private MethodNode methodNode;
    
    /**
     * Constructs a ProgramMethod with the given name, descriptor, and access flags.
     * @param name the method name
     * @param descriptor the method descriptor
     * @param access the access flags
     */
    public ProgramMethod(String name, String descriptor, int access) {
        this.name = name;
        this.descriptor = descriptor;
        this.access = access;
    }

    /**
     * Constructs a ProgramMethod from an ASM MethodNode.
     * @param methodNode the ASM MethodNode
     */
    public ProgramMethod(MethodNode methodNode) {
        this.methodNode = methodNode;
        this.name = methodNode.name;
        this.descriptor = methodNode.desc;
        this.signature = methodNode.signature;
        this.access = methodNode.access;
        this.exceptions = methodNode.exceptions != null ? 
            methodNode.exceptions.toArray(new String[0]) : new String[0];
        this.maxStack = methodNode.maxStack;
        this.maxLocals = methodNode.maxLocals;

        if (methodNode.instructions != null) {
            for (AbstractInsnNode insn : methodNode.instructions) {
                instructions.add(new Instruction(insn));
            }
        }

        extractAttributesFromMethodNode(methodNode);
        if (methodNode.instructions != null && methodNode.instructions.size() > 0) {
            List<LineNumberNode> lineNumberNodes = new ArrayList<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof LineNumberNode lnn) {
                    lineNumberNodes.add(lnn);
                }
            }
            if (!lineNumberNodes.isEmpty()) {
                var lineNumberTableAttr = AttributeFactory.createLineNumberTable(lineNumberNodes, methodNode);
                addAttribute(lineNumberTableAttr);
                lineNumbers.clear();
                lineNumbers.addAll(lineNumberTableAttr.getLineNumbers());
            }
        }
    }

    /**
     * Adds an attribute to this method.
     * @param attribute the Attribute to add
     */
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }

    /**
     * Adds a local variable to this method.
     * @param localVariable the LocalVariable to add
     */
    public void addLocalVariable(LocalVariable localVariable) {
        localVariables.add(localVariable);
    }

    /**
     * Removes a local variable from this method.
     * @param localVariable the LocalVariable to remove
     */
    public void removeLocalVariable(LocalVariable localVariable) {
        localVariables.remove(localVariable);
    }

    /**
     * Adds a line number entry to this method.
     * @param lineNumber the LineNumber to add
     */
    public void addLineNumber(LineNumber lineNumber) {
        lineNumbers.add(lineNumber);
    }

    /**
     * Removes a line number entry from this method.
     * @param lineNumber the LineNumber to remove
     */
    public void removeLineNumber(LineNumber lineNumber) {
        lineNumbers.remove(lineNumber);
    }

    /**
     * Adds a bytecode instruction to this method.
     * @param instruction the Instruction to add
     */
    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
        if (methodNode != null && methodNode.instructions != null) {
            methodNode.instructions.add(instruction.getInstructionNode());
        }
    }

    /**
     * Inserts a bytecode instruction at the specified index.
     * @param index the index to insert at
     * @param instruction the Instruction to insert
     */
    public void insertInstruction(int index, Instruction instruction) {
        instructions.add(index, instruction);
        if (methodNode != null && methodNode.instructions != null) {
            methodNode.instructions.insert(
                methodNode.instructions.get(index), 
                instruction.getInstructionNode()
            );
        }
    }

    /**
     * Removes a bytecode instruction at the specified index.
     * @param index the index to remove
     */
    public void removeInstruction(int index) {
        if (index >= 0 && index < instructions.size()) {
            Instruction removed = instructions.remove(index);
            if (methodNode != null && methodNode.instructions != null) {
                methodNode.instructions.remove(removed.getInstructionNode());
            }
        }
    }

    /**
     * Replaces a bytecode instruction at the specified index.
     * @param index the index to replace
     * @param newInstruction the new Instruction
     */
    public void replaceInstruction(int index, Instruction newInstruction) {
        if (index >= 0 && index < instructions.size()) {
            Instruction old = instructions.set(index, newInstruction);
            if (methodNode != null && methodNode.instructions != null) {
                methodNode.instructions.set(old.getInstructionNode(), newInstruction.getInstructionNode());
            }
        }
    }

    /**
     * Clears all bytecode instructions from this method.
     */
    public void clearInstructions() {
        instructions.clear();
        if (methodNode != null && methodNode.instructions != null) {
            methodNode.instructions.clear();
        }
    }

    /**
     * Returns all attributes of this method.
     * @return unmodifiable list of attributes
     */
    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    /**
     * Returns all bytecode instructions of this method.
     * @return unmodifiable list of instructions
     */
    public List<Instruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    /**
     * Returns all local variables of this method.
     * @return unmodifiable list of local variables
     */
    public List<LocalVariable> getLocalVariables() {
        return Collections.unmodifiableList(localVariables);
    }

    /**
     * Returns all line numbers of this method.
     * @return unmodifiable list of line numbers
     */
    public List<LineNumber> getLineNumbers() {
        return Collections.unmodifiableList(lineNumbers);
    }

    /**
     * Gets the method name.
     * @return the method name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the method name.
     * @param name the new method name
     */
    public void setName(String name) {
        this.name = name;
        if (methodNode != null) {
            methodNode.name = name;
        }
    }

    /**
     * Gets the method descriptor.
     * @return the method descriptor
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * Sets the method descriptor.
     * @param descriptor the new descriptor
     */
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
        if (methodNode != null) {
            methodNode.desc = descriptor;
        }
    }

    /**
     * Gets the generic signature for this method.
     * @return the signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Sets the generic signature for this method.
     * @param signature the new signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
        if (methodNode != null) {
            methodNode.signature = signature;
        }
    }

    /**
     * Gets the access flags for this method.
     * @return the access flags
     */
    public int getAccess() {
        return access;
    }

    /**
     * Sets the access flags for this method.
     * @param access the new access flags
     */
    public void setAccess(int access) {
        this.access = access;
        if (methodNode != null) {
            methodNode.access = access;
        }
    }

    /**
     * Gets the exceptions thrown by this method.
     * @return array of exception class names
     */
    public String[] getExceptions() {
        return exceptions != null ? exceptions.clone() : new String[0];
    }

    /**
     * Sets the exceptions thrown by this method.
     * @param exceptions array of exception class names
     */
    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions != null ? exceptions.clone() : new String[0];
        if (methodNode != null) {
            methodNode.exceptions = exceptions != null ? Arrays.asList(exceptions) : new ArrayList<>();
        }
    }

    /**
     * Gets the maximum stack size for this method.
     * @return the max stack size
     */
    public int getMaxStack() {
        return maxStack;
    }

    /**
     * Sets the maximum stack size for this method.
     * @param maxStack the new max stack size
     */
    public void setMaxStack(int maxStack) {
        this.maxStack = maxStack;
        if (methodNode != null) {
            methodNode.maxStack = maxStack;
        }
    }

    /**
     * Gets the maximum number of local variables for this method.
     * @return the max locals
     */
    public int getMaxLocals() {
        return maxLocals;
    }

    /**
     * Sets the maximum number of local variables for this method.
     * @param maxLocals the new max locals
     */
    public void setMaxLocals(int maxLocals) {
        this.maxLocals = maxLocals;
        if (methodNode != null) {
            methodNode.maxLocals = maxLocals;
        }
    }

    /**
     * Gets the owning ProgramClass for this method.
     * @return the owner class
     */
    public ProgramClass getOwner() {
        return owner;
    }

    /**
     * Sets the owning ProgramClass for this method.
     * @param owner the new owner class
     */
    public void setOwner(ProgramClass owner) {
        this.owner = owner;
    }

    /**
     * Gets the underlying ASM MethodNode for this method.
     * @return the MethodNode
     */
    public MethodNode getMethodNode() {
        return methodNode;
    }

    /**
     * Sets the underlying ASM MethodNode for this method.
     * @param methodNode the new MethodNode
     */
    public void setMethodNode(MethodNode methodNode) {
        this.methodNode = methodNode;
    }

    /**
     * Returns true if this method is static.
     * @return true if static
     */
    public boolean isStatic() {
        return (access & 0x0008) != 0;
    }

    /**
     * Returns true if this method is final.
     * @return true if final
     */
    public boolean isFinal() {
        return (access & 0x0010) != 0;
    }

    /**
     * Returns true if this method is public.
     * @return true if public
     */
    public boolean isPublic() {
        return (access & 0x0001) != 0;
    }

    /**
     * Returns true if this method is private.
     * @return true if private
     */
    public boolean isPrivate() {
        return (access & 0x0002) != 0;
    }

    /**
     * Returns true if this method is protected.
     * @return true if protected
     */
    public boolean isProtected() {
        return (access & 0x0004) != 0;
    }

    /**
     * Returns true if this method is abstract.
     * @return true if abstract
     */
    public boolean isAbstract() {
        return (access & 0x0400) != 0;
    }

    /**
     * Returns true if this method is synchronized.
     * @return true if synchronized
     */
    public boolean isSynchronized() {
        return (access & 0x0020) != 0;
    }

    /**
     * Returns true if this method is native.
     * @return true if native
     */
    public boolean isNative() {
        return (access & 0x0100) != 0;
    }

    /**
     * Returns true if this method is synthetic.
     * @return true if synthetic
     */
    public boolean isSynthetic() {
        return (access & 0x1000) != 0;
    }

    /**
     * Returns true if this method is a constructor.
     * @return true if constructor
     */
    public boolean isConstructor() {
        return "<init>".equals(name);
    }

    /**
     * Returns true if this method is a static initializer.
     * @return true if static initializer
     */
    public boolean isStaticInitializer() {
        return "<clinit>".equals(name);
    }

    /**
     * Gets the full name of this method (class name + method name + descriptor).
     * @return the full method name
     */
    public String getFullName() {
        return owner != null ? owner.getName() + "." + name + descriptor : name + descriptor;
    }

    /**
     * Gets the number of bytecode instructions in this method.
     * @return the instruction count
     */
    public int getInstructionCount() {
        return instructions.size();
    }

    /**
     * Returns true if this method has any bytecode instructions.
     * @return true if instructions exist
     */
    public boolean hasInstructions() {
        return !instructions.isEmpty();
    }

    /**
     * Extracts attributes from the ASM MethodNode and adds them to this method.
     * @param methodNode the ASM MethodNode
     */
    private void extractAttributesFromMethodNode(MethodNode methodNode) {
        if (methodNode == null) return;
        if (methodNode.signature != null) {
            addAttribute(AttributeFactory.createSignatureAttribute(methodNode.signature));
        }
        if (methodNode.exceptions != null && !methodNode.exceptions.isEmpty()) {
            addAttribute(AttributeFactory.createExceptionsAttribute(methodNode.exceptions));
        }
        if (methodNode.instructions != null && methodNode.instructions.size() > 0) {
            CodeAttribute codeAttribute = AttributeFactory.createCodeAttribute(methodNode);
            if (codeAttribute != null) {
                addAttribute(codeAttribute);
            }
        }
        if (methodNode.localVariables != null && !methodNode.localVariables.isEmpty()) {
            LocalVariableTableAttribute lvtAttribute = AttributeFactory.createLocalVariableTable(methodNode.localVariables, methodNode);
            addAttribute(lvtAttribute);
            for (var lvNode : methodNode.localVariables) {
                LocalVariable localVar = new LocalVariable(
                    lvNode.name,
                    lvNode.desc,
                    lvNode.signature,
                    0,
                    0,
                    lvNode.index
                );
                addLocalVariable(localVar);
            }
        }
        if ((methodNode.access & 0x1000) != 0) {
            addAttribute(AttributeFactory.createSyntheticAttribute());
        }
        if (methodNode.visibleAnnotations != null || methodNode.invisibleAnnotations != null) {
            if (methodNode.visibleAnnotations != null) {
                AnnotationAttribute visibleAnnotations = new AnnotationAttribute("RuntimeVisibleAnnotations", true);
                addAttribute(visibleAnnotations);
            }
            if (methodNode.invisibleAnnotations != null) {
                AnnotationAttribute invisibleAnnotations = new AnnotationAttribute("RuntimeInvisibleAnnotations", false);
                addAttribute(invisibleAnnotations);
            }
        }
        if (methodNode.parameters != null && !methodNode.parameters.isEmpty()) {
            MethodParametersAttribute methodParamsAttribute = AttributeFactory.createMethodParametersAttribute();
            for (var param : methodNode.parameters) {
                MethodParametersAttribute.Parameter parameter = new MethodParametersAttribute.Parameter(
                    param.name, param.access
                );
                methodParamsAttribute.addParameter(parameter);
            }
            addAttribute(methodParamsAttribute);
        }
    }

    /**
     * Gets the CodeAttribute for this method, or null if not present.
     * @return the CodeAttribute or null
     */
    public CodeAttribute getCodeAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof CodeAttribute)
                .map(attr -> (CodeAttribute) attr)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the LocalVariableTableAttribute for this method, or null if not present.
     * @return the LocalVariableTableAttribute or null
     */
    public LocalVariableTableAttribute getLocalVariableTableAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof LocalVariableTableAttribute)
                .map(attr -> (LocalVariableTableAttribute) attr)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the ExceptionsAttribute for this method, or null if not present.
     * @return the ExceptionsAttribute or null
     */
    public ExceptionsAttribute getExceptionsAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof ExceptionsAttribute)
                .map(attr -> (ExceptionsAttribute) attr)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the SignatureAttribute for this method, or null if not present.
     * @return the SignatureAttribute or null
     */
    public SignatureAttribute getSignatureAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof SignatureAttribute)
                .map(attr -> (SignatureAttribute) attr)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the MethodParametersAttribute for this method, or null if not present.
     * @return the MethodParametersAttribute or null
     */
    public MethodParametersAttribute getMethodParametersAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof MethodParametersAttribute)
                .map(attr -> (MethodParametersAttribute) attr)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns true if this method has an attribute with the given name.
     * @param attributeName the attribute name
     * @return true if present
     */
    public boolean hasAttribute(String attributeName) {
        return attributes.stream().anyMatch(attr -> attributeName.equals(attr.getName()));
    }

    /**
     * Gets an attribute by name, or null if not present.
     * @param attributeName the attribute name
     * @return the Attribute or null
     */
    public Attribute getAttribute(String attributeName) {
        return attributes.stream()
                .filter(attr -> attributeName.equals(attr.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all attributes of a given type.
     * @param attributeType the attribute class
     * @return list of attributes of the given type
     */
    public List<Attribute> getAttributesByType(Class<? extends Attribute> attributeType) {
        return attributes.stream()
                .filter(attributeType::isInstance)
                .toList();
    }

    /**
     * Removes an attribute by name.
     * @param attributeName the attribute name
     */
    public void removeAttribute(String attributeName) {
        attributes.removeIf(attr -> attributeName.equals(attr.getName()));
    }

    /**
     * Removes an attribute instance from this method.
     * @param attribute the Attribute to remove
     */
    public void removeAttribute(Attribute attribute) {
        attributes.remove(attribute);
    }
}
