/**
 * JAR I/O and cross-cutting bytecode helpers tied to {@link io.github.cvs0.bytecode.JarMapping}:
 *
 * <ul>
 *   <li><b>I/O</b> — {@link io.github.cvs0.bytecode.util.JarReader}, {@link io.github.cvs0.bytecode.util.JarWriter},
 *       {@link io.github.cvs0.bytecode.util.SafeClassWriter}, {@link io.github.cvs0.bytecode.util.JarLayout}</li>
 *   <li><b>Naming</b> — {@link io.github.cvs0.bytecode.util.BytecodeNames}</li>
 *   <li><b>After renames</b> — {@link io.github.cvs0.bytecode.util.ManifestPatcher},
 *       {@link io.github.cvs0.bytecode.util.ServiceLoaderResourcePatcher},
 *       {@link io.github.cvs0.bytecode.util.JarGraphMetadataReconciler}</li>
 *   <li><b>Traversal</b> — {@link io.github.cvs0.bytecode.util.BytecodeTraversal}</li>
 * </ul>
 */
package io.github.cvs0.bytecode.util;
