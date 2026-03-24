package io.github.cvs0.bytecode.example.local;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.analysis.JarStatistics;

import java.nio.file.Path;

/**
 * Same idea as {@link io.github.cvs0.bytecode.example.DemoApp}. The library JAR is wired via {@code system} scope using
 * {@code lib/bytecode-processor-local.jar}, which the repository root build copies from {@code target/} when you run
 * {@code mvn package} or {@code verify} there.
 */
public final class LocalDemoApp {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: LocalDemoApp <path-to.jar>");
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

    private LocalDemoApp() {}
}
