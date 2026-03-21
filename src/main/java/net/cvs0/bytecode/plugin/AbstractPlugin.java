package net.cvs0.bytecode.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for plugins that provides configuration management and common plugin logic.
 * Implements ConfigurablePlugin and provides default implementations for most methods.
 */
public abstract class AbstractPlugin implements ConfigurablePlugin {
    private final String name;
    private final String version;
    private final String description;
    private boolean enabled = true;
    private final Map<String, Object> configuration = new ConcurrentHashMap<>();

    /**
     * Constructs an AbstractPlugin with the given name, version, and description.
     * @param name the plugin name
     * @param version the plugin version
     * @param description the plugin description
     */
    protected AbstractPlugin(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    /**
     * Returns the name of the plugin.
     * @return the plugin name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the version of the plugin.
     * @return the plugin version
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Returns the description of the plugin.
     * @return the plugin description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Returns true if the plugin is enabled.
     * @return true if enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the plugin.
     * @param enabled true to enable, false to disable
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Configures the plugin with the given configuration map.
     * @param configuration the configuration map
     */
    @Override
    public void configure(Map<String, Object> configuration) {
        this.configuration.clear();
        this.configuration.putAll(configuration);
    }

    /**
     * Returns a copy of the current configuration map.
     * @return the configuration map
     */
    @Override
    public Map<String, Object> getConfiguration() {
        return new HashMap<>(configuration);
    }

    /**
     * Sets a single configuration value.
     * @param key the configuration key
     * @param value the configuration value
     */
    @Override
    public void setConfigurationValue(String key, Object value) {
        configuration.put(key, value);
    }

    /**
     * Gets a single configuration value.
     * @param key the configuration key
     * @return the configuration value
     */
    @Override
    public Object getConfigurationValue(String key) {
        return configuration.get(key);
    }

    /**
     * Initializes the plugin. Default implementation does nothing.
     */
    @Override
    public void initialize() {
    }

    /**
     * Cleans up the plugin and clears configuration.
     */
    @Override
    public void cleanup() {
        configuration.clear();
    }

    /**
     * Returns the plugin priority (default 0).
     * @return the plugin priority
     */
    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * Gets a boolean configuration value or default.
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the boolean value
     */
    protected boolean getBooleanConfig(String key, boolean defaultValue) {
        Object value = configuration.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Gets an int configuration value or default.
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the int value
     */
    protected int getIntConfig(String key, int defaultValue) {
        Object value = configuration.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Gets a String configuration value or default.
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the String value
     */
    protected String getStringConfig(String key, String defaultValue) {
        Object value = configuration.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    /**
     * Returns a string representation of the plugin.
     * @return a string with name, version, description, and enabled state
     */
    @Override
    public String toString() {
        return String.format("%s v%s - %s (enabled: %s)", name, version, description, enabled);
    }
}