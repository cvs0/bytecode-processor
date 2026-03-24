# Examples

| Directory | What it is |
|-----------|------------|
| **local-app/** | Maven app: **`bytecode-processor`** is a **`system`** dependency on **`lib/bytecode-processor-local.jar`**. The root build copies the current **`target/bytecode-processor-${revision}.jar`** there during **`mvn package`** / **`verify`**, so you never edit version strings by hand. ASM and Picocli come from Maven Central. **`mvn verify`** here runs **`FatJarObfuscationIT`** (merge deps, **`ObfuscationPlugin`**, **`java -jar`** smoke test). |
| **demo-app/** | Same stats idea; resolves the library from Central or your local repo after **`mvn install`**. |
| **obfuscate-app/** | Maven driver for **`ObfuscateApp`**: obfuscate any **`input.jar`** into a runnable fat **`output.jar`** by passing **`--lib`** / **`--libDir`** for every runtime dependency (same set you would put on **`java -cp`**). The plugin rewrites merged library bytecode so it still calls your renamed classes, and patches **`Main-Class`**, **`Start-Class`**, and **`META-INF/services/*`** when those name program classes. |

## Obfuscating an external application JAR

```bash
mvn -q install   # once, if the library is not in Central

mvn -q -f examples/obfuscate-app/pom.xml exec:java \
  "-Dexec.args=C:/path/to/app.jar C:/path/to/app-obf.jar --libDir C:/path/to/app/libs"
```

Use **`--lib`** for individual JARs. Run **`ObfuscateApp --help`** for the full option list.

## Local copy workflow

```bash
# repository root — packages the library and refreshes examples/local-app/lib/bytecode-processor-local.jar
mvn -q verify

# example module
mvn -q -f examples/local-app/pom.xml verify
```

Run the demo only:

```bash
mvn -q -f examples/local-app/pom.xml exec:java -Dexec.args="/absolute/path/to/any.jar"
```
