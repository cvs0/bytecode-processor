package io.github.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the Exceptions attribute, which stores the list of exceptions a method can throw.
 * Provides methods for managing and querying exception types.
 */
public class ExceptionsAttribute extends Attribute {
    private final List<String> exceptions;
    
    /**
     * Constructs an ExceptionsAttribute with a list of exception types.
     * @param exceptions the list of exception types
     */
    public ExceptionsAttribute(List<String> exceptions) {
        super("Exceptions");
        this.exceptions = new ArrayList<>(exceptions);
    }
    /**
     * Constructs an empty ExceptionsAttribute.
     */
    public ExceptionsAttribute() {
        super("Exceptions");
        this.exceptions = new ArrayList<>();
    }
    
    /**
     * Adds an exception type to this attribute.
     * @param exception the exception type
     */
    public void addException(String exception) {
        exceptions.add(exception);
    }
    
    /**
     * Removes an exception type from this attribute.
     * @param exception the exception type
     */
    public void removeException(String exception) {
        exceptions.remove(exception);
    }
    
    /**
     * Returns all exception types in this attribute.
     * @return unmodifiable list of exception types
     */
    public List<String> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }
    
    /**
     * Returns the number of exception types in this attribute.
     * @return the exception count
     */
    public int getExceptionCount() {
        return exceptions.size();
    }
    
    /**
     * Removes all exception types from this attribute.
     */
    public void clearExceptions() {
        exceptions.clear();
    }
    
    /**
     * Returns a string representation of this attribute.
     * @return a string with the number of exceptions
     */
    @Override
    public String toString() {
        return "ExceptionsAttribute{" +
                "exceptions=" + exceptions.size() +
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
        
        ExceptionsAttribute that = (ExceptionsAttribute) o;
        return exceptions.equals(that.exceptions);
    }
    
    /**
     * Returns a hash code for this attribute.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + exceptions.hashCode();
        return result;
    }
}