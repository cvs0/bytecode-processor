package io.github.cvs0.bytecode.runtime.url;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Objects;

/**
 * Creates {@link URL} instances backed by in-memory byte arrays, using {@link MethodHandle}-based
 * construction to avoid deprecated API warnings.
 *
 * <p>This is useful when injecting content into a classloader via its URL-based classpath
 * (e.g. {@code addURL(URL)}). The URL's {@link URLConnection#getInputStream()} serves the
 * provided bytes directly from memory — no disk I/O involved.</p>
 *
 * <p>Example: inject an in-memory JAR into a classloader:</p>
 * <pre>{@code
 * byte[] jarBytes = JarWriter.writeToBytes(mapping);
 * URL url = ByteBackedUrl.create("my-payload.jar", jarBytes);
 * ClassLoaderInjector.forMethod(targetClassLoader, "addUrlFwd").inject(url);
 * }</pre>
 *
 * <p>Example: provide a single class file:</p>
 * <pre>{@code
 * byte[] classBytes = JarWriter.getClassBytes(programClass, mapping);
 * URL url = ByteBackedUrl.create("com/foo/Bar.class", classBytes);
 * }</pre>
 *
 * <p>Example: configure protocol and host:</p>
 * <pre>{@code
 * URL url = ByteBackedUrl.builder("payload.jar", jarBytes)
 *     .protocol("bytecode")
 *     .host("internal")
 *     .build();
 * }</pre>
 *
 * @see JarWriter#writeToBytes(io.github.cvs0.bytecode.JarMapping)
 * @see ClassLoaderInjector
 */
public final class ByteBackedUrl {

    /** MethodHandle for URL(String, String, int, String, URLStreamHandler) — avoids deprecation. */
    private static final MethodHandle URL_CTOR;

    static {
        try {
            URL_CTOR = MethodHandles.publicLookup().findConstructor(URL.class, MethodType.methodType(
                    void.class, String.class, String.class, int.class, String.class, URLStreamHandler.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ByteBackedUrl() {
    }

    /**
     * Creates a URL backed by the given byte array. The URL uses the {@code memory:} protocol
     * and serves the bytes via a custom {@link URLStreamHandler}.
     *
     * <p>URL construction uses {@link MethodHandle} to invoke the multi-arg {@link URL} constructor
     * without deprecation warnings.</p>
     *
     * @param name  a descriptive name for the URL path (e.g. {@code "payload.jar"} or
     *              {@code "com/foo/Bar.class"}); used only for display/debugging
     * @param bytes the content bytes (defensively copied)
     * @return a URL whose input stream serves the given bytes
     */
    public static URL create(String name, byte[] bytes) {
        return builder(name, bytes).build();
    }

    /**
     * Returns a builder for fine-grained control over the URL's protocol, host, and port.
     *
     * @param name  the URL path component
     * @param bytes the content bytes (defensively copied at build time)
     * @return a new builder
     */
    public static Builder builder(String name, byte[] bytes) {
        return new Builder(name, bytes);
    }

    /** Invokes the URL constructor via MethodHandle. */
    private static URL newUrl(String protocol, String host, int port, String file,
                               URLStreamHandler handler) {
        try {
            return (URL) URL_CTOR.invoke(protocol, host, port, file, handler);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to construct byte-backed URL", t);
        }
    }

    // ========================================================================
    //  Builder
    // ========================================================================

    /**
     * Builder for constructing byte-backed URLs with configurable protocol, host, and port.
     * Defaults to {@code memory:///<name>}.
     */
    public static final class Builder {
        private final String name;
        private final byte[] bytes;
        private String protocol = "memory";
        private String host = "";
        private int port = -1;

        private Builder(String name, byte[] bytes) {
            this.name = Objects.requireNonNull(name, "name");
            this.bytes = Objects.requireNonNull(bytes, "bytes");
        }

        /** Sets the URL protocol (default: {@code "memory"}). */
        public Builder protocol(String protocol) {
            this.protocol = Objects.requireNonNull(protocol, "protocol");
            return this;
        }

        /** Sets the URL host (default: {@code ""}). */
        public Builder host(String host) {
            this.host = Objects.requireNonNull(host, "host");
            return this;
        }

        /** Sets the URL port (default: {@code -1}). */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Builds the byte-backed URL. The byte array is defensively copied.
         *
         * @return a URL whose input stream serves the provided bytes
         */
        public URL build() {
            byte[] copy = bytes.clone();
            return newUrl(protocol, host, port, "/" + name, new ByteBackedStreamHandler(copy));
        }
    }

    // ========================================================================
    //  Stream handler + connection
    // ========================================================================

    private static final class ByteBackedStreamHandler extends URLStreamHandler {
        private final byte[] data;

        ByteBackedStreamHandler(byte[] data) {
            this.data = data;
        }

        @Override
        protected URLConnection openConnection(URL u) {
            return new ByteBackedConnection(u, data);
        }
    }

    private static final class ByteBackedConnection extends URLConnection {
        private final byte[] data;

        ByteBackedConnection(URL url, byte[] data) {
            super(url);
            this.data = data;
        }

        @Override
        public void connect() {
            // No-op: content is in memory
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public int getContentLength() {
            return data.length;
        }

        @Override
        public long getContentLengthLong() {
            return data.length;
        }
    }
}
