package io.github.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents the BootstrapMethods attribute which contains bootstrap method specifiers referenced by invokedynamic instructions.
 * Provides methods for managing bootstrap methods and their arguments.
 */
public class BootstrapMethodsAttribute extends Attribute {
    private final List<BootstrapMethod> bootstrapMethods = new ArrayList<>();
    
    /**
     * Constructs a BootstrapMethodsAttribute.
     */
    public BootstrapMethodsAttribute() {
        super("BootstrapMethods");
    }
    
    /**
     * Adds a bootstrap method to this attribute.
     * @param bootstrapMethod the BootstrapMethod to add
     */
    public void addBootstrapMethod(BootstrapMethod bootstrapMethod) {
        bootstrapMethods.add(bootstrapMethod);
    }
    
    /**
     * Removes a bootstrap method from this attribute.
     * @param bootstrapMethod the BootstrapMethod to remove
     */
    public void removeBootstrapMethod(BootstrapMethod bootstrapMethod) {
        bootstrapMethods.remove(bootstrapMethod);
    }
    
    /**
     * Returns all bootstrap methods in this attribute.
     * @return unmodifiable list of bootstrap methods
     */
    public List<BootstrapMethod> getBootstrapMethods() {
        return Collections.unmodifiableList(bootstrapMethods);
    }
    
    /**
     * Gets a bootstrap method by index.
     * @param index the index
     * @return the BootstrapMethod or null
     */
    public BootstrapMethod getBootstrapMethod(int index) {
        if (index >= 0 && index < bootstrapMethods.size()) {
            return bootstrapMethods.get(index);
        }
        return null;
    }
    
    /**
     * Returns the number of bootstrap methods in this attribute.
     * @return the bootstrap method count
     */
    public int getBootstrapMethodCount() {
        return bootstrapMethods.size();
    }
    
    /**
     * Removes all bootstrap methods from this attribute.
     */
    public void clearBootstrapMethods() {
        bootstrapMethods.clear();
    }
    
    /**
     * Returns the index of a bootstrap method in this attribute.
     * @param bootstrapMethod the BootstrapMethod
     * @return the index, or -1 if not found
     */
    public int indexOf(BootstrapMethod bootstrapMethod) {
        return bootstrapMethods.indexOf(bootstrapMethod);
    }
    
    /**
     * Returns a string representation of this attribute.
     * @return a string with the number of bootstrap methods
     */
    @Override
    public String toString() {
        return "BootstrapMethodsAttribute{" +
                "bootstrapMethods=" + bootstrapMethods.size() +
                '}';
    }
    
    /**
     * Represents a single bootstrap method specification, including its method handle and arguments.
     * Provides methods for managing arguments and querying metadata.
     */
    public static class BootstrapMethod {
        private String methodHandle;
        private final List<Object> arguments = new ArrayList<>();
        
        /**
         * Constructs a BootstrapMethod with the given method handle.
         * @param methodHandle the method handle
         */
        public BootstrapMethod(String methodHandle) {
            this.methodHandle = methodHandle;
        }
        
        /**
         * Constructs a BootstrapMethod with the given method handle and arguments.
         * @param methodHandle the method handle
         * @param arguments the arguments
         */
        public BootstrapMethod(String methodHandle, Object... arguments) {
            this.methodHandle = methodHandle;
            if (arguments != null) {
                this.arguments.addAll(Arrays.asList(arguments));
            }
        }
        
        /**
         * Gets the method handle for this bootstrap method.
         * @return the method handle
         */
        public String getMethodHandle() {
            return methodHandle;
        }
        
        /**
         * Sets the method handle for this bootstrap method.
         * @param methodHandle the new method handle
         */
        public void setMethodHandle(String methodHandle) {
            this.methodHandle = methodHandle;
        }
        
        /**
         * Adds an argument to this bootstrap method.
         * @param argument the argument to add
         */
        public void addArgument(Object argument) {
            arguments.add(argument);
        }
        
        /**
         * Removes an argument from this bootstrap method.
         * @param argument the argument to remove
         */
        public void removeArgument(Object argument) {
            arguments.remove(argument);
        }
        
        /**
         * Returns all arguments for this bootstrap method.
         * @return unmodifiable list of arguments
         */
        public List<Object> getArguments() {
            return Collections.unmodifiableList(arguments);
        }
        
        /**
         * Gets an argument by index.
         * @param index the index
         * @return the argument or null
         */
        public Object getArgument(int index) {
            if (index >= 0 && index < arguments.size()) {
                return arguments.get(index);
            }
            return null;
        }
        
        /**
         * Returns the number of arguments for this bootstrap method.
         * @return the argument count
         */
        public int getArgumentCount() {
            return arguments.size();
        }
        
        /**
         * Removes all arguments from this bootstrap method.
         */
        public void clearArguments() {
            arguments.clear();
        }
        
        /**
         * Returns true if this bootstrap method has any arguments.
         * @return true if arguments exist
         */
        public boolean hasArguments() {
            return !arguments.isEmpty();
        }
        
        /**
         * Returns a string representation of this bootstrap method.
         * @return a string with method handle and arguments
         */
        @Override
        public String toString() {
            return "BootstrapMethod{" +
                    "methodHandle='" + methodHandle + '\'' +
                    ", arguments=" + arguments +
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
            
            BootstrapMethod that = (BootstrapMethod) o;
            
            if (methodHandle != null ? !methodHandle.equals(that.methodHandle) : that.methodHandle != null) return false;
            return arguments.equals(that.arguments);
        }
        
        /**
         * Returns a hash code for this bootstrap method.
         * @return the hash code
         */
        @Override
        public int hashCode() {
            int result = methodHandle != null ? methodHandle.hashCode() : 0;
            result = 31 * result + arguments.hashCode();
            return result;
        }
    }
}