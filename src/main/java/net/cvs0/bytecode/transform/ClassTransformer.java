package net.cvs0.bytecode.transform;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import net.cvs0.bytecode.util.BytecodeTraversal;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Performs transformations on classes, methods, fields, resources, and method bodies within a {@link JarMapping}.
 *
 * <p><b>Apply order</b> when you call {@link #applyTransformations()}:
 * <ol>
 *   <li>Structural tasks (access, superclass, interfaces, versions, signatures, member removal, debug stripping, {@link #visitProgramClasses})</li>
 *   <li>Scheduled field renames, then method renames, then class renames</li>
 *   <li>Reference propagation (owners, descriptors, signatures, invokedynamic, etc.)</li>
 *   <li>Post tasks ({@link #transformStringConstants}, {@link #transformLdcConstants}, {@link #transformInstructions}, resources, {@link #visitMethodsAfterReferences}, …)</li>
 * </ol>
 *
 * <p>APIs such as {@link #renamePackage}, {@link #renameClassesMatching}, and {@link #renameClass} enqueue renames; use internal names
 * (e.g. {@code com/foo/Bar}) consistent with the mapping at the time you call {@code applyTransformations()}.
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
     */
    public void applyTransformations() {
        runStructuralTasks();
        applyFieldRenames();
        applyMethodRenames();
        applyClassRenames();
        updateReferences();
        runPostReferenceTasks();
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

        List<String> interfaces = new ArrayList<>(clazz.getInterfaces());
        boolean ifaceChanged = false;
        for (int i = 0; i < interfaces.size(); i++) {
            String interfaceName = interfaces.get(i);
            if (classNameMappings.containsKey(interfaceName)) {
                interfaces.set(i, classNameMappings.get(interfaceName));
                ifaceChanged = true;
            }
        }
        if (ifaceChanged) {
            clazz.setInterfaces(interfaces);
        }

        if (!classNameMappings.isEmpty()) {
            if (clazz.getSignature() != null) {
                clazz.setSignature(DescriptorRemapper.remap(clazz.getSignature(), classNameMappings));
            }
            ClassNode cn = clazz.getClassNode();
            if (cn != null) {
                if (cn.outerMethodDesc != null) {
                    cn.outerMethodDesc = DescriptorRemapper.remap(cn.outerMethodDesc, classNameMappings);
                }
                if (cn.nestHostClass != null && classNameMappings.containsKey(cn.nestHostClass)) {
                    cn.nestHostClass = classNameMappings.get(cn.nestHostClass);
                }
                if (cn.nestMembers != null) {
                    for (int i = 0; i < cn.nestMembers.size(); i++) {
                        String m = cn.nestMembers.get(i);
                        if (classNameMappings.containsKey(m)) {
                            cn.nestMembers.set(i, classNameMappings.get(m));
                        }
                    }
                }
                if (cn.permittedSubclasses != null) {
                    for (int i = 0; i < cn.permittedSubclasses.size(); i++) {
                        String p = cn.permittedSubclasses.get(i);
                        if (classNameMappings.containsKey(p)) {
                            cn.permittedSubclasses.set(i, classNameMappings.get(p));
                        }
                    }
                }
            }
            for (ProgramField field : clazz.getFields()) {
                field.setDescriptor(DescriptorRemapper.remap(field.getDescriptor(), classNameMappings));
                if (field.getSignature() != null) {
                    field.setSignature(DescriptorRemapper.remap(field.getSignature(), classNameMappings));
                }
            }
            for (ProgramMethod method : clazz.getMethods()) {
                method.setDescriptor(DescriptorRemapper.remap(method.getDescriptor(), classNameMappings));
                if (method.getSignature() != null) {
                    method.setSignature(DescriptorRemapper.remap(method.getSignature(), classNameMappings));
                }
                if (method.getMethodNode() != null && method.getMethodNode().exceptions != null) {
                    List<String> exceptions = method.getMethodNode().exceptions;
                    for (int i = 0; i < exceptions.size(); i++) {
                        String ex = exceptions.get(i);
                        if (classNameMappings.containsKey(ex)) {
                            exceptions.set(i, classNameMappings.get(ex));
                        }
                    }
                    method.setExceptions(exceptions.toArray(new String[0]));
                }
            }
        }

        for (ProgramMethod method : clazz.getMethods()) {
            updateMethodReferences(method);
        }
        if (!classNameMappings.isEmpty()) {
            for (ProgramMethod method : clazz.getMethods()) {
                remapInstructionDescriptors(method);
            }
        }
    }

    /**
     * Updates owners and simple names in instructions (descriptors are remapped afterward).
     */
    private void updateMethodReferences(ProgramMethod method) {
        if (method.getMethodNode() != null && method.getMethodNode().instructions != null) {
            method.getMethodNode().instructions.forEach(insn -> {
                if (insn instanceof FieldInsnNode fieldInsn) {
                    if (classNameMappings.containsKey(fieldInsn.owner)) {
                        fieldInsn.owner = classNameMappings.get(fieldInsn.owner);
                    }

                    String fieldKey = fieldInsn.owner + "." + fieldInsn.name;
                    if (fieldNameMappings.containsKey(fieldKey)) {
                        fieldInsn.name = fieldNameMappings.get(fieldKey);
                    }
                }

                if (insn instanceof MethodInsnNode methodInsn) {
                    if (classNameMappings.containsKey(methodInsn.owner)) {
                        methodInsn.owner = classNameMappings.get(methodInsn.owner);
                    }

                    String methodKey = methodInsn.owner + "." + methodInsn.name + methodInsn.desc;
                    if (methodNameMappings.containsKey(methodKey)) {
                        methodInsn.name = methodNameMappings.get(methodKey);
                    }
                }

                if (insn instanceof TypeInsnNode typeInsn) {
                    if (classNameMappings.containsKey(typeInsn.desc)) {
                        typeInsn.desc = classNameMappings.get(typeInsn.desc);
                    }
                }
            });
        }
    }

    private void remapInstructionDescriptors(ProgramMethod method) {
        if (method.getMethodNode() == null || method.getMethodNode().instructions == null) {
            return;
        }
        for (AbstractInsnNode insn = method.getMethodNode().instructions.getFirst();
             insn != null;
             insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode fieldInsn) {
                fieldInsn.desc = DescriptorRemapper.remap(fieldInsn.desc, classNameMappings);
            } else if (insn instanceof MethodInsnNode methodInsn) {
                methodInsn.desc = DescriptorRemapper.remap(methodInsn.desc, classNameMappings);
            } else if (insn instanceof InvokeDynamicInsnNode indy) {
                indy.desc = DescriptorRemapper.remap(indy.desc, classNameMappings);
                remapBootstrapArguments(indy.bsmArgs);
            } else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type t) {
                Type remapped = remapTypeConstant(t);
                if (remapped != t) {
                    ldc.cst = remapped;
                }
            } else if (insn instanceof MultiANewArrayInsnNode multi) {
                multi.desc = DescriptorRemapper.remap(multi.desc, classNameMappings);
            }
        }
    }

    private void remapBootstrapArguments(Object[] args) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Type t) {
                args[i] = remapTypeConstant(t);
            } else if (arg instanceof Handle h) {
                args[i] = remapHandle(h);
            }
        }
    }

    private Type remapTypeConstant(Type t) {
        if (t.getSort() == Type.OBJECT) {
            String in = t.getInternalName();
            if (classNameMappings.containsKey(in)) {
                return Type.getObjectType(classNameMappings.get(in));
            }
            return t;
        }
        if (t.getSort() == Type.ARRAY) {
            String d = t.getDescriptor();
            String remapped = DescriptorRemapper.remap(d, classNameMappings);
            return remapped.equals(d) ? t : Type.getType(remapped);
        }
        return t;
    }

    private Handle remapHandle(Handle h) {
        String owner = classNameMappings.getOrDefault(h.getOwner(), h.getOwner());
        String desc = DescriptorRemapper.remap(h.getDesc(), classNameMappings);
        if (owner.equals(h.getOwner()) && desc.equals(h.getDesc())) {
            return h;
        }
        return new Handle(h.getTag(), owner, h.getName(), desc, h.isInterface());
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