/**
 * Scheduled edits on a {@link io.github.cvs0.bytecode.JarMapping}: {@link io.github.cvs0.bytecode.transform.ClassTransformer}
 * applies renames and reference updates via {@link io.github.cvs0.bytecode.transform.MappingRemapper} and ASM's
 * {@link org.objectweb.asm.commons.ClassRemapper}. Shared contracts: {@link io.github.cvs0.bytecode.transform.Remapper},
 * {@link io.github.cvs0.bytecode.transform.Transformer}. Instruction helpers:
 * {@link io.github.cvs0.bytecode.transform.InstructionTransformer}.
 */
package io.github.cvs0.bytecode.transform;
