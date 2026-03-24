package io.github.cvs0.bytecode.attribute;

import io.github.cvs0.bytecode.member.LocalVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the LocalVariableTypeTable attribute, which stores generic type information for local variables.
 * Provides methods for managing and querying local variable types.
 */
public class LocalVariableTypeTableAttribute extends Attribute {
    private final List<LocalVariableType> localVariableTypes = new ArrayList<>();
    
    /**
     * Constructs a LocalVariableTypeTableAttribute.
     */
    public LocalVariableTypeTableAttribute() {
        super("LocalVariableTypeTable");
    }
    
    /**
     * Adds a local variable type entry to this attribute.
     * @param localVariableType the local variable type string
     */
    public void addLocalVariableType(LocalVariableType localVariableType) {
        localVariableTypes.add(localVariableType);
    }
    
    /**
     * Removes a local variable type entry from this attribute.
     * @param localVariableType the local variable type string
     */
    public void removeLocalVariableType(LocalVariableType localVariableType) {
        localVariableTypes.remove(localVariableType);
    }
    
    /**
     * Returns all local variable types in this attribute.
     * @return unmodifiable list of local variable types
     */
    public List<LocalVariableType> getLocalVariableTypes() {
        return Collections.unmodifiableList(localVariableTypes);
    }
    
    /**
     * Returns the number of local variable types in this attribute.
     * @return the local variable type count
     */
    public int getLocalVariableTypeCount() {
        return localVariableTypes.size();
    }
    
    /**
     * Removes all local variable types from this attribute.
     */
    public void clearLocalVariableTypes() {
        localVariableTypes.clear();
    }
    
    /**
     * Returns a string representation of this attribute.
     * @return a string with the number of local variable types
     */
    @Override
    public String toString() {
        return "LocalVariableTypeTableAttribute{" +
                "localVariableTypes=" + localVariableTypes.size() +
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
        LocalVariableTypeTableAttribute that = (LocalVariableTypeTableAttribute) o;
        return localVariableTypes.equals(that.localVariableTypes);
    }

    /**
     * Returns a hash code for this attribute.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + localVariableTypes.hashCode();
        return result;
    }

    /**
     * Represents type information for a local variable with generic signature.
     */
    public static class LocalVariableType {
        private String name;
        private String signature;
        private int startPc;
        private int length;
        private int index;
        
        public LocalVariableType(String name, String signature, int startPc, int length, int index) {
            this.name = name;
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
        
        public boolean isGeneric() {
            return signature != null && (signature.contains("<") || signature.contains("T"));
        }
        
        @Override
        public String toString() {
            return "LocalVariableType{" +
                    "name='" + name + '\'' +
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
            
            LocalVariableType that = (LocalVariableType) o;
            
            if (startPc != that.startPc) return false;
            if (length != that.length) return false;
            if (index != that.index) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            return signature != null ? signature.equals(that.signature) : that.signature == null;
        }
        
        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (signature != null ? signature.hashCode() : 0);
            result = 31 * result + startPc;
            result = 31 * result + length;
            result = 31 * result + index;
            return result;
        }
    }
}