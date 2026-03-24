package io.github.cvs0.bytecode.clazz;

import io.github.cvs0.bytecode.attribute.*;
import io.github.cvs0.bytecode.member.InnerClass;
import io.github.cvs0.bytecode.member.ProgramField;
import io.github.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a program (internal) class, including its name, superclass, interfaces, fields, methods, attributes, and inner classes.
 * Provides methods for querying and modifying class structure, metadata, and ASM ClassNode integration.
 */
public class ProgramClass {
    private String name;
    private String superName;
    private List<String> interfaces;
    private int access;
    private String signature;
    private String sourceFile;
    private String sourceDebug;
    private String outerClass;
    private String outerMethod;
    private String outerMethodDesc;
    
    private final Map<String, ProgramField> fields = new ConcurrentHashMap<>();
    private final Map<String, ProgramMethod> methods = new ConcurrentHashMap<>();
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<InnerClass> innerClasses = new ArrayList<>();
    
    private ClassNode classNode;

    private int classVersion;
    private List<RecordComponentNode> recordComponents = new ArrayList<>();
    private String nestHostClass;
    private List<String> nestMembers = new ArrayList<>();
    private List<String> permittedSubclasses = new ArrayList<>();

    /**
     * Constructs a ProgramClass with the given name.
     * @param name the class name
     */
    public ProgramClass(String name) {
        this.name = name;
        this.interfaces = new ArrayList<>();
    }

    /**
     * Constructs a ProgramClass from an ASM ClassNode.
     * @param classNode the ASM ClassNode
     */
    public ProgramClass(ClassNode classNode) {
        this.classNode = classNode;
        this.name = classNode.name;
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
     * Gets the class name.
     * @return the class name
     */
    public String getName() {
        return name;
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
     * Gets the superclass name.
     * @return the superclass name
     */
    public String getSuperName() {
        return superName;
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
     * Gets the access flags for this class.
     * @return the access flags
     */
    public int getAccess() {
        return access;
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
     * Gets the generic signature for this class.
     * @return the signature
     */
    public String getSignature() {
        return signature;
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
     * Gets the source file name for this class.
     * @return the source file name
     */
    public String getSourceFile() {
        return sourceFile;
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
     * Gets the source debug information for this class.
     * @return the source debug info
     */
    public String getSourceDebug() {
        return sourceDebug;
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
     * Gets the outer class name for this class.
     * @return the outer class name
     */
    public String getOuterClass() {
        return outerClass;
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
     * Gets the outer method name for this class.
     * @return the outer method name
     */
    public String getOuterMethod() {
        return outerMethod;
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
     * Gets the outer method descriptor for this class.
     * @return the outer method descriptor
     */
    public String getOuterMethodDesc() {
        return outerMethodDesc;
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
     * Gets the underlying ASM ClassNode for this class.
     * @return the ClassNode
     */
    public ClassNode getClassNode() {
        return classNode;
    }

    /**
     * Sets the underlying ASM ClassNode for this class.
     * @param classNode the new ClassNode
     */
    public void setClassNode(ClassNode classNode) {
        this.classNode = classNode;
    }

    /**
     * Returns true if this class is an interface.
     * @return true if interface
     */
    public boolean isInterface() {
        return (access & 0x0200) != 0;
    }

    /**
     * Returns true if this class is abstract.
     * @return true if abstract
     */
    public boolean isAbstract() {
        return (access & 0x0400) != 0;
    }

    /**
     * Returns true if this class is final.
     * @return true if final
     */
    public boolean isFinal() {
        return (access & 0x0010) != 0;
    }

    /**
     * Returns true if this class is public.
     * @return true if public
     */
    public boolean isPublic() {
        return (access & 0x0001) != 0;
    }

    /**
     * Returns true if this class is private.
     * @return true if private
     */
    public boolean isPrivate() {
        return (access & 0x0002) != 0;
    }

    /**
     * Returns true if this class is protected.
     * @return true if protected
     */
    public boolean isProtected() {
        return (access & 0x0004) != 0;
    }

    /**
     * Returns true if this class is static.
     * @return true if static
     */
    public boolean isStatic() {
        return (access & 0x0008) != 0;
    }

    /**
     * Returns true if this class is an enum.
     * @return true if enum
     */
    public boolean isEnum() {
        return (access & 0x4000) != 0;
    }

    /**
     * Returns true if this class is an annotation.
     * @return true if annotation
     */
    public boolean isAnnotation() {
        return (access & 0x2000) != 0;
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
     * Gets the class file version (major version).
     * @return the class file version
     */
    public int getClassVersion() {
        return classVersion;
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
     * Gets the nest host class name (if this class is a nest member).
     * @return the nest host class name, or null
     */
    public String getNestHostClass() {
        return nestHostClass;
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
