package io.github.cvs0.bytecode.member;

import io.github.cvs0.bytecode.util.ObjectUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a local variable in a method, including its name, type, scope, and index.
 * Provides utility methods for querying variable properties and type information.
 */
@Getter
@Setter
public class LocalVariable {
    private final String name;
    private String descriptor;
    private String signature;
    private int startPc;
    private int length;
    private final int index;

    /**
     * Constructs a LocalVariable with name, descriptor, start PC, length, and index.
     * @param name the variable name
     * @param descriptor the type descriptor
     * @param startPc the start program counter
     * @param length the length of the variable's scope
     * @param index the variable index
     */
    public LocalVariable(String name, String descriptor, int startPc, int length, int index) {
        this.name = name;
        this.descriptor = descriptor;
        this.startPc = startPc;
        this.length = length;
        this.index = index;
    }

    /**
     * Constructs a LocalVariable with name, descriptor, signature, start PC, length, and index.
     * @param name the variable name
     * @param descriptor the type descriptor
     * @param signature the generic signature
     * @param startPc the start program counter
     * @param length the length of the variable's scope
     * @param index the variable index
     */
    public LocalVariable(String name, String descriptor, String signature, int startPc, int length, int index) {
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.startPc = startPc;
        this.length = length;
        this.index = index;
    }

    /**
     * Returns the end program counter for this variable's scope.
     * @return the end PC
     */
    public int getEndPc() {
        return startPc + length;
    }

    /**
     * Returns true if the variable is active at the given program counter.
     * @param pc the program counter
     * @return true if active, false otherwise
     */
    public boolean isActive(int pc) {
        return pc >= startPc && pc < getEndPc();
    }

    /**
     * Returns true if this variable has a generic signature.
     * @return true if signature is present
     */
    public boolean hasSignature() {
        return signature != null;
    }

    /**
     * Returns the type descriptor (alias for getDescriptor).
     * @return the type descriptor
     */
    public String getType() {
        return descriptor;
    }

    /**
     * Returns true if the variable is of a primitive type.
     * @return true if primitive
     */
    public boolean isPrimitive() {
        return descriptor != null && descriptor.length() == 1 && !"L[".contains(descriptor);
    }

    /**
     * Returns true if the variable is an object reference.
     * @return true if object
     */
    public boolean isObject() {
        return descriptor != null && descriptor.startsWith("L");
    }

    /**
     * Returns true if the variable is an array.
     * @return true if array
     */
    public boolean isArray() {
        return descriptor != null && descriptor.startsWith("[");
    }

    /**
     * Returns a string representation of this local variable.
     */
    @Override
    public String toString() {
        return "LocalVariable{" +
                "name='" + name + '\'' +
                ", descriptor='" + descriptor + '\'' +
                ", signature='" + signature + '\'' +
                ", startPc=" + startPc +
                ", length=" + length +
                ", index=" + index +
                '}';
    }

    /**
     * Checks equality with another object.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalVariable that = (LocalVariable) o;
        return startPc == that.startPc &&
               length == that.length &&
               index == that.index &&
               ObjectUtils.equals(name, that.name) &&
               ObjectUtils.equals(descriptor, that.descriptor) &&
               ObjectUtils.equals(signature, that.signature);
    }

    /**
     * Returns a hash code for this local variable.
     */
    @Override
    public int hashCode() {
        return ObjectUtils.combinedHashCode(name, descriptor, signature, startPc, length, index);
    }
}