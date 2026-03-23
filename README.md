# Bytecode Processor

A Java bytecode analysis and transformation library built on ASM (tree API). It models JAR contents as editable program classes plus resources, runs dependency analysis, and applies coordinated rewrites (renames, descriptors, instructions, resources) through a single `ClassTransformer` pipeline.

## Features

- **Dependency analysis**: Class-level dependency graphs, cycle detection, reverse dependents, Graphviz DOT export
- **Transformation**: Class, method, and field renames with reference propagation; package moves; instruction and LDC hooks; optional debug stripping
- **Optimization hooks**: NOP removal and related passes via bundled plugins
- **Plugin system**: Ordered `Plugin` execution with `java.util.logging` diagnostics
- **CLI**: Picocli-based JAR analysis without writing code

---

## Setup

### Requirements

| Tool | Version |
|------|---------|
| JDK | **21** or newer (enforced by Maven) |
| Maven | **3.6.3** or newer |

### Build from source

```bash
git clone https://github.com/cvs0/bytecode-processor.git
cd bytecode-processor
mvn clean verify
```

Artifacts under `target/` after `mvn package`:

| File | Role |
|------|------|
| `bytecode-processor-1.1.jar` | **Library**: modular JAR (`module bytecode.processor`). Use on the **module path** (or as an automatic module on the classpath). Does **not** embed ASM or Picocli. |
| `bytecode-processor-1.1-all.jar` | **Runnable CLI**: shaded uber-JAR with ASM, ASM Tree/Commons/Util/Analysis, and Picocli. No `module-info`; run with `java -jar`. |

Install into the local repository for use in other projects:

```bash
mvn install
```

### Git hooks (optional)

Hooks live in `.githooks`. Point Git at that directory once per clone:

```bash
git config core.hooksPath .githooks
```

On Unix, ensure the hook is executable: `chmod +x .githooks/pre-commit`.  
**pre-commit** runs `mvn test` from the repository root (faster than full `verify`; CI still runs `verify`).

### Use as a Maven dependency

```xml
<dependency>
    <groupId>net.cvs0</groupId>
    <artifactId>bytecode-processor</artifactId>
    <version>1.1</version>
</dependency>
```

Released versions are published to **GitHub Packages** when you push a `v*` tag; add the `github` repository and credentials as described under [CI and releases](#ci-and-releases). For a local `mvn install` build, no extra repository is needed.

Your module (or automatic module) must **read** ASM tree and, if you use the CLI types, Picocli. The library module declares:

- `requires org.objectweb.asm.tree;`
- `requires java.logging;`
- `requires info.picocli;` (compile-time for `net.cvs0.bytecode.cli`, which is not exported)

**Module path example** (application `module app { requires bytecode.processor; … }`):

```bash
java --module-path lib/bytecode-processor-1.1.jar:lib/asm-tree-9.7.jar:... -m app/com.example.Main
```

(Resolve the full ASM stack to match the versions in this project’s `pom.xml`.)

---

## Library documentation

### Module layout

- **Module name**: `bytecode.processor`
- **Exported packages** (public API):  
  `net.cvs0.bytecode`, `net.cvs0.bytecode.attribute`, `net.cvs0.bytecode.member`, `net.cvs0.bytecode.analysis`, `net.cvs0.bytecode.util`, `net.cvs0.bytecode.clazz`, `net.cvs0.bytecode.instruction`, `net.cvs0.bytecode.plugin`, `net.cvs0.bytecode.test`, `net.cvs0.bytecode.transform`
- **Non-exported**: `net.cvs0.bytecode.cli` is opened only to `info.picocli` for command metadata; embed the CLI by depending on this artifact and invoking `BytecodeCli`, or run the shaded JAR.

### Naming conventions

- **Internal names**: JVM slash form, e.g. `com/example/MyClass`, `java/lang/String`.
- **Method keys** for scheduled renames: `ClassTransformer.renameMethod` uses `className + "." + oldMethodName + descriptor` where `descriptor` is the raw method descriptor (e.g. `(I)V`).
- **Packages** in `renamePackage`: accepts internal prefix (`com/foo`) or dot form (`com.foo`); inner classes use `$` in the internal class name (`com/foo/Outer$Inner`).

### Core workflow

1. **Load**: `JarMapping.fromJar(Path)` or `fromJar(String)` — parses classes and resources via `JarReader`.
2. **Inspect / analyze**: e.g. `DependencyAnalyzer.buildDependencyGraph(mapping)`, `JarStatistics.from(mapping)`, `UnusedCodeAnalyzer`, etc.
3. **Transform**: construct `ClassTransformer(mapping)`, call mutators (`renameClass`, `renamePackage`, `transformInstructions`, …), then **`applyTransformations()`** once to run the full pipeline.
4. **Write**: `mapping.writeToJar(Path)` or `writeToJar(String)` — delegates to `JarWriter`.

**JAR write semantics**: If the mapping contains a resource entry `META-INF/MANIFEST.MF`, `JarWriter` **does not** write it as a separate ZIP entry, because `JarOutputStream` already supplies a manifest slot. Use the `JarWriter` overload that accepts a `Manifest` if you need explicit manifest control; the embedded manifest wins over a duplicate resource name.

### `JarMapping`

Thread-safe **maps** (`ConcurrentHashMap`) for program classes, library classes, and resources; individual `ProgramClass` instances are not thread-safe unless stated otherwise.

Typical operations: `addClass`, `addLibraryClass`, `addResource`, `getProgramClass`, `getProgramClasses`, `removeClass`, `removeResource`, `renameClass` (mapping keys only — prefer `ClassTransformer` for bytecode-aware renames), `writeToJar`.

### `ClassTransformer`

Single entry point for coordinated bytecode edits. Renames are **queued** until `applyTransformations()`.

**Apply order** (see class Javadoc):

1. Structural tasks (access, hierarchy, interfaces, version, signatures, removals, debug stripping, `visitProgramClasses` hooks).
2. Scheduled field renames, then method renames, then class renames.
3. Reference propagation (owners, descriptors, signatures, `invokedynamic`, etc.).
4. Post tasks: string/LDC transforms, `transformInstructions`, resource renames/removals, `visitMethodsAfterReferences`, etc.

Use internal names consistent with the mapping at the time `applyTransformations()` runs. Higher-level helpers include `renamePackage`, `renameClassesMatching`, descriptor remapping via related utilities, and resource transforms.

### `DependencyAnalyzer`

- `findClassDependencies(ProgramClass)`, `findMethodDependencies(ProgramMethod)` — per-unit edges.
- `buildDependencyGraph(JarMapping)` — adjacency: program class → set of referenced internal names.
- `buildReverseDependencyGraph(JarMapping)` — dependent → set of types it depends on.
- `findUnusedClasses(JarMapping)` — program classes that never appear as a dependency of another program class in the forward graph (not entry-point analysis; reflection and external references can cause false positives).
- `findCircularDependencies(JarMapping)` — internal names participating in cycles.
- `getTopologicalOrder(JarMapping)` — ordering consistent with the forward graph when acyclic.
- `findDependents(mapping, internalClassName)` — program classes that reference the given type.
- `toDotFormat(graph, graphId)` — Graphviz DOT for a forward `Map<String, Set<String>>`.

### Plugin API

- **`Plugin`**: `getName`, `getVersion`, `getDescription`, `initialize()`, `process(JarMapping)`, `cleanup()`, optional `isEnabled()`, `getPriority()` (higher runs first).
- **`PluginManager`**: `registerPlugin`, `initializePlugins()` (throws `IllegalStateException` with **suppressed** causes if any plugin’s `initialize()` fails), `processWithPlugins(JarMapping)` returns `boolean` (`false` if any enabled plugin’s `process` threw); process/cleanup failures are **`java.util.logging` WARNING** with stack traces; processing continues with remaining plugins.

Bundled examples: `OptimizationPlugin`, `ObfuscationPlugin` (see `net.cvs0.bytecode.plugin.impl`).

### Analysis and utilities

- **`JarStatistics`**: aggregate counts (classes, methods, fields, resources, modifiers).
- **`BytecodeTraversal`**: walk method bodies when reconciling `ProgramMethod` / `ClassNode`.
- **`DescriptorRemapper`**: type and method descriptor rewriting when class names change.
- **`JarReader` / `JarWriter`**: low-level I/O; I/O failures surface as `IOException`.

---

## CLI documentation

Entry point: **`net.cvs0.bytecode.cli.BytecodeCli`** (`main` in shaded JAR).

Global Picocli options (all commands): **`-h`**, **`--help`**, **`-V`**, **`--version`**.

### Invocation

```bash
java -jar target/bytecode-processor-1.1-all.jar [COMMAND] [ARGS]
```

Without a subcommand, the root command prints usage to stdout.

### Commands

#### `analyze <JAR>`

Runs the same report as `JarAnalyzer.analyzeJar` (full textual analysis to stdout).

- **Arguments**: one positional `JAR` path.
- **Exit codes**: `0` success; `2` if the path is not a regular file.

#### `stats <JAR>`

Prints aggregate statistics from `JarStatistics`.

- **Arguments**: one positional `JAR` path.
- **Options**:
  - `--json` — single-line JSON with keys: `programClasses`, `libraryClasses`, `resources`, `interfaces`, `abstractClasses`, `finalClasses`, `publicClasses`, `methods`, `fields`.
- **Exit codes**: `0`; `2` if JAR missing.

#### `deps <JAR>`

Dependency summary from `DependencyAnalyzer`.

- **Arguments**: one positional `JAR` path.
- **Options**:
  - `--dot <path>` — write forward dependency graph as Graphviz DOT to `path`.
  - `--class <internalName>` — list program classes that depend on `internalName` (e.g. `com/foo/Bar`). Prints count and up to 50 names sorted.
- **Stdout**: node count, cycle-involved class count, optional dependent listing, optional DOT path confirmation.
- **Exit codes**: `0`; `2` if JAR missing.

Picocli parse errors use the library’s default exit codes (non-zero, typically `2`).

---

## Build and quality gates

```bash
mvn clean verify   # unit tests + integration test (shaded CLI) + JaCoCo + enforcer
mvn test           # unit tests only (no shaded JAR subprocess check)
mvn package        # library JAR + shaded `-all` JAR
```

Coverage report: `target/site/jacoco/index.html` after `verify`.

### CI and releases

- **GitHub Actions**: on pushes and pull requests targeting `main` or `master`, [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs `mvn -B verify` (build and test only; **no** Maven publish).
- **Releases**: after updating `<version>` in `pom.xml`, create and push a tag (e.g. `git tag -a v1.2.0 -m "1.2.0"` then `git push origin v1.2.0`). [`.github/workflows/release.yml`](.github/workflows/release.yml) runs **`mvn verify deploy`**, publishes **`net.cvs0:bytecode-processor`** (main JAR, POM, and the **`all`** shaded classifier) to [**GitHub Packages**](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry), and uploads both JARs to a GitHub Release for that tag.

**Consume the artifact from GitHub Packages** (Maven requires authentication to this registry even for public repos):

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/cvs0/bytecode-processor</url>
    </repository>
</repositories>
```

## Architecture

1. **ASM (tree)** — `ClassNode` / `MethodNode` graphs.
2. **Program model** — `ProgramClass`, `ProgramMethod`, `ProgramField` synchronized with ASM nodes where applicable.
3. **Analysis / transform** — graph algorithms, `ClassTransformer` scheduling, plugins.

---

## License

This project is licensed under the MIT License.
