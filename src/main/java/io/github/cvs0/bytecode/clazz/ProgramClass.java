package io.github.cvs0.bytecode.clazz;

import io.github.cvs0.bytecode.attribute.*;
import io.github.cvs0.bytecode.member.InnerClass;
import io.github.cvs0.bytecode.member.ProgramField;
import io.github.cvs0.bytecode.member.ProgramMethod;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One {@code .class} entry from a JAR (or similar), with full bytecode via {@link ClassNode} when loaded from disk.
 *
 * <p>Hierarchy links ({@link #getParentProgramClass()}, {@link #getChildProgramClasses()},
 * {@link #getResolvedInterfaces()}) are resolved at read time by the {@link io.github.cvs0.bytecode.util.JarReader},
 * so downstream code never needs to infer relationships.</p>
 *
 * <p>{@link #isApplicationClass()} distinguishes host-project code from shaded dependencies.
 * Method override info ({@link io.github.cvs0.bytecode.member.ProgramMethod#isOverridesExternal()})
 * is also resolved at read time, eliminating the need for a separate inheritance graph.</p>
 */
public class ProgramClass {
    /** JAR entry path for this class (e.g. {@code com/foo/Bar.class}). */
    private String jarEntryName;

    @Getter
    private String name;

    @Getter
    private String superName;

    private List<String> interfaces;

    @Getter
    private int access;

    @Getter
    private String signature;

    @Getter
    private String sourceFile;

    @Getter
    private String sourceDebug;

    @Getter
    private String outerClass;

    @Getter
    private String outerMethod;

    @Getter
    private String outerMethodDesc;

    private final Map<String, ProgramField> fields = new ConcurrentHashMap<>();
    private final Map<String, ProgramMethod> methods = new ConcurrentHashMap<>();
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<InnerClass> innerClasses = new ArrayList<>();

    @Getter
    @Setter
    private ClassNode classNode;

    @Getter
    private int classVersion;
    private List<RecordComponentNode> recordComponents = new ArrayList<>();
    @Getter
    private String nestHostClass;
    private List<String> nestMembers = new ArrayList<>();
    private List<String> permittedSubclasses = new ArrayList<>();

    /** Resolved parent ProgramClass, or {@code null} if the supertype is external (e.g. java/lang/Object). */
    @Getter
    @Setter
    private ProgramClass parentProgramClass;
    /** Direct subclasses and implementors that are ProgramClasses in this JAR. */
    private final List<ProgramClass> childProgramClasses = new ArrayList<>();
    /** Interfaces resolved to ProgramClass instances (external interfaces are excluded). */
    private final List<ProgramClass> resolvedInterfaces = new ArrayList<>();
    /** External supertypes (class or interface) that could not be resolved to a ProgramClass. */
    private final Set<String> unresolvedSuperTypes = new LinkedHashSet<>();

    /** {@code true} when this class belongs to the host application (not a shaded dependency). Default: true. */
    @Getter
    @Setter
    private boolean applicationClass = true;


    /**
     * Constructs a ProgramClass with the given name.
     * @param name the class name
     */
    public ProgramClass(String name) {
        this.name = name;
        this.jarEntryName = name + ".class";
        this.interfaces = new ArrayList<>();
    }

    /**
     * Constructs a ProgramClass from an ASM ClassNode.
     * @param classNode the ASM ClassNode
     */
    public ProgramClass(ClassNode classNode) {
        this.classNode = classNode;
        this.name = classNode.name;
        this.jarEntryName = classNode.name != null ? classNode.name + ".class" : null;
        this.superName = classNode.superName;
        this.interfaces = new ArrayList<>(classNode.interfaces);
        this.access = classNode.access;
        this.signature = classNode.signature;
        this.sourceFile = classNode.sourceFile;
        this.sourceDebug = classNode.sourceDebug;
        this.outerClass = classNode.outerClass;
        this.outerMethod = classNode.outerMethod;
        this.outerMethodDesc = classNode.outerMethodDesc;

        if (classNode.version > 0) {
            this.classVersion = classNode.version;
        }

        if (classNode.recordComponents != null) {
            this.recordComponents = new ArrayList<>(classNode.recordComponents);
        }

        if (classNode.nestHostClass != null) {
            this.nestHostClass = classNode.nestHostClass;
        }

        if (classNode.nestMembers != null) {
            this.nestMembers = new ArrayList<>(classNode.nestMembers);
        }

        if (classNode.permittedSubclasses != null) {
            this.permittedSubclasses = new ArrayList<>(classNode.permittedSubclasses);
        }
    }

    /**
     * Adds a field to this class.
     * @param field the ProgramField to add
     */
    public void addField(ProgramField field) {
        fields.put(field.getName(), field);
        field.setOwner(this);
        if (classNode != null) {
            if (classNode.fields == null) {
                classNode.fields = new ArrayList<>();
            }
            FieldNode fn = field.getFieldNode();
            if (fn == null) {
                fn = new FieldNode(field.getAccess(), field.getName(), field.getDescriptor(), field.getSignature(), field.getValue());
                field.setFieldNode(fn);
            }
            if (!classNode.fields.contains(fn)) {
                classNode.fields.add(fn);
            }
        }
    }

    /**
     * Adds a method to this class.
     * @param method the ProgramMethod to add
     */
    public void addMethod(ProgramMethod method) {
        String key = method.getName() + method.getDescriptor();
        methods.put(key, method);
        method.setOwner(this);
        if (classNode != null) {
            if (classNode.methods == null) {
                classNode.methods = new ArrayList<>();
            }
            MethodNode mn = method.getMethodNode();
            if (mn == null) {
                String[] ex = method.getExceptions();
                String[] exForCtor = ex != null && ex.length > 0 ? ex.clone() : null;
                mn = new MethodNode(method.getAccess(), method.getName(), method.getDescriptor(), method.getSignature(), exForCtor);
                method.setMethodNode(mn);
            }
            if (!classNode.methods.contains(mn)) {
                classNode.methods.add(mn);
            }
        }
    }

    /**
     * Adds an attribute to this class.
     * @param attribute the Attribute to add
     */
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }

    /**
     * Adds an inner class to this class.
     * @param innerClass the InnerClass to add
     */
    public void addInnerClass(InnerClass innerClass) {
        innerClasses.add(innerClass);
    }

    /**
     * Removes an inner class from this class.
     * @param innerClass the InnerClass to remove
     */
    public void removeInnerClass(InnerClass innerClass) {
        innerClasses.remove(innerClass);
    }

    /**
     * Gets a field by name.
     * @param name the field name
     * @return the ProgramField or null
     */
    public ProgramField getField(String name) {
        return fields.get(name);
    }

    /**
     * Gets a method by name and descriptor.
     * @param name the method name
     * @param descriptor the method descriptor
     * @return the ProgramMethod or null
     */
    public ProgramMethod getMethod(String name, String descriptor) {
        return methods.get(name + descriptor);
    }

    /**
     * Returns all fields in this class.
     * @return unmodifiable collection of fields
     */
    public Collection<ProgramField> getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }

    /**
     * Returns all methods in this class.
     * @return unmodifiable collection of methods
     */
    public Collection<ProgramMethod> getMethods() {
        return Collections.unmodifiableCollection(methods.values());
    }

    /**
     * Returns all attributes in this class.
     * @return unmodifiable list of attributes
     */
    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    /**
     * Returns all inner classes in this class.
     * @return unmodifiable list of inner classes
     */
    public List<InnerClass> getInnerClasses() {
        return Collections.unmodifiableList(innerClasses);
    }

    /**
     * Removes a field by name.
     * @param name the field name
     */
    public void removeField(String name) {
        ProgramField field = fields.remove(name);
        if (field != null) {
            field.setOwner(null);
            if (classNode != null && classNode.fields != null) {
                if (field.getFieldNode() != null) {
                    classNode.fields.remove(field.getFieldNode());
                } else {
                    classNode.fields.removeIf(fn -> name.equals(fn.name));
                }
            }
        }
    }

    /**
     * Removes a method by name and descriptor.
     * @param name the method name
     * @param descriptor the method descriptor
     */
    public void removeMethod(String name, String descriptor) {
        ProgramMethod method = methods.remove(name + descriptor);
        if (method != null) {
            method.setOwner(null);
            if (classNode != null && classNode.methods != null) {
                if (method.getMethodNode() != null) {
                    classNode.methods.remove(method.getMethodNode());
                } else {
                    classNode.methods.removeIf(m -> name.equals(m.name) && descriptor.equals(m.desc));
                }
            }
        }
    }

    /**
     * Renames a field in this class.
     * @param oldName the old field name
     * @param newName the new field name
     */
    public void renameField(String oldName, String newName) {
        ProgramField field = fields.remove(oldName);
        if (field != null) {
            field.setName(newName);
            fields.put(newName, field);
        }
    }

    /**
     * Renames a method in this class.
     * @param oldName the old method name
     * @param descriptor the method descriptor
     * @param newName the new method name
     */
    public void renameMethod(String oldName, String descriptor, String newName) {
        ProgramMethod method = methods.remove(oldName + descriptor);
        if (method != null) {
            method.setName(newName);
            methods.put(newName + descriptor, method);
        }
    }

    /**
     * JAR path for this class when writing (defaults to {@code getName() + ".class"}).
     */
    public String getJarEntryName() {
        return jarEntryName != null ? jarEntryName : (name != null ? name + ".class" : null);
    }

    public void setJarEntryName(String jarEntryName) {
        this.jarEntryName = Objects.requireNonNull(jarEntryName, "jarEntryName");
    }

    /**
     * Updates {@link #jarEntryName} after an internal rename when the path ends with {@code oldInternalName + ".class"}.
     */
    public void remapJarEntryPath(String oldInternalName, String newInternalName) {
        Objects.requireNonNull(oldInternalName, "oldInternalName");
        Objects.requireNonNull(newInternalName, "newInternalName");
        String path = jarEntryName != null ? jarEntryName : oldInternalName + ".class";
        String oldSuffix = oldInternalName + ".class";
        if (path.endsWith(oldSuffix)) {
            this.jarEntryName = path.substring(0, path.length() - oldSuffix.length()) + newInternalName + ".class";
        }
    }


    /**
     * Sets the class name.
     * @param name the new class name
     */
    public void setName(String name) {
        this.name = name;
        if (classNode != null) {
            classNode.name = name;
        }
    }

    /**
     * Sets the superclass name.
     * @param superName the new superclass name
     */
    public void setSuperName(String superName) {
        this.superName = superName;
        if (classNode != null) {
            classNode.superName = superName;
        }
    }

    /**
     * Gets the list of interfaces implemented by this class.
     * @return unmodifiable list of interface names
     */
    public List<String> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    /**
     * Sets the list of interfaces implemented by this class.
     * @param interfaces the new list of interface names
     */
    public void setInterfaces(List<String> interfaces) {
        this.interfaces = new ArrayList<>(interfaces);
        if (classNode != null) {
            classNode.interfaces = new ArrayList<>(interfaces);
        }
    }

    /**
     * Adds an interface to this class.
     * @param interfaceName the interface name
     */
    public void addInterface(String interfaceName) {
        if (!interfaces.contains(interfaceName)) {
            interfaces.add(interfaceName);
            if (classNode != null) {
                classNode.interfaces.add(interfaceName);
            }
        }
    }

    /**
     * Removes an interface from this class.
     * @param interfaceName the interface name
     */
    public void removeInterface(String interfaceName) {
        interfaces.remove(interfaceName);
        if (classNode != null) {
            classNode.interfaces.remove(interfaceName);
        }
    }

    /**
     * Sets the access flags for this class.
     * @param access the new access flags
     */
    public void setAccess(int access) {
        this.access = access;
        if (classNode != null) {
            classNode.access = access;
        }
    }

    /**
     * Sets the generic signature for this class.
     * @param signature the new signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
        if (classNode != null) {
            classNode.signature = signature;
        }
    }

    /**
     * Sets the source file name for this class.
     * @param sourceFile the new source file name
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
        if (classNode != null) {
            classNode.sourceFile = sourceFile;
        }
    }

    /**
     * Sets the source debug information for this class.
     * @param sourceDebug the new source debug info
     */
    public void setSourceDebug(String sourceDebug) {
        this.sourceDebug = sourceDebug;
        if (classNode != null) {
            classNode.sourceDebug = sourceDebug;
        }
    }

    /**
     * Sets the outer class name for this class.
     * @param outerClass the new outer class name
     */
    public void setOuterClass(String outerClass) {
        this.outerClass = outerClass;
        if (classNode != null) {
            classNode.outerClass = outerClass;
        }
    }

    /**
     * Sets the outer method name for this class.
     * @param outerMethod the new outer method name
     */
    public void setOuterMethod(String outerMethod) {
        this.outerMethod = outerMethod;
        if (classNode != null) {
            classNode.outerMethod = outerMethod;
        }
    }

    /**
     * Sets the outer method descriptor for this class.
     * @param outerMethodDesc the new outer method descriptor
     */
    public void setOuterMethodDesc(String outerMethodDesc) {
        this.outerMethodDesc = outerMethodDesc;
        if (classNode != null) {
            classNode.outerMethodDesc = outerMethodDesc;
        }
    }

    /**
     * Returns true if this class is an interface.
     * @return true if interface
     */
    public boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    /**
     * Returns true if this class is abstract.
     * @return true if abstract
     */
    public boolean isAbstract() {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    /**
     * Returns true if this class is final.
     * @return true if final
     */
    public boolean isFinal() {
        return (access & Opcodes.ACC_FINAL) != 0;
    }

    /**
     * Returns true if this class is public.
     * @return true if public
     */
    public boolean isPublic() {
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    /**
     * Returns true if this class is private.
     * @return true if private
     */
    public boolean isPrivate() {
        return (access & Opcodes.ACC_PRIVATE) != 0;
    }

    /**
     * Returns true if this class is protected.
     * @return true if protected
     */
    public boolean isProtected() {
        return (access & Opcodes.ACC_PROTECTED) != 0;
    }

    /**
     * Returns true if this class is static.
     * @return true if static
     */
    public boolean isStatic() {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    /**
     * Returns true if this class is an enum.
     * @return true if enum
     */
    public boolean isEnum() {
        return (access & Opcodes.ACC_ENUM) != 0;
    }

    /**
     * Returns true if this class is an annotation.
     * @return true if annotation
     */
    public boolean isAnnotation() {
        return (access & Opcodes.ACC_ANNOTATION) != 0;
    }

    /**
     * Gets the simple (unqualified) class name.
     * @return the simple class name
     */
    public String getSimpleName() {
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }

    /**
     * Gets the package name for this class.
     * @return the package name
     */
    public String getPackageName() {
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(0, lastSlash).replace('/', '.') : "";
    }

    /**
     * Sets the class file version.
     * @param classVersion the class file version
     */
    public void setClassVersion(int classVersion) {
        this.classVersion = classVersion;
        if (classNode != null) {
            classNode.version = classVersion;
        }
    }


    /** Direct subclasses / implementors that are ProgramClasses in this mapping. */
    public List<ProgramClass> getChildProgramClasses() {
        return Collections.unmodifiableList(childProgramClasses);
    }

    public void addChildProgramClass(ProgramClass child) {
        childProgramClasses.add(child);
    }

    /** Interfaces resolved to ProgramClass instances (external interfaces omitted). */
    public List<ProgramClass> getResolvedInterfaces() {
        return Collections.unmodifiableList(resolvedInterfaces);
    }

    public void addResolvedInterface(ProgramClass iface) {
        resolvedInterfaces.add(iface);
    }

    /** External supertypes that couldn't be resolved to a ProgramClass. */
    public Set<String> getUnresolvedSuperTypes() {
        return Collections.unmodifiableSet(unresolvedSuperTypes);
    }

    public void addUnresolvedSuperType(String externalType) {
        unresolvedSuperTypes.add(externalType);
    }

    /**
     * Collects every ProgramClass in this class's hierarchy — walking up through parents
     * and down through children — including {@code this}.
     */
    public Set<ProgramClass> getHierarchyClasses() {
        Set<ProgramClass> result = new LinkedHashSet<>();
        collectHierarchy(this, result);
        return result;
    }

    private static void collectHierarchy(ProgramClass current, Set<ProgramClass> visited) {
        if (current == null || !visited.add(current)) {
            return;
        }
        collectHierarchy(current.parentProgramClass, visited);
        for (ProgramClass iface : current.resolvedInterfaces) {
            collectHierarchy(iface, visited);
        }
        for (ProgramClass child : current.childProgramClasses) {
            collectHierarchy(child, visited);
        }
    }

    /**
     * Rebuilds this ProgramClass's wrapper state from its underlying {@link ClassNode}.
     * Called after ASM's {@link org.objectweb.asm.commons.ClassRemapper} produces
     * a remapped ClassNode — the wrapper fields, field map, and method map are re-synced.
     *
     * <p>Hierarchy links ({@link #parentProgramClass}, children, interfaces) are preserved
     * across sync because they are ProgramClass references, not name-based.</p>
     */
    public void syncFromClassNode() {
        if (classNode == null) {
            return;
        }
        this.name = classNode.name;
        this.superName = classNode.superName;
        this.interfaces = classNode.interfaces != null ? new ArrayList<>(classNode.interfaces) : new ArrayList<>();
        this.access = classNode.access;
        this.signature = classNode.signature;
        this.sourceFile = classNode.sourceFile;
        this.sourceDebug = classNode.sourceDebug;
        this.outerClass = classNode.outerClass;
        this.outerMethod = classNode.outerMethod;
        this.outerMethodDesc = classNode.outerMethodDesc;
        if (classNode.version > 0) {
            this.classVersion = classNode.version;
        }
        this.nestHostClass = classNode.nestHostClass;
        this.nestMembers = classNode.nestMembers != null ? new ArrayList<>(classNode.nestMembers) : new ArrayList<>();
        this.permittedSubclasses = classNode.permittedSubclasses != null ? new ArrayList<>(classNode.permittedSubclasses) : new ArrayList<>();
        this.recordComponents = classNode.recordComponents != null ? new ArrayList<>(classNode.recordComponents) : new ArrayList<>();

        // Rebuild field map
        fields.clear();
        if (classNode.fields != null) {
            for (FieldNode fn : classNode.fields) {
                ProgramField field = new ProgramField(fn);
                field.setOwner(this);
                fields.put(field.getName(), field);
            }
        }

        // Rebuild method map
        methods.clear();
        if (classNode.methods != null) {
            for (MethodNode mn : classNode.methods) {
                ProgramMethod method = new ProgramMethod(mn);
                method.setOwner(this);
                methods.put(method.getName() + method.getDescriptor(), method);
            }
        }
    }

    /**
     * Gets the list of record components (for records).
     * @return unmodifiable list of record components
     */
    public List<RecordComponentNode> getRecordComponents() {
        return Collections.unmodifiableList(recordComponents);
    }

    /**
     * Adds a record component.
     * @param rc the record component
     */
    public void addRecordComponent(RecordComponentNode rc) {
        recordComponents.add(rc);
        if (classNode != null) {
            if (classNode.recordComponents == null) {
                classNode.recordComponents = new ArrayList<>();
            }
            if (!classNode.recordComponents.contains(rc)) {
                classNode.recordComponents.add(rc);
            }
        }
    }

    /**
     * Sets the nest host class name.
     * @param nestHostClass the nest host class name
     */
    public void setNestHostClass(String nestHostClass) {
        this.nestHostClass = nestHostClass;
        if (classNode != null) {
            classNode.nestHostClass = nestHostClass;
        }
    }

    /**
     * Gets the list of nest member class names.
     * @return unmodifiable list of nest member class names
     */
    public List<String> getNestMembers() {
        return Collections.unmodifiableList(nestMembers);
    }

    /**
     * Adds a nest member class name.
     * @param member the nest member class name
     */
    public void addNestMember(String member) {
        if (member == null) {
            return;
        }
        if (!nestMembers.contains(member)) {
            nestMembers.add(member);
        }
        if (classNode != null) {
            if (classNode.nestMembers == null) {
                classNode.nestMembers = new ArrayList<>();
            }
            if (!classNode.nestMembers.contains(member)) {
                classNode.nestMembers.add(member);
            }
        }
    }

    /**
     * Gets the list of permitted subclass names (for sealed classes).
     * @return unmodifiable list of permitted subclass names
     */
    public List<String> getPermittedSubclasses() {
        return Collections.unmodifiableList(permittedSubclasses);
    }

    /**
     * Adds a permitted subclass name.
     * @param subclass the permitted subclass name
     */
    public void addPermittedSubclass(String subclass) {
        if (subclass == null) {
            return;
        }
        if (!permittedSubclasses.contains(subclass)) {
            permittedSubclasses.add(subclass);
        }
        if (classNode != null) {
            if (classNode.permittedSubclasses == null) {
                classNode.permittedSubclasses = new ArrayList<>();
            }
            if (!classNode.permittedSubclasses.contains(subclass)) {
                classNode.permittedSubclasses.add(subclass);
            }
        }
    }
}
