package net.cvs0.bytecode.member;

/**
 * Represents an inner class entry, including its name, outer class, and access flags.
 * Provides utility methods for querying class properties and access modifiers.
 */
public class InnerClass {
    private String innerClass;
    private String outerClass;
    private String innerName;
    private int access;
    /**
     * Constructs an InnerClass with the given names and access flags.
     * @param innerClass the internal name of the inner class
     * @param outerClass the internal name of the outer class
     * @param innerName the simple name of the inner class
     * @param access the access flags
     */
    public InnerClass(String innerClass, String outerClass, String innerName, int access) {
        this.innerClass = innerClass;
        this.outerClass = outerClass;
        this.innerName = innerName;
        this.access = access;
    }

    /**
     * Returns the internal name of the inner class.
     * @return the inner class name
     */
    public String getInnerClass() {
        return innerClass;
    }

    /**
     * Sets the internal name of the inner class.
     * @param innerClass the new inner class name
     */
    public void setInnerClass(String innerClass) {
        this.innerClass = innerClass;
    }

    /**
     * Returns the internal name of the outer class.
     * @return the outer class name
     */
    public String getOuterClass() {
        return outerClass;
    }

    /**
     * Sets the internal name of the outer class.
     * @param outerClass the new outer class name
     */
    public void setOuterClass(String outerClass) {
        this.outerClass = outerClass;
    }

    /**
     * Returns the simple name of the inner class.
     * @return the inner class simple name
     */
    public String getInnerName() {
        return innerName;
    }

    /**
     * Sets the simple name of the inner class.
     * @param innerName the new inner class simple name
     */
    public void setInnerName(String innerName) {
        this.innerName = innerName;
    }

    /**
     * Returns the access flags for this inner class.
     * @return the access flags
     */
    public int getAccess() {
        return access;
    }

    /**
     * Sets the access flags for this inner class.
     * @param access the new access flags
     */
    public void setAccess(int access) {
        this.access = access;
    }

    /**
     * Returns true if the inner class is public.
     */
    public boolean isPublic() {
        return (access & 0x0001) != 0;
    }

    /**
     * Returns true if the inner class is private.
     */
    public boolean isPrivate() {
        return (access & 0x0002) != 0;
    }

    /**
     * Returns true if the inner class is protected.
     */
    public boolean isProtected() {
        return (access & 0x0004) != 0;
    }

    /**
     * Returns true if the inner class is static.
     */
    public boolean isStatic() {
        return (access & 0x0008) != 0;
    }

    /**
     * Returns true if the inner class is final.
     */
    public boolean isFinal() {
        return (access & 0x0010) != 0;
    }

    /**
     * Returns true if the inner class is an interface.
     */
    public boolean isInterface() {
        return (access & 0x0200) != 0;
    }

    /**
     * Returns true if the inner class is abstract.
     */
    public boolean isAbstract() {
        return (access & 0x0400) != 0;
    }

    /**
     * Returns true if the inner class is synthetic.
     */
    public boolean isSynthetic() {
        return (access & 0x1000) != 0;
    }

    /**
     * Returns true if the inner class is an annotation.
     */
    public boolean isAnnotation() {
        return (access & 0x2000) != 0;
    }

    /**
     * Returns true if the inner class is an enum.
     */
    public boolean isEnum() {
        return (access & 0x4000) != 0;
    }

    /**
     * Returns true if the inner class is anonymous.
     */
    public boolean isAnonymous() {
        return innerName == null;
    }

    /**
     * Returns true if the inner class is a local class.
     */
    public boolean isLocal() {
        return outerClass == null && innerName != null;
    }

    /**
     * Returns true if the inner class is a member class.
     */
    public boolean isMember() {
        return outerClass != null && innerName != null;
    }

    /**
     * Returns a string representation of this inner class.
     */
    @Override
    public String toString() {
        return "InnerClass{" +
                "innerClass='" + innerClass + '\'' +
                ", outerClass='" + outerClass + '\'' +
                ", innerName='" + innerName + '\'' +
                ", access=" + access +
                '}';
    }

    /**
     * Checks equality with another object.
     */
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

    /**
     * Returns a hash code for this inner class.
     */
    @Override
    public int hashCode() {
        int result = innerClass != null ? innerClass.hashCode() : 0;
        result = 31 * result + (outerClass != null ? outerClass.hashCode() : 0);
        result = 31 * result + (innerName != null ? innerName.hashCode() : 0);
        result = 31 * result + access;
        return result;
    }
}