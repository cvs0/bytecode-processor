package net.cvs0.bytecode.member;

import net.cvs0.bytecode.attribute.*;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.instruction.Instruction;
import net.cvs0.bytecode.member.LineNumber;
import net.cvs0.bytecode.member.LocalVariable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

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
    
    public ProgramMethod(String name, String descriptor, int access) {
        this.name = name;
        this.descriptor = descriptor;
        this.access = access;
    }
    
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
    }
    
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }
    
    public void addLocalVariable(LocalVariable localVariable) {
        localVariables.add(localVariable);
    }
    
    public void removeLocalVariable(LocalVariable localVariable) {
        localVariables.remove(localVariable);
    }
    
    public void addLineNumber(LineNumber lineNumber) {
        lineNumbers.add(lineNumber);
    }
    
    public void removeLineNumber(LineNumber lineNumber) {
        lineNumbers.remove(lineNumber);
    }
    
    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
        if (methodNode != null && methodNode.instructions != null) {
            methodNode.instructions.add(instruction.getInstructionNode());
        }
    }
    
    public void insertInstruction(int index, Instruction instruction) {
        instructions.add(index, instruction);
        if (methodNode != null && methodNode.instructions != null) {
            methodNode.instructions.insert(
                methodNode.instructions.get(index), 
                instruction.getInstructionNode()
            );
        }
    }
    
    public void removeInstruction(int index) {
        if (index >= 0 && index < instructions.size()) {
            Instruction removed = instructions.remove(index);
            if (methodNode != null && methodNode.instructions != null) {
                methodNode.instructions.remove(removed.getInstructionNode());
            }
        }
    }
    
    public void replaceInstruction(int index, Instruction newInstruction) {
        if (index >= 0 && index < instructions.size()) {
            Instruction old = instructions.set(index, newInstruction);
            if (methodNode != null && methodNode.instructions != null) {
                methodNode.instructions.set(old.getInstructionNode(), newInstruction.getInstructionNode());
            }
        }
    }
    
    public void clearInstructions() {
        instructions.clear();
        if (methodNode != null && methodNode.instructions != null) {
            methodNode.instructions.clear();
        }
    }
    
    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }
    
    public List<Instruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }
    
    public List<LocalVariable> getLocalVariables() {
        return Collections.unmodifiableList(localVariables);
    }
    
    public List<LineNumber> getLineNumbers() {
        return Collections.unmodifiableList(lineNumbers);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        if (methodNode != null) {
            methodNode.name = name;
        }
    }
    
    public String getDescriptor() {
        return descriptor;
    }
    
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
        if (methodNode != null) {
            methodNode.desc = descriptor;
        }
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
        if (methodNode != null) {
            methodNode.signature = signature;
        }
    }
    
    public int getAccess() {
        return access;
    }
    
    public void setAccess(int access) {
        this.access = access;
        if (methodNode != null) {
            methodNode.access = access;
        }
    }
    
    public String[] getExceptions() {
        return exceptions != null ? exceptions.clone() : new String[0];
    }
    
    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions != null ? exceptions.clone() : new String[0];
        if (methodNode != null) {
            methodNode.exceptions = exceptions != null ? Arrays.asList(exceptions) : new ArrayList<>();
        }
    }
    
    public int getMaxStack() {
        return maxStack;
    }
    
    public void setMaxStack(int maxStack) {
        this.maxStack = maxStack;
        if (methodNode != null) {
            methodNode.maxStack = maxStack;
        }
    }
    
    public int getMaxLocals() {
        return maxLocals;
    }
    
    public void setMaxLocals(int maxLocals) {
        this.maxLocals = maxLocals;
        if (methodNode != null) {
            methodNode.maxLocals = maxLocals;
        }
    }
    
    public ProgramClass getOwner() {
        return owner;
    }
    
    public void setOwner(ProgramClass owner) {
        this.owner = owner;
    }
    
    public MethodNode getMethodNode() {
        return methodNode;
    }
    
    public void setMethodNode(MethodNode methodNode) {
        this.methodNode = methodNode;
    }
    
    public boolean isStatic() {
        return (access & 0x0008) != 0;
    }
    
    public boolean isFinal() {
        return (access & 0x0010) != 0;
    }
    
    public boolean isPublic() {
        return (access & 0x0001) != 0;
    }
    
    public boolean isPrivate() {
        return (access & 0x0002) != 0;
    }
    
    public boolean isProtected() {
        return (access & 0x0004) != 0;
    }
    
    public boolean isAbstract() {
        return (access & 0x0400) != 0;
    }
    
    public boolean isSynchronized() {
        return (access & 0x0020) != 0;
    }
    
    public boolean isNative() {
        return (access & 0x0100) != 0;
    }
    
    public boolean isSynthetic() {
        return (access & 0x1000) != 0;
    }
    
    public boolean isConstructor() {
        return "<init>".equals(name);
    }
    
    public boolean isStaticInitializer() {
        return "<clinit>".equals(name);
    }
    
    public String getFullName() {
        return owner != null ? owner.getName() + "." + name + descriptor : name + descriptor;
    }
    
    public int getInstructionCount() {
        return instructions.size();
    }
    
    public boolean hasInstructions() {
        return !instructions.isEmpty();
    }
    
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
            LocalVariableTableAttribute lvtAttribute = AttributeFactory.createLocalVariableTable(methodNode.localVariables);
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
    
    public CodeAttribute getCodeAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof CodeAttribute)
                .map(attr -> (CodeAttribute) attr)
                .findFirst()
                .orElse(null);
    }
    
    public LocalVariableTableAttribute getLocalVariableTableAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof LocalVariableTableAttribute)
                .map(attr -> (LocalVariableTableAttribute) attr)
                .findFirst()
                .orElse(null);
    }
    
    public ExceptionsAttribute getExceptionsAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof ExceptionsAttribute)
                .map(attr -> (ExceptionsAttribute) attr)
                .findFirst()
                .orElse(null);
    }
    
    public SignatureAttribute getSignatureAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof SignatureAttribute)
                .map(attr -> (SignatureAttribute) attr)
                .findFirst()
                .orElse(null);
    }
    
    public MethodParametersAttribute getMethodParametersAttribute() {
        return attributes.stream()
                .filter(attr -> attr instanceof MethodParametersAttribute)
                .map(attr -> (MethodParametersAttribute) attr)
                .findFirst()
                .orElse(null);
    }
    
    public boolean hasAttribute(String attributeName) {
        return attributes.stream().anyMatch(attr -> attributeName.equals(attr.getName()));
    }
    
    public Attribute getAttribute(String attributeName) {
        return attributes.stream()
                .filter(attr -> attributeName.equals(attr.getName()))
                .findFirst()
                .orElse(null);
    }
    
    public List<Attribute> getAttributesByType(Class<? extends Attribute> attributeType) {
        return attributes.stream()
                .filter(attributeType::isInstance)
                .toList();
    }
    
    public void removeAttribute(String attributeName) {
        attributes.removeIf(attr -> attributeName.equals(attr.getName()));
    }
    
    public void removeAttribute(Attribute attribute) {
        attributes.remove(attribute);
    }
}