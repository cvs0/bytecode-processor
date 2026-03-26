package io.github.cvs0.bytecode.runtime;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A general-purpose utility for reflectively traversing and mutating fields on deeply-nested
 * singleton or service objects. Useful for hot-swapping delegate services, intercepting
 * singleton registries, or replacing internal implementations at runtime.
 *
 * <p>Many frameworks store their core services behind a chain of private fields on singleton
 * objects. For example, a typical pattern is:</p>
 * <pre>{@code
 * ServiceRegistry.instance (static) → RegistryImpl.delegate (instance) → the service to replace
 * }</pre>
 *
 * <p>{@code FieldReplacer} builds a field traversal chain via a builder, then evaluates it
 * to either <b>get</b> or <b>set</b> the final field in one call.</p>
 *
 * <h3>Example: replace a service behind two levels of indirection</h3>
 * <pre>{@code
 * // Equivalent to:
 * //   Object registry = ServiceRegistry.class.getDeclaredField("instance").get(null);
 * //   Field delegateField = registry.getClass().getDeclaredField("service");
 * //   delegateField.set(registry, myNewService);
 *
 * FieldReplacer.on(ServiceRegistry.class)
 *     .field("instance")       // static field → gets the singleton
 *     .field("service")        // instance field on the singleton → target
 *     .set(myNewService);
 * }</pre>
 *
 * <h3>Example: read a deeply-nested value</h3>
 * <pre>{@code
 * Object currentService = FieldReplacer.on(ServiceRegistry.class)
 *     .field("instance")
 *     .field("service")
 *     .get();
 * }</pre>
 *
 * <h3>Example: single static field</h3>
 * <pre>{@code
 * FieldReplacer.on(MySingleton.class)
 *     .field("INSTANCE")
 *     .set(newInstance);
 * }</pre>
 *
 * <p>All field accesses use {@link Field#setAccessible(boolean)} to bypass visibility checks.
 * This works in environments where the calling module has the appropriate
 * {@code --add-opens} or where the security manager allows it.</p>
 *
 * @see ClassLoaderInjector
 */
public final class FieldReplacer {

    private final Class<?> rootClass;
    private final List<String> fieldNames;

    private FieldReplacer(Class<?> rootClass) {
        this.rootClass = Objects.requireNonNull(rootClass, "rootClass");
        this.fieldNames = new ArrayList<>();
    }

    /**
     * Starts a field traversal chain rooted at the given class. The first {@link #field(String)}
     * call will look up a static field on this class.
     *
     * @param rootClass the class that owns the first (static) field in the chain
     * @return a new builder for chaining {@link #field(String)} calls
     */
    public static FieldReplacer on(Class<?> rootClass) {
        return new FieldReplacer(rootClass);
    }

    /**
     * Appends a field name to the traversal chain.
     *
     * <ul>
     *   <li>The <b>first</b> field name is resolved as a <b>static</b> field on the root class.</li>
     *   <li>Each subsequent field name is resolved as an <b>instance</b> field on the object
     *       obtained from the previous step. The field is looked up on the object's
     *       <b>runtime class</b>, so it works correctly with subclasses and implementation types.</li>
     * </ul>
     *
     * @param name the declared field name
     * @return this builder, for chaining
     */
    public FieldReplacer field(String name) {
        Objects.requireNonNull(name, "field name");
        fieldNames.add(name);
        return this;
    }

    /**
     * Traverses the field chain and returns the value of the <b>last</b> field.
     *
     * <p>If only one field is chained, this returns the static field's value.
     * If multiple fields are chained, intermediate values are read and the final
     * field's value is returned.</p>
     *
     * @return the value of the last field in the chain (may be {@code null})
     * @throws IllegalStateException    if no fields have been chained
     * @throws ReflectiveOperationException if any field cannot be found or accessed
     */
    public Object get() throws ReflectiveOperationException {
        validate();
        ResolvedChain chain = resolve();
        return chain.targetField.get(chain.owner);
    }

    /**
     * Traverses the field chain and sets the value of the <b>last</b> field.
     *
     * @param newValue the new value to assign to the last field
     * @throws IllegalStateException    if no fields have been chained
     * @throws ReflectiveOperationException if any field cannot be found or accessed
     */
    public void set(Object newValue) throws ReflectiveOperationException {
        validate();
        ResolvedChain chain = resolve();
        chain.targetField.set(chain.owner, newValue);
    }

    /**
     * Traverses the field chain, reads the current value, replaces it, and returns the
     * previous value. This is useful for storing the original service for later restoration.
     *
     * @param newValue the new value to assign
     * @return the previous value of the last field
     * @throws IllegalStateException    if no fields have been chained
     * @throws ReflectiveOperationException if any field cannot be found or accessed
     */
    public Object getAndSet(Object newValue) throws ReflectiveOperationException {
        validate();
        ResolvedChain chain = resolve();
        Object previous = chain.targetField.get(chain.owner);
        chain.targetField.set(chain.owner, newValue);
        return previous;
    }

    private void validate() {
        if (fieldNames.isEmpty()) {
            throw new IllegalStateException("No fields specified — call field(\"name\") at least once");
        }
    }

    /**
     * Walks the chain: first field is static on rootClass, subsequent fields are instance
     * fields on each intermediate object's runtime class.
     */
    private ResolvedChain resolve() throws ReflectiveOperationException {
        // First field: static on rootClass
        Field first = findField(rootClass, fieldNames.get(0));
        first.setAccessible(true);
        Object current = first.get(null); // static get

        if (fieldNames.size() == 1) {
            return new ResolvedChain(null, first);
        }

        // Intermediate fields: walk the chain
        for (int i = 1; i < fieldNames.size() - 1; i++) {
            if (current == null) {
                throw new NullPointerException(
                        "Field '" + fieldNames.get(i - 1) + "' was null; cannot traverse to '"
                                + fieldNames.get(i) + "'");
            }
            Field f = findField(current.getClass(), fieldNames.get(i));
            f.setAccessible(true);
            current = f.get(current);
        }

        // Last field: the target
        String lastFieldName = fieldNames.get(fieldNames.size() - 1);
        if (current == null) {
            throw new NullPointerException(
                    "Field '" + fieldNames.get(fieldNames.size() - 2) + "' was null; cannot traverse to '"
                            + lastFieldName + "'");
        }
        Field target = findField(current.getClass(), lastFieldName);
        target.setAccessible(true);
        return new ResolvedChain(current, target);
    }

    /**
     * Finds a declared field on the given class or any of its superclasses.
     */
    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(
                "Field '" + name + "' not found on " + clazz.getName() + " or any superclass");
    }

    private record ResolvedChain(Object owner, Field targetField) {
    }
}
