package io.github.cvs0.bytecode.example;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.analysis.JarStatistics;
import io.github.cvs0.bytecode.log.BPLogger;

import java.nio.file.Path;

/**
 * Loads a JAR into a {@link JarMapping} and prints aggregate {@link JarStatistics}.
 *
 * <p>From {@code examples/demo-app}: {@code mvn exec:java -Dexec.args="path/to/sample.jar"} (install the library from
 * the repo root if you use a SNAPSHOT).
 */
public final class DemoApp {

    private static final BPLogger LOG = BPLogger.of(DemoApp.class);

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            LOG.error("Usage: DemoApp <path-to.jar>");
            System.exit(2);
        }
        Path jar = Path.of(args[0]).toAbsolutePath().normalize();
        JarMapping mapping = JarMapping.fromJar(jar);
        JarStatistics stats = JarStatistics.from(mapping);

        LOG.info("JAR: %s", jar);
        LOG.info("Application:       %d", stats.getApplicationClassCount());
        LOG.info("Embedded libs:     %d", stats.getEmbeddedLibraryClassCount());
        LOG.info("Total classes:     %d", stats.getTotalModeledClassCount());
        LOG.info("Library total:     %d", stats.getLibraryClassCount());
        LOG.info("Resources:         %d", stats.getResourceCount());
        LOG.info("Methods / fields:  %d / %d", stats.getTotalMethods(), stats.getTotalFields());
        LOG.info("Interfaces:        %d", stats.getInterfaceCount());
        LOG.info("Module descriptors: %d", stats.getModuleDescriptorCount());
        LOG.info("package-info:       %d", stats.getPackageInfoCount());
    }

    private DemoApp() {}
}
