package net.cvs0.bytecode.member;

public class InnerClass {
    private String innerClass;
    private String outerClass;
    private String innerName;
    private int access;
    
    public InnerClass(String innerClass, String outerClass, String innerName, int access) {
        this.innerClass = innerClass;
        this.outerClass = outerClass;
        this.innerName = innerName;
        this.access = access;
    }
    
    public String getInnerClass() {
        return innerClass;
    }
    
    public void setInnerClass(String innerClass) {
        this.innerClass = innerClass;
    }
    
    public String getOuterClass() {
        return outerClass;
    }
    
    public void setOuterClass(String outerClass) {
        this.outerClass = outerClass;
    }
    
    public String getInnerName() {
        return innerName;
    }
    
    public void setInnerName(String innerName) {
        this.innerName = innerName;
    }
    
    public int getAccess() {
        return access;
    }
    
    public void setAccess(int access) {
        this.access = access;
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
    
    public boolean isFinal() {
        return (access & 0x0010) != 0;
    }
    
    public boolean isInterface() {
        return (access & 0x0200) != 0;
    }
    
    public boolean isAbstract() {
        return (access & 0x0400) != 0;
    }
    
    public boolean isSynthetic() {
        return (access & 0x1000) != 0;
    }
    
    public boolean isAnnotation() {
        return (access & 0x2000) != 0;
    }
    
    public boolean isEnum() {
        return (access & 0x4000) != 0;
    }
    
    public boolean isAnonymous() {
        return innerName == null;
    }
    
    public boolean isLocal() {
        return outerClass == null && innerName != null;
    }
    
    public boolean isMember() {
        return outerClass != null && innerName != null;
    }
    
    @Override
    public String toString() {
        return "InnerClass{" +
                "innerClass='" + innerClass + '\'' +
                ", outerClass='" + outerClass + '\'' +
                ", innerName='" + innerName + '\'' +
                ", access=" + access +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        InnerClass that = (InnerClass) o;
        
        if (access != that.access) return false;
        if (innerClass != null ? !innerClass.equals(that.innerClass) : that.innerClass != null) return false;
        if (outerClass != null ? !outerClass.equals(that.outerClass) : that.outerClass != null) return false;
        return innerName != null ? innerName.equals(that.innerName) : that.innerName == null;
    }
    
    @Override
    public int hashCode() {
        int result = innerClass != null ? innerClass.hashCode() : 0;
        result = 31 * result + (outerClass != null ? outerClass.hashCode() : 0);
        result = 31 * result + (innerName != null ? innerName.hashCode() : 0);
        result = 31 * result + access;
        return result;
    }
}