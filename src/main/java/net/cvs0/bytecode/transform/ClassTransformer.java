package net.cvs0.bytecode.transform;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Performs transformations on classes, methods, and fields within a JarMapping.
 * Supports renaming and applying custom transformations, with automatic reference updates.
 */
public class ClassTransformer {
    /** The JarMapping to operate on. */
    private final JarMapping mapping;
    /** Map of old class names to new class names. */
    private final Map<String, String> classNameMappings = new HashMap<>();
    /** Map of class+field names to new field names. */
    private final Map<String, String> fieldNameMappings = new HashMap<>();
    /** Map of class+method+descriptor to new method names. */
    private final Map<String, String> methodNameMappings = new HashMap<>();
    
    /**
     * Constructs a ClassTransformer for the given JarMapping.
     * @param mapping the JarMapping to transform
     */
    public ClassTransformer(JarMapping mapping) {
        this.mapping = mapping;
    }
    
    /**
     * Schedules a class to be renamed.
     * @param oldName the original class name
     * @param newName the new class name
     */
    public void renameClass(String oldName, String newName) {
        classNameMappings.put(oldName, newName);
    }
    
    /**
     * Schedules a field to be renamed.
     * @param className the class containing the field
     * @param oldFieldName the original field name
     * @param newFieldName the new field name
     */
    public void renameField(String className, String oldFieldName, String newFieldName) {
        fieldNameMappings.put(className + "." + oldFieldName, newFieldName);
    }
    
    /**
     * Schedules a method to be renamed.
     * @param className the class containing the method
     * @param oldMethodName the original method name
     * @param descriptor the method descriptor
     * @param newMethodName the new method name
     */
    public void renameMethod(String className, String oldMethodName, String descriptor, String newMethodName) {
        methodNameMappings.put(className + "." + oldMethodName + descriptor, newMethodName);
    }
    
    /**
     * Applies all scheduled transformations (renames and reference updates).
     */
    public void applyTransformations() {
        applyFieldRenames();
        applyMethodRenames();
        applyClassRenames();
        updateReferences();
    }
    
    /**
     * Applies class renames to the mapping.
     */
    private void applyClassRenames() {
        for (Map.Entry<String, String> entry : classNameMappings.entrySet()) {
            String oldName = entry.getKey();
            String newName = entry.getValue();
            mapping.renameClass(oldName, newName);
        }
    }
    
    /**
     * Applies field renames to the mapping.
     */
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
    
    /**
     * Applies method renames to the mapping.
     */
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
    
    /**
     * Updates all class, field, and method references after renaming.
     */
    private void updateReferences() {
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            updateClassReferences(clazz);
        }
    }
    
    /**
     * Updates references within a single class.
     * @param clazz the ProgramClass to update
     */
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
    
    /**
     * Updates references within a single method.
     * @param method the ProgramMethod to update
     */
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
    
    /**
     * Applies a transformation function to all classes.
     * @param transformer the function to apply
     */
    public void transformClasses(Function<ProgramClass, ProgramClass> transformer) {
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            ProgramClass transformed = transformer.apply(clazz);
            if (transformed != clazz) {
                mapping.removeClass(clazz.getName());
                mapping.addClass(transformed);
            }
        }
    }
    
    /**
     * Applies a transformation function to all methods.
     * @param transformer the function to apply
     */
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
    
    /**
     * Applies a transformation function to all fields.
     * @param transformer the function to apply
     */
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
    
    /**
     * Returns a copy of the class name mappings.
     * @return a map of old to new class names
     */
    public Map<String, String> getClassNameMappings() {
        return new HashMap<>(classNameMappings);
    }
    
    /**
     * Returns a copy of the field name mappings.
     * @return a map of class+field to new field names
     */
    public Map<String, String> getFieldNameMappings() {
        return new HashMap<>(fieldNameMappings);
    }
    
    /**
     * Returns a copy of the method name mappings.
     * @return a map of class+method+descriptor to new method names
     */
    public Map<String, String> getMethodNameMappings() {
        return new HashMap<>(methodNameMappings);
    }
    
    /**
     * Clears all scheduled renames.
     */
    public void clearMappings() {
        classNameMappings.clear();
        fieldNameMappings.clear();
        methodNameMappings.clear();
    }
}