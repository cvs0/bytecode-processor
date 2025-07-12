package net.cvs0.bytecode.clazz;

import net.cvs0.bytecode.attribute.*;
import net.cvs0.bytecode.member.InnerClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    
    public ProgramClass(String name) {
        this.name = name;
        this.interfaces = new ArrayList<>();
    }
    
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
    }
    
    public void addField(ProgramField field) {
        fields.put(field.getName(), field);
        field.setOwner(this);
    }
    
    public void addMethod(ProgramMethod method) {
        String key = method.getName() + method.getDescriptor();
        methods.put(key, method);
        method.setOwner(this);
    }
    
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }
    
    public void addInnerClass(InnerClass innerClass) {
        innerClasses.add(innerClass);
    }
    
    public void removeInnerClass(InnerClass innerClass) {
        innerClasses.remove(innerClass);
    }
    
    public ProgramField getField(String name) {
        return fields.get(name);
    }
    
    public ProgramMethod getMethod(String name, String descriptor) {
        return methods.get(name + descriptor);
    }
    
    public Collection<ProgramField> getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }
    
    public Collection<ProgramMethod> getMethods() {
        return Collections.unmodifiableCollection(methods.values());
    }
    
    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }
    
    public List<InnerClass> getInnerClasses() {
        return Collections.unmodifiableList(innerClasses);
    }
    
    public void removeField(String name) {
        ProgramField field = fields.remove(name);
        if (field != null) {
            field.setOwner(null);
        }
    }
    
    public void removeMethod(String name, String descriptor) {
        ProgramMethod method = methods.remove(name + descriptor);
        if (method != null) {
            method.setOwner(null);
        }
    }
    
    public void renameField(String oldName, String newName) {
        ProgramField field = fields.remove(oldName);
        if (field != null) {
            field.setName(newName);
            fields.put(newName, field);
        }
    }
    
    public void renameMethod(String oldName, String descriptor, String newName) {
        ProgramMethod method = methods.remove(oldName + descriptor);
        if (method != null) {
            method.setName(newName);
            methods.put(newName + descriptor, method);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        if (classNode != null) {
            classNode.name = name;
        }
    }
    
    public String getSuperName() {
        return superName;
    }
    
    public void setSuperName(String superName) {
        this.superName = superName;
        if (classNode != null) {
            classNode.superName = superName;
        }
    }
    
    public List<String> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }
    
    public void setInterfaces(List<String> interfaces) {
        this.interfaces = new ArrayList<>(interfaces);
        if (classNode != null) {
            classNode.interfaces = new ArrayList<>(interfaces);
        }
    }
    
    public void addInterface(String interfaceName) {
        if (!interfaces.contains(interfaceName)) {
            interfaces.add(interfaceName);
            if (classNode != null) {
                classNode.interfaces.add(interfaceName);
            }
        }
    }
    
    public void removeInterface(String interfaceName) {
        interfaces.remove(interfaceName);
        if (classNode != null) {
            classNode.interfaces.remove(interfaceName);
        }
    }
    
    public int getAccess() {
        return access;
    }
    
    public void setAccess(int access) {
        this.access = access;
        if (classNode != null) {
            classNode.access = access;
        }
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
        if (classNode != null) {
            classNode.signature = signature;
        }
    }
    
    public String getSourceFile() {
        return sourceFile;
    }
    
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
        if (classNode != null) {
            classNode.sourceFile = sourceFile;
        }
    }
    
    public String getSourceDebug() {
        return sourceDebug;
    }
    
    public void setSourceDebug(String sourceDebug) {
        this.sourceDebug = sourceDebug;
        if (classNode != null) {
            classNode.sourceDebug = sourceDebug;
        }
    }
    
    public String getOuterClass() {
        return outerClass;
    }
    
    public void setOuterClass(String outerClass) {
        this.outerClass = outerClass;
        if (classNode != null) {
            classNode.outerClass = outerClass;
        }
    }
    
    public String getOuterMethod() {
        return outerMethod;
    }
    
    public void setOuterMethod(String outerMethod) {
        this.outerMethod = outerMethod;
        if (classNode != null) {
            classNode.outerMethod = outerMethod;
        }
    }
    
    public String getOuterMethodDesc() {
        return outerMethodDesc;
    }
    
    public void setOuterMethodDesc(String outerMethodDesc) {
        this.outerMethodDesc = outerMethodDesc;
        if (classNode != null) {
            classNode.outerMethodDesc = outerMethodDesc;
        }
    }
    
    public ClassNode getClassNode() {
        return classNode;
    }
    
    public void setClassNode(ClassNode classNode) {
        this.classNode = classNode;
    }
    
    public boolean isInterface() {
        return (access & 0x0200) != 0;
    }
    
    public boolean isAbstract() {
        return (access & 0x0400) != 0;
    }
    
    public boolean isFinal() {
        return (access & 0x0010) != 0;
    }
    
    public boolean isPublic() {
        return (access & 0x0001) != 0;
    }
    
    public boolean isPrivate() {
        return (access & 0x0002) != 0;
    }
    
    public boolean isProtected() {
        return (access & 0x0004) != 0;
    }
    
    public boolean isStatic() {
        return (access & 0x0008) != 0;
    }
    
    public boolean isEnum() {
        return (access & 0x4000) != 0;
    }
    
    public boolean isAnnotation() {
        return (access & 0x2000) != 0;
    }
    
    public String getSimpleName() {
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }
    
    public String getPackageName() {
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(0, lastSlash).replace('/', '.') : "";
    }
}