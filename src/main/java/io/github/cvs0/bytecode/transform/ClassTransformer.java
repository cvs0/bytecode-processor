package io.github.cvs0.bytecode.transform;

import io.github.cvs0.bytecode.FieldKey;
import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.MethodKey;
import io.github.cvs0.bytecode.clazz.ModuleInfoClass;
import io.github.cvs0.bytecode.clazz.PackageInfoClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramField;
import io.github.cvs0.bytecode.member.ProgramMethod;
import io.github.cvs0.bytecode.util.BytecodeNames;
import io.github.cvs0.bytecode.util.BytecodeTraversal;
import io.github.cvs0.bytecode.util.JarGraphMetadataReconciler;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleOpenNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * {@link Transformer} that performs class/field/method renames and structural edits on a {@link JarMapping}.
 *
 * <p><b>Apply order</b> ({@link #applyTransformations()}):</p>
 * <ol>
 *   <li>Structural tasks (access, superclass, interfaces, debug stripping, visitors)</li>
 *   <li>Validate renames (drop unsafe renames for constructors, native, external overrides)</li>
 *   <li>Propagate method renames across the hierarchy (using {@link ProgramClass#getHierarchyClasses()})</li>
 *   <li>Remap every ClassNode via ASM {@link ClassRemapper} + {@link MappingRemapper}</li>
 *   <li>Sync ProgramClass wrappers, update JarMapping indexes</li>
 *   <li>Post-reference tasks (string/LDC transforms, instruction hooks, resources)</li>
 *   <li>{@link JarGraphMetadataReconciler#reconcile(JarMapping)}</li>
 * </ol>
 *
 * <p>Use internal names (e.g. {@code com/foo/Bar}) when scheduling renames.</p>
 */
public class ClassTransformer implements Transformer {
    /** The JarMapping to operate on. */
    private final JarMapping mapping;
    /** Map of old class names to new class names. */
    private final Map<String, String> classNameMappings = new HashMap<>();
    /** Typed field rename keys → new field name. */
    private final Map<FieldKey, String> fieldNameMappings = new HashMap<>();
    /** Typed method rename keys → new method name. */
    private final Map<MethodKey, String> methodNameMappings = new HashMap<>();
    /** Runs before renames; use internal names as they exist when {@link #applyTransformations()} starts. */
    private final List<Runnable> structuralTasks = new ArrayList<>();
    /** Runs after reference propagation (e.g. LDC strings, instruction hooks). */
    private final List<Runnable> postReferenceTasks = new ArrayList<>();

    /**
     * Constructs a ClassTransformer for the given JarMapping.
     * @param mapping the JarMapping to transform
     */
    public ClassTransformer(JarMapping mapping) {
        this.mapping = Objects.requireNonNull(mapping, "mapping");
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
        fieldNameMappings.put(FieldKey.of(className, oldFieldName), newFieldName);
    }
    
    /**
     * Schedules a method to be renamed.
     * @param className the class containing the method
     * @param oldMethodName the original method name
     * @param descriptor the method descriptor
     * @param newMethodName the new method name
     */
    public void renameMethod(String className, String oldMethodName, String descriptor, String newMethodName) {
        if ("<init>".equals(oldMethodName) || "<clinit>".equals(oldMethodName)) {
            return;
        }
        methodNameMappings.put(MethodKey.of(className, oldMethodName, descriptor), newMethodName);
    }

    /**
     * Schedules {@link #renameClass(String, String)} for every program class in {@code oldPackage} (internal names:
     * {@code com/foo} or {@code com.foo}), including subpackages and inner classes ({@code com/foo/Outer$Inner}).
     */
    public void renamePackage(String oldPackage, String newPackage) {
        String oldP = internalPackagePrefix(oldPackage);
        String newP = internalPackagePrefix(newPackage);
        if (oldP.isEmpty()) {
            throw new IllegalArgumentException("oldPackage must not be empty");
        }
        for (ProgramClass c : new ArrayList<>(mapping.getProgramClasses())) {
            String n = c.getName();
            if (n.equals(oldP) || n.startsWith(oldP + '/')) {
                String tail = n.equals(oldP) ? "" : n.substring(oldP.length());
                renameClass(n, newP + tail);
            }
        }
        for (PackageInfoClass pic : new ArrayList<>(mapping.getPackageInfos())) {
            String n = pic.getInternalName();
            if (n.startsWith(oldP + '/')) {
                String tail = n.substring(oldP.length());
                renameClass(n, newP + tail);
            }
        }
        structuralTasks.add(() -> remapModuleDescriptorPackages(mapping, oldP, newP));
    }

    private static void remapModuleDescriptorPackages(JarMapping mapping, String oldP, String newP) {
        for (ModuleInfoClass mic : mapping.getModuleInfos()) {
            ClassNode cn = mic.getClassNode();
            if (cn == null || cn.module == null) {
                continue;
            }
            ModuleNode mod = cn.module;
            if (mod.exports != null) {
                for (ModuleExportNode e : mod.exports) {
                    e.packaze = remapSlashPackagePrefix(e.packaze, oldP, newP);
                }
            }
            if (mod.opens != null) {
                for (ModuleOpenNode o : mod.opens) {
                    o.packaze = remapSlashPackagePrefix(o.packaze, oldP, newP);
                }
            }
            if (mod.packages != null) {
                for (int i = 0; i < mod.packages.size(); i++) {
                    mod.packages.set(i, remapSlashPackagePrefix(mod.packages.get(i), oldP, newP));
                }
            }
        }
    }

    private static String remapSlashPackagePrefix(String pkgSlash, String oldP, String newP) {
        if (pkgSlash == null) {
            return null;
        }
        if (pkgSlash.equals(oldP)) {
            return newP;
        }
        if (pkgSlash.startsWith(oldP + '/')) {
            return newP + pkgSlash.substring(oldP.length());
        }
        return pkgSlash;
    }

    /**
     * Schedules {@link #renameClass(String, String)} for each program class whose internal name matches {@code filter}.
     * The {@code namer} receives the current internal name and returns the new one, or {@code null} / same string to skip.
     */
    public void renameClassesMatching(Predicate<String> filter, UnaryOperator<String> namer) {
        for (ProgramClass c : new ArrayList<>(mapping.getProgramClasses())) {
            String n = c.getName();
            if (!filter.test(n)) {
                continue;
            }
            String next = namer.apply(n);
            if (next != null && !next.equals(n)) {
                renameClass(n, next);
            }
        }
    }

    /**
     * Before renames: run a callback on every program class (snapshot at apply time). Use for custom edits not covered elsewhere.
     */
    public void visitProgramClasses(Consumer<ProgramClass> visitor) {
        structuralTasks.add(() -> {
            for (ProgramClass c : new ArrayList<>(mapping.getProgramClasses())) {
                visitor.accept(c);
            }
        });
    }

    /**
     * After reference updates: visit every program class (names reflect completed renames).
     */
    public void visitProgramClassesAfterReferences(Consumer<ProgramClass> visitor) {
        postReferenceTasks.add(() -> {
            for (ProgramClass c : mapping.getProgramClasses()) {
                visitor.accept(c);
            }
        });
    }

    /**
     * After reference updates: visit every method on every program class.
     */
    public void visitMethodsAfterReferences(BiConsumer<ProgramClass, ProgramMethod> visitor) {
        postReferenceTasks.add(() -> BytecodeTraversal.forEachMethod(mapping, visitor));
    }

    /**
     * Before renames: set access flags on a class (internal name as loaded in the mapping).
     */
    public void setClassAccess(String internalClassName, int access) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                c.setAccess(access);
            }
        });
    }

    /**
     * Before renames: set access on a field.
     */
    public void setFieldAccess(String internalClassName, String fieldName, int access) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                ProgramField f = c.getField(fieldName);
                if (f != null) {
                    f.setAccess(access);
                }
            }
        });
    }

    /**
     * Before renames: set access on a method.
     */
    public void setMethodAccess(String internalClassName, String methodName, String descriptor, int access) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                ProgramMethod m = c.getMethod(methodName, descriptor);
                if (m != null) {
                    m.setAccess(access);
                }
            }
        });
    }

    /**
     * Before renames: change superclass (internal names). Does not validate hierarchy.
     */
    public void setSuperClass(String internalClassName, String newSuperInternalName) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                c.setSuperName(newSuperInternalName);
            }
        });
    }

    /**
     * Before renames: add a superinterface if not already present.
     */
    public void addInterface(String internalClassName, String interfaceInternalName) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                c.addInterface(interfaceInternalName);
            }
        });
    }

    /**
     * Before renames: remove a superinterface if present.
     */
    public void removeInterface(String internalClassName, String interfaceInternalName) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                c.removeInterface(interfaceInternalName);
            }
        });
    }

    /**
     * Before renames: set class file major version ({@link org.objectweb.asm.Opcodes#V21} etc.) for one class.
     */
    public void setClassFileVersion(String internalClassName, int version) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                c.setClassVersion(version);
            }
        });
    }

    /**
     * Before renames: set class file version for every program class.
     */
    public void setClassFileVersionForAll(int version) {
        structuralTasks.add(() -> {
            for (ProgramClass c : mapping.getProgramClasses()) {
                c.setClassVersion(version);
            }
        });
    }

    /**
     * Before renames: strip debug metadata on one class (use the name as in the mapping at apply time).
     */
    public void stripDebugOnClass(String internalClassName, StripDebugMode first, StripDebugMode... more) {
        EnumSet<StripDebugMode> modes = EnumSet.of(first, more);
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                stripDebugOnClass(c, modes);
            }
        });
    }

    /**
     * Before renames: strip debug metadata on all program classes.
     */
    public void stripDebugEverywhere(StripDebugMode first, StripDebugMode... more) {
        EnumSet<StripDebugMode> modes = EnumSet.of(first, more);
        structuralTasks.add(() -> {
            for (ProgramClass c : mapping.getProgramClasses()) {
                stripDebugOnClass(c, modes);
            }
        });
    }

    /**
     * After reference updates: replace {@code LDC} string constants where the filter matches.
     *
     * @param filter   which class/method to touch (evaluated after renames)
     * @param replacer {@code null} return leaves the constant unchanged
     */
    public void transformStringConstants(
            BiPredicate<ProgramClass, ProgramMethod> filter,
            UnaryOperator<String> replacer) {
        postReferenceTasks.add(() -> BytecodeTraversal.forEachMethod(mapping, (clazz, method) -> {
            if (!filter.test(clazz, method)) {
                return;
            }
            if (method.getMethodNode() == null || method.getMethodNode().instructions == null) {
                return;
            }
            for (AbstractInsnNode insn = method.getMethodNode().instructions.getFirst();
                 insn != null;
                 insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                    String out = replacer.apply(s);
                    if (out != null && !out.equals(s)) {
                        ldc.cst = out;
                    }
                }
            }
        }));
    }

    /**
     * After reference updates: rewrite any {@code LDC} constant (string, primitive wrappers, {@link org.objectweb.asm.Type}, etc.).
     * Return {@code null} or the same reference to leave unchanged.
     */
    public void transformLdcConstants(
            BiPredicate<ProgramClass, ProgramMethod> filter,
            Function<Object, Object> replacer) {
        postReferenceTasks.add(() -> BytecodeTraversal.forEachMethod(mapping, (clazz, method) -> {
            if (!filter.test(clazz, method)) {
                return;
            }
            if (method.getMethodNode() == null || method.getMethodNode().instructions == null) {
                return;
            }
            for (AbstractInsnNode insn = method.getMethodNode().instructions.getFirst();
                 insn != null;
                 insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode ldc) {
                    Object out = replacer.apply(ldc.cst);
                    if (out != null && !Objects.equals(out, ldc.cst)) {
                        ldc.cst = out;
                    }
                }
            }
        }));
    }

    /**
     * After reference updates: run an {@link InstructionTransformer} on each matching method.
     */
    public void transformInstructions(
            BiPredicate<ProgramClass, ProgramMethod> filter,
            Consumer<InstructionTransformer> transformation) {
        postReferenceTasks.add(() -> BytecodeTraversal.forEachMethod(mapping, (clazz, method) -> {
            if (filter.test(clazz, method)) {
                transformation.accept(InstructionTransformer.forMethod(method));
            }
        }));
    }

    /**
     * After reference updates: rename a non-class JAR entry.
     */
    public void renameResource(String fromEntryName, String toEntryName) {
        postReferenceTasks.add(() -> {
            byte[] data = mapping.getResource(fromEntryName);
            if (data != null) {
                mapping.removeResource(fromEntryName);
                mapping.addResource(toEntryName, data);
            }
        });
    }

    /**
     * After reference updates: remove a resource entry.
     */
    public void removeResource(String entryName) {
        postReferenceTasks.add(() -> mapping.removeResource(entryName));
    }

    /**
     * Before renames: set the JVM generic signature on a class (may be {@code null} to clear).
     */
    public void setClassSignature(String internalClassName, String signature) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                c.setSignature(signature);
            }
        });
    }

    /**
     * Before renames: set the JVM generic signature on a field.
     */
    public void setFieldSignature(String internalClassName, String fieldName, String signature) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                ProgramField f = c.getField(fieldName);
                if (f != null) {
                    f.setSignature(signature);
                }
            }
        });
    }

    /**
     * Before renames: set the JVM generic signature on a method.
     */
    public void setMethodSignature(String internalClassName, String methodName, String descriptor, String signature) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                ProgramMethod m = c.getMethod(methodName, descriptor);
                if (m != null) {
                    m.setSignature(signature);
                }
            }
        });
    }

    /**
     * Before renames: remove a method from a program class.
     */
    public void removeMethod(String internalClassName, String methodName, String descriptor) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                c.removeMethod(methodName, descriptor);
            }
        });
    }

    /**
     * Before renames: remove a field from a program class.
     */
    public void removeField(String internalClassName, String fieldName) {
        structuralTasks.add(() -> {
            ProgramClass c = mapping.getProgramClass(internalClassName);
            if (c != null) {
                c.removeField(fieldName);
            }
        });
    }

    /**
     * Before renames: drop a program or library class entry from the mapping.
     */
    public void removeClassFromMapping(String internalClassName) {
        structuralTasks.add(() -> mapping.removeClass(internalClassName));
    }

    /**
     * Applies all scheduled transformations (renames and reference updates).
     *
     * <p><b>Phases:</b></p>
     * <ol>
     *   <li>Run structural tasks</li>
     *   <li>Validate renames — drop unsafe class/field/method renames</li>
     *   <li>Propagate method renames across the class hierarchy</li>
     *   <li>Build {@link MappingRemapper} and remap every ClassNode via ASM {@link ClassRemapper}</li>
     *   <li>Sync ProgramClass wrappers and update JarMapping indexes</li>
     *   <li>Run post-reference tasks, reconcile metadata</li>
     * </ol>
     */
    public void applyTransformations() {
        runStructuralTasks();

        validateRenames();
        propagateMethodRenames();

        if (!classNameMappings.isEmpty() || !fieldNameMappings.isEmpty() || !methodNameMappings.isEmpty()) {
            MappingRemapper remapper = new MappingRemapper(classNameMappings, fieldNameMappings, methodNameMappings);
            remapAllClassNodes(remapper);
        }

        runPostReferenceTasks();
        JarGraphMetadataReconciler.reconcile(mapping);
    }

    @Override
    public void transform(JarMapping mapping) {
        applyTransformations();
    }

    /**
     * Drops renames that would break JVM contracts:
     * <ul>
     *   <li>Class renames for JVM runtime or known third-party types</li>
     *   <li>Field renames on JVM/third-party types</li>
     *   <li>Method renames on JVM/third-party types, constructors, or methods overriding external contracts</li>
     * </ul>
     */
    private void validateRenames() {
        // Class renames
        classNameMappings.entrySet().removeIf(e -> BytecodeNames.isUnsafeToRename(e.getKey()));

        // Field renames
        fieldNameMappings.entrySet().removeIf(e -> BytecodeNames.isUnsafeToRename(e.getKey().owner()));

        // Method renames — check owner, constructor, native, external override
        methodNameMappings.entrySet().removeIf(e -> {
            MethodKey mk = e.getKey();
            if (BytecodeNames.isUnsafeToRename(mk.owner())) {
                return true;
            }
            if ("<init>".equals(mk.name()) || "<clinit>".equals(mk.name())) {
                return true;
            }
            ProgramClass clazz = mapping.getProgramClass(mk.owner());
            if (clazz != null) {
                ProgramMethod m = clazz.getMethod(mk.name(), mk.descriptor());
                if (m != null && !m.isSafeToRename()) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * For each scheduled method rename, walks the hierarchy (up and down) and applies the
     * same rename to every related class that defines the same method — unless that method is unsafe.
     */
    private void propagateMethodRenames() {
        Map<MethodKey, String> propagated = new HashMap<>();
        for (Map.Entry<MethodKey, String> entry : methodNameMappings.entrySet()) {
            MethodKey mk = entry.getKey();
            String newName = entry.getValue();

            ProgramClass clazz = mapping.getProgramClass(mk.owner());
            if (clazz == null) {
                continue;
            }
            Set<ProgramClass> hierarchy = clazz.getHierarchyClasses();
            for (ProgramClass related : hierarchy) {
                if (related.getName().equals(mk.owner())) {
                    continue; // already in the map
                }
                ProgramMethod m = related.getMethod(mk.name(), mk.descriptor());
                if (m != null && m.isSafeToRename()) {
                    propagated.put(MethodKey.of(related.getName(), mk.name(), mk.descriptor()), newName);
                }
            }
        }
        methodNameMappings.putAll(propagated);
    }

    /**
     * Remaps every ClassNode via ASM's {@link ClassRemapper}, then updates JarMapping indexes,
     * then syncs all ProgramClass wrappers from the remapped ClassNodes.
     *
     * <p>For ProgramClasses without a ClassNode (e.g. test-constructed), a manual fallback
     * applies class/field/method renames and updates instruction references.</p>
     */
    private void remapAllClassNodes(MappingRemapper remapper) {
        org.objectweb.asm.commons.Remapper asmRemapper = remapper.toAsm();

        // Phase 1: Remap ClassNodes (or apply manual fallback for null-ClassNode classes)
        Map<String, String> nameChanges = new HashMap<>();
        List<ProgramClass> noClassNodePCs = new ArrayList<>();

        for (ProgramClass pc : new ArrayList<>(mapping.getProgramClasses())) {
            ClassNode original = pc.getClassNode();
            if (original == null) {
                noClassNodePCs.add(pc);
                continue;
            }
            String oldName = pc.getName();
            ClassNode remapped = new ClassNode();
            ClassRemapper cr = new ClassRemapper(remapped, asmRemapper);
            original.accept(cr);
            pc.setClassNode(remapped);

            String newName = remapped.name;
            if (newName != null && !oldName.equals(newName)) {
                nameChanges.put(oldName, newName);
            }
        }

        // Phase 1b: Manual fallback for ProgramClasses without a ClassNode
        for (ProgramClass pc : noClassNodePCs) {
            String oldName = pc.getName();
            applyManualRenames(pc, remapper, asmRemapper);
            String newName = classNameMappings.get(oldName);
            if (newName != null) {
                nameChanges.put(oldName, newName);
            }
        }

        // Phase 2: Update JarMapping indexes (includes package-infos, library classes, etc.)
        // First, add any classNameMappings entries not already covered (e.g. package-info renames)
        for (Map.Entry<String, String> entry : classNameMappings.entrySet()) {
            if (!nameChanges.containsKey(entry.getKey())) {
                nameChanges.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : nameChanges.entrySet()) {
            mapping.renameClass(entry.getKey(), entry.getValue());
        }

        // Phase 3: Sync ProgramClass wrappers from remapped ClassNodes
        for (ProgramClass pc : mapping.getProgramClasses()) {
            pc.syncFromClassNode();
        }

        // Phase 4: Remap module descriptors in-place
        remapModuleDescriptors(asmRemapper);
    }

    /**
     * Manual rename fallback for ProgramClasses that have no ClassNode.
     * Applies field/method renames and updates instruction references.
     */
    private void applyManualRenames(ProgramClass pc, MappingRemapper remapper,
                                     org.objectweb.asm.commons.Remapper asmRemapper) {
        String className = pc.getName();

        // Remap superName and interfaces
        if (pc.getSuperName() != null) {
            String mappedSuper = classNameMappings.get(pc.getSuperName());
            if (mappedSuper != null) {
                pc.setSuperName(mappedSuper);
            }
        }
        if (pc.getInterfaces() != null) {
            List<String> ifaces = new ArrayList<>(pc.getInterfaces());
            for (int i = 0; i < ifaces.size(); i++) {
                String mapped = classNameMappings.get(ifaces.get(i));
                if (mapped != null) {
                    ifaces.set(i, mapped);
                }
            }
            pc.setInterfaces(ifaces);
        }

        // Apply field renames for this class
        for (ProgramField f : new ArrayList<>(pc.getFields())) {
            String newFieldName = fieldNameMappings.get(FieldKey.of(className, f.getName()));
            if (newFieldName != null) {
                pc.renameField(f.getName(), newFieldName);
            }
        }

        // Apply method renames for this class
        for (ProgramMethod m : new ArrayList<>(pc.getMethods())) {
            String newMethodName = methodNameMappings.get(MethodKey.of(className, m.getName(), m.getDescriptor()));
            if (newMethodName != null) {
                pc.renameMethod(m.getName(), m.getDescriptor(), newMethodName);
            }
        }

        // Update instruction references in any MethodNodes
        for (ProgramMethod pm : pc.getMethods()) {
            MethodNode mn = pm.getMethodNode();
            if (mn == null || mn.instructions == null) {
                continue;
            }
            remapInstructions(mn, asmRemapper);
            // Remap method descriptor
            mn.desc = asmRemapper.mapMethodDesc(mn.desc);
        }
    }

    /**
     * Walks instruction nodes and updates class/field/method references using the ASM remapper.
     */
    private static void remapInstructions(MethodNode mn, org.objectweb.asm.commons.Remapper asmRemapper) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode fin) {
                fin.owner = asmRemapper.mapType(fin.owner);
                fin.desc = asmRemapper.mapDesc(fin.desc);
                fin.name = asmRemapper.mapFieldName(fin.owner, fin.name, fin.desc);
            } else if (insn instanceof MethodInsnNode min) {
                String origOwner = min.owner;
                min.owner = asmRemapper.mapType(min.owner);
                min.name = asmRemapper.mapMethodName(origOwner, min.name, min.desc);
                min.desc = asmRemapper.mapMethodDesc(min.desc);
            } else if (insn instanceof TypeInsnNode tin) {
                tin.desc = asmRemapper.mapType(tin.desc);
            } else if (insn instanceof MultiANewArrayInsnNode man) {
                man.desc = asmRemapper.mapDesc(man.desc);
            } else if (insn instanceof InvokeDynamicInsnNode idin) {
                idin.desc = asmRemapper.mapMethodDesc(idin.desc);
            } else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof org.objectweb.asm.Type t) {
                ldc.cst = org.objectweb.asm.Type.getType(asmRemapper.mapDesc(t.getDescriptor()));
            } else if (insn instanceof FrameNode frame) {
                remapFrameTypes(frame.local, asmRemapper);
                remapFrameTypes(frame.stack, asmRemapper);
            }
        }
        // Remap try-catch handler types
        if (mn.tryCatchBlocks != null) {
            for (var tcb : mn.tryCatchBlocks) {
                if (tcb.type != null) {
                    tcb.type = asmRemapper.mapType(tcb.type);
                }
            }
        }
        // Remap local variable types
        if (mn.localVariables != null) {
            for (var lv : mn.localVariables) {
                lv.desc = asmRemapper.mapDesc(lv.desc);
                if (lv.signature != null) {
                    lv.signature = asmRemapper.mapSignature(lv.signature, true);
                }
            }
        }
    }

    private static void remapFrameTypes(List<Object> types, org.objectweb.asm.commons.Remapper asmRemapper) {
        if (types == null) {
            return;
        }
        for (int i = 0; i < types.size(); i++) {
            Object o = types.get(i);
            if (o instanceof String s) {
                types.set(i, asmRemapper.mapType(s));
            }
        }
    }

    /**
     * Remaps module descriptors (mainClass, uses, provides, exports, opens, packages) in-place.
     */
    private void remapModuleDescriptors(org.objectweb.asm.commons.Remapper asmRemapper) {
        for (ModuleInfoClass mic : mapping.getModuleInfos()) {
            ClassNode cn = mic.getClassNode();
            if (cn == null || cn.module == null) {
                continue;
            }
            ModuleNode mod = cn.module;
            if (mod.mainClass != null) {
                mod.mainClass = asmRemapper.mapType(mod.mainClass);
            }
            if (mod.uses != null) {
                for (int i = 0; i < mod.uses.size(); i++) {
                    mod.uses.set(i, asmRemapper.mapType(mod.uses.get(i)));
                }
            }
            if (mod.provides != null) {
                for (ModuleProvideNode pn : mod.provides) {
                    pn.service = asmRemapper.mapType(pn.service);
                    if (pn.providers != null) {
                        for (int i = 0; i < pn.providers.size(); i++) {
                            pn.providers.set(i, asmRemapper.mapType(pn.providers.get(i)));
                        }
                    }
                }
            }
            if (mod.exports != null) {
                for (ModuleExportNode e : mod.exports) {
                    e.packaze = asmRemapper.mapType(e.packaze);
                }
            }
            if (mod.opens != null) {
                for (ModuleOpenNode o : mod.opens) {
                    o.packaze = asmRemapper.mapType(o.packaze);
                }
            }
            if (mod.packages != null) {
                for (int i = 0; i < mod.packages.size(); i++) {
                    mod.packages.set(i, asmRemapper.mapType(mod.packages.get(i)));
                }
            }
        }
    }

    private void runStructuralTasks() {
        for (Runnable r : structuralTasks) {
            r.run();
        }
        structuralTasks.clear();
    }

    private void runPostReferenceTasks() {
        for (Runnable r : postReferenceTasks) {
            r.run();
        }
        postReferenceTasks.clear();
    }

    private static String internalPackagePrefix(String pkg) {
        if (pkg == null) {
            throw new IllegalArgumentException("package must not be null");
        }
        String p = pkg.trim().replace('.', '/');
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private static void stripDebugOnClass(ProgramClass clazz, EnumSet<StripDebugMode> modes) {
        ClassNode cn = clazz.getClassNode();
        if (cn == null) {
            return;
        }
        if (modes.contains(StripDebugMode.SOURCE_FILE)) {
            clazz.setSourceFile(null);
            clazz.setSourceDebug(null);
        }
        if (cn.methods == null) {
            return;
        }
        for (MethodNode mn : cn.methods) {
            if (mn.instructions != null && modes.contains(StripDebugMode.LINE_NUMBERS)) {
                AbstractInsnNode insn = mn.instructions.getFirst();
                while (insn != null) {
                    AbstractInsnNode next = insn.getNext();
                    if (insn instanceof LineNumberNode) {
                        mn.instructions.remove(insn);
                    }
                    insn = next;
                }
            }
            if (modes.contains(StripDebugMode.LOCAL_VARIABLES)) {
                mn.localVariables = null;
            }
            if (modes.contains(StripDebugMode.METHOD_PARAMETERS)) {
                mn.parameters = null;
            }
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
     * Returns a copy of scheduled class renames (internal slash names). Still populated after
     * {@link #applyTransformations()} for use when updating manifests or diagnostics.
     */
    public Map<String, String> getClassNameMappings() {
        return new HashMap<>(classNameMappings);
    }
    
    /**
     * Returns a copy of the field name mappings (legacy string-keyed form).
     * @return a map of {@code "owner.fieldName"} to new field names
     */
    public Map<String, String> getFieldNameMappings() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<FieldKey, String> e : fieldNameMappings.entrySet()) {
            result.put(e.getKey().toKeyString(), e.getValue());
        }
        return result;
    }

    /**
     * Returns a copy of the field name mappings with typed keys.
     */
    public Map<FieldKey, String> getTypedFieldNameMappings() {
        return new HashMap<>(fieldNameMappings);
    }
    
    /**
     * Returns a copy of the method name mappings (legacy string-keyed form).
     * @return a map of {@code "owner.methodName(descriptor)"} to new method names
     */
    public Map<String, String> getMethodNameMappings() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<MethodKey, String> e : methodNameMappings.entrySet()) {
            result.put(e.getKey().toKeyString(), e.getValue());
        }
        return result;
    }

    /**
     * Returns a copy of the method name mappings with typed keys.
     */
    public Map<MethodKey, String> getTypedMethodNameMappings() {
        return new HashMap<>(methodNameMappings);
    }
    
    /**
     * Clears all scheduled renames.
     */
    public void clearMappings() {
        classNameMappings.clear();
        fieldNameMappings.clear();
        methodNameMappings.clear();
        structuralTasks.clear();
        postReferenceTasks.clear();
    }

    /**
     * Whether any renames or phased tasks are queued (after {@link #applyTransformations()}, this is false).
     */
    public boolean hasPendingWork() {
        return !classNameMappings.isEmpty()
                || !fieldNameMappings.isEmpty()
                || !methodNameMappings.isEmpty()
                || !structuralTasks.isEmpty()
                || !postReferenceTasks.isEmpty();
    }
}