# JarMapping and Class Model

## JarMapping

`JarMapping` is the central in-memory representation of a JAR file. It holds:

- **ProgramClasses**: indexed by JAR entry path and by internal name.
- **LibraryClasses**: external type stubs.
- **ModuleInfoClass / PackageInfoClass**: module and package descriptors.
- **Resources**: raw byte arrays keyed by entry path.

### Loading

```java
JarMapping mapping = JarMapping.fromJar(Path.of("app.jar"));
// or incrementally:
JarMapping mapping = new JarMapping();
mapping.addClassFromBytes(classBytes);
mapping.resolveHierarchy(); // required after adding all classes
```

### Key Methods

| Method | Description |
|--------|-------------|
| `getProgramClass(String)` | O(1) lookup by internal name |
| `getApplicationClasses()` | Host-project classes only (excludes shaded deps) |
| `renameClass(old, new)` | Updates both indexes, jar entry paths, package-infos |
| `merge(JarMapping)` | Combines two mappings; call `resolveHierarchy()` after |
| `writeToJar(Path)` | Delegates to `JarWriter` |
| `writeToBytes()` | Serializes to in-memory JAR byte array |
| `toClassBytesMap()` | Exports `Map<String, byte[]>` for classloaders |
| `remapManifestMainClass(Map)` | Patches Main-Class/Start-Class in manifest |
| `remapServiceLoaderResourcePaths(Map)` | Renames META-INF/services/* files |
| `remapServiceLoaderImplementations(Map)` | Rewrites provider lines inside service files |

### ClassBytesSource / ResourceBytesSource

`JarMapping` implements both interfaces, making it usable as a byte source for runtime classloaders and byte-backed URL schemes.

## ProgramClass

Wraps a `.class` entry backed by an ASM `ClassNode`.

### Hierarchy Links (resolved at read time by JarReader)

- `parentProgramClass`: resolved superclass within the JAR
- `childProgramClasses`: direct subclasses/implementors in the JAR
- `resolvedInterfaces`: interfaces resolved to ProgramClass instances
- `unresolvedSuperTypes`: external supertypes not in the JAR
- `getHierarchyClasses()`: walks the full hierarchy (up and down)

### Application Classification

`isApplicationClass()` defaults to `true`. JarReader sets it to `false` for classes outside the manifest-derived root package.

### Wrapper Sync

After ASM `ClassRemapper` produces a remapped `ClassNode`, `syncFromClassNode()` rebuilds the wrapper's field/method maps from the new node.

## ProgramMethod / ProgramField

- Keyed by `name + descriptor` (methods) or `name` (fields).
- `ProgramMethod.isOverridesExternal()`: set by JarReader when the method overrides an external contract.
- `ProgramMethod.isSafeToRename()`: false for constructors, static initializers, native methods, and external overrides.

