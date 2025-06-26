package net.cvs0.bytecode.attribute;

import net.cvs0.bytecode.member.LocalVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the LocalVariableTypeTable attribute which provides signature
 * information for local variables that have generic types.
 */
public class LocalVariableTypeTableAttribute extends Attribute {
    private final List<LocalVariableType> localVariableTypes = new ArrayList<>();
    
    public LocalVariableTypeTableAttribute() {
        super("LocalVariableTypeTable");
    }
    
    public void addLocalVariableType(LocalVariableType localVariableType) {
        localVariableTypes.add(localVariableType);
    }
    
    public void removeLocalVariableType(LocalVariableType localVariableType) {
        localVariableTypes.remove(localVariableType);
    }
    
    public List<LocalVariableType> getLocalVariableTypes() {
        return Collections.unmodifiableList(localVariableTypes);
    }
    
    public LocalVariableType getLocalVariableTypeByIndex(int index) {
        return localVariableTypes.stream()
                .filter(lvt -> lvt.getIndex() == index)
                .findFirst()
                .orElse(null);
    }
    
    public List<LocalVariableType> getActiveVariableTypesAtPc(int pc) {
        return localVariableTypes.stream()
                .filter(lvt -> lvt.isActive(pc))
                .toList();
    }
    
    public LocalVariableType getVariableTypeByName(String name) {
        return localVariableTypes.stream()
                .filter(lvt -> name.equals(lvt.getName()))
                .findFirst()
                .orElse(null);
    }
    
    public int getLocalVariableTypeCount() {
        return localVariableTypes.size();
    }
    
    public void clearLocalVariableTypes() {
        localVariableTypes.clear();
    }
    
    public boolean hasVariableType(String name) {
        return localVariableTypes.stream().anyMatch(lvt -> name.equals(lvt.getName()));
    }
    
    public void sortLocalVariableTypes() {
        localVariableTypes.sort((a, b) -> {
            int indexCompare = Integer.compare(a.getIndex(), b.getIndex());
            if (indexCompare != 0) {
                return indexCompare;
            }
            return Integer.compare(a.getStartPc(), b.getStartPc());
        });
    }
    
    @Override
    public String toString() {
        return "LocalVariableTypeTableAttribute{" +
                "localVariableTypes=" + localVariableTypes.size() +
                '}';
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