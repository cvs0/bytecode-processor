# Architecture Overview

## Package Layout

```
io.github.cvs0.bytecode
├── JarMapping                 # Central in-memory JAR model
├── analysis/                  # Dependency graphs, unused code detection, JAR statistics
├── attribute/                 # Typed wrappers for class-file attributes (Code, Signature, etc.)
├── clazz/                     # Class representations: ProgramClass, LibraryClass, ModuleInfoClass, PackageInfoClass
├── cli/                       # Command-line interface and plugin registry
├── instruction/               # Instruction-level abstraction
├── io/                        # JarReader, JarWriter, SafeClassWriter, JarLayout
├── log/                       # BPLogger (internal logging)
├── member/                    # ProgramField, ProgramMethod, InnerClass, LocalVariable, LineNumber
├── plugin/                    # Plugin SPI, PluginManager, AbstractPlugin, ConfigurablePlugin
│   └── impl/                  # Built-in plugins: ObfuscationPlugin, OptimizationPlugin
├── runtime/                   # Runtime utilities: FieldReplacer, ClassLoaderInjector, byte-backed URL
├── transform/                 # Rename keys, remapper, debug stripping, patchers
│   ├── patcher/               # ManifestPatcher, ServiceLoaderResourcePatcher
│   ├── remapper/              # Remapper abstraction
│   └── transformer/           # ClassTransformer, InstructionTransformer, Transformer interface
└── util/                      # BytecodeNames, BytecodeTraversal, JarGraphMetadataReconciler
```

## Core Data Flow

```
JAR on disk
    │
    ▼
JarReader.read()        Parse .class entries via ASM ClassReader/ClassNode
    │                   Resolve hierarchy links (parent/child/interface)
    │                   Mark external-override methods
    │                   Classify application vs embedded-library classes
    │
    ▼
JarMapping              In-memory model: ProgramClasses + resources + module/package-info
    │
    ▼
ClassTransformer /      Schedule renames, structural edits, instruction transforms
PluginManager           Run plugins in priority order
    │
    ▼
JarWriter.write()       Serialize ClassNodes via SafeClassWriter (hierarchy-aware frames)
    │                   Write module-info, package-info, resources
    ▼
JAR on disk (or byte[])
```

## Key Design Decisions

- **Read-time enrichment**: Hierarchy links and external-override flags are resolved during `JarReader.read()`, so downstream code never builds separate inheritance graphs.
- **Dual indexing**: `JarMapping` indexes classes by both JAR entry path and internal name for O(1) lookup in both dimensions.
- **Application vs library**: Classes are automatically classified by inspecting `Main-Class`/`Start-Class` manifest attributes. Shaded dependencies are excluded from obfuscation by default.
- **ASM tree API**: All bytecode is held as `ClassNode` trees. Renames use ASM's `ClassRemapper` with a custom `MappingRemapper`.
- **ConcurrentHashMap** storage in `JarMapping` and `ProgramClass` allows safe concurrent reads during analysis passes.

