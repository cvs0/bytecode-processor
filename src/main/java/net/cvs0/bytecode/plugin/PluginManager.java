package net.cvs0.bytecode.plugin;

import net.cvs0.bytecode.JarMapping;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the registration, initialization, and execution of plugins for bytecode processing.
 * Supports plugin lifecycle management, prioritization, and safe concurrent access.
 */
public class PluginManager {
    /** Map of plugin names to Plugin instances. */
    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    /** List of plugins sorted by priority. */
    private final List<Plugin> sortedPlugins = new ArrayList<>();
    /** Indicates whether plugins have been initialized. */
    private boolean initialized = false;
    
    /**
     * Registers a new plugin with the manager.
     * @param plugin the Plugin to register
     * @throws IllegalArgumentException if the plugin is null, has no name, or name is already registered
     */
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
    
    /**
     * Unregisters a plugin by name and performs cleanup.
     * @param name the plugin name
     */
    public void unregisterPlugin(String name) {
        Plugin removed = plugins.remove(name);
        if (removed != null) {
            try {
                removed.cleanup();
            } catch (Exception e) {
                System.err.println("Error cleaning up plugin '" + name + "': " + e.getMessage());
                throw new RuntimeException("Failed to cleanup plugin '" + name + "'", e);
            }
            updateSortedPlugins();
        }
    }
    
    /**
     * Retrieves a plugin by name.
     * @param name the plugin name
     * @return the Plugin instance, or null if not found
     */
    public Plugin getPlugin(String name) {
        return plugins.get(name);
    }
    
    /**
     * Returns all registered plugins.
     * @return an unmodifiable collection of plugins
     */
    public Collection<Plugin> getAllPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }
    
    /**
     * Returns a list of enabled plugins, sorted by priority.
     * @return a list of enabled plugins
     */
    public List<Plugin> getEnabledPlugins() {
        return sortedPlugins.stream()
                .filter(Plugin::isEnabled)
                .toList();
    }
    
    /**
     * Initializes all enabled plugins. Safe to call multiple times.
     */
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
    
    /**
     * Processes the given JarMapping with all enabled plugins.
     * @param mapping the JarMapping to process
     */
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
    
    /**
     * Cleans up all registered plugins and resets initialization state.
     */
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
    
    /**
     * Updates the sorted plugin list based on priority.
     */
    private void updateSortedPlugins() {
        sortedPlugins.clear();
        sortedPlugins.addAll(plugins.values());
        sortedPlugins.sort(Comparator.comparingInt(Plugin::getPriority).reversed());
    }
    
    /**
     * Checks if a plugin with the given name is registered.
     * @param name the plugin name
     * @return true if the plugin exists, false otherwise
     */
    public boolean hasPlugin(String name) {
        return plugins.containsKey(name);
    }
    
    /**
     * Returns the number of registered plugins.
     * @return the plugin count
     */
    public int getPluginCount() {
        return plugins.size();
    }
    
    /**
     * Returns the number of enabled plugins.
     * @return the enabled plugin count
     */
    public int getEnabledPluginCount() {
        return (int) plugins.values().stream().filter(Plugin::isEnabled).count();
    }
    
    /**
     * Enables a plugin by name if it is configurable.
     * @param name the plugin name
     */
    public void enablePlugin(String name) {
        Plugin plugin = plugins.get(name);
        if (plugin instanceof ConfigurablePlugin) {
            ((ConfigurablePlugin) plugin).setEnabled(true);
        }
    }
    
    /**
     * Disables a plugin by name if it is configurable.
     * @param name the plugin name
     */
    public void disablePlugin(String name) {
        Plugin plugin = plugins.get(name);
        if (plugin instanceof ConfigurablePlugin) {
            ((ConfigurablePlugin) plugin).setEnabled(false);
        }
    }
    
    /**
     * Returns a list of all registered plugin names.
     * @return a list of plugin names
     */
    public List<String> getPluginNames() {
        return new ArrayList<>(plugins.keySet());
    }
    
    /**
     * Removes all plugins and performs cleanup.
     */
    public void clear() {
        cleanupPlugins();
        plugins.clear();
        sortedPlugins.clear();
    }
}