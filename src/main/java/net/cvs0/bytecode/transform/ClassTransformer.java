package net.cvs0.bytecode.transform;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ClassTransformer {
    private final JarMapping mapping;
    private final Map<String, String> classNameMappings = new HashMap<>();
    private final Map<String, String> fieldNameMappings = new HashMap<>();
    private final Map<String, String> methodNameMappings = new HashMap<>();
    
    public ClassTransformer(JarMapping mapping) {
        this.mapping = mapping;
    }
    
    public void renameClass(String oldName, String newName) {
        classNameMappings.put(oldName, newName);
    }
    
    public void renameField(String className, String oldFieldName, String newFieldName) {
        fieldNameMappings.put(className + "." + oldFieldName, newFieldName);
    }
    
    public void renameMethod(String className, String oldMethodName, String descriptor, String newMethodName) {
        methodNameMappings.put(className + "." + oldMethodName + descriptor, newMethodName);
    }
    
    public void applyTransformations() {
        applyFieldRenames();
        applyMethodRenames();
        applyClassRenames();
        updateReferences();
    }
    
    private void applyClassRenames() {
        for (Map.Entry<String, String> entry : classNameMappings.entrySet()) {
            String oldName = entry.getKey();
            String newName = entry.getValue();
            mapping.renameClass(oldName, newName);
        }
    }
    
    private void applyFieldRenames() {
        for (Map.Entry<String, String> entry : fieldNameMappings.entrySet()) {
            String key = entry.getKey();
            String newName = entry.getValue();
            
            int lastDot = key.lastIndexOf('.');
            String className = key.substring(0, lastDot);
            String oldFieldName = key.substring(lastDot + 1);
            
            ProgramClass clazz = mapping.getProgramClass(className);
            if (clazz != null) {
                clazz.renameField(oldFieldName, newName);
            }
        }
    }
    
    private void applyMethodRenames() {
        for (Map.Entry<String, String> entry : methodNameMappings.entrySet()) {
            String key = entry.getKey();
            String newName = entry.getValue();
            
            int lastDot = key.lastIndexOf('.');
            String className = key.substring(0, lastDot);
            String methodPart = key.substring(lastDot + 1);
            
            int parenIndex = methodPart.indexOf('(');
            String oldMethodName = methodPart.substring(0, parenIndex);
            String descriptor = methodPart.substring(parenIndex);
            
            ProgramClass clazz = mapping.getProgramClass(className);
            if (clazz != null) {
                clazz.renameMethod(oldMethodName, descriptor, newName);
            }
        }
    }
    
    private void updateReferences() {
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            updateClassReferences(clazz);
        }
    }
    
    private void updateClassReferences(ProgramClass clazz) {
        if (clazz.getSuperName() != null && classNameMappings.containsKey(clazz.getSuperName())) {
            clazz.setSuperName(classNameMappings.get(clazz.getSuperName()));
        }
        
        for (int i = 0; i < clazz.getInterfaces().size(); i++) {
            String interfaceName = clazz.getInterfaces().get(i);
            if (classNameMappings.containsKey(interfaceName)) {
                clazz.getInterfaces().set(i, classNameMappings.get(interfaceName));
            }
        }
        
        for (ProgramMethod method : clazz.getMethods()) {
            updateMethodReferences(method);
        }
    }
    
    private void updateMethodReferences(ProgramMethod method) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            method.getMethodNode().instructions.forEach(insn -> {
                if (insn instanceof org.objectweb.asm.tree.FieldInsnNode) {
                    org.objectweb.asm.tree.FieldInsnNode fieldInsn = (org.objectweb.asm.tree.FieldInsnNode) insn;
                    
                    if (classNameMappings.containsKey(fieldInsn.owner)) {
                        fieldInsn.owner = classNameMappings.get(fieldInsn.owner);
                    }
                    
                    String fieldKey = fieldInsn.owner + "." + fieldInsn.name;
                    if (fieldNameMappings.containsKey(fieldKey)) {
                        fieldInsn.name = fieldNameMappings.get(fieldKey);
                    }
                }
                
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode) {
                    org.objectweb.asm.tree.MethodInsnNode methodInsn = (org.objectweb.asm.tree.MethodInsnNode) insn;
                    
                    if (classNameMappings.containsKey(methodInsn.owner)) {
                        methodInsn.owner = classNameMappings.get(methodInsn.owner);
                    }
                    
                    String methodKey = methodInsn.owner + "." + methodInsn.name + methodInsn.desc;
                    if (methodNameMappings.containsKey(methodKey)) {
                        methodInsn.name = methodNameMappings.get(methodKey);
                    }
                }
                
                if (insn instanceof org.objectweb.asm.tree.TypeInsnNode) {
                    org.objectweb.asm.tree.TypeInsnNode typeInsn = (org.objectweb.asm.tree.TypeInsnNode) insn;
                    
                    if (classNameMappings.containsKey(typeInsn.desc)) {
                        typeInsn.desc = classNameMappings.get(typeInsn.desc);
                    }
                }
            });
        }
    }
    
    public void transformClasses(Function<ProgramClass, ProgramClass> transformer) {
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            ProgramClass transformed = transformer.apply(clazz);
            if (transformed != clazz) {
                mapping.removeClass(clazz.getName());
                mapping.addClass(transformed);
            }
        }
    }
    
    public void transformMethods(Function<ProgramMethod, ProgramMethod> transformer) {
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramMethod method : clazz.getMethods()) {
                ProgramMethod transformed = transformer.apply(method);
                if (transformed != method) {
                    clazz.removeMethod(method.getName(), method.getDescriptor());
                    clazz.addMethod(transformed);
                }
            }
        }
    }
    
    public void transformFields(Function<ProgramField, ProgramField> transformer) {
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            for (ProgramField field : clazz.getFields()) {
                ProgramField transformed = transformer.apply(field);
                if (transformed != field) {
                    clazz.removeField(field.getName());
                    clazz.addField(transformed);
                }
            }
        }
    }
    
    public Map<String, String> getClassNameMappings() {
        return new HashMap<>(classNameMappings);
    }
    
    public Map<String, String> getFieldNameMappings() {
        return new HashMap<>(fieldNameMappings);
    }
    
    public Map<String, String> getMethodNameMappings() {
        return new HashMap<>(methodNameMappings);
    }
    
    public void clearMappings() {
        classNameMappings.clear();
        fieldNameMappings.clear();
        methodNameMappings.clear();
    }
}