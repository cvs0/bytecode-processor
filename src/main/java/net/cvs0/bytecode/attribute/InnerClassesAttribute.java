package net.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the InnerClasses attribute which contains information about
 * inner classes referenced by the class.
 */
public class InnerClassesAttribute extends Attribute {
    private final List<InnerClass> innerClasses = new ArrayList<>();
    
    public InnerClassesAttribute() {
        super("InnerClasses");
    }
    
    public void addInnerClass(InnerClass innerClass) {
        innerClasses.add(innerClass);
    }
    
    public void removeInnerClass(InnerClass innerClass) {
        innerClasses.remove(innerClass);
    }
    
    public List<InnerClass> getInnerClasses() {
        return Collections.unmodifiableList(innerClasses);
    }
    
    public InnerClass getInnerClass(String innerClassName) {
        return innerClasses.stream()
                .filter(ic -> innerClassName.equals(ic.getInnerClassName()))
                .findFirst()
                .orElse(null);
    }
    
    public boolean hasInnerClass(String innerClassName) {
        return innerClasses.stream()
                .anyMatch(ic -> innerClassName.equals(ic.getInnerClassName()));
    }
    
    public int getInnerClassCount() {
        return innerClasses.size();
    }
    
    public void clearInnerClasses() {
        innerClasses.clear();
    }
    
    @Override
    public String toString() {
        return "InnerClassesAttribute{" +
                "innerClasses=" + innerClasses.size() +
                '}';
    }
    
    /**
     * Represents information about a single inner class.
     */
    public static class InnerClass {
        private String innerClassName;
        private String outerClassName;
        private String innerName;
        private int access;
        
        public InnerClass(String innerClassName, String outerClassName, String innerName, int access) {
            this.innerClassName = innerClassName;
            this.outerClassName = outerClassName;
            this.innerName = innerName;
            this.access = access;
        }
        
        public String getInnerClassName() {
            return innerClassName;
        }
        
        public void setInnerClassName(String innerClassName) {
            this.innerClassName = innerClassName;
        }
        
        public String getOuterClassName() {
            return outerClassName;
        }
        
        public void setOuterClassName(String outerClassName) {
            this.outerClassName = outerClassName;
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
        
        public boolean isLocalClass() {
            return outerClassName == null && innerName != null;
        }
        
        public boolean isMemberClass() {
            return outerClassName != null && innerName != null;
        }
        
        @Override
        public String toString() {
            return "InnerClass{" +
                    "innerClassName='" + innerClassName + '\'' +
                    ", outerClassName='" + outerClassName + '\'' +
                    ", innerName='" + innerName + '\'' +
                    ", access=0x" + Integer.toHexString(access) +
                    '}';
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            InnerClass that = (InnerClass) o;
            
            if (access != that.access) return false;
            if (innerClassName != null ? !innerClassName.equals(that.innerClassName) : that.innerClassName != null) return false;
            if (outerClassName != null ? !outerClassName.equals(that.outerClassName) : that.outerClassName != null) return false;
            return innerName != null ? innerName.equals(that.innerName) : that.innerName == null;
        }
        
        @Override
        public int hashCode() {
            int result = innerClassName != null ? innerClassName.hashCode() : 0;
            result = 31 * result + (outerClassName != null ? outerClassName.hashCode() : 0);
            result = 31 * result + (innerName != null ? innerName.hashCode() : 0);
            result = 31 * result + access;
            return result;
        }
    }
}