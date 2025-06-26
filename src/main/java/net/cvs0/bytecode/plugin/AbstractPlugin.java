package net.cvs0.bytecode.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractPlugin implements ConfigurablePlugin {
    private final String name;
    private final String version;
    private final String description;
    private boolean enabled = true;
    private final Map<String, Object> configuration = new ConcurrentHashMap<>();
    
    protected AbstractPlugin(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
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
    
    protected String getStringConfig(String key, String defaultValue) {
        Object value = configuration.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
    
    @Override
    public String toString() {
        return String.format("%s v%s - %s (enabled: %s)", name, version, description, enabled);
    }
}