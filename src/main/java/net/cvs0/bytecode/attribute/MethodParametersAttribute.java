package net.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the MethodParameters attribute, which stores information about method parameters.
 * Provides methods for managing and querying parameter entries.
 */
public class MethodParametersAttribute extends Attribute {
    private final List<Parameter> parameters = new ArrayList<>();
    
    /**
     * Constructs a MethodParametersAttribute.
     */
    public MethodParametersAttribute() {
        super("MethodParameters");
    }
    
    /**
     * Adds a parameter entry to this attribute.
     * @param parameter the parameter entry
     */
    public void addParameter(Parameter parameter) {
        parameters.add(parameter);
    }
    
    /**
     * Removes a parameter entry from this attribute.
     * @param parameter the parameter entry
     */
    public void removeParameter(Parameter parameter) {
        parameters.remove(parameter);
    }
    
    /**
     * Returns all parameter entries in this attribute.
     * @return unmodifiable list of parameters
     */
    public List<Parameter> getParameters() {
        return Collections.unmodifiableList(parameters);
    }
    
    /**
     * Returns the number of parameter entries in this attribute.
     * @return the parameter count
     */
    public int getParameterCount() {
        return parameters.size();
    }
    
    /**
     * Removes all parameter entries from this attribute.
     */
    public void clearParameters() {
        parameters.clear();
    }
    
    /**
     * Returns a string representation of this attribute.
     * @return a string with the number of parameters
     */
    @Override
    public String toString() {
        return "MethodParametersAttribute{" +
                "parameters=" + parameters.size() +
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
        MethodParametersAttribute that = (MethodParametersAttribute) o;
        return parameters.equals(that.parameters);
    }

    /**
     * Returns a hash code for this attribute.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + parameters.hashCode();
        return result;
    }

    /**
     * Represents a single method parameter entry, including its name and access flags.
     * Provides methods for accessing and modifying parameter metadata.
     */
    public static class Parameter {
        private String name;
        private int access;
        
        /**
         * Constructs a Parameter with the given name and access flags.
         * @param name the parameter name
         * @param access the access flags
         */
        public Parameter(String name, int access) {
            this.name = name;
            this.access = access;
        }
        
        /**
         * Gets the parameter name.
         * @return the parameter name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Sets the parameter name.
         * @param name the new parameter name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Gets the access flags for this parameter.
         * @return the access flags
         */
        public int getAccess() {
            return access;
        }
        
        /**
         * Sets the access flags for this parameter.
         * @param access the new access flags
         */
        public void setAccess(int access) {
            this.access = access;
        }
        
        /**
         * Returns a string representation of this parameter.
         * @return a string with the name and access flags
         */
        @Override
        public String toString() {
            return "Parameter{" +
                    "name='" + name + '\'' +
                    ", access=" + access +
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
            Parameter that = (Parameter) o;
            if (access != that.access) return false;
            return name != null ? name.equals(that.name) : that.name == null;
        }
        
        /**
         * Returns a hash code for this parameter.
         * @return the hash code
         */
        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + access;
            return result;
        }
    }
}