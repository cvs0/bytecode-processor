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
| `bytecode-processor-${revision}.jar` | **Library**: modular JAR (`module bytecode.processor`). Resolved version from `${revision}` in `pom.xml` (flattened at build time). Does **not** embed ASM or Picocli. |
| `bytecode-processor-${revision}-all.jar` | **Runnable CLI**: shaded uber-JAR (ASM stack + Picocli). No `module-info`; run with `java -jar`. |

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

Via [JitPack](https://jitpack.io/#cvs0/bytecode-processor) (use a Git **tag** such as `v1.2.0`, a **branch**, or a **commit** as `<version>`):

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>net.cvs0</groupId>
    <artifactId>bytecode-processor</artifactId>
    <version>v1.2.0</version>
</dependency>
```

Replace the version with a **Git tag** (e.g. `v1.2.0`), **branch name**, or **commit hash** — [JitPack](https://jitpack.io) builds this repo on demand. Add the JitPack repository and use coordinates from your `pom.xml` (`net.cvs0:bytecode-processor`). For the shaded CLI artifact, use classifier `all`. See [CI and releases](#ci-and-releases). For a local `mvn install` build, no extra repository is needed.

This project uses **Lombok** (`provided` scope) for generated accessors on a few types (e.g. `AbstractPlugin`, `InnerClass`). Install the [Lombok plugin](https://projectlombok.org/setup/) for your IDE so code navigation and completion stay accurate.

Your module (or automatic module) must **read** ASM tree and, if you use the CLI types, Picocli. The library module declares:

- `requires org.objectweb.asm.tree;`
- `requires java.logging;`
- `requires info.picocli;` (compile-time for `net.cvs0.bytecode.cli`, which is not exported)

**Module path example** (application `module app { requires bytecode.processor; … }`):

```bash
java --module-path lib/bytecode-processor-1.2.0-SNAPSHOT.jar:lib/asm-tree-9.7.jar:... -m app/com.example.Main
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

Bundled examples: `OptimizationPlugin`, `ObfuscationPlugin` in `net.cvs0.bytecode.plugin.impl` (see [Example plugins](#example-plugins) under Contributing).

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
java -jar target/bytecode-processor-*-all.jar [COMMAND] [ARGS]
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

- **GitHub Actions** ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)): on pushes and pull requests for **`main`**, **`master`**, **`develop`**, **`development`**, and **`release/**`**, runs **`mvn -B verify`**. Pushes of a **`v*`** tag run verify with **`-Drevision`** derived from the tag (no leading `v`) and attach the library and **`-all`** shaded JARs to a **GitHub Release**. Use **workflow_dispatch** in the Actions tab for a manual run (same verify logic; no release upload unless the ref is a `v*` tag).
- **Maven / Gradle consumers**: artifacts are **not** deployed from CI to a custom registry. Use [**JitPack**](https://jitpack.io) against this GitHub repo (`cvs0/bytecode-processor`). Configuration lives in [`jitpack.yml`](jitpack.yml) (JDK 21, full **`mvn install`** including tests); version tags like `v1.2.0` map to `-Drevision=1.2.0` during the JitPack build.

[![JitPack](https://jitpack.io/v/cvs0/bytecode-processor.svg)](https://jitpack.io/#cvs0/bytecode-processor)

**Consume via JitPack** (no authentication for public GitHub repos):

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>net.cvs0</groupId>
    <artifactId>bytecode-processor</artifactId>
    <version>v1.2.0</version>
</dependency>
```

Shaded CLI (`-all` JAR) as a classified artifact:

```xml
<dependency>
    <groupId>net.cvs0</groupId>
    <artifactId>bytecode-processor</artifactId>
    <version>v1.2.0</version>
    <classifier>all</classifier>
</dependency>
```

Use a **commit hash** or **branch name** as `<version>` for snapshots-style consumption; see [JitPack versioning](https://docs.jitpack.io/).

---

## Contributing

### Branch layout

| Branch pattern | Purpose |
|----------------|---------|
| **`main`** | Default branch. Production-ready code; `<version>` in `pom.xml` matches what you intend to ship next. Merge only via pull request (reviews, CI green). |
| **`develop`** / **`development`** | Optional integration lines (this repo runs CI on both names). Merge features here first, then merge into `main` at release time, or use **trunk-based** flow and target **`main`** only. |
| **`release/x.y.z`** | Short-lived stabilization (e.g. `release/1.2.0`). Branch from `main` or `develop` when the version is frozen; only bugfixes and release prep; merge back to `main` (and `develop` if used), then tag. |
| **`v1.x`** (example: `v1.1`) | **Maintenance** for an already-shipped major/minor line. Patch releases (`1.1.1`, `1.1.2`) happen here; cherry-pick or merge fixes from `main` as appropriate, bump patch version, tag `v1.1.1`, etc. |
| **`feature/…`** | New work. Branch from `main` (or `develop` if you use it). Name briefly: `feature/deps-dot-export`, `fix/jar-writer-manifest`. |
| **`hotfix/…`** | Urgent production fix. Branch from `main` (or from the active **`v*.*`** line if the hotfix is for that line only). |

**Tags** (`v1.2.0`, …) mark immutable releases, drive CI **GitHub Release** uploads, and are the usual **JitPack** version selectors. Do not rewrite published tags.

### Quick setup (maintainer)

Create the integration branch once and publish it:

```bash
git fetch origin
git checkout main
git pull origin main
git branch develop main          # optional integration branch
git push -u origin develop
git branch development main      # optional alias (CI also watches `development`)
git push -u origin development
```

Create a maintenance line after a major/minor release (example: supporting `1.1.x` while `main` moves to `1.2`):

```bash
git checkout -b v1.1 main       # or the tag you released from
git push -u origin v1.1
```

Start a feature:

```bash
git checkout main && git pull
git checkout -b feature/my-change
# … commit …
git push -u origin feature/my-change
```

Then open a **pull request** on GitHub into `main` (or `develop`).

### Pull request checklist

- `mvn verify` passes locally (or rely on CI).
- For user-visible behavior, update **README** or CLI help if needed.
- Keep commits focused; follow existing style (see [`.editorconfig`](.editorconfig)).

### Versioning and `pom.xml`

The project version is driven by the **`revision`** property and the [**flatten-maven-plugin**](https://www.mojohaus.org/flatten-maven-plugin/) (`flattenMode: resolveCiFriendliesOnly`):

```xml
<version>${revision}</version>
<!-- in <properties>: -->
<revision>1.2.0-SNAPSHOT</revision>
```

- **Default (`main` / `development`)**: `<revision>1.2.0-SNAPSHOT</revision>` — next minor development line. Override on the CLI with `-Drevision=…` when needed.
- **Maintenance branch (`v1.1`, …)**: set `<revision>` to that line only, e.g. `1.1.1-SNAPSHOT`, until you cut a patch release.
- **Release commit**: drop `-SNAPSHOT` (e.g. `1.2.0`), tag `v1.2.0`, then bump `main` to the next SNAPSHOT (e.g. `1.3.0-SNAPSHOT`) on the branch that continues development.
- Align the Git tag with `${revision}` when cutting a release (CI and JitPack use `-Drevision` stripped from `v*` tags; see [`jitpack.yml`](jitpack.yml)).

### Example plugins

Reference implementations live under `net.cvs0.bytecode.plugin.impl`. They expose **configuration key constants** (e.g. `ObfuscationPlugin.CFG_NAME_PREFIX`, `OptimizationPlugin.CFG_REMOVE_NOPS`) and Javadoc tables describing each flag. Prefer these constants over string literals when wiring `PluginManager` / `configure(Map)`.

## Architecture

1. **ASM (tree)** — `ClassNode` / `MethodNode` graphs.
2. **Program model** — `ProgramClass`, `ProgramMethod`, `ProgramField` synchronized with ASM nodes where applicable.
3. **Analysis / transform** — graph algorithms, `ClassTransformer` scheduling, plugins.

---

## License

This project is licensed under the MIT License.
