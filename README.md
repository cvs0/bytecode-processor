# Bytecode Processor

Java bytecode analysis and transformation on ASM’s tree API. Model a JAR as editable program classes and resources, analyze dependencies, and run coordinated rewrites (renames, descriptors, instructions, resources) through one `ClassTransformer` pipeline.

**Stack:** ASM `ClassNode` / `ProgramClass` model → `DependencyAnalyzer` and utilities → `ClassTransformer` + optional `Plugin`s. CLI is Picocli, shipped as a shaded JAR.

## Features

- Dependency graphs, cycles, reverse dependents, Graphviz DOT export
- Renames (class, method, field), package moves, instruction/LDC hooks, optional debug stripping
- Ordered plugins with `java.util.logging` diagnostics
- Picocli CLI for JAR analysis without embedding the library in your own code

## Requirements

JDK **21+** and Maven **3.6.3+** (enforced in the POM).

## Quick start

```bash
git clone https://github.com/cvs0/bytecode-processor.git
cd bytecode-processor
mvn clean verify
```

Artifacts in `target/` after `package` / `verify`:

| Output | Purpose |
|--------|---------|
| `bytecode-processor-${revision}.jar` | Library (`module bytecode.processor`). Does **not** bundle ASM or Picocli. |
| `bytecode-processor-${revision}-all.jar` | Runnable CLI (shaded). Use `java -jar …`. |

`${revision}` comes from `pom.xml` and is resolved at build time (flatten plugin).

### Maven dependency

Releases are on **Maven Central**. Replace `VERSION` with the latest release (see [Central](https://central.sonatype.com/) or repo tags).

```xml
<dependency>
    <groupId>io.github.cvs0</groupId>
    <artifactId>bytecode-processor</artifactId>
    <version>VERSION</version>
</dependency>
```

**Shaded CLI** as a classified artifact:

```xml
<classifier>all</classifier>
```

**Snapshots** (e.g. `1.2.1-SNAPSHOT`) are published to [GitHub Packages](https://github.com/cvs0/bytecode-processor/packages) on pushes to `development`. Add the `https://maven.pkg.github.com/cvs0/bytecode-processor` repository and authenticate as [GitHub documents for Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry).

**JitPack** is also supported: [jitpack.io/#cvs0/bytecode-processor](https://jitpack.io/#cvs0/bytecode-processor).

### IDE / modules

The project uses **Lombok** (`provided`). Install the [Lombok IDE support](https://projectlombok.org/setup/) for navigation.

Consumers need ASM on the module or classpath. The library `requires org.objectweb.asm.tree`, `java.logging`, and `info.picocli` (for the non-exported CLI package). Exported packages are listed in [`module-info.java`](src/main/java/module-info.java); `io.github.cvs0.bytecode.cli` is opened to Picocli only.

## Using the library

**Naming:** internal names use JVM slash form (`com/example/Foo`, inner classes `Outer$Inner`). Method rename keys are `className + "." + name + descriptor` (raw descriptor, e.g. `(I)V`). `renamePackage` accepts internal or dot prefixes.

**Typical flow:**

1. `JarMapping.fromJar(path)` — load classes and resources.
2. Analyze or inspect (`DependencyAnalyzer`, `JarStatistics`, etc.).
3. `ClassTransformer(mapping)` — queue changes, then **`applyTransformations()`** once.
4. `mapping.writeToJar(path)` — write the JAR.

**Details** (apply order, manifest/resource behavior, thread safety): see Javadoc on `ClassTransformer`, `JarWriter`, and `JarMapping`.

**Plugins:** implement `Plugin` (`initialize`, `process(JarMapping)`, `cleanup`, optional priority). Register with `PluginManager`. Reference configs live under `io.github.cvs0.bytecode.plugin.impl` (`OptimizationPlugin`, `ObfuscationPlugin`).

## CLI

```bash
java -jar target/bytecode-processor-*-all.jar [COMMAND] [ARGS]
```

Global options: `-h` / `--help`, `-V` / `--version`. Without a subcommand, usage is printed.

| Command | What it does |
|---------|----------------|
| `analyze <jar>` | Full textual report (`JarAnalyzer`-style). Exit `2` if path is not a file. |
| `stats <jar>` | Aggregates; add `--json` for one-line stats. Exit `2` if missing. |
| `deps <jar>` | Dependency summary; `--dot <path>` for DOT; `--class <internalName>` for dependents (up to 50). Exit `2` if missing. |

## Development

```bash
mvn verify    # tests + integration (shaded CLI) + JaCoCo + enforcer
mvn test      # unit tests only
mvn package   # library + shaded JAR
```

Coverage: `target/site/jacoco/index.html` after `verify`.

**CI** ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)): `mvn -B verify` on pushes/PRs to `main`, `master`, `develop`, `development`, and `release/**`. Tags `v*` attach both JARs to a GitHub Release.

**Publish** ([`.github/workflows/publish.yml`](.github/workflows/publish.yml)): tag `v*` → Maven Central; push to `development` → snapshot to GitHub Packages.

**Optional git hooks:** `git config core.hooksPath .githooks` (on Unix, `chmod +x .githooks/pre-commit`). Pre-commit runs `mvn test`.

**Contributing:** `mvn verify` green; keep changes focused; match existing style ([`.editorconfig`](.editorconfig)). Version is `${revision}` in the POM (flattened); release tags should match the published version.

## License

MIT.
