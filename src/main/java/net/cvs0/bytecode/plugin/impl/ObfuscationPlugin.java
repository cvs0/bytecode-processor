package net.cvs0.bytecode.plugin.impl;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import net.cvs0.bytecode.plugin.AbstractPlugin;
import net.cvs0.bytecode.transform.ClassTransformer;

import java.util.Random;

/**
 * Plugin that obfuscates class, method, and field names in a JarMapping.
 * Supports configuration for which elements to obfuscate and name prefix/seed.
 */
public class ObfuscationPlugin extends AbstractPlugin {
    private Random random;
    private int counter;

    /**
     * Constructs an ObfuscationPlugin with default metadata.
     */
    public ObfuscationPlugin() {
        super("Obfuscation Plugin", "1.0.0", "Obfuscates class, method, and field names");
    }

    /**
     * Initializes the plugin and random seed.
     */
    @Override
    public void initialize() {
        super.initialize();
        long seed = getIntConfig("seed", (int) System.currentTimeMillis());
        random = new Random(seed);
        counter = 0;
    }

    /**
     * Processes the JarMapping and applies obfuscation to classes, methods, and fields as configured.
     *
     * @param mapping the JarMapping to process
     */
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

    /**
     * Returns true if the class should be obfuscated.
     *
     * @param clazz the ProgramClass
     * @return true if obfuscate
     */
    private boolean shouldObfuscateClass(ProgramClass clazz) {
        return !clazz.getName().startsWith("java/") &&
               !clazz.getName().startsWith("javax/") &&
               !clazz.getName().startsWith("sun/") &&
               !clazz.getName().contains("Main");
    }

    /**
     * Returns true if the method should be obfuscated.
     *
     * @param method the ProgramMethod
     * @return true if obfuscate
     */
    private boolean shouldObfuscateMethod(ProgramMethod method) {
        return !method.isConstructor() &&
               !method.isStaticInitializer() &&
               !"main".equals(method.getName()) &&
               !method.getName().startsWith("get") &&
               !method.getName().startsWith("set") &&
               !method.getName().startsWith("is");
    }

    /**
     * Returns true if the field should be obfuscated.
     *
     * @param field the ProgramField
     * @return true if obfuscate
     */
    private boolean shouldObfuscateField(ProgramField field) {
        return !field.isFinal() || !field.isStatic();
    }

    /**
     * Generates a new obfuscated name using the configured prefix and counter.
     *
     * @return the obfuscated name
     */
    private String generateObfuscatedName() {
        String prefix = getStringConfig("namePrefix", "a");
        return prefix + (counter++);
    }

    /**
     * Returns the plugin priority (higher runs first).
     *
     * @return the plugin priority
     */
    @Override
    public int getPriority() {
        return 100;
    }
}