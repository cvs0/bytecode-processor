package io.github.cvs0.bytecode.plugin;

import io.github.cvs0.bytecode.JarMapping;

/**
 * One stage in the pipeline managed by {@link PluginManager}: {@link #process(JarMapping)} mutates a shared
 * {@link JarMapping} (typically after {@link io.github.cvs0.bytecode.util.JarReader} and before
 * {@link io.github.cvs0.bytecode.util.JarWriter}).
 */
public interface Plugin {
    /**
     * Returns the name of the plugin.
     * @return the plugin name
     */
    String getName();

    /**
     * Returns the version of the plugin.
     * @return the plugin version
     */
    String getVersion();

    /**
     * Returns a description of the plugin.
     * @return the plugin description
     */
    String getDescription();

    /**
     * Initializes the plugin. Called before processing.
     */
    void initialize();

    /**
     * Processes the given JarMapping.
     * @param mapping the JarMapping to process
     */
    void process(JarMapping mapping);

    /**
     * Cleans up any resources used by the plugin.
     */
    void cleanup();

    /**
     * Returns true if the plugin is enabled.
     * @return true if enabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Returns the priority of the plugin (higher runs first).
     * @return the plugin priority
     */
    default int getPriority() {
        return 0;
    }
}