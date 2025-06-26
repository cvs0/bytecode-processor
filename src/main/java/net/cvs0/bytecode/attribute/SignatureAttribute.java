package net.cvs0.bytecode.attribute;

public class SignatureAttribute extends Attribute {
    private String signature;
    
    public SignatureAttribute(String signature) {
        super("Signature");
        this.signature = signature;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public boolean isGeneric() {
        return signature != null && (signature.contains("<") || signature.contains("T"));
    }
    
    public boolean isClassSignature() {
        return signature != null && signature.contains(":");
    }
    
    public boolean isMethodSignature() {
        return signature != null && signature.contains("(");
    }
    
    public boolean isFieldSignature() {
        return signature != null && !signature.contains("(") && !signature.contains(":");
    }
    
    @Override
    public String toString() {
        return "SignatureAttribute{" +
                "signature='" + signature + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        SignatureAttribute that = (SignatureAttribute) o;
        
        return signature != null ? signature.equals(that.signature) : that.signature == null;
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        return result;
    }
}