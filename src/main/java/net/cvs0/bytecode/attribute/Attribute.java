package net.cvs0.bytecode.attribute;

import java.util.HashMap;
import java.util.Map;

public class Attribute {
    private String name;
    private byte[] data;
    private Map<String, Object> properties;
    
    public Attribute(String name) {
        this.name = name;
        this.properties = new HashMap<>();
    }
    
    public Attribute(String name, byte[] data) {
        this.name = name;
        this.data = data != null ? data.clone() : null;
        this.properties = new HashMap<>();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public byte[] getData() {
        return data != null ? data.clone() : null;
    }
    
    public void setData(byte[] data) {
        this.data = data != null ? data.clone() : null;
    }
    
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    public void removeProperty(String key) {
        properties.remove(key);
    }
    
    public boolean isAnnotation() {
        return "RuntimeVisibleAnnotations".equals(name) || 
               "RuntimeInvisibleAnnotations".equals(name) ||
               "RuntimeVisibleParameterAnnotations".equals(name) ||
               "RuntimeInvisibleParameterAnnotations".equals(name);
    }
    
    public boolean isSignature() {
        return "Signature".equals(name);
    }
    
    public boolean isSourceFile() {
        return "SourceFile".equals(name);
    }
    
    public boolean isSourceDebug() {
        return "SourceDebugExtension".equals(name);
    }
    
    public boolean isLineNumberTable() {
        return "LineNumberTable".equals(name);
    }
    
    public boolean isLocalVariableTable() {
        return "LocalVariableTable".equals(name);
    }
    
    public boolean isLocalVariableTypeTable() {
        return "LocalVariableTypeTable".equals(name);
    }
    
    public boolean isCode() {
        return "Code".equals(name);
    }
    
    public boolean isExceptions() {
        return "Exceptions".equals(name);
    }
    
    public boolean isInnerClasses() {
        return "InnerClasses".equals(name);
    }
    
    public boolean isEnclosingMethod() {
        return "EnclosingMethod".equals(name);
    }
    
    public boolean isSynthetic() {
        return "Synthetic".equals(name);
    }
    
    public boolean isDeprecated() {
        return "Deprecated".equals(name);
    }
    
    public boolean isBootstrapMethods() {
        return "BootstrapMethods".equals(name);
    }
    
    public boolean isMethodParameters() {
        return "MethodParameters".equals(name);
    }
    
    @Override
    public String toString() {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", dataLength=" + (data != null ? data.length : 0) +
                ", properties=" + properties.size() +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Attribute attribute = (Attribute) o;
        
        if (!name.equals(attribute.name)) return false;
        return properties.equals(attribute.properties);
    }
    
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + properties.hashCode();
        return result;
    }
}