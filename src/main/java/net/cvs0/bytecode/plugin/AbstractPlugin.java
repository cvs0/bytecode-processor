package net.cvs0.bytecode.plugin;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for plugins that provides configuration management and common plugin logic.
 * Implements ConfigurablePlugin and provides default implementations for most methods.
 */
@Getter
@Setter
@ToString(of = {"name", "version", "description", "enabled"})
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractPlugin implements ConfigurablePlugin {
    private final String name;
    private final String version;
    private final String description;
    @Setter
    private boolean enabled = true;
    private final Map<String, Object> configuration = new ConcurrentHashMap<>();

    @Override
    public void configure(Map<String, Object> configuration) {
        this.configuration.clear();
        this.configuration.putAll(configuration);
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return new HashMap<>(configuration);
    }

    @Override
    public void setConfigurationValue(String key, Object value) {
        configuration.put(key, value);
    }

    @Override
    public Object getConfigurationValue(String key) {
        return configuration.get(key);
    }

    @Override
    public void initialize() {
    }

    @Override
    public void cleanup() {
        configuration.clear();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    protected boolean getBooleanConfig(String key, boolean defaultValue) {
        Object value = configuration.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    protected int getIntConfig(String key, int defaultValue) {
        Object value = configuration.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    protected long getLongConfig(String key, long defaultValue) {
        Object value = configuration.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    protected String getStringConfig(String key, String defaultValue) {
        Object value = configuration.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
}
