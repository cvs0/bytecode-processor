package net.cvs0.bytecode.plugin.impl;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import net.cvs0.bytecode.plugin.AbstractPlugin;
import net.cvs0.bytecode.transform.ClassTransformer;

import java.util.Random;

public class ObfuscationPlugin extends AbstractPlugin {
    private Random random;
    private int counter;
    
    public ObfuscationPlugin() {
        super("Obfuscation Plugin", "1.0.0", "Obfuscates class, method, and field names");
    }
    
    @Override
    public void initialize() {
        super.initialize();
        long seed = getIntConfig("seed", (int) System.currentTimeMillis());
        random = new Random(seed);
        counter = 0;
    }
    
    @Override
    public void process(JarMapping mapping) {
        boolean obfuscateClasses = getBooleanConfig("obfuscateClasses", true);
        boolean obfuscateMethods = getBooleanConfig("obfuscateMethods", true);
        boolean obfuscateFields = getBooleanConfig("obfuscateFields", true);
        
        ClassTransformer transformer = new ClassTransformer(mapping);
        
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            if (obfuscateClasses && shouldObfuscateClass(clazz)) {
                String newName = generateObfuscatedName();
                transformer.renameClass(clazz.getName(), newName);
            }
            
            if (obfuscateFields) {
                for (ProgramField field : clazz.getFields()) {
                    if (shouldObfuscateField(field)) {
                        String newName = generateObfuscatedName();
                        transformer.renameField(clazz.getName(), field.getName(), newName);
                    }
                }
            }
            
            if (obfuscateMethods) {
                for (ProgramMethod method : clazz.getMethods()) {
                    if (shouldObfuscateMethod(method)) {
                        String newName = generateObfuscatedName();
                        transformer.renameMethod(clazz.getName(), method.getName(), method.getDescriptor(), newName);
                    }
                }
            }
        }
        
        transformer.applyTransformations();
    }
    
    private boolean shouldObfuscateClass(ProgramClass clazz) {
        return !clazz.getName().startsWith("java/") &&
               !clazz.getName().startsWith("javax/") &&
               !clazz.getName().startsWith("sun/") &&
               !clazz.getName().contains("Main");
    }
    
    private boolean shouldObfuscateMethod(ProgramMethod method) {
        return !method.isConstructor() &&
               !method.isStaticInitializer() &&
               !"main".equals(method.getName()) &&
               !method.getName().startsWith("get") &&
               !method.getName().startsWith("set") &&
               !method.getName().startsWith("is");
    }
    
    private boolean shouldObfuscateField(ProgramField field) {
        return !field.isFinal() || !field.isStatic();
    }
    
    private String generateObfuscatedName() {
        String prefix = getStringConfig("namePrefix", "a");
        return prefix + (counter++);
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
}