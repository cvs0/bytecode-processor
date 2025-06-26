package net.cvs0.bytecode.member;

public class LocalVariable {
    private String name;
    private String descriptor;
    private String signature;
    private int startPc;
    private int length;
    private int index;
    
    public LocalVariable(String name, String descriptor, int startPc, int length, int index) {
        this.name = name;
        this.descriptor = descriptor;
        this.startPc = startPc;
        this.length = length;
        this.index = index;
    }
    
    public LocalVariable(String name, String descriptor, String signature, int startPc, int length, int index) {
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.startPc = startPc;
        this.length = length;
        this.index = index;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescriptor() {
        return descriptor;
    }
    
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public int getStartPc() {
        return startPc;
    }
    
    public void setStartPc(int startPc) {
        this.startPc = startPc;
    }
    
    public int getLength() {
        return length;
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
    public int getIndex() {
        return index;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    
    public int getEndPc() {
        return startPc + length;
    }
    
    public boolean isActive(int pc) {
        return pc >= startPc && pc < getEndPc();
    }
    
    public boolean hasSignature() {
        return signature != null;
    }
    
    public String getType() {
        return descriptor;
    }
    
    public boolean isPrimitive() {
        return descriptor != null && descriptor.length() == 1 && !"L[".contains(descriptor);
    }
    
    public boolean isObject() {
        return descriptor != null && descriptor.startsWith("L");
    }
    
    public boolean isArray() {
        return descriptor != null && descriptor.startsWith("[");
    }
    
    @Override
    public String toString() {
        return "LocalVariable{" +
                "name='" + name + '\'' +
                ", descriptor='" + descriptor + '\'' +
                ", signature='" + signature + '\'' +
                ", startPc=" + startPc +
                ", length=" + length +
                ", index=" + index +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LocalVariable that = (LocalVariable) o;
        
        if (startPc != that.startPc) return false;
        if (length != that.length) return false;
        if (index != that.index) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (descriptor != null ? !descriptor.equals(that.descriptor) : that.descriptor != null) return false;
        return signature != null ? signature.equals(that.signature) : that.signature == null;
    }
    
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (descriptor != null ? descriptor.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        result = 31 * result + startPc;
        result = 31 * result + length;
        result = 31 * result + index;
        return result;
    }
}