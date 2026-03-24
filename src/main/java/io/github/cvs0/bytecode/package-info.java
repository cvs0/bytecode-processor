/**
 * Core model for JAR-backed bytecode: {@link io.github.cvs0.bytecode.JarMapping} is the in-memory graph (every
 * {@code .class} entry as {@link io.github.cvs0.bytecode.clazz.ProgramClass}, {@code module-info}, {@code package-info},
 * library stubs, and all non-class resources). Typical flows:
 *
 * <ol>
 *   <li><b>Load</b> — {@link io.github.cvs0.bytecode.util.JarReader#read(java.io.File, JarMapping)}</li>
 *   <li><b>Transform</b> — {@link io.github.cvs0.bytecode.transform.ClassTransformer} and/or
 *       {@link io.github.cvs0.bytecode.plugin.PluginManager#processWithPlugins(JarMapping)}</li>
 *   <li><b>Post-process</b> — {@link JarMapping#remapManifestMainClass(java.util.Map)}, {@link JarMapping#remapServiceLoaderResourcePaths(java.util.Map)},
 *       {@link JarMapping#remapServiceLoaderImplementations(java.util.Map)}</li>
 *   <li><b>Write</b> — {@link io.github.cvs0.bytecode.util.JarWriter} or {@link JarMapping#writeToJar(java.nio.file.Path)}</li>
 * </ol>
 *
 * <p>Analysis without writing: {@link io.github.cvs0.bytecode.analysis.DependencyAnalyzer},
 * {@link io.github.cvs0.bytecode.analysis.JarStatistics}. CLI: {@link io.github.cvs0.bytecode.cli.BytecodeCli}.</p>
 */
package io.github.cvs0.bytecode;
