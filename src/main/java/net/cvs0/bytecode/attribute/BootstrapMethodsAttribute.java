package net.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents the BootstrapMethods attribute which contains bootstrap method
 * specifiers referenced by invokedynamic instructions.
 */
public class BootstrapMethodsAttribute extends Attribute {
    private final List<BootstrapMethod> bootstrapMethods = new ArrayList<>();
    
    public BootstrapMethodsAttribute() {
        super("BootstrapMethods");
    }
    
    public void addBootstrapMethod(BootstrapMethod bootstrapMethod) {
        bootstrapMethods.add(bootstrapMethod);
    }
    
    public void removeBootstrapMethod(BootstrapMethod bootstrapMethod) {
        bootstrapMethods.remove(bootstrapMethod);
    }
    
    public List<BootstrapMethod> getBootstrapMethods() {
        return Collections.unmodifiableList(bootstrapMethods);
    }
    
    public BootstrapMethod getBootstrapMethod(int index) {
        if (index >= 0 && index < bootstrapMethods.size()) {
            return bootstrapMethods.get(index);
        }
        return null;
    }
    
    public int getBootstrapMethodCount() {
        return bootstrapMethods.size();
    }
    
    public void clearBootstrapMethods() {
        bootstrapMethods.clear();
    }
    
    public int indexOf(BootstrapMethod bootstrapMethod) {
        return bootstrapMethods.indexOf(bootstrapMethod);
    }
    
    @Override
    public String toString() {
        return "BootstrapMethodsAttribute{" +
                "bootstrapMethods=" + bootstrapMethods.size() +
                '}';
    }
    
    /**
     * Represents a single bootstrap method specification.
     */
    public static class BootstrapMethod {
        private String methodHandle;
        private final List<Object> arguments = new ArrayList<>();
        
        public BootstrapMethod(String methodHandle) {
            this.methodHandle = methodHandle;
        }
        
        public BootstrapMethod(String methodHandle, Object... arguments) {
            this.methodHandle = methodHandle;
            if (arguments != null) {
                this.arguments.addAll(Arrays.asList(arguments));
            }
        }
        
        public String getMethodHandle() {
            return methodHandle;
        }
        
        public void setMethodHandle(String methodHandle) {
            this.methodHandle = methodHandle;
        }
        
        public void addArgument(Object argument) {
            arguments.add(argument);
        }
        
        public void removeArgument(Object argument) {
            arguments.remove(argument);
        }
        
        public List<Object> getArguments() {
            return Collections.unmodifiableList(arguments);
        }
        
        public Object getArgument(int index) {
            if (index >= 0 && index < arguments.size()) {
                return arguments.get(index);
            }
            return null;
        }
        
        public int getArgumentCount() {
            return arguments.size();
        }
        
        public void clearArguments() {
            arguments.clear();
        }
        
        public boolean hasArguments() {
            return !arguments.isEmpty();
        }
        
        @Override
        public String toString() {
            return "BootstrapMethod{" +
                    "methodHandle='" + methodHandle + '\'' +
                    ", arguments=" + arguments +
                    '}';
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            BootstrapMethod that = (BootstrapMethod) o;
            
            if (methodHandle != null ? !methodHandle.equals(that.methodHandle) : that.methodHandle != null) return false;
            return arguments.equals(that.arguments);
        }
        
        @Override
        public int hashCode() {
            int result = methodHandle != null ? methodHandle.hashCode() : 0;
            result = 31 * result + arguments.hashCode();
            return result;
        }
    }
}