/**
 * Scheduled edits on a {@link io.github.cvs0.bytecode.JarMapping}: {@link io.github.cvs0.bytecode.transform.ClassTransformer}
 * applies renames and reference updates, then {@link io.github.cvs0.bytecode.util.JarGraphMetadataReconciler} aligns
 * {@code module-info} / {@code package-info} with remaining program classes. Descriptor rewriting:
 * {@link io.github.cvs0.bytecode.transform.DescriptorRemapper}; instruction helpers:
 * {@link io.github.cvs0.bytecode.transform.InstructionTransformer}.
 */
package io.github.cvs0.bytecode.transform;
