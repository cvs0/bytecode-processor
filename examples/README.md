# Examples

| Directory | What it is |
|-----------|------------|
| **demo-app/** | Maven app that loads a JAR and prints {@code JarStatistics}. Resolves the library from Maven Central or your local repo after {@code mvn install}. |
| **obfuscate-app/** | Maven driver for {@code ObfuscateApp}: obfuscate an {@code input.jar} into {@code output.jar}. Put application and dependency classes in one JAR (shaded/uber JAR) when you need a self-contained runnable; the plugin patches {@code Main-Class}, {@code Start-Class}, and {@code META-INF/services/*} when those name program classes. |

## Obfuscating an external application JAR

```bash
mvn -q install   # once, if the library is not in Central

mvn -q -f examples/obfuscate-app/pom.xml exec:java \
  "-Dexec.args=C:/path/to/app.jar C:/path/to/app-obf.jar --libDir C:/path/to/app/libs"
```

Use **`--lib`** for individual JARs. Run **`ObfuscateApp --help`** for the full option list.

## Run demo-app

```bash
mvn -q install
mvn -q -f examples/demo-app/pom.xml exec:java -Dexec.args="/absolute/path/to/any.jar"
```

Fat-JAR obfuscation is covered by **`ObfuscationRunnableJarIT`** in the main module’s tests (`mvn test` at the repository root).
