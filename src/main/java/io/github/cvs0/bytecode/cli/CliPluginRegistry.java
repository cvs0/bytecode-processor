package io.github.cvs0.bytecode.cli;

import io.github.cvs0.bytecode.plugin.Plugin;
import io.github.cvs0.bytecode.plugin.impl.ObfuscationPlugin;
import io.github.cvs0.bytecode.plugin.impl.OptimizationPlugin;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Built-in CLI plugin ids for {@link BytecodeCli.TransformCommand}.
 */
public final class CliPluginRegistry {

    private static final Map<String, Supplier<Plugin>> FACTORIES = Map.ofEntries(
            Map.entry("obfuscation", ObfuscationPlugin::new),
            Map.entry("obfuscate", ObfuscationPlugin::new),
            Map.entry("optimization", OptimizationPlugin::new),
            Map.entry("optimize", OptimizationPlugin::new));

    private CliPluginRegistry() {}

    /**
     * Creates a new plugin instance for the given id (case-insensitive).
     *
     * @throws IllegalArgumentException if the id is unknown
     */
    public static Plugin create(String id) {
        String key = normalizeId(id);
        Supplier<Plugin> factory = FACTORIES.get(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown plugin: " + id + ". Known: " + String.join(", ", FACTORIES.keySet()));
        }
        return factory.get();
    }

    public static String normalizeId(String id) {
        return Objects.requireNonNull(id, "id").trim().toLowerCase(Locale.ROOT);
    }

    public static String knownPluginsHelp() {
        return String.join(", ", FACTORIES.keySet());
    }
}
