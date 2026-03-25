package io.github.cvs0.bytecode.attribute;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Represents an annotation attribute, containing a list of annotation entries and visibility information.
 * Provides methods for managing annotations and querying annotation metadata.
 */
public class AnnotationAttribute extends Attribute {
    private final List<AnnotationInfo> annotations = new ArrayList<>();

    @Getter
    @Setter
    private boolean visible;

    /**
     * Constructs an AnnotationAttribute with the given name and visibility.
     * @param name the attribute name
     * @param visible true if the annotation is visible at runtime
     */
    public AnnotationAttribute(String name, boolean visible) {
        super(name);
        this.visible = visible;
    }

    /**
     * Adds an annotation to this attribute.
     * @param annotation the AnnotationInfo to add
     */
    public void addAnnotation(AnnotationInfo annotation) {
        annotations.add(annotation);
    }

    /**
     * Removes an annotation from this attribute.
     * @param annotation the AnnotationInfo to remove
     */
    public void removeAnnotation(AnnotationInfo annotation) {
        annotations.remove(annotation);
    }

    /**
     * Returns all annotations in this attribute.
     * @return unmodifiable list of annotations
     */
    public List<AnnotationInfo> getAnnotations() {
        return Collections.unmodifiableList(annotations);
    }

    /**
     * Finds an annotation by type.
     * @param type the annotation type
     * @return the AnnotationInfo or null
     */
    public AnnotationInfo findAnnotation(String type) {
        return annotations.stream()
                .filter(ann -> ann.getType().equals(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns true if an annotation of the given type exists.
     * @param type the annotation type
     * @return true if present
     */
    public boolean hasAnnotation(String type) {
        return findAnnotation(type) != null;
    }

    /**
     * Returns the number of annotations in this attribute.
     * @return the annotation count
     */
    public int getAnnotationCount() {
        return annotations.size();
    }

    /**
     * Removes all annotations from this attribute.
     */
    public void clearAnnotations() {
        annotations.clear();
    }

    /**
     * Represents a single annotation entry, including its type and key-value pairs.
     * Provides methods for managing annotation values and querying metadata.
     */
    public static class AnnotationInfo {
        @Getter
        @Setter
        private String type;
        private final Map<String, Object> values = new HashMap<>();

        /**
         * Constructs an AnnotationInfo with the given type.
         * @param type the annotation type
         */
        public AnnotationInfo(String type) {
            this.type = type;
        }


        /**
         * Sets a value for the given name.
         * @param name the value name
         * @param value the value
         */
        public void setValue(String name, Object value) {
            values.put(name, value);
        }

        /**
         * Gets a value by name.
         * @param name the value name
         * @return the value, or null
         */
        public Object getValue(String name) {
            return values.get(name);
        }

        /**
         * Gets all values for this annotation.
         * @return unmodifiable map of values
         */
        public Map<String, Object> getValues() {
            return Collections.unmodifiableMap(values);
        }

        /**
         * Returns true if a value with the given name exists.
         * @param name the value name
         * @return true if present
         */
        public boolean hasValue(String name) {
            return values.containsKey(name);
        }

        /**
         * Removes a value by name.
         * @param name the value name
         */
        public void removeValue(String name) {
            values.remove(name);
        }

        /**
         * Gets a string value by name, or null if not present or not a string.
         * @param name the value name
         * @return the string value or null
         */
        public String getStringValue(String name) {
            Object value = values.get(name);
            return value instanceof String ? (String) value : null;
        }

        /**
         * Gets an integer value by name, or null if not present or not an integer.
         * @param name the value name
         * @return the integer value or null
         */
        public Integer getIntValue(String name) {
            Object value = values.get(name);
            return value instanceof Integer ? (Integer) value : null;
        }

        /**
         * Gets a boolean value by name, or null if not present or not a boolean.
         * @param name the value name
         * @return the boolean value or null
         */
        public Boolean getBooleanValue(String name) {
            Object value = values.get(name);
            return value instanceof Boolean ? (Boolean) value : null;
        }

        /**
         * Returns a string representation of this annotation info.
         * @return a string with type and values
         */
        @Override
        public String toString() {
            return "AnnotationInfo{" +
                    "type='" + type + '\'' +
                    ", values=" + values +
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

            AnnotationInfo that = (AnnotationInfo) o;

            if (!type.equals(that.type)) return false;
            return values.equals(that.values);
        }

        /**
         * Returns a hash code for this annotation info.
         * @return the hash code
         */
        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + values.hashCode();
            return result;
        }
    }
}
