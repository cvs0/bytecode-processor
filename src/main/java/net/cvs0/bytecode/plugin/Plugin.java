package net.cvs0.bytecode.plugin;

import net.cvs0.bytecode.JarMapping;

public interface Plugin {
    
    String getName();
    
    String getVersion();
    
    String getDescription();
    
    void initialize();
    
    void process(JarMapping mapping);
    
    void cleanup();
    
    default boolean isEnabled() {
        return true;
    }
    
    default int getPriority() {
        return 0;
    }
}