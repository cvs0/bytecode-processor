package io.github.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the Code attribute, which contains bytecode, exception handlers, and code-level attributes for a method.
 * Provides methods for managing code, exception handlers, and nested attributes.
 */
public class CodeAttribute extends Attribute {
    private int maxStack;
    private int maxLocals;
    private byte[] code;
    private final List<ExceptionHandler> exceptionHandlers = new ArrayList<>();
    private final List<Attribute> codeAttributes = new ArrayList<>();
    
    /**
     * Constructs a CodeAttribute.
     */
    public CodeAttribute() {
        super("Code");
    }
    
    /**
     * Gets the maximum stack size for this code attribute.
     * @return the max stack size
     */
    public int getMaxStack() {
        return maxStack;
    }
    
    /**
     * Sets the maximum stack size for this code attribute.
     * @param maxStack the new max stack size
     */
    public void setMaxStack(int maxStack) {
        this.maxStack = maxStack;
    }
    
    /**
     * Gets the maximum number of local variables for this code attribute.
     * @return the max locals
     */
    public int getMaxLocals() {
        return maxLocals;
    }
    
    /**
     * Sets the maximum number of local variables for this code attribute.
     * @param maxLocals the new max locals
     */
    public void setMaxLocals(int maxLocals) {
        this.maxLocals = maxLocals;
    }
    
    /**
     * Gets the bytecode for this code attribute.
     * @return a copy of the bytecode, or null
     */
    public byte[] getCode() {
        return code != null ? code.clone() : null;
    }
    
    /**
     * Sets the bytecode for this code attribute.
     * @param code the new bytecode
     */
    public void setCode(byte[] code) {
        this.code = code != null ? code.clone() : null;
    }
    
    /**
     * Adds an exception handler to this code attribute.
     * @param handler the ExceptionHandler to add
     */
    public void addExceptionHandler(ExceptionHandler handler) {
        exceptionHandlers.add(handler);
    }
    
    /**
     * Removes an exception handler from this code attribute.
     * @param handler the ExceptionHandler to remove
     */
    public void removeExceptionHandler(ExceptionHandler handler) {
        exceptionHandlers.remove(handler);
    }
    
    /**
     * Returns all exception handlers for this code attribute.
     * @return unmodifiable list of exception handlers
     */
    public List<ExceptionHandler> getExceptionHandlers() {
        return Collections.unmodifiableList(exceptionHandlers);
    }
    
    /**
     * Adds a nested code-level attribute to this code attribute.
     * @param attribute the Attribute to add
     */
    public void addCodeAttribute(Attribute attribute) {
        codeAttributes.add(attribute);
    }
    
    /**
     * Removes a nested code-level attribute from this code attribute.
     * @param attribute the Attribute to remove
     */
    public void removeCodeAttribute(Attribute attribute) {
        codeAttributes.remove(attribute);
    }
    
    /**
     * Returns all nested code-level attributes for this code attribute.
     * @return unmodifiable list of code attributes
     */
    public List<Attribute> getCodeAttributes() {
        return Collections.unmodifiableList(codeAttributes);
    }
    
    /**
     * Gets the length of the bytecode for this code attribute.
     * @return the code length
     */
    public int getCodeLength() {
        return code != null ? code.length : 0;
    }
    
    /**
     * Gets the number of exception handlers for this code attribute.
     * @return the exception handler count
     */
    public int getExceptionHandlerCount() {
        return exceptionHandlers.size();
    }
    
    /**
     * Removes all exception handlers from this code attribute.
     */
    public void clearExceptionHandlers() {
        exceptionHandlers.clear();
    }
    
    /**
     * Removes all nested code-level attributes from this code attribute.
     */
    public void clearCodeAttributes() {
        codeAttributes.clear();
    }
    
    /**
     * Represents a single exception handler entry, including its range, handler location, and catch type.
     * Provides methods for querying and modifying handler metadata.
     */
    public static class ExceptionHandler {
        private int startPc;
        private int endPc;
        private int handlerPc;
        private String catchType;
        
        /**
         * Constructs an ExceptionHandler with the given range, handler location, and catch type.
         * @param startPc the start program counter
         * @param endPc the end program counter
         * @param handlerPc the handler program counter
         * @param catchType the catch type (null for catch-all)
         */
        public ExceptionHandler(int startPc, int endPc, int handlerPc, String catchType) {
            this.startPc = startPc;
            this.endPc = endPc;
            this.handlerPc = handlerPc;
            this.catchType = catchType;
        }
        
        /**
         * Gets the start program counter for this handler.
         * @return the start PC
         */
        public int getStartPc() {
            return startPc;
        }
        
        /**
         * Sets the start program counter for this handler.
         * @param startPc the new start PC
         */
        public void setStartPc(int startPc) {
            this.startPc = startPc;
        }
        
        /**
         * Gets the end program counter for this handler.
         * @return the end PC
         */
        public int getEndPc() {
            return endPc;
        }
        
        /**
         * Sets the end program counter for this handler.
         * @param endPc the new end PC
         */
        public void setEndPc(int endPc) {
            this.endPc = endPc;
        }
        
        /**
         * Gets the handler program counter for this handler.
         * @return the handler PC
         */
        public int getHandlerPc() {
            return handlerPc;
        }
        
        /**
         * Sets the handler program counter for this handler.
         * @param handlerPc the new handler PC
         */
        public void setHandlerPc(int handlerPc) {
            this.handlerPc = handlerPc;
        }
        
        /**
         * Gets the catch type for this handler.
         * @return the catch type, or null for catch-all
         */
        public String getCatchType() {
            return catchType;
        }
        
        /**
         * Sets the catch type for this handler.
         * @param catchType the new catch type
         */
        public void setCatchType(String catchType) {
            this.catchType = catchType;
        }
        
        /**
         * Returns true if this handler is a catch-all handler.
         * @return true if catch-all
         */
        public boolean isCatchAll() {
            return catchType == null;
        }
        
        /**
         * Returns true if this handler covers the given program counter.
         * @param pc the program counter
         * @return true if covered
         */
        public boolean covers(int pc) {
            return pc >= startPc && pc < endPc;
        }
        
        /**
         * Returns a string representation of this exception handler.
         * @return a string with range, handler, and catch type
         */
        @Override
        public String toString() {
            return "ExceptionHandler{" +
                    "startPc=" + startPc +
                    ", endPc=" + endPc +
                    ", handlerPc=" + handlerPc +
                    ", catchType='" + catchType + '\'' +
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
            
            ExceptionHandler that = (ExceptionHandler) o;
            
            if (startPc != that.startPc) return false;
            if (endPc != that.endPc) return false;
            if (handlerPc != that.handlerPc) return false;
            return catchType != null ? catchType.equals(that.catchType) : that.catchType == null;
        }
        
        /**
         * Returns a hash code for this exception handler.
         * @return the hash code
         */
        @Override
        public int hashCode() {
            int result = startPc;
            result = 31 * result + endPc;
            result = 31 * result + handlerPc;
            result = 31 * result + (catchType != null ? catchType.hashCode() : 0);
            return result;
        }
    }
}