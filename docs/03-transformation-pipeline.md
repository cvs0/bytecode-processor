# Transformation Pipeline

## ClassTransformer

`ClassTransformer` is the primary transformation engine. It operates on a `JarMapping` and executes work in two phases: structural tasks (pre-rename) and post-reference tasks (post-rename).

### Apply Order (`applyTransformations()`)

1. Run structural tasks (access changes, superclass edits, debug stripping, visitors)
2. Validate renames (drop unsafe renames for constructors, native methods, external overrides)
3. Propagate method renames across the class hierarchy via `ProgramClass.getHierarchyClasses()`
4. Build `MappingRemapper` and remap every `ClassNode` via ASM `ClassRemapper`
5. Sync `ProgramClass` wrappers and update `JarMapping` indexes
6. Run post-reference tasks (LDC/string transforms, instruction hooks, resource renames)
7. `JarGraphMetadataReconciler.reconcile()` to clean up stale metadata

### Scheduling Renames

```java
ClassTransformer t = new ClassTransformer(mapping);
t.renameClass("com/foo/Bar", "com/foo/Baz");
t.renameField("com/foo/Bar", "oldField", "newField");
t.renameMethod("com/foo/Bar", "oldMethod", "(I)V", "newMethod");
t.renamePackage("com/foo", "com/bar");
t.applyTransformations();
```

All names use internal slash format.

### Rename Validation

- Classes matching `BytecodeNames.isUnsafeToRename()` (JVM runtime, known third-party) are dropped.
- Constructors (`<init>`, `<clinit>`) are never renamed.
- Methods where `ProgramMethod.isSafeToRename()` returns false are skipped.

### Hierarchy Propagation

Method renames automatically propagate to all classes in the hierarchy that define the same method signature, provided those methods are safe to rename.

### Structural Tasks (pre-rename)

- `setClassAccess()`, `setFieldAccess()`, `setMethodAccess()`
- `setSuperClass()`, `addInterface()`, `removeInterface()`
- `setClassFileVersion()`, `setClassFileVersionForAll()`
- `stripDebugOnClass()`, `stripDebugEverywhere()` with `StripDebugMode` enum (SOURCE_FILE, LINE_NUMBERS, LOCAL_VARIABLES, METHOD_PARAMETERS)
- `removeMethod()`, `removeField()`, `removeClassFromMapping()`
- `visitProgramClasses()` for arbitrary pre-rename edits

### Post-Reference Tasks (post-rename)

- `transformStringConstants()`: rewrite LDC string constants
- `transformLdcConstants()`: rewrite any LDC constant (Type, primitives, etc.)
- `transformInstructions()`: apply `InstructionTransformer` to matching methods
- `renameResource()`, `removeResource()`
- `visitProgramClassesAfterReferences()`, `visitMethodsAfterReferences()`

## MappingRemapper

Bridges scheduled class/field/method renames into ASM's `Remapper` contract. Used internally by `ClassTransformer` to drive `ClassRemapper`.

## JarGraphMetadataReconciler

Runs after all transforms. Reconciles module-info exports/opens/packages and prunes orphaned package-info entries that no longer correspond to any class in the mapping.

