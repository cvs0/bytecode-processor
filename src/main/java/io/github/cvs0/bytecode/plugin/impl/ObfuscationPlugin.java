package io.github.cvs0.bytecode.plugin.impl;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramField;
import io.github.cvs0.bytecode.member.ProgramMethod;
import io.github.cvs0.bytecode.plugin.AbstractPlugin;
import io.github.cvs0.bytecode.transform.transformer.ClassTransformer;
import io.github.cvs0.bytecode.util.BytecodeNames;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Renames program classes, methods, and fields to opaque identifiers using {@link ClassTransformer}.
 * Class renames preserve the original package structure (only the simple class name is replaced). All application
 * classes in the JAR are processed; only JVM runtime types and known third-party library types are skipped
 * (via {@link BytecodeNames#isUnsafeToRename}). Embedded library classes (shaded dependencies detected
 * by the {@link io.github.cvs0.bytecode.io.JarReader} at load time) are also skipped.</p>
 *
 * <p>Program classes are visited in sorted name order so naming is stable across runs. All bytecode is
 * updated by the transformer; {@code META-INF/services/*} paths and provider lines plus manifest launch
 * attributes ({@code Main-Class}, {@code Start-Class}, etc.) are updated. For modular JARs,
 * {@code module-info} {@code uses}, {@code provides}, and modular {@code mainClass} strings are remapped.
 * Stale {@code exports}, {@code opens}, and {@code packages} entries (and orphan {@code package-info} files)
 * are pruned after transforms.</p>
 *
 * <p>Method renames are hierarchy-aware: the {@link ClassTransformer} propagates renames across
 * superclasses and subclasses automatically using the hierarchy links built by the
 * {@link io.github.cvs0.bytecode.io.JarReader}, and methods implementing external contracts
 * (e.g. {@code Runnable.run()}) are protected via
 * {@link io.github.cvs0.bytecode.member.ProgramMethod#isSafeToRename()}.</p>
 *
 * <p><b>Configuration keys</b> (via {@link io.github.cvs0.bytecode.plugin.ConfigurablePlugin#configure})</p>
 * <table border="1" summary="ObfuscationPlugin configuration">
 *   <tr><th>Key</th><th>Type</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>{@value ObfuscationPlugin#CFG_OBFUSCATE_CLASSES}</td><td>boolean</td><td>{@code true}</td><td>Rename internal program class names (preserving package structure).</td></tr>
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

        // Collect all program classes that should be obfuscated:
        // - Skip non-application classes (shaded third-party dependencies)
        // - Skip JVM runtime types and known third-party runtime types
        List<ProgramClass> classes = new ArrayList<>();
        for (ProgramClass pc : mapping.getProgramClasses()) {
            if (!pc.isApplicationClass()) {
                continue;
            }
            if (BytecodeNames.isUnsafeToRename(pc.getName())) {
                continue;
            }
            classes.add(pc);
        }
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
                    // Preserve package structure: only rename the simple class name
                    String pkg = BytecodeNames.internalNameToPackage(on);
                    String obfuscatedSimpleName = nextName();
                    String newName = pkg.isEmpty() ? obfuscatedSimpleName : pkg + "/" + obfuscatedSimpleName;
                    transformer.renameClass(on, newName);
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
