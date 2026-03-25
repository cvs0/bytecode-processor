package io.github.cvs0.bytecode.transform;

import io.github.cvs0.bytecode.JarMapping;

/**
 * A single transformation pass over a {@link JarMapping}.
 *
 * <p>Implementations include rename transforms ({@link ClassTransformer}), instruction transforms
 * ({@link InstructionTransformer}), and any future custom passes. The interface ensures every
 * transformer has a clean, uniform contract.</p>
 */
public interface Transformer {

    /**
     * Applies this transformation to the given JAR model. All reference updates, metadata
     * reconciliation, and side effects are handled internally — callers simply invoke this method.
     *
     * @param mapping the in-memory JAR to transform (mutated in place)
     */
    void transform(JarMapping mapping);
}
