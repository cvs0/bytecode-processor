package io.github.cvs0.bytecode.example;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.analysis.JarStatistics;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramField;

import java.nio.file.Path;

/**
 * Loads a JAR into a {@link JarMapping} and prints aggregate {@link JarStatistics}.
 *
 * <p>From {@code examples/demo-app}: {@code mvn exec:java -Dexec.args="path/to/sample.jar"} (install the library from
 * the repo root if you use a SNAPSHOT).
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

        for (ProgramClass clazz : mapping.getProgramClasses())
        {
            System.out.println(clazz.getName());

            for (ProgramField field : clazz.getFields())
            {
                System.out.println(field.getName());

                ProgramField newField = new ProgramField(field.getName() + "1", "Ljava/lang/String;", 0x0001);
                clazz.addField(newField);
            }
        }

        System.out.println("JAR: " + jar);
        System.out.println("Application:       " + stats.getApplicationClassCount());
        System.out.println("Embedded libs:     " + stats.getEmbeddedLibraryClassCount());
        System.out.println("Total classes:     " + stats.getTotalModeledClassCount());
        System.out.println("Library total:     " + stats.getLibraryClassCount());
        System.out.println("Resources:         " + stats.getResourceCount());
        System.out.println("Methods / fields:  " + stats.getTotalMethods() + " / " + stats.getTotalFields());
        System.out.println("Interfaces:        " + stats.getInterfaceCount());
        System.out.println("Module descriptors: " + stats.getModuleDescriptorCount());
        System.out.println("package-info:       " + stats.getPackageInfoCount());
    }

    private DemoApp() {}
}
