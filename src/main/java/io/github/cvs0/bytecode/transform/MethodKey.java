package io.github.cvs0.bytecode.transform;

import java.util.Objects;

/**
 * Typed key identifying a method by its owning class, name, and descriptor.
 *
 * <p>Replaces ad-hoc {@code "owner.methodName(descriptor)"} strings that had to be
 * manually parsed with substring / indexOf to recover the individual parts.</p>
 *
 * @param owner      internal class name (e.g. {@code com/foo/Bar})
 * @param name       method name (e.g. {@code run})
 * @param descriptor JVM method descriptor (e.g. {@code (I)V})
 */
public record MethodKey(String owner, String name, String descriptor) {

    public MethodKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(descriptor, "descriptor");
    }

    /**
     * Creates a MethodKey from its components.
     */
    public static MethodKey of(String owner, String name, String descriptor) {
        return new MethodKey(owner, name, descriptor);
    }

    /**
     * Parses a legacy {@code "owner.methodName(descriptor)"} string.
     *
     * @return the parsed key, or {@code null} if the format is invalid
     */
    public static MethodKey parse(String key) {
        if (key == null) {
            return null;
        }
        int dot = key.indexOf('.');
        if (dot <= 0 || dot >= key.length() - 1) {
            return null;
        }
        String owner = key.substring(0, dot);
        String rest = key.substring(dot + 1);
        int paren = rest.indexOf('(');
        if (paren <= 0) {
            return null;
        }
        return new MethodKey(owner, rest.substring(0, paren), rest.substring(paren));
    }

    /**
     * Returns the legacy {@code "owner.methodName(descriptor)"} form for backward compatibility
     * with map keys that still use strings.
     */
    public String toKeyString() {
        return owner + "." + name + descriptor;
    }

    @Override
    public String toString() {
        return owner + "." + name + descriptor;
    }
}
