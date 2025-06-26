package net.cvs0.bytecode.plugin;

import net.cvs0.bytecode.JarMapping;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PluginManager {
    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    private final List<Plugin> sortedPlugins = new ArrayList<>();
    private boolean initialized = false;
    
    public void registerPlugin(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        
        String name = plugin.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin name cannot be null or empty");
        }
        
        if (plugins.containsKey(name)) {
            throw new IllegalArgumentException("Plugin with name '" + name + "' is already registered");
        }
        
        plugins.put(name, plugin);
        updateSortedPlugins();
    }
    
    public void unregisterPlugin(String name) {
        Plugin removed = plugins.remove(name);
        if (removed != null) {
            try {
                removed.cleanup();
            } catch (Exception e) {
                System.err.println("Error cleaning up plugin '" + name + "': " + e.getMessage());
            }
            updateSortedPlugins();
        }
    }
    
    public Plugin getPlugin(String name) {
        return plugins.get(name);
    }
    
    public Collection<Plugin> getAllPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }
    
    public List<Plugin> getEnabledPlugins() {
        return sortedPlugins.stream()
                .filter(Plugin::isEnabled)
                .toList();
    }
    
    public void initializePlugins() {
        if (initialized) {
            return;
        }
        
        for (Plugin plugin : sortedPlugins) {
            if (plugin.isEnabled()) {
                try {
                    plugin.initialize();
                } catch (Exception e) {
                    System.err.println("Error initializing plugin '" + plugin.getName() + "': " + e.getMessage());
                }
            }
        }
        
        initialized = true;
    }
    
    public void processWithPlugins(JarMapping mapping) {
        if (!initialized) {
            initializePlugins();
        }
        
        for (Plugin plugin : getEnabledPlugins()) {
            try {
                plugin.process(mapping);
            } catch (Exception e) {
                System.err.println("Error processing with plugin '" + plugin.getName() + "': " + e.getMessage());
            }
        }
    }
    
    public void cleanupPlugins() {
        for (Plugin plugin : plugins.values()) {
            try {
                plugin.cleanup();
            } catch (Exception e) {
                System.err.println("Error cleaning up plugin '" + plugin.getName() + "': " + e.getMessage());
            }
        }
        initialized = false;
    }
    
    private void updateSortedPlugins() {
        sortedPlugins.clear();
        sortedPlugins.addAll(plugins.values());
        sortedPlugins.sort(Comparator.comparingInt(Plugin::getPriority).reversed());
    }
    
    public boolean hasPlugin(String name) {
        return plugins.containsKey(name);
    }
    
    public int getPluginCount() {
        return plugins.size();
    }
    
    public int getEnabledPluginCount() {
        return (int) plugins.values().stream().filter(Plugin::isEnabled).count();
    }
    
    public void enablePlugin(String name) {
        Plugin plugin = plugins.get(name);
        if (plugin instanceof ConfigurablePlugin) {
            ((ConfigurablePlugin) plugin).setEnabled(true);
        }
    }
    
    public void disablePlugin(String name) {
        Plugin plugin = plugins.get(name);
        if (plugin instanceof ConfigurablePlugin) {
            ((ConfigurablePlugin) plugin).setEnabled(false);
        }
    }
    
    public List<String> getPluginNames() {
        return new ArrayList<>(plugins.keySet());
    }
    
    public void clear() {
        cleanupPlugins();
        plugins.clear();
        sortedPlugins.clear();
    }
}