package net.cvs0.bytecode.attribute;

/**
 * Represents the SourceFile attribute which stores the name of the source file
 * from which the class was compiled.
 */
public class SourceFileAttribute extends Attribute {
    private String sourceFile;
    
    public SourceFileAttribute(String sourceFile) {
        super("SourceFile");
        this.sourceFile = sourceFile;
    }
    
    public String getSourceFile() {
        return sourceFile;
    }
    
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }
    
    @Override
    public String toString() {
        return "SourceFileAttribute{" +
                "sourceFile='" + sourceFile + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        SourceFileAttribute that = (SourceFileAttribute) o;
        return sourceFile != null ? sourceFile.equals(that.sourceFile) : that.sourceFile == null;
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sourceFile != null ? sourceFile.hashCode() : 0);
        return result;
    }
}