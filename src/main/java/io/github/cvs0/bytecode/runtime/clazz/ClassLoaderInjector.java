package io.github.cvs0.bytecode.runtime.clazz;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.runtime.url.ByteBackedUrl;
import io.github.cvs0.bytecode.io.JarWriter;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.util.Objects;

/**
 * Injects {@link URL}s (including byte-backed ones) into an arbitrary classloader's
 * URL classpath via {@link MethodHandle}-based reflective access.
 *
 * <p>Different classloader implementations expose URL-adding methods under different names
 * ({@code addURL}, {@code addUrlFwd}, etc.) and different visibility levels. This utility
 * uses {@link MethodHandles#privateLookupIn} to obtain a handle to whatever method the
 * target classloader exposes, making it work across Fabric/Knot, Forge, Paper, or any
 * custom classloader.</p>
 *
 * <h3>Usage patterns</h3>
 *
 * <p>Inject a single byte-backed JAR:</p>
 * <pre>{@code
 * ClassLoaderInjector injector = ClassLoaderInjector.forMethod(targetCL, "addUrlFwd");
 * byte[] jarBytes = mapping.writeToBytes();
 * injector.injectBytes("payload.jar", jarBytes);
 * }</pre>
 *
 * <p>Inject an entire JarMapping as an in-memory JAR:</p>
 * <pre>{@code
 * ClassLoaderInjector.forMethod(targetCL, "addURL")
 *     .injectMapping(mapping);
 * }</pre>
 *
 * <p>Use the standard {@code URLClassLoader.addURL} with a custom URL:</p>
 * <pre>{@code
 * ClassLoaderInjector.forAddUrl(targetCL)
 *     .inject(ByteBackedUrl.create("mod.jar", bytes));
 * }</pre>
 *
 * <p>Expose the raw {@link MethodHandle} for native code bridges:</p>
 * <pre>{@code
 * MethodHandle handle = ClassLoaderInjector.forMethod(targetCL, "addUrlFwd")
 *     .getHandle();
 * nativeInvoke(handle);
 * }</pre>
 *
 * @see ByteBackedUrl
 * @see JarWriter#writeToBytes(JarMapping)
 */
public final class ClassLoaderInjector {

    private final MethodHandle addUrlHandle;
    private final ClassLoader classLoader;

    private ClassLoaderInjector(ClassLoader classLoader, MethodHandle addUrlHandle) {
        this.classLoader = classLoader;
        this.addUrlHandle = addUrlHandle;
    }

    // ========================================================================
    //  Static factories
    // ========================================================================

    /**
     * Creates an injector that targets the named method on the given classloader.
     * The method must accept a single {@link URL} parameter and return {@code void}.
     *
     * <p>Uses {@link MethodHandles#privateLookupIn} for access, so this works even
     * for private or package-private methods.</p>
     *
     * @param classLoader the target classloader instance
     * @param methodName  the name of the URL-accepting method (e.g. {@code "addUrlFwd"},
     *                    {@code "addURL"}, {@code "addPath"})
     * @return a new injector bound to the given classloader and method
     * @throws ReflectiveOperationException if the method cannot be found or accessed
     */
    public static ClassLoaderInjector forMethod(ClassLoader classLoader, String methodName)
            throws ReflectiveOperationException {
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(methodName, "methodName");

        Class<?> clClass = classLoader.getClass();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clClass, MethodHandles.lookup());
        MethodHandle handle = lookup.findVirtual(clClass, methodName,
                MethodType.methodType(void.class, URL.class)).bindTo(classLoader);
        return new ClassLoaderInjector(classLoader, handle);
    }

    /**
     * Creates an injector targeting the standard {@code addURL(URL)} method found on
     * {@link java.net.URLClassLoader} and its subclasses.
     *
     * @param classLoader the target classloader
     * @return a new injector
     * @throws ReflectiveOperationException if the method cannot be found or accessed
     */
    public static ClassLoaderInjector forAddUrl(ClassLoader classLoader)
            throws ReflectiveOperationException {
        return forMethod(classLoader, "addURL");
    }

    /**
     * Creates an injector from a pre-built {@link MethodHandle}. The handle must accept
     * a single {@link URL} parameter. This is useful when the handle is obtained from
     * a native bridge or other non-standard source.
     *
     * @param classLoader the classloader the handle is bound to (for context only)
     * @param handle      a MethodHandle that accepts a {@link URL}
     * @return a new injector
     */
    public static ClassLoaderInjector fromHandle(ClassLoader classLoader, MethodHandle handle) {
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(handle, "handle");
        return new ClassLoaderInjector(classLoader, handle);
    }

    // ========================================================================
    //  Injection methods
    // ========================================================================

    /**
     * Injects a URL into the target classloader.
     *
     * @param url the URL to inject
     */
    public void inject(URL url) {
        Objects.requireNonNull(url, "url");
        try {
            addUrlHandle.invoke(url);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to inject URL into classloader: " + url, t);
        }
    }

    /**
     * Creates a {@link ByteBackedUrl} from the given bytes and injects it into the
     * target classloader.
     *
     * @param name  descriptive name for the URL (e.g. {@code "payload.jar"})
     * @param bytes the content bytes
     */
    public void injectBytes(String name, byte[] bytes) {
        inject(ByteBackedUrl.create(name, bytes));
    }

    /**
     * Serializes the given {@link JarMapping} to an in-memory JAR and injects it
     * into the target classloader as a byte-backed URL.
     *
     * @param mapping the mapping to inject
     * @throws IOException if serialization fails
     */
    public void injectMapping(JarMapping mapping) throws IOException {
        Objects.requireNonNull(mapping, "mapping");
        byte[] jarBytes = JarWriter.writeToBytes(mapping);
        injectBytes(deriveName(mapping), jarBytes);
    }

    /**
     * Serializes a single {@link ProgramClass} to class bytes and injects it into the
     * target classloader as a byte-backed URL.
     *
     * @param programClass the class to inject
     * @param mapping      the JarMapping for hierarchy-aware frame computation (may be {@code null})
     */
    public void injectClass(ProgramClass programClass, JarMapping mapping) {
        Objects.requireNonNull(programClass, "programClass");
        byte[] classBytes = JarWriter.getClassBytes(programClass, mapping);
        injectBytes(programClass.getName() + ".class", classBytes);
    }

    // ========================================================================
    //  Handle access
    // ========================================================================

    /**
     * Returns the raw {@link MethodHandle} for the URL-adding method, already bound to
     * the target classloader. Useful for passing to native code bridges.
     *
     * @return the bound MethodHandle
     */
    public MethodHandle getHandle() {
        return addUrlHandle;
    }

    /**
     * Returns the target classloader this injector operates on.
     *
     * @return the classloader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    private static String deriveName(JarMapping mapping) {
        String path = mapping.getJarPath();
        if (path == null || path.isBlank() || "<in-memory>".equals(path)) {
            return "injected-" + System.identityHashCode(mapping) + ".jar";
        }
        int sep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return sep >= 0 ? path.substring(sep + 1) : path;
    }
}
