package io.github.cvs0.bytecode.plugin.impl;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramField;
import io.github.cvs0.bytecode.member.ProgramMethod;
import io.github.cvs0.bytecode.plugin.AbstractPlugin;
import io.github.cvs0.bytecode.transform.ClassTransformer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Example plugin: renames program classes, methods, and fields to opaque identifiers and applies
 * {@link ClassTransformer} once at the end. Program classes are visited in sorted name order so naming is stable
 * across runs. All {@link io.github.cvs0.bytecode.clazz.ProgramClass} bytecode is updated by the transformer; {@code META-INF/services/*} paths and provider lines plus
 * manifest launch attributes ({@code Main-Class}, {@code Start-Class}, etc.) are updated when possible.
 * For modular JARs, {@code module-info} {@code uses}, {@code provides}, and modular {@code mainClass} strings are
 * remapped with renamed types. Stale {@code exports}, {@code opens}, and {@code packages} entries (and orphan
 * {@code package-info} files) are pruned after transforms when no program class remains in that package.</p>
 *
 * <p>Unsafe renames (manifest entry classes, JVM runtime types, annotation class literals, high fan-in types, etc.)
 * are filtered inside {@link ClassTransformer}, not by naming specific third-party packages here.</p>
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

        List<ProgramClass> classes = new ArrayList<>(mapping.getApplicationClasses());
        classes.sort(Comparator.comparing(ProgramClass::getJarEntryName));
        Map<ProgramClass, String> originalInternalName = new IdentityHashMap<>();
        for (ProgramClass c : classes) {
            originalInternalName.put(c, c.getName());
        }

        if (obfuscateClasses) {
            Set<String> classRenameScheduled = new HashSet<>();
            for (ProgramClass clazz : classes) {
                String on = originalInternalName.get(clazz);
                if (classRenameScheduled.add(on)) {
                    transformer.renameClass(on, nextName());
                }
            }
        }

        for (ProgramClass clazz : classes) {
            String className = clazz.getName();

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
        mapping.remapServiceLoaderResourcePaths(transformer.getClassNameMappings());
        mapping.remapServiceLoaderImplementations(transformer.getClassNameMappings());
        mapping.remapManifestMainClass(transformer.getClassNameMappings());
    }

    private String nextName() {
        String prefix = getStringConfig(CFG_NAME_PREFIX, "a");
        return prefix + (nameCounter++);
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
