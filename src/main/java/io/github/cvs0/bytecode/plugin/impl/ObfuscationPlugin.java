package io.github.cvs0.bytecode.plugin.impl;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramField;
import io.github.cvs0.bytecode.member.ProgramMethod;
import io.github.cvs0.bytecode.plugin.AbstractPlugin;
import io.github.cvs0.bytecode.transform.ClassTransformer;

/**
 * <p>Example plugin: renames program classes, methods, and fields to opaque identifiers and applies
 * {@link ClassTransformer} once at the end.</p>
 *
 * <p><b>Configuration keys</b> (via {@link io.github.cvs0.bytecode.plugin.ConfigurablePlugin#configure})</p>
 * <table border="1" summary="ObfuscationPlugin configuration">
 *   <tr><th>Key</th><th>Type</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>{@value ObfuscationPlugin#CFG_OBFUSCATE_CLASSES}</td><td>boolean</td><td>{@code true}</td><td>Rename internal program class names.</td></tr>
 *   <tr><td>{@value ObfuscationPlugin#CFG_OBFUSCATE_METHODS}</td><td>boolean</td><td>{@code true}</td><td>Rename methods (constructors and {@code main} skipped).</td></tr>
 *   <tr><td>{@value ObfuscationPlugin#CFG_OBFUSCATE_FIELDS}</td><td>boolean</td><td>{@code true}</td><td>Rename fields ({@code static final} constants skipped).</td></tr>
 *   <tr><td>{@value ObfuscationPlugin#CFG_NAME_PREFIX}</td><td>String</td><td>{@code "a"}</td><td>Prefix for generated names; suffix is a monotonic counter.</td></tr>
 * </table>
 */
public class ObfuscationPlugin extends AbstractPlugin {

    public static final String CFG_OBFUSCATE_CLASSES = "obfuscateClasses";
    public static final String CFG_OBFUSCATE_METHODS = "obfuscateMethods";
    public static final String CFG_OBFUSCATE_FIELDS = "obfuscateFields";
    public static final String CFG_NAME_PREFIX = "namePrefix";

    private static final String[] JDK_PREFIXES = {
            "java/", "javax/", "jdk/", "sun/", "com/sun/", "org/w3c/", "org/xml/"
    };

    private int nameCounter;

    public ObfuscationPlugin() {
        super(
                "ObfuscationPlugin",
                "2.0.0",
                "Example: rename program classes, methods, and fields using ClassTransformer");
    }

    @Override
    public void initialize() {
        super.initialize();
        nameCounter = 0;
    }

    @Override
    public void process(JarMapping mapping) {
        boolean obfuscateClasses = getBooleanConfig(CFG_OBFUSCATE_CLASSES, true);
        boolean obfuscateMethods = getBooleanConfig(CFG_OBFUSCATE_METHODS, true);
        boolean obfuscateFields = getBooleanConfig(CFG_OBFUSCATE_FIELDS, true);

        ClassTransformer transformer = new ClassTransformer(mapping);

        for (ProgramClass clazz : mapping.getProgramClasses()) {
            String className = clazz.getName();

            if (obfuscateClasses && shouldObfuscateClass(className)) {
                transformer.renameClass(className, nextName());
            }

            if (obfuscateFields) {
                for (ProgramField field : clazz.getFields()) {
                    if (shouldObfuscateField(field)) {
                        transformer.renameField(className, field.getName(), nextName());
                    }
                }
            }

            if (obfuscateMethods) {
                for (ProgramMethod method : clazz.getMethods()) {
                    if (shouldObfuscateMethod(method)) {
                        transformer.renameMethod(className, method.getName(), method.getDescriptor(), nextName());
                    }
                }
            }
        }

        transformer.applyTransformations();
    }

    private String nextName() {
        String prefix = getStringConfig(CFG_NAME_PREFIX, "a");
        return prefix + (nameCounter++);
    }

    private static boolean shouldObfuscateClass(String internalName) {
        for (String p : JDK_PREFIXES) {
            if (internalName.startsWith(p)) {
                return false;
            }
        }
        int slash = internalName.lastIndexOf('/');
        String simple = slash < 0 ? internalName : internalName.substring(slash + 1);
        return !simple.equals("Main");
    }

    private static boolean shouldObfuscateMethod(ProgramMethod method) {
        if (method.isConstructor() || method.isStaticInitializer()) {
            return false;
        }
        String n = method.getName();
        if ("main".equals(n) || "equals".equals(n) || "hashCode".equals(n) || "toString".equals(n)) {
            return false;
        }
        return !n.startsWith("get") && !n.startsWith("set") && !n.startsWith("is");
    }

    /**
     * Skips {@code static final} fields (typical constants) to avoid breaking reflection and switch tables.
     */
    private static boolean shouldObfuscateField(ProgramField field) {
        return !(field.isStatic() && field.isFinal());
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
