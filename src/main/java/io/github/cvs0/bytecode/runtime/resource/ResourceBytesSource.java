package io.github.cvs0.bytecode.runtime.resource;

import java.io.IOException;
import java.util.Objects;

/**
 * Abstraction for resolving raw resource bytes by resource path.
 *
 * <p>This is the resource counterpart to {@link ClassBytesSource}. Implementations can
 * back resource resolution with in-memory stores, native bridges, network fetches, or
 * any custom source.</p>
 *
 * <p>Sources can be chained via {@link #orElse(ResourceBytesSource)} for priority-based
 * resolution (e.g. check native store first, then fall back to a {@link JarMapping}).</p>
 *
 * <p>{@link JarMapping} implements this interface, so any loaded mapping can be used
 * directly as a resource bytes source.</p>
 *
 * @see ClassBytesSource
 * @see JarMapping
 */
@FunctionalInterface
public interface ResourceBytesSource {

    /**
     * Resolves raw resource bytes for the given resource path.
     *
     * @param resourcePath the JAR-relative resource path, e.g. {@code META-INF/services/com.foo.SPI}
     * @return the raw bytes, or {@code null} if this source cannot provide them
     * @throws IOException if an I/O error occurs during resolution
     */
    byte[] getResourceBytes(String resourcePath) throws IOException;

    /**
     * Returns a composite source that tries this source first, then falls back to {@code other}
     * if this source returns {@code null}.
     *
     * @param other the fallback source
     * @return a composite source
     */
    default ResourceBytesSource orElse(ResourceBytesSource other) {
        Objects.requireNonNull(other, "other");
        return resourcePath -> {
            byte[] result = this.getResourceBytes(resourcePath);
            return result != null ? result : other.getResourceBytes(resourcePath);
        };
    }

    /**
     * Returns a source backed by a simple in-memory map.
     *
     * @param resourceMap map of resource path → raw bytes
     * @return a source that looks up bytes in the map
     */
    static ResourceBytesSource fromMap(java.util.Map<String, byte[]> resourceMap) {
        Objects.requireNonNull(resourceMap, "resourceMap");
        return resourceMap::get;
    }
}
