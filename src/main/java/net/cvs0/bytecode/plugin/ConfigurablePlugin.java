package net.cvs0.bytecode.plugin;

import java.util.Map;

public interface ConfigurablePlugin extends Plugin {
    
    void setEnabled(boolean enabled);
    
    void configure(Map<String, Object> configuration);
    
    Map<String, Object> getConfiguration();
    
    void setConfigurationValue(String key, Object value);
    
    Object getConfigurationValue(String key);
    
    default boolean hasConfiguration(String key) {
        return getConfiguration().containsKey(key);
    }
}