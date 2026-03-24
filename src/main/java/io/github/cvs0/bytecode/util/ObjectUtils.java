package io.github.cvs0.bytecode.util;

/**
 * Utility class for common object operations, such as null-safe equality and hash code generation.
 * Provides static helper methods for use throughout the codebase.
 */
public final class ObjectUtils {
    /**
     * Checks if two objects are equal using null-safe comparison.
     * @param a first object
     * @param b second object
     * @return true if objects are equal, false otherwise
     */
    public static boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Generates a hash code for an object using null-safe approach.
     * @param obj object to hash
     * @return hash code or 0 if object is null
     */
    public static int hashCode(Object obj) {
        return obj != null ? obj.hashCode() : 0;
    }

    /**
     * Generates a combined hash code for multiple objects.
     * @param objects objects to combine
     * @return combined hash code
     */
    public static int combinedHashCode(Object... objects) {
        int result = 1;
        for (Object obj : objects) {
            result = 31 * result + hashCode(obj);
        }
        return result;
    }
}
