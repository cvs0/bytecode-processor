# IO, Analysis, and Runtime Utilities

## IO Layer

### JarReader

`JarReader.read(File, JarMapping)` is the primary entry point for loading JARs. It:

1. Iterates all JAR entries, dispatching `.class` files to ASM parsing and everything else to resource storage.
2. Distinguishes `module-info.class`, `package-info.class`, and regular class entries.
3. Calls `resolveHierarchyAndOverrides()` to link parent/child/interface relationships and mark external overrides.
4. Calls `classifyApplicationClasses()` to split application code from shaded dependencies using the manifest `Main-Class`/`Start-Class` or module-info `mainClass`.

Standalone class reading: `JarReader.readClass(File)` and `JarReader.readClass(byte[])` parse a single class without hierarchy resolution.

### JarWriter

`JarWriter.write(JarMapping, File)` serializes the mapping to a JAR:

- Writes module-info entries first, then package-info, then all program classes, then resources.
- Uses `SafeClassWriter` which extends ASM `ClassWriter` with hierarchy-aware `getCommonSuperClass()` backed by the `JarMapping`.
- `writeToBytes(JarMapping)` produces an in-memory JAR byte array.
- `getClassBytes(ProgramClass, JarMapping)` serializes a single class node (used by `JarMapping.getClassBytes()`).

### JarLayout

Constants for well-known JAR paths (`META-INF/MANIFEST.MF`, etc.).

## Analysis

### DependencyAnalyzer

Static methods for class-level and method-level dependency analysis:

- `findDependencies(ProgramClass)`: all types referenced by a class (superclass, interfaces, instruction operands).
- `buildDependencyGraph(JarMapping)`: `Map<String, Set<String>>` adjacency list.
- `topologicalSort(Map)`: build-order sorting.
- `findCircularDependencies(Map)`: cycle detection.

### UnusedCodeAnalyzer

- `findUnusedMethods(JarMapping)`: returns method keys not referenced by any other method's instructions.
- `findUnusedFields(JarMapping)`: returns field keys not referenced by any instruction.

### JarStatistics

Collects metrics: class count, method count, field count, resource count, instruction counts, etc.

## Runtime Utilities

### FieldReplacer

Builder-pattern utility for reflective field traversal. Walks a chain of field names (static or instance) and supports `get()` and `set(value)` on the terminal field. Useful for hot-swapping delegates behind private singletons.

```java
FieldReplacer.on(ServiceRegistry.class)
    .field("instance")
    .field("service")
    .set(myNewService);
```

### ClassLoaderInjector

Injects class bytes from a `JarMapping` (via `ClassBytesSource`) into a target classloader at runtime.

### ByteBackedUrl

Custom `URLStreamHandler` that serves class/resource bytes from a `JarMapping`, enabling `URLClassLoader`-based loading without writing to disk.

## Utility Classes

- **BytecodeNames**: `isUnsafeToRename(String)` checks if an internal name belongs to JVM runtime or known third-party libraries that must not be renamed.
- **BytecodeTraversal**: `forEachMethod(JarMapping, BiConsumer)` iterates all classes and methods.
- **JarGraphMetadataReconciler**: post-transform cleanup of module-info exports/opens/packages and orphaned package-info entries.

