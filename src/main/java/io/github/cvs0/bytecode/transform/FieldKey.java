package io.github.cvs0.bytecode.transform;

import java.util.Objects;

/**
 * Typed key identifying a field by its owning class and name.
 *
 * <p>Replaces ad-hoc {@code "owner.fieldName"} strings that had to be manually
 * split at the dot to recover the owner or field name.</p>
 *
 * @param owner internal class name (e.g. {@code com/foo/Bar})
 * @param name  field name
 */
public record FieldKey(String owner, String name) {

    public FieldKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
    }

    /**
     * Creates a FieldKey from an owner and field name.
     */
    public static FieldKey of(String owner, String name) {
        return new FieldKey(owner, name);
    }

    /**
     * Parses a legacy {@code "owner.fieldName"} string.
     *
     * @return the parsed key, or {@code null} if the format is invalid
     */
    public static FieldKey parse(String key) {
        if (key == null) {
            return null;
        }
        int dot = key.indexOf('.');
        if (dot <= 0 || dot == key.length() - 1) {
            return null;
        }
        return new FieldKey(key.substring(0, dot), key.substring(dot + 1));
    }

    /**
     * Returns the legacy {@code "owner.fieldName"} form for backward compatibility
     * with map keys that still use strings.
     */
    public String toKeyString() {
        return owner + "." + name;
    }

    @Override
    public String toString() {
        return owner + "." + name;
    }
}
