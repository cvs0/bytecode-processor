package io.github.cvs0.bytecode.attribute;

/**
 * Represents the SourceFile attribute which stores the name of the source file
 * from which the class was compiled.
 * Provides methods for accessing and modifying the source file name.
 */
public class SourceFileAttribute extends Attribute {
    private String sourceFile;
    
    /**
     * Constructs a SourceFileAttribute with the given source file name.
     * @param sourceFile the source file name
     */
    public SourceFileAttribute(String sourceFile) {
        super("SourceFile");
        this.sourceFile = sourceFile;
    }
    
    /**
     * Gets the source file name.
     * @return the source file name
     */
    public String getSourceFile() {
        return sourceFile;
    }
    
    /**
     * Sets the source file name.
     * @param sourceFile the new source file name
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }
    
    /**
     * Returns a string representation of this attribute.
     * @return a string with the source file name
     */
    @Override
    public String toString() {
        return "SourceFileAttribute{" +
                "sourceFile='" + sourceFile + '\'' +
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
        
        SourceFileAttribute that = (SourceFileAttribute) o;
        return sourceFile != null ? sourceFile.equals(that.sourceFile) : that.sourceFile == null;
    }
    
    /**
     * Returns a hash code for this attribute.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sourceFile != null ? sourceFile.hashCode() : 0);
        return result;
    }
}