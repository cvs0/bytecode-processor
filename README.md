# Bytecode Processor

A comprehensive Java bytecode analysis and processing library that provides powerful tools for analyzing, transforming, and optimizing Java bytecode. Built on top of ASM, this library offers high-level abstractions for complex bytecode operations while maintaining fine-grained control over the transformation process.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Core Components](#core-components)
- [Usage Examples](#usage-examples)
- [Attribute System](#attribute-system)
- [Plugin System](#plugin-system)
- [Analysis Tools](#analysis-tools)
- [Transformation Capabilities](#transformation-capabilities)
- [Building and Testing](#building-and-testing)
- [API Reference](#api-reference)
- [Contributing](#contributing)
- [License](#license)

## Features

### 🔍 Dependency Analysis
- **Class-level dependency tracking** with inheritance and interface analysis
- **Method-level dependency extraction** from bytecode instructions
- **Topological sorting** using Kahn's algorithm for proper initialization order
- **Circular dependency detection** with cycle identification
- **Dead code analysis** for unused class and method detection
- **Dependency graph visualization** support

### 🔧 Code Transformation
- **Class renaming** with automatic reference updates
- **Method and field obfuscation** with configurable naming schemes
- **Instruction-level transformations** with ASM integration
- **Attribute manipulation** for debugging and metadata management
- **Exception handler modification** and optimization

### 🎯 Optimization
- **Dead code elimination** for unused methods and fields
- **Constant folding** and optimization
- **NOP instruction removal** and cleanup
- **Stack frame optimization** for reduced memory usage
- **Method complexity analysis** and reporting

### 🔌 Plugin Architecture
- **Extensible plugin system** for custom transformations
- **Built-in plugins** for common operations (obfuscation, optimization)
- **Configuration management** with type-safe parameter handling
- **Priority-based execution** for plugin ordering
- **Plugin lifecycle management** with initialization and cleanup

## Architecture

The library is structured around several core concepts:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   JarMapping    │────│  ProgramClass   │────│  ProgramMethod  │
│                 │    │                 │    │                 │
│ • Class storage │    │ • Fields        │    │ • Instructions  │
│ • Resource mgmt │    │ • Methods       │    │ • Local vars    │
│ • Metadata      │    │ • Attributes    │    │ • Line numbers  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ DependencyGraph │    │   Attributes    │    │  Instructions   │
│                 │    │                 │    │                 │
│ • Relationships │    │ • Code attr     │    │ • ASM nodes     │
│ • Cycles        │    │ • Debug info    │    │ • Transformers  │
│ • Ordering      │    │ • Annotations   │    │ • Analysis      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## How It Works - Technical Deep Dive

This section explores the fundamental mechanisms and algorithms that power the bytecode processor, explaining how complex bytecode operations are performed efficiently and reliably.

### The Bytecode Processing Pipeline

The entire system operates through a sophisticated multi-stage pipeline that transforms raw JAR files into analyzable and modifiable representations:

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────────┐
│   JAR File  │───▶│   Parsing   │───▶│  Analysis   │───▶│Transformation│
│             │    │             │    │             │    │              │
│ • Classes   │    │ • ASM Layer │    │ • Dep Graph │    │ • Renaming   │
│ • Resources │    │ • Metadata  │    │ • Cycles    │    │ • Injection  │
│ • Manifest  │    │ • Validation│    │ • Ordering  │    │ • Removal    │
└─────────────┘    └─────────────┘    └─────────────┘    └──────────────┘
                            │                   │                   │
                            ▼                   ▼                   ▼
                   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
                   │ Instruction │    │ Dependency  │    │ Reference   │
                   │  Analysis   │    │  Resolution │    │  Updating   │
                   └─────────────┘    └─────────────┘    └─────────────┘
```

### Bytecode Representation and Abstraction

#### Multi-Layer Architecture
The system employs a three-tier abstraction model that bridges the gap between raw bytecode and high-level operations:

**Layer 1: ASM Foundation**
At the lowest level, the system leverages the ASM library's ClassNode, MethodNode, and FieldNode structures. These provide direct access to bytecode instructions, constant pools, and metadata. However, ASM's API is complex and requires deep knowledge of JVM internals.

**Layer 2: Program Abstractions**
The middle layer wraps ASM nodes in ProgramClass, ProgramMethod, and ProgramField objects. These abstractions hide ASM complexity while preserving full bytecode access. Each wrapper maintains bidirectional synchronization with its underlying ASM node, ensuring changes propagate correctly.

**Layer 3: Analysis and Transformation**
The top layer provides domain-specific operations like dependency analysis, obfuscation, and optimization. This layer operates on collections of program abstractions and implements complex algorithms for bytecode manipulation.

#### Instruction Processing Model

Every bytecode instruction undergoes sophisticated analysis to extract semantic information:

```
Raw Bytecode Instruction
         │
         ▼
┌─────────────────┐
│  Instruction    │
│   Wrapper       │◄─── Maintains ASM node reference
│                 │
│ • Type Analysis │◄─── Categorizes instruction purpose
│ • Stack Effects │◄─── Calculates stack depth changes
│ • Dependencies  │◄─── Extracts class/method references
│ • Control Flow  │◄─── Identifies jumps and branches
└─────────────────┘
         │
         ▼
   Semantic Metadata
```

The system categorizes instructions into semantic groups (arithmetic, control flow, object manipulation, method calls) and tracks their effects on the JVM stack. This metadata enables sophisticated analysis without repeatedly parsing raw bytecode.

### Dependency Analysis Engine

#### Graph Construction Strategy

The dependency analyzer employs a multi-phase approach to build comprehensive relationship maps:

**Phase 1: Structural Dependencies**
The system first extracts obvious dependencies from class declarations - superclasses, interfaces, field types, and method signatures. These form the backbone of the dependency graph and represent compile-time relationships that must be preserved.

**Phase 2: Behavioral Dependencies**
Next, the analyzer examines method bodies, parsing every instruction to identify runtime dependencies. Method calls, field accesses, object instantiations, and type checks all create behavioral dependencies that may not be apparent from class signatures alone.

**Phase 3: Transitive Resolution**
Finally, the system resolves transitive dependencies by following chains of relationships. If class A depends on B, and B depends on C, then A transitively depends on C. This phase is crucial for understanding the full impact of modifications.

#### Topological Ordering Algorithm

The system uses Kahn's algorithm for dependency ordering, enhanced with cycle detection:

```
Dependency Graph Processing:

1. Calculate In-Degrees
   ┌─────┐     ┌─────┐     ┌─────┐
   │  A  │────▶│  B  │────▶│  C  │
   │(0)  │     │(1)  │     │(1)  │
   └─────┘     └─────┘     └─────┘
      │           │
      ▼           ▼
   ┌─────┐     ┌─────┐
   │  D  │     │  E  │
   │(1)  │     │(1)  │
   └─────┘     └─────┘

2. Process Zero In-Degree Nodes
   Queue: [A] → Process A → Queue: [B, D]
   
3. Update Dependencies
   Remove A's edges → Update in-degrees
   
4. Repeat Until Complete
   Final Order: A → B → D → E → C
```

#### Circular Dependency Detection

The system implements Tarjan's strongly connected components algorithm to identify circular dependencies. This algorithm performs a depth-first search while maintaining a stack of visited nodes, detecting back edges that indicate cycles.

When cycles are found, the system provides detailed reports showing the exact dependency chains that create the circular references. This information is crucial for developers to understand and resolve complex dependency issues.

### Transformation Engine Architecture

#### Reference Tracking System

The transformation engine maintains comprehensive mappings of all references throughout the codebase:

```
Reference Tracking Database:

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Class Names   │    │  Method Names   │    │  Field Names    │
│                 │    │                 │    │                 │
│ Old → New       │    │ Class.method    │    │ Class.field     │
│ com/A → com/a   │    │ → newName       │    │ → newName       │
│ com/B → com/b   │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │   Reference Validator   │
                    │                         │
                    │ • Ensures consistency   │
                    │ • Detects broken refs   │
                    │ • Validates integrity   │
                    └─────────────────────────┘
```

#### Multi-Phase Transformation Process

Transformations occur in carefully orchestrated phases to maintain bytecode integrity:

**Phase 1: Planning**
The system analyzes all requested transformations and builds an execution plan. This phase identifies potential conflicts, orders operations to avoid reference breaks, and validates that the transformation set is internally consistent.

**Phase 2: Direct Updates**
Simple transformations like class renaming are applied first. These operations update the primary definitions without affecting references, establishing the new names in the system.

**Phase 3: Reference Propagation**
The system then propagates changes throughout the codebase, updating every reference to renamed elements. This includes method calls, field accesses, type annotations, exception handlers, and constant pool entries.

**Phase 4: Bytecode Synchronization**
Finally, the system ensures that all bytecode instructions reflect the changes. This involves updating instruction operands, recalculating offsets, and maintaining the integrity of jump targets and exception handlers.

### Attribute Processing Framework

#### Comprehensive Attribute Support

Java bytecode contains numerous attributes that store metadata, debugging information, and runtime data. The system provides specialized handling for each attribute type:

```
Attribute Processing Hierarchy:

                    ┌─────────────────┐
                    │   Base Attribute│
                    │                 │
                    │ • Name          │
                    │ • Data          │
                    │ • Properties    │
                    └─────────────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
    ┌───────────────┐ ┌─────────────┐ ┌─────────────┐
    │ Debug Attrs   │ │Runtime Attrs│ │Struct Attrs │
    │               │ │             │ │             │
    │• LineNumbers  │ │• Annotations│ │• Code       │
    │• LocalVars    │ │• Signatures │ │• Exceptions │
    │• SourceFile   │ │• InnerClass │ │• Bootstrap  │
    └───────────────┘ └─────────────┘ └─────────────┘
```

#### Attribute Synchronization

During transformations, attributes must remain synchronized with their associated code elements. The system maintains this consistency through:

**Dependency Tracking**: Each attribute maintains references to the code elements it describes. When those elements change, the system automatically updates the attribute data.

**Validation Chains**: After transformations, the system validates that all attributes contain consistent information. Line number tables must reference valid instruction offsets, local variable tables must use correct variable indices, and signature attributes must match actual generic types.

**Lazy Regeneration**: Some attributes (like stack map frames) are computationally expensive to maintain. The system marks these for lazy regeneration, recalculating them only when the bytecode is finalized.

### Plugin Architecture Implementation

#### Plugin Lifecycle Management

The plugin system implements a sophisticated lifecycle with dependency resolution and error isolation:

```
Plugin Execution Pipeline:

Registration → Dependency → Initialization → Execution → Cleanup
     │         Resolution        │             │          │
     ▼             │             ▼             ▼          ▼
┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐ ┌─────────┐
│Register │   │ Resolve │   │Initialize│   │ Process │ │ Cleanup │
│Plugins  │──▶│ Order   │──▶│ State   │──▶│ JAR     │▶│Resources│
│         │   │         │   │         │   │         │ │         │
└─────────┘   └─────────┘   └─────────┘   └─────────┘ └─────────┘
                   │
                   ▼
            ┌─────────────┐
            │   Priority  │
            │   Sorting   │
            │             │
            │ High → Low  │
            └─────────────┘
```

#### Configuration Management

The plugin system provides type-safe configuration with runtime validation:

**Schema Definition**: Each plugin defines its configuration schema, specifying parameter types, default values, and validation rules. This schema serves as both documentation and runtime validation.

**Type Safety**: Configuration parameters are strongly typed, preventing common errors like passing strings where integers are expected. The system performs type checking at configuration time, not during plugin execution.

**Validation Chains**: Complex validation rules can be chained together, allowing plugins to implement sophisticated parameter validation. For example, a file path parameter might be validated for existence, readability, and format compliance.

### Memory Management and Performance

#### Lazy Loading Strategy

The system implements sophisticated lazy loading to minimize memory usage:

```
Memory Management Strategy:

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Class Cache   │    │ Instruction     │    │  Dependency     │
│                 │    │    Cache        │    │    Cache        │
│ • Weak Refs     │    │                 │    │                 │
│ • LRU Eviction  │    │ • Method-level  │    │ • Graph Nodes   │
│ • Background    │    │ • Lazy Parsing  │    │ • Edge Lists    │
│   Preloading    │    │ • ASM Wrappers  │    │ • Cycle Cache   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │   Memory Monitor        │
                    │                         │
                    │ • Usage Tracking        │
                    │ • Pressure Detection    │
                    │ • Automatic Cleanup     │
                    └─────────────────────────┘
```

#### Performance Optimization Techniques

**Instruction Caching**: Frequently accessed instructions are cached with their analyzed metadata, avoiding repeated parsing and analysis.

**Background Preloading**: The system predicts which classes will be needed based on dependency relationships and preloads them in background threads.

**Incremental Analysis**: When possible, the system performs incremental analysis, updating only the portions of dependency graphs that are affected by changes.

### Error Handling and Validation

#### Multi-Level Validation System

The system implements comprehensive validation at multiple levels:

**Structural Validation**: Ensures that class files conform to JVM specifications, with proper constant pools, valid access flags, and correct attribute structures.

**Reference Validation**: Verifies that all class, method, and field references point to valid targets. This includes checking inheritance hierarchies, interface implementations, and method overrides.

**Bytecode Validation**: Analyzes instruction sequences for validity, including stack depth consistency, type safety, and proper exception handler coverage.

**Semantic Validation**: Performs higher-level checks like ensuring that abstract methods are properly implemented and that final classes aren't extended.

#### Error Recovery and Reporting

When errors are detected, the system provides detailed diagnostic information:

**Error Context**: Each error report includes the specific location where the problem was detected, including class names, method signatures, and instruction offsets.

**Impact Analysis**: The system analyzes how errors might affect other parts of the codebase, providing warnings about potential cascading failures.

**Recovery Suggestions**: Where possible, the system suggests specific actions to resolve detected problems, such as adding missing dependencies or correcting invalid references.

This comprehensive technical architecture ensures that the bytecode processor can handle complex Java applications reliably while providing the flexibility needed for sophisticated analysis and transformation tasks.

## Core Components

### JarMapping
Central container for managing classes and resources within a JAR file:

```java
JarMapping mapping = new JarMapping("application.jar");

// Access classes
Collection<ProgramClass> classes = mapping.getProgramClasses();
ProgramClass mainClass = mapping.getProgramClass("com/example/Main");

// Manage resources
mapping.addResource("config.properties", configData);
byte[] manifest = mapping.getResource("META-INF/MANIFEST.MF");

// Class operations
mapping.addClass(newClass);
mapping.removeClass("com/example/UnusedClass");
mapping.renameClass("OldName", "NewName");
```

### ProgramClass
Represents a single class with full metadata and member access:

```java
ProgramClass clazz = new ProgramClass("com/example/MyClass");

// Class metadata
clazz.setSuperName("java/lang/Object");
clazz.addInterface("java/lang/Runnable");
clazz.setAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);

// Members
clazz.addField(new ProgramField("counter", "I", Opcodes.ACC_PRIVATE));
clazz.addMethod(new ProgramMethod("increment", "()V", Opcodes.ACC_PUBLIC));

// Attributes
clazz.addAttribute(new SourceFileAttribute("MyClass.java"));
clazz.addAttribute(new SignatureAttribute("<T:Ljava/lang/Object;>"));
```

### ProgramMethod
Detailed method representation with instruction-level access:

```java
ProgramMethod method = clazz.getMethod("processData", "(Ljava/lang/String;)I");

// Method properties
String signature = method.getSignature();
boolean isStatic = method.isStatic();
int complexity = method.getInstructionCount();

// Instructions
List<Instruction> instructions = method.getInstructions();
method.addInstruction(new Instruction(new InsnNode(Opcodes.RETURN)));

// Local variables and debugging
LocalVariableTableAttribute lvt = method.getLocalVariableTableAttribute();
List<LocalVariable> variables = method.getLocalVariables();
```

## Usage Examples

### Basic Dependency Analysis

```java
// Load and analyze a JAR file
JarMapping mapping = new JarMapping("myapp.jar");

// Build complete dependency graph
Map<String, Set<String>> dependencies = DependencyAnalyzer.buildDependencyGraph(mapping);

// Analyze specific class
ProgramClass targetClass = mapping.getProgramClass("com/example/Service");
Set<String> classDeps = DependencyAnalyzer.findClassDependencies(targetClass);

// Get build order
List<String> topologicalOrder = DependencyAnalyzer.getTopologicalOrder(mapping);
System.out.println("Classes in dependency order: " + topologicalOrder);

// Detect problems
Set<String> circularDeps = DependencyAnalyzer.findCircularDependencies(mapping);
Set<String> unusedClasses = DependencyAnalyzer.findUnusedClasses(mapping);

if (!circularDeps.isEmpty()) {
    System.err.println("Circular dependencies found: " + circularDeps);
}
```

### Advanced Code Transformation

```java
// Create transformer
ClassTransformer transformer = new ClassTransformer(mapping);

// Rename elements
transformer.renameClass("com/example/Service", "com/example/a");
transformer.renameMethod("com/example/Service", "processData", "(Ljava/lang/String;)I", "a");
transformer.renameField("com/example/Service", "cache", "b");

// Apply transformations
transformer.applyTransformations();

// Verify changes
ProgramClass renamedClass = mapping.getProgramClass("com/example/a");
assert renamedClass != null;
assert renamedClass.getMethod("a", "(Ljava/lang/String;)I") != null;
```

### Plugin-Based Processing

```java
// Create plugin manager
PluginManager pluginManager = new PluginManager();

// Configure obfuscation plugin
ObfuscationPlugin obfuscator = new ObfuscationPlugin();
Map<String, Object> obfConfig = Map.of(
    "obfuscateClasses", true,
    "obfuscateMethods", true,
    "obfuscateFields", true,
    "namePrefix", "a",
    "seed", 12345
);
obfuscator.configure(obfConfig);

// Configure optimization plugin
OptimizationPlugin optimizer = new OptimizationPlugin();
Map<String, Object> optConfig = Map.of(
    "removeNops", true,
    "optimizeConstants", true,
    "removeUnusedMethods", true
);
optimizer.configure(optConfig);

// Register and execute
pluginManager.registerPlugin(obfuscator);
pluginManager.registerPlugin(optimizer);
pluginManager.processWithPlugins(mapping);
```

## Attribute System

The library provides comprehensive support for Java bytecode attributes:

### Built-in Attributes

| Attribute | Purpose | Usage |
|-----------|---------|-------|
| `CodeAttribute` | Method bytecode and exception handlers | Instruction analysis |
| `LocalVariableTableAttribute` | Debug information for variables | Variable name mapping |
| `LineNumberTableAttribute` | Source line mapping | Debug support |
| `SignatureAttribute` | Generic type information | Type analysis |
| `AnnotationAttribute` | Runtime annotations | Metadata processing |
| `ExceptionsAttribute` | Checked exception declarations | Exception analysis |
| `InnerClassesAttribute` | Inner class relationships | Class hierarchy |
| `BootstrapMethodsAttribute` | Dynamic method invocation | Lambda support |
| `MethodParametersAttribute` | Parameter names and flags | Reflection support |

### Working with Attributes

```java
// Create attributes using factory
LocalVariableTableAttribute lvt = AttributeFactory.createLocalVariableTable(methodNode.localVariables);
CodeAttribute code = AttributeFactory.createCodeAttribute(methodNode);
SourceFileAttribute source = AttributeFactory.createSourceFileAttribute("Example.java");

// Add to method or class
method.addAttribute(lvt);
method.addAttribute(code);
clazz.addAttribute(source);

// Query attributes
boolean hasDebugInfo = method.hasAttribute("LocalVariableTable");
SignatureAttribute signature = method.getSignatureAttribute();
List<Attribute> debugAttrs = method.getAttributesByType(LocalVariableTableAttribute.class);

// Attribute classification
boolean isDebug = AttributeFactory.isDebugAttribute(attribute);
boolean isRuntime = AttributeFactory.isRuntimeAttribute(attribute);
```

## Plugin System

### Creating Custom Plugins

```java
public class MyCustomPlugin extends AbstractPlugin {
    public MyCustomPlugin() {
        super("Custom Plugin", "1.0.0", "Performs custom transformations");
    }
    
    @Override
    public void initialize() {
        super.initialize();
        // Plugin initialization logic
    }
    
    @Override
    public void process(JarMapping mapping) {
        boolean enabled = getBooleanConfig("enabled", true);
        String prefix = getStringConfig("prefix", "custom");
        
        if (!enabled) return;
        
        for (ProgramClass clazz : mapping.getProgramClasses()) {
            // Custom processing logic
            processClass(clazz, prefix);
        }
    }
    
    @Override
    public int getPriority() {
        return 50; // Medium priority
    }
    
    private void processClass(ProgramClass clazz, String prefix) {
        // Implementation details
    }
}
```

### Plugin Configuration

```java
Map<String, Object> config = new HashMap<>();
config.put("enabled", true);
config.put("prefix", "transformed");
config.put("maxIterations", 10);
config.put("debugMode", false);

plugin.configure(config);
```

## Analysis Tools

### Unused Code Analysis

```java
// Find unused methods
Set<String> unusedMethods = UnusedCodeAnalyzer.findUnusedMethods(mapping);

// Calculate method complexity
Map<String, Integer> complexity = UnusedCodeAnalyzer.getMethodComplexity(mapping);

// Identify entry points
Set<String> entryPoints = UnusedCodeAnalyzer.findEntryPoints(mapping);

// Generate usage report
UnusedCodeAnalyzer.generateReport(mapping, "unused-code-report.txt");
```

### Dependency Visualization

```java
// Export dependency graph
Map<String, Set<String>> graph = DependencyAnalyzer.buildDependencyGraph(mapping);
DependencyGraphExporter.exportToDot(graph, "dependencies.dot");
DependencyGraphExporter.exportToJson(graph, "dependencies.json");

// Analyze dependency metrics
int maxDepth = DependencyAnalyzer.getMaxDependencyDepth(mapping);
double avgFanOut = DependencyAnalyzer.getAverageFanOut(mapping);
List<String> mostDependent = DependencyAnalyzer.getMostDependentClasses(mapping, 10);
```

## Transformation Capabilities

### Method Transformation

```java
// Transform method instructions
transformer.transformMethods(method -> {
    // Add logging to method entry
    method.insertInstruction(0, new Instruction(
        new MethodInsnNode(INVOKESTATIC, "Logger", "enter", "(Ljava/lang/String;)V")
    ));
    
    // Modify access flags
    method.setAccess(method.getAccess() | ACC_SYNCHRONIZED);
    
    return method;
});
```

### Field Transformation

```java
// Transform field properties
transformer.transformFields(field -> {
    // Make all fields private
    field.setAccess((field.getAccess() & ~ACC_PUBLIC & ~ACC_PROTECTED) | ACC_PRIVATE);
    
    // Add getter/setter methods if needed
    if (!field.isStatic() && !field.isFinal()) {
        generateAccessors(field);
    }
    
    return field;
});
```

### Class Hierarchy Modification

```java
// Modify class relationships
for (ProgramClass clazz : mapping.getProgramClasses()) {
    // Add common interface
    if (!clazz.isInterface() && !clazz.hasInterface("com/example/Trackable")) {
        clazz.addInterface("com/example/Trackable");
    }
    
    // Modify superclass
    if ("java/lang/Object".equals(clazz.getSuperName())) {
        clazz.setSuperName("com/example/BaseClass");
    }
}
```

## Building and Testing

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Build Commands

```bash
# Clean and compile
mvn clean compile

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DependencyAnalyzerTest

# Run integration tests
mvn test -Dtest=*IntegrationTest

# Generate test coverage report
mvn jacoco:report

# Package JAR
mvn package

# Install to local repository
mvn install
```

### Test Categories

- **Unit Tests**: Individual component testing
- **Integration Tests**: End-to-end workflow testing
- **Performance Tests**: Scalability and performance benchmarks
- **Compatibility Tests**: Cross-version compatibility validation

## API Reference

### Core Classes

| Class | Package | Description |
|-------|---------|-------------|
| `JarMapping` | `net.cvs0.bytecode` | Main container for JAR contents |
| `ProgramClass` | `net.cvs0.bytecode.clazz` | Class representation |
| `ProgramMethod` | `net.cvs0.bytecode.member` | Method representation |
| `ProgramField` | `net.cvs0.bytecode.member` | Field representation |
| `DependencyAnalyzer` | `net.cvs0.bytecode.analysis` | Dependency analysis utilities |
| `ClassTransformer` | `net.cvs0.bytecode.transform` | Code transformation engine |
| `PluginManager` | `net.cvs0.bytecode.plugin` | Plugin management system |

### Utility Classes

| Class | Package | Description |
|-------|---------|-------------|
| `AttributeFactory` | `net.cvs0.bytecode.attribute` | Attribute creation utilities |
| `UnusedCodeAnalyzer` | `net.cvs0.bytecode.analysis` | Dead code detection |

### Configuration Options

#### Obfuscation Plugin
- `obfuscateClasses` (boolean): Enable class name obfuscation
- `obfuscateMethods` (boolean): Enable method name obfuscation  
- `obfuscateFields` (boolean): Enable field name obfuscation
- `namePrefix` (string): Prefix for generated names
- `seed` (int): Random seed for consistent results

#### Optimization Plugin
- `removeNops` (boolean): Remove NOP instructions
- `optimizeConstants` (boolean): Perform constant folding
- `removeUnusedMethods` (boolean): Remove unused methods
- `optimizeStackFrames` (boolean): Optimize stack frame usage

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Clone your fork: `git clone https://github.com/cvs0/bytecode-processor.git`
3. Create a feature branch: `git checkout -b feature/amazing-feature`
4. Make your changes and add tests
5. Run the test suite: `mvn test`
6. Commit your changes: `git commit -m 'Add amazing feature'`
7. Push to your branch: `git push origin feature/amazing-feature`
8. Open a Pull Request

### Code Style
- Follow Java naming conventions
- Add Javadoc for public APIs
- Include unit tests for new functionality
- Maintain backward compatibility when possible

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built on top of the [ASM](https://asm.ow2.io/) bytecode manipulation framework
- Inspired by various bytecode analysis and transformation tools
- Thanks to all contributors and users of this library
