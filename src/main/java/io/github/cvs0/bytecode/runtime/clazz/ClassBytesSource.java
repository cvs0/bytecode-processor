package io.github.cvs0.bytecode.runtime.clazz;

import java.io.IOException;
import java.util.Objects;

/**
 * Abstraction for resolving raw {@code .class} file bytes by internal class name.
 *
 * <p>This is the core interface for dynamic class delivery pipelines — loaders, custom
 * classloaders, memory-backed stores, or native bridges can implement this to provide
 * class bytecode on demand.</p>
 *
 * <p>Sources can be chained via {@link #orElse(ClassBytesSource)}, composing a
 * priority-based resolution chain (e.g. check in-memory store first, then fall back
 * to a {@link JarMapping}).</p>
 *
 * <p>{@link JarMapping} implements this interface, so any loaded mapping can be used
 * directly as a class bytes source.</p>
 *
 * @see ResourceBytesSource
 * @see JarMapping
 */
@FunctionalInterface
public interface ClassBytesSource {

    /**
     * Resolves raw class bytes for the given internal class name.
     *
     * @param internalName the internal (slash-separated) class name, e.g. {@code com/foo/Bar}
     * @return the raw {@code .class} bytes, or {@code null} if this source cannot provide them
     * @throws IOException if an I/O error occurs during resolution
     */
    byte[] getClassBytes(String internalName) throws IOException;

    /**
     * Returns a composite source that tries this source first, then falls back to {@code other}
     * if this source returns {@code null}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ClassBytesSource chain = nativeStore
     *     .orElse(jarMapping)
     *     .orElse(name -> loadFromNetwork(name));
     * }</pre>
     *
     * @param other the fallback source
     * @return a composite source
     */
    default ClassBytesSource orElse(ClassBytesSource other) {
        Objects.requireNonNull(other, "other");
        return internalName -> {
            byte[] result = this.getClassBytes(internalName);
            return result != null ? result : other.getClassBytes(internalName);
        };
    }

    /**
     * Returns a source backed by a simple in-memory map.
     *
     * @param classBytesMap map of internal class name → raw class bytes
     * @return a source that looks up bytes in the map
     */
    static ClassBytesSource fromMap(java.util.Map<String, byte[]> classBytesMap) {
        Objects.requireNonNull(classBytesMap, "classBytesMap");
        return classBytesMap::get;
    }
}
