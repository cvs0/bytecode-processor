package net.cvs0.bytecode.plugin;

import java.util.Map;

/**
 * Interface for plugins that support configuration and can be enabled/disabled at runtime.
 * Extends Plugin with configuration management methods.
 */
public interface ConfigurablePlugin extends Plugin {
    /**
     * Enables or disables the plugin.
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);

    /**
     * Configures the plugin with the given configuration map.
     * @param configuration the configuration map
     */
    void configure(Map<String, Object> configuration);

    /**
     * Returns the current configuration map.
     * @return the configuration map
     */
    Map<String, Object> getConfiguration();

    /**
     * Sets a single configuration value.
     * @param key the configuration key
     * @param value the configuration value
     */
    void setConfigurationValue(String key, Object value);

    /**
     * Gets a single configuration value.
     * @param key the configuration key
     * @return the configuration value
     */
    Object getConfigurationValue(String key);

    /**
     * Returns true if the configuration contains the given key.
     * @param key the configuration key
     * @return true if present
     */
    default boolean hasConfiguration(String key) {
        return getConfiguration().containsKey(key);
    }
}