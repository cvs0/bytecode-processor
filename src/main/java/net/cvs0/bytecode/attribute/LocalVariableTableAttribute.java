package net.cvs0.bytecode.attribute;

import net.cvs0.bytecode.member.LocalVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the LocalVariableTable attribute, which stores information about local variables in a method.
 * Provides methods for managing and querying local variables.
 */
public class LocalVariableTableAttribute extends Attribute {
    private final List<LocalVariable> localVariables = new ArrayList<>();
    
    /**
     * Constructs a LocalVariableTableAttribute.
     */
    public LocalVariableTableAttribute() {
        super("LocalVariableTable");
    }
    
    /**
     * Adds a local variable to this attribute.
     * @param localVariable the LocalVariable to add
     */
    public void addLocalVariable(LocalVariable localVariable) {
        localVariables.add(localVariable);
    }
    
    /**
     * Removes a local variable from this attribute.
     * @param localVariable the LocalVariable to remove
     */
    public void removeLocalVariable(LocalVariable localVariable) {
        localVariables.remove(localVariable);
    }
    
    /**
     * Returns all local variables in this attribute.
     * @return unmodifiable list of local variables
     */
    public List<LocalVariable> getLocalVariables() {
        return Collections.unmodifiableList(localVariables);
    }
    
    /**
     * Returns the local variable with the given index, or null if not found.
     * @param index the index of the local variable
     * @return the LocalVariable with the given index, or null
     */
    public LocalVariable getLocalVariableByIndex(int index) {
        return localVariables.stream()
                .filter(lv -> lv.getIndex() == index)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Returns the active variables at the given program counter (PC) value.
     * @param pc the program counter value
     * @return list of active LocalVariables at the given PC
     */
    public List<LocalVariable> getActiveVariablesAtPc(int pc) {
        return localVariables.stream()
                .filter(lv -> lv.isActive(pc))
                .toList();
    }
    
    /**
     * Returns the local variable with the given name, or null if not found.
     * @param name the name of the local variable
     * @return the LocalVariable with the given name, or null
     */
    public LocalVariable getVariableByName(String name) {
        return localVariables.stream()
                .filter(lv -> name.equals(lv.getName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Returns the number of local variables in this attribute.
     * @return the local variable count
     */
    public int getLocalVariableCount() {
        return localVariables.size();
    }
    
    /**
     * Removes all local variables from this attribute.
     */
    public void clearLocalVariables() {
        localVariables.clear();
    }
    
    /**
     * Sorts the local variables in this attribute by index and start PC.
     */
    public void sortLocalVariables() {
        localVariables.sort(Comparator.comparingInt(LocalVariable::getIndex).thenComparingInt(LocalVariable::getStartPc));
    }
    
    /**
     * Checks if a variable with the given name exists in this attribute.
     * @param name the name of the variable
     * @return true if a variable with the given name exists
     */
    public boolean hasVariable(String name) {
        return localVariables.stream().anyMatch(lv -> name.equals(lv.getName()));
    }
    
    /**
     * Returns the maximum index of all local variables in this attribute.
     * @return the maximum index, or -1 if there are no local variables
     */
    public int getMaxIndex() {
        return localVariables.stream()
                .mapToInt(LocalVariable::getIndex)
                .max()
                .orElse(-1);
    }
    
    /**
     * Returns a string representation of this attribute.
     * @return a string with the number of local variables
     */
    @Override
    public String toString() {
        return "LocalVariableTableAttribute{" +
                "localVariables=" + localVariables.size() +
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
        LocalVariableTableAttribute that = (LocalVariableTableAttribute) o;
        return localVariables.equals(that.localVariables);
    }

    /**
     * Returns a hash code for this attribute.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + localVariables.hashCode();
        return result;
    }
}