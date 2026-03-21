# Bytecode Processor

A Java bytecode analysis and transformation library built on ASM.

## Features

- **Dependency Analysis**: Extract and analyze class/method dependencies with topological sorting
- **Code Transformation**: Rename classes, methods, fields with automatic reference updates
- **Optimization**: Dead code elimination, constant folding, NOP removal
- **Plugin System**: Extensible architecture for custom transformations
- **Attribute Support**: Comprehensive bytecode attribute handling

## Quick Start

```java
// Load and analyze a JAR
JarMapping mapping = JarMapping.fromJar("application.jar");

// Build dependency graph
Map<String, Set<String>> deps = DependencyAnalyzer.buildDependencyGraph(mapping);

// Transform code
ClassTransformer transformer = new ClassTransformer(mapping);
transformer.renameClass("com.example.OldClass", "com.example.NewClass");
transformer.applyTransformations();

// Save results
mapping.writeToJar("output.jar");
```

## Core Components

### JarMapping
Central container for JAR contents - classes, resources, and metadata.

### ProgramClass/ProgramMethod/ProgramField
High-level wrappers around ASM nodes with bidirectional synchronization.

### DependencyAnalyzer
Extracts dependencies from inheritance, interfaces, method signatures, and bytecode instructions.

### ClassTransformer
Handles renaming and reference updates across the entire codebase.

### Plugin System
```java
PluginManager manager = new PluginManager();
manager.registerPlugin(new ObfuscationPlugin());
manager.registerPlugin(new OptimizationPlugin());
manager.processWithPlugins(mapping);
```

## Build

```bash
mvn clean compile
mvn test
mvn package
```

## Requirements

- Java 21+
- Maven 3.6+

## Architecture

The library uses a three-layer architecture:
1. **ASM Foundation** - Low-level bytecode access
2. **Program Abstractions** - High-level wrappers around ASM nodes
3. **Analysis/Transformation** - Domain-specific operations

## License

This project is licensed under the MIT License.
