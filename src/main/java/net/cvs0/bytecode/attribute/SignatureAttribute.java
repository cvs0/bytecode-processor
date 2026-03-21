package net.cvs0.bytecode.attribute;

/**
 * Represents the Signature attribute, which stores generic type information for classes, methods, or fields.
 * Provides methods for accessing and classifying the signature.
 */
public class SignatureAttribute extends Attribute {
    private String signature;
    
    /**
     * Constructs a SignatureAttribute with the given signature string.
     * @param signature the generic signature
     */
    public SignatureAttribute(String signature) {
        super("Signature");
        this.signature = signature;
    }
    
    /**
     * Gets the generic signature string.
     * @return the signature
     */
    public String getSignature() {
        return signature;
    }
    
    /**
     * Sets the generic signature string.
     * @param signature the new signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    /**
     * Returns true if this signature represents a generic type.
     * @return true if generic
     */
    public boolean isGeneric() {
        return signature != null && (signature.contains("<") || signature.contains("T"));
    }
    
    /**
     * Returns true if this signature is a class signature.
     * @return true if class signature
     */
    public boolean isClassSignature() {
        return signature != null && signature.contains(":");
    }
    
    /**
     * Returns true if this signature is a method signature.
     * @return true if method signature
     */
    public boolean isMethodSignature() {
        return signature != null && signature.contains("(");
    }
    
    /**
     * Returns true if this signature is a field signature.
     * @return true if field signature
     */
    public boolean isFieldSignature() {
        return signature != null && !signature.contains("(") && !signature.contains(":");
    }
    
    /**
     * Returns a string representation of this attribute.
     * @return a string with the signature
     */
    @Override
    public String toString() {
        return "SignatureAttribute{" +
                "signature='" + signature + '\'' +
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
        
        SignatureAttribute that = (SignatureAttribute) o;
        
        return signature != null ? signature.equals(that.signature) : that.signature == null;
    }
    
    /**
     * Returns a hash code for this attribute.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        return result;
    }
}