package net.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the MethodParameters attribute which stores information about
 * the formal parameters of a method.
 */
public class MethodParametersAttribute extends Attribute {
    private final List<Parameter> parameters = new ArrayList<>();
    
    public MethodParametersAttribute() {
        super("MethodParameters");
    }
    
    public void addParameter(Parameter parameter) {
        parameters.add(parameter);
    }
    
    public void removeParameter(Parameter parameter) {
        parameters.remove(parameter);
    }
    
    public List<Parameter> getParameters() {
        return Collections.unmodifiableList(parameters);
    }
    
    public Parameter getParameter(int index) {
        if (index >= 0 && index < parameters.size()) {
            return parameters.get(index);
        }
        return null;
    }
    
    public Parameter getParameterByName(String name) {
        return parameters.stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst()
                .orElse(null);
    }
    
    public int getParameterCount() {
        return parameters.size();
    }
    
    public void clearParameters() {
        parameters.clear();
    }
    
    public boolean hasParameter(String name) {
        return parameters.stream().anyMatch(p -> name.equals(p.getName()));
    }
    
    @Override
    public String toString() {
        return "MethodParametersAttribute{" +
                "parameters=" + parameters.size() +
                '}';
    }
    
    /**
     * Represents information about a single method parameter.
     */
    public static class Parameter {
        private String name;
        private int access;
        
        public Parameter(String name, int access) {
            this.name = name;
            this.access = access;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getAccess() {
            return access;
        }
        
        public void setAccess(int access) {
            this.access = access;
        }
        
        public boolean isFinal() {
            return (access & 0x0010) != 0;
        }
        
        public boolean isSynthetic() {
            return (access & 0x1000) != 0;
        }
        
        public boolean isMandated() {
            return (access & 0x8000) != 0;
        }
        
        public boolean isImplicit() {
            return isSynthetic() || isMandated();
        }
        
        @Override
        public String toString() {
            return "Parameter{" +
                    "name='" + name + '\'' +
                    ", access=0x" + Integer.toHexString(access) +
                    '}';
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            Parameter parameter = (Parameter) o;
            
            if (access != parameter.access) return false;
            return name != null ? name.equals(parameter.name) : parameter.name == null;
        }
        
        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + access;
            return result;
        }
    }
}