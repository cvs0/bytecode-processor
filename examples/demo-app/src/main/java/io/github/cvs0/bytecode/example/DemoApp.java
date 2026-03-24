package io.github.cvs0.bytecode.example;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.analysis.JarStatistics;

import java.nio.file.Path;

/**
 * Loads a JAR into a {@link JarMapping} and prints aggregate {@link JarStatistics}.
 *
 * <p>Build the library first from the repository root: {@code mvn install -DskipTests},
 * then run from {@code examples/demo-app}:
 * {@code mvn exec:java -Dexec.args="path/to/sample.jar"}.
 */
public final class DemoApp {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: DemoApp <path-to.jar>");
            System.exit(2);
        }
        Path jar = Path.of(args[0]).toAbsolutePath().normalize();
        JarMapping mapping = JarMapping.fromJar(jar);
        JarStatistics stats = JarStatistics.from(mapping);

        System.out.println("JAR: " + jar);
        System.out.println("Program classes:   " + stats.getProgramClassCount());
        System.out.println("Library classes:   " + stats.getLibraryClassCount());
        System.out.println("Resources:         " + stats.getResourceCount());
        System.out.println("Methods / fields:  " + stats.getTotalMethods() + " / " + stats.getTotalFields());
        System.out.println("Interfaces:        " + stats.getInterfaceCount());
        System.out.println("Module descriptors: " + stats.getModuleDescriptorCount());
        System.out.println("package-info:       " + stats.getPackageInfoCount());
    }

    private DemoApp() {}
}
