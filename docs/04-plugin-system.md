# Plugin System

## Plugin Interface

All plugins implement `io.github.cvs0.bytecode.plugin.Plugin`:

```java
public interface Plugin {
    String getName();
    String getVersion();
    String getDescription();
    void initialize();
    void process(JarMapping mapping);
    void cleanup();
    default boolean isEnabled() { return true; }
    default int getPriority() { return 0; }
}
```

`AbstractPlugin` provides a base implementation with `initialize()`/`cleanup()` no-ops. `ConfigurablePlugin` extends it with `configure(Map<String, Object>)` and typed config accessors (`getBooleanConfig`, `getStringConfig`).

## PluginManager

Registers plugins, initializes them once, and runs `process()` in descending priority order.

```java
PluginManager pm = new PluginManager();
pm.registerPlugin(new ObfuscationPlugin());
pm.registerPlugin(new OptimizationPlugin());
pm.initializePlugins();
pm.processWithPlugins(mapping);
pm.cleanupPlugins();
```

- `processWithPlugins()` returns `true` if all plugins succeed; failures are logged and processing continues.
- `enablePlugin(name)` / `disablePlugin(name)` toggle `ConfigurablePlugin` instances at runtime.
- Duplicate plugin names throw `IllegalArgumentException`.

## Built-in Plugins

### ObfuscationPlugin

Renames application classes, methods, and fields to opaque identifiers using `ClassTransformer`. Configuration keys:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `obfuscateClasses` | boolean | true | Rename class simple names (preserves package structure) |
| `obfuscateMethods` | boolean | true | Rename methods (skips constructors, `main`) |
| `obfuscateFields` | boolean | true | Rename fields (skips `static final` constants) |
| `namePrefix` | String | `"a"` | Prefix for generated names; suffix is a counter |

Skips embedded library classes (shaded deps) and JVM/third-party types. Method renames propagate across the hierarchy. Service loader files and manifest attributes are updated automatically.

### OptimizationPlugin

Dead-code removal and peephole instruction cleanup. Configuration keys:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `removeUnusedMethods` | boolean | false | Remove methods reported unused by `UnusedCodeAnalyzer` |
| `removeUnusedFields` | boolean | false | Remove unused fields |
| `removeNops` | boolean | true | Strip NOP instructions |
| `optimizeConstants` | boolean | true | Replace bipush/sipush of -1..5 with iconst_* |

