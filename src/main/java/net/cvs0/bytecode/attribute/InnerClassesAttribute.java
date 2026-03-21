package net.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the InnerClasses attribute, which stores information about inner classes referenced by a class.
 * Provides methods for managing and querying inner class entries.
 */
public class InnerClassesAttribute extends Attribute {
    private final List<InnerClass> innerClasses = new ArrayList<>();
    
    /**
     * Constructs an InnerClassesAttribute.
     */
    public InnerClassesAttribute() {
        super("InnerClasses");
    }
    
    /**
     * Adds an inner class entry to this attribute.
     * @param innerClass the inner class entry
     */
    public void addInnerClass(InnerClass innerClass) {
        innerClasses.add(innerClass);
    }
    
    /**
     * Removes an inner class entry from this attribute.
     * @param innerClass the inner class entry
     */
    public void removeInnerClass(InnerClass innerClass) {
        innerClasses.remove(innerClass);
    }
    
    /**
     * Returns all inner class entries in this attribute.
     * @return unmodifiable list of inner class entries
     */
    public List<InnerClass> getInnerClasses() {
        return Collections.unmodifiableList(innerClasses);
    }
    
    /**
     * Returns the number of inner class entries in this attribute.
     * @return the inner class count
     */
    public int getInnerClassCount() {
        return innerClasses.size();
    }
    
    /**
     * Removes all inner class entries from this attribute.
     */
    public void clearInnerClasses() {
        innerClasses.clear();
    }
    
    /**
     * Returns a string representation of this attribute.
     * @return a string with the number of inner classes
     */
    @Override
    public String toString() {
        return "InnerClassesAttribute{" +
                "innerClasses=" + innerClasses.size() +
                '}';
    }
    
    /**
     * Checks equality with another object.
     * @param o the other object
     * @return true if equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InnerClassesAttribute that = (InnerClassesAttribute) o;
        return innerClasses.equals(that.innerClasses);
    }

    /**
     * Returns a hash code for this attribute.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + innerClasses.hashCode();
        return result;
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