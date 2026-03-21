package net.cvs0.bytecode.attribute;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a generic bytecode attribute, including its name, raw data, and arbitrary properties.
 * Provides methods for querying and modifying attribute metadata, as well as type checks for standard attributes.
 */
public class Attribute {
    private String name;
    private byte[] data;
    private Map<String, Object> properties;
    
    /**
     * Constructs an Attribute with the given name.
     * @param name the attribute name
     */
    public Attribute(String name) {
        this.name = name;
        this.properties = new HashMap<>();
    }

    /**
     * Constructs an Attribute with the given name and raw data.
     * @param name the attribute name
     * @param data the raw attribute data
     */
    public Attribute(String name, byte[] data) {
        this.name = name;
        this.data = data != null ? data.clone() : null;
        this.properties = new HashMap<>();
    }

    /**
     * Gets the attribute name.
     * @return the attribute name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the attribute name.
     * @param name the new attribute name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the raw data for this attribute.
     * @return a copy of the data, or null
     */
    public byte[] getData() {
        return data != null ? data.clone() : null;
    }

    /**
     * Sets the raw data for this attribute.
     * @param data the new data
     */
    public void setData(byte[] data) {
        this.data = data != null ? data.clone() : null;
    }

    /**
     * Gets a copy of the properties map for this attribute.
     * @return a copy of the properties map
     */
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    /**
     * Sets a property for this attribute.
     * @param key the property key
     * @param value the property value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Gets a property value by key.
     * @param key the property key
     * @return the property value, or null
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Returns true if this attribute has a property with the given key.
     * @param key the property key
     * @return true if present
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * Removes a property by key.
     * @param key the property key
     */
    public void removeProperty(String key) {
        properties.remove(key);
    }

    /**
     * Returns true if this attribute is an annotation attribute.
     * @return true if annotation
     */
    public boolean isAnnotation() {
        return "RuntimeVisibleAnnotations".equals(name) || 
               "RuntimeInvisibleAnnotations".equals(name) ||
               "RuntimeVisibleParameterAnnotations".equals(name) ||
               "RuntimeInvisibleParameterAnnotations".equals(name);
    }

    /**
     * Returns true if this attribute is a signature attribute.
     * @return true if signature
     */
    public boolean isSignature() {
        return "Signature".equals(name);
    }

    /**
     * Returns true if this attribute is a SourceFile attribute.
     * @return true if SourceFile
     */
    public boolean isSourceFile() {
        return "SourceFile".equals(name);
    }

    /**
     * Returns true if this attribute is a SourceDebugExtension attribute.
     * @return true if SourceDebugExtension
     */
    public boolean isSourceDebug() {
        return "SourceDebugExtension".equals(name);
    }

    /**
     * Returns true if this attribute is a LineNumberTable attribute.
     * @return true if LineNumberTable
     */
    public boolean isLineNumberTable() {
        return "LineNumberTable".equals(name);
    }

    /**
     * Returns true if this attribute is a LocalVariableTable attribute.
     * @return true if LocalVariableTable
     */
    public boolean isLocalVariableTable() {
        return "LocalVariableTable".equals(name);
    }

    /**
     * Returns true if this attribute is a LocalVariableTypeTable attribute.
     * @return true if LocalVariableTypeTable
     */
    public boolean isLocalVariableTypeTable() {
        return "LocalVariableTypeTable".equals(name);
    }

    /**
     * Returns true if this attribute is a Code attribute.
     * @return true if Code
     */
    public boolean isCode() {
        return "Code".equals(name);
    }

    /**
     * Returns true if this attribute is an Exceptions attribute.
     * @return true if Exceptions
     */
    public boolean isExceptions() {
        return "Exceptions".equals(name);
    }

    /**
     * Returns true if this attribute is an InnerClasses attribute.
     * @return true if InnerClasses
     */
    public boolean isInnerClasses() {
        return "InnerClasses".equals(name);
    }

    /**
     * Returns true if this attribute is an EnclosingMethod attribute.
     * @return true if EnclosingMethod
     */
    public boolean isEnclosingMethod() {
        return "EnclosingMethod".equals(name);
    }

    /**
     * Returns true if this attribute is a Synthetic attribute.
     * @return true if Synthetic
     */
    public boolean isSynthetic() {
        return "Synthetic".equals(name);
    }

    /**
     * Returns true if this attribute is a Deprecated attribute.
     * @return true if Deprecated
     */
    public boolean isDeprecated() {
        return "Deprecated".equals(name);
    }

    /**
     * Returns true if this attribute is a BootstrapMethods attribute.
     * @return true if BootstrapMethods
     */
    public boolean isBootstrapMethods() {
        return "BootstrapMethods".equals(name);
    }

    /**
     * Returns true if this attribute is a MethodParameters attribute.
     * @return true if MethodParameters
     */
    public boolean isMethodParameters() {
        return "MethodParameters".equals(name);
    }

    /**
     * Returns a string representation of this attribute.
     * @return a string with name, data length, and property count
     */
    @Override
    public String toString() {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", dataLength=" + (data != null ? data.length : 0) +
                ", properties=" + properties.size() +
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
        
        Attribute attribute = (Attribute) o;
        
        if (!name.equals(attribute.name)) return false;
        return properties.equals(attribute.properties);
    }

    /**
     * Returns a hash code for this attribute.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + properties.hashCode();
        return result;
    }
}