package io.github.cvs0.bytecode.member;

import io.github.cvs0.bytecode.attribute.*;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.instruction.Instruction;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

/**
 * Represents a method in a program class, including its name, descriptor, signature, access flags, instructions, attributes, local variables, and line numbers.
 * Provides methods for manipulating method structure, bytecode, and metadata, as well as ASM MethodNode integration.
 */
public class ProgramMethod {
    @Getter
    private String name;
    @Getter
    private String descriptor;
    @Getter
    private String signature;
    @Getter
    private int access;
    private String[] exceptions;
    @Getter
    private int maxStack;
    @Getter
    private int maxLocals;

    @Getter
    @Setter
    private ProgramClass owner;
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<Instruction> instructions = new ArrayList<>();
    private final List<LocalVariable> localVariables = new ArrayList<>();
    private final List<LineNumber> lineNumbers = new ArrayList<>();
    @Getter
    @Setter
    private MethodNode methodNode;

    /**
     * {@code true} when this method overrides or implements a contract from an external type
     * (not present as a ProgramClass in the JAR). Set by the JarReader at read time.
     * Such methods must not be renamed or the JVM will fail to resolve the override at runtime.
     */
    @Getter
    @Setter
    private boolean overridesExternal;

    /**
     * Whether this method can be safely renamed without breaking JVM contracts.
     * A method is safe to rename if it is not a constructor, static initializer, native method,
     * or an external contract override.
     */
    public boolean isSafeToRename() {
        return !isConstructor()
                && !isStaticInitializer()
                && !isNative()
                && !overridesExternal;
    }

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
        this.exceptions = methodNode.exceptions != null
                ? methodNode.exceptions.toArray(new String[0])
                : new String[0];
        this.maxStack = methodNode.maxStack;
        this.maxLocals = methodNode.maxLocals;

        if (methodNode.instructions != null) {
            for (AbstractInsnNode insn : methodNode.instructions) {
                instructions.add(new Instruction(insn));
            }
        }

        extractAttributesFromMethodNode(methodNode);
        syncCachedListsFromAttributes();
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
        if (index < 0 || index > instructions.size()) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + instructions.size());
        }
        instructions.add(index, instruction);
        if (methodNode != null && methodNode.instructions != null) {
            InsnList il = methodNode.instructions;
            if (index == il.size()) {
                il.add(instruction.getInstructionNode());
            } else {
                il.insert(il.get(index), instruction.getInstructionNode());
            }
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
     * Rebuilds the high-level {@link Instruction} list from the backing {@link MethodNode}'s {@link InsnList}.
     * The ASM list is not modified.
     */
    public void resyncInstructionsFromMethodNode() {
        instructions.clear();
        if (methodNode != null && methodNode.instructions != null) {
            for (AbstractInsnNode insn : methodNode.instructions) {
                instructions.add(new Instruction(insn));
            }
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
            if (exceptions != null && exceptions.length > 0) {
                methodNode.exceptions = new ArrayList<>(Arrays.asList(exceptions));
            } else {
                methodNode.exceptions = null;
            }
        }
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
     * Returns true if this method is static.
     * @return true if static
     */
    public boolean isStatic() {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    /**
     * Returns true if this method is final.
     * @return true if final
     */
    public boolean isFinal() {
        return (access & Opcodes.ACC_FINAL) != 0;
    }

    /**
     * Returns true if this method is public.
     * @return true if public
     */
    public boolean isPublic() {
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    /**
     * Returns true if this method is private.
     * @return true if private
     */
    public boolean isPrivate() {
        return (access & Opcodes.ACC_PRIVATE) != 0;
    }

    /**
     * Returns true if this method is protected.
     * @return true if protected
     */
    public boolean isProtected() {
        return (access & Opcodes.ACC_PROTECTED) != 0;
    }

    /**
     * Returns true if this method is abstract.
     * @return true if abstract
     */
    public boolean isAbstract() {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    /**
     * Returns true if this method is synchronized.
     * @return true if synchronized
     */
    public boolean isSynchronized() {
        return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
    }

    /**
     * Returns true if this method is native.
     * @return true if native
     */
    public boolean isNative() {
        return (access & Opcodes.ACC_NATIVE) != 0;
    }

    /**
     * Returns true if this method is synthetic.
     * @return true if synthetic
     */
    public boolean isSynthetic() {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
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
     * Fills {@link #lineNumbers} and {@link #localVariables} from the {@link CodeAttribute} (nested tables) or, when there is no code body, from a top-level {@link LocalVariableTableAttribute}.
     */
    private void syncCachedListsFromAttributes() {
        lineNumbers.clear();
        localVariables.clear();
        CodeAttribute code = firstAttribute(CodeAttribute.class);
        if (code != null) {
            for (Attribute nested : code.getCodeAttributes()) {
                if (nested instanceof LineNumberTableAttribute lnt) {
                    lineNumbers.addAll(lnt.getLineNumbers());
                } else if (nested instanceof LocalVariableTableAttribute lvt) {
                    localVariables.addAll(lvt.getLocalVariables());
                }
            }
            return;
        }
        LocalVariableTableAttribute topLvt = firstAttribute(LocalVariableTableAttribute.class);
        if (topLvt != null) {
            localVariables.addAll(topLvt.getLocalVariables());
        }
    }

    /**
     * Extracts attributes from the ASM MethodNode and adds them to this method.
     * @param methodNode the ASM MethodNode
     */
    private void extractAttributesFromMethodNode(MethodNode methodNode) {
        if (methodNode == null) {
            return;
        }
        if (methodNode.signature != null) {
            addAttribute(AttributeFactory.createSignatureAttribute(methodNode.signature));
        }
        if (methodNode.exceptions != null && !methodNode.exceptions.isEmpty()) {
            addAttribute(AttributeFactory.createExceptionsAttribute(methodNode.exceptions));
        }

        boolean hasCode = methodNode.instructions != null && methodNode.instructions.size() > 0;
        if (hasCode) {
            CodeAttribute codeAttribute = AttributeFactory.createCodeAttribute(methodNode);
            if (codeAttribute != null) {
                addAttribute(codeAttribute);
            }
        } else if (methodNode.localVariables != null && !methodNode.localVariables.isEmpty()) {
            LocalVariableTableAttribute lvtAttribute =
                    AttributeFactory.createLocalVariableTable(methodNode.localVariables, methodNode);
            addAttribute(lvtAttribute);
        }

        if ((methodNode.access & Opcodes.ACC_SYNTHETIC) != 0) {
            addAttribute(AttributeFactory.createSyntheticAttribute());
        }

        if (methodNode.parameters != null && !methodNode.parameters.isEmpty()) {
            MethodParametersAttribute methodParamsAttribute = AttributeFactory.createMethodParametersAttribute();
            for (var param : methodNode.parameters) {
                MethodParametersAttribute.Parameter parameter =
                        new MethodParametersAttribute.Parameter(param.name, param.access);
                methodParamsAttribute.addParameter(parameter);
            }
            addAttribute(methodParamsAttribute);
        }
    }

    private <T extends Attribute> T firstAttribute(Class<T> type) {
        for (Attribute attr : attributes) {
            if (type.isInstance(attr)) {
                return type.cast(attr);
            }
        }
        return null;
    }

    /**
     * Gets the CodeAttribute for this method, or null if not present.
     * @return the CodeAttribute or null
     */
    public CodeAttribute getCodeAttribute() {
        return firstAttribute(CodeAttribute.class);
    }

    /**
     * Gets the LocalVariableTableAttribute for this method, or null if not present.
     * Prefers a top-level table; otherwise the one nested under {@link CodeAttribute} (the usual JVM layout).
     * @return the LocalVariableTableAttribute or null
     */
    public LocalVariableTableAttribute getLocalVariableTableAttribute() {
        LocalVariableTableAttribute top = firstAttribute(LocalVariableTableAttribute.class);
        if (top != null) {
            return top;
        }
        CodeAttribute code = firstAttribute(CodeAttribute.class);
        if (code != null) {
            for (Attribute nested : code.getCodeAttributes()) {
                if (nested instanceof LocalVariableTableAttribute lvt) {
                    return lvt;
                }
            }
        }
        return null;
    }

    /**
     * Gets the ExceptionsAttribute for this method, or null if not present.
     * @return the ExceptionsAttribute or null
     */
    public ExceptionsAttribute getExceptionsAttribute() {
        return firstAttribute(ExceptionsAttribute.class);
    }

    /**
     * Gets the SignatureAttribute for this method, or null if not present.
     * @return the SignatureAttribute or null
     */
    public SignatureAttribute getSignatureAttribute() {
        return firstAttribute(SignatureAttribute.class);
    }

    /**
     * Gets the MethodParametersAttribute for this method, or null if not present.
     * @return the MethodParametersAttribute or null
     */
    public MethodParametersAttribute getMethodParametersAttribute() {
        return firstAttribute(MethodParametersAttribute.class);
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
