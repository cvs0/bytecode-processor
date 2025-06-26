package net.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the Exceptions attribute which lists the checked exceptions
 * that a method may throw.
 */
public class ExceptionsAttribute extends Attribute {
    private final List<String> exceptions = new ArrayList<>();
    
    public ExceptionsAttribute() {
        super("Exceptions");
    }
    
    public ExceptionsAttribute(List<String> exceptions) {
        super("Exceptions");
        if (exceptions != null) {
            this.exceptions.addAll(exceptions);
        }
    }
    
    public void addException(String exceptionType) {
        if (exceptionType != null && !exceptions.contains(exceptionType)) {
            exceptions.add(exceptionType);
        }
    }
    
    public void removeException(String exceptionType) {
        exceptions.remove(exceptionType);
    }
    
    public List<String> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }
    
    public boolean hasException(String exceptionType) {
        return exceptions.contains(exceptionType);
    }
    
    public int getExceptionCount() {
        return exceptions.size();
    }
    
    public void clearExceptions() {
        exceptions.clear();
    }
    
    public boolean isEmpty() {
        return exceptions.isEmpty();
    }
    
    public String[] getExceptionsArray() {
        return exceptions.toArray(new String[0]);
    }
    
    @Override
    public String toString() {
        return "ExceptionsAttribute{" +
                "exceptions=" + exceptions +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        ExceptionsAttribute that = (ExceptionsAttribute) o;
        return exceptions.equals(that.exceptions);
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + exceptions.hashCode();
        return result;
    }
}