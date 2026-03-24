package net.cvs0.bytecode.analysis;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;

import java.util.Collection;

/**
 * Aggregated counts for a {@link JarMapping}: classes, members, resources, and simple structural flags.
 */
public final class JarStatistics {
    private final int programClassCount;
    private final int libraryClassCount;
    private final int resourceCount;
    private final int interfaceCount;
    private final int abstractClassCount;
    private final int finalClassCount;
    private final int publicClassCount;
    private final int totalMethods;
    private final int totalFields;
    private final int moduleDescriptorCount;
    private final int packageInfoCount;

    private JarStatistics(
            int programClassCount,
            int libraryClassCount,
            int resourceCount,
            int interfaceCount,
            int abstractClassCount,
            int finalClassCount,
            int publicClassCount,
            int totalMethods,
            int totalFields,
            int moduleDescriptorCount,
            int packageInfoCount) {
        this.programClassCount = programClassCount;
        this.libraryClassCount = libraryClassCount;
        this.resourceCount = resourceCount;
        this.interfaceCount = interfaceCount;
        this.abstractClassCount = abstractClassCount;
        this.finalClassCount = finalClassCount;
        this.publicClassCount = publicClassCount;
        this.totalMethods = totalMethods;
        this.totalFields = totalFields;
        this.moduleDescriptorCount = moduleDescriptorCount;
        this.packageInfoCount = packageInfoCount;
    }

    /**
     * Computes statistics from the given mapping.
     */
    public static JarStatistics from(JarMapping mapping) {
        Collection<ProgramClass> classes = mapping.getProgramClasses();
        int interfaces = 0;
        int abstractClasses = 0;
        int finals = 0;
        int publics = 0;
        int methods = 0;
        int fields = 0;
        for (ProgramClass clazz : classes) {
            if (clazz.isInterface()) {
                interfaces++;
            }
            if (clazz.isAbstract()) {
                abstractClasses++;
            }
            if (clazz.isFinal()) {
                finals++;
            }
            if (clazz.isPublic()) {
                publics++;
            }
            methods += clazz.getMethods().size();
            fields += clazz.getFields().size();
        }
        return new JarStatistics(
                classes.size(),
                mapping.getLibraryClasses().size(),
                mapping.getResourceCount(),
                interfaces,
                abstractClasses,
                finals,
                publics,
                methods,
                fields,
                mapping.getModuleInfoCount(),
                mapping.getPackageInfoCount());
    }

    public int getProgramClassCount() {
        return programClassCount;
    }

    public int getLibraryClassCount() {
        return libraryClassCount;
    }

    public int getResourceCount() {
        return resourceCount;
    }

    public int getInterfaceCount() {
        return interfaceCount;
    }

    public int getAbstractClassCount() {
        return abstractClassCount;
    }

    public int getFinalClassCount() {
        return finalClassCount;
    }

    public int getPublicClassCount() {
        return publicClassCount;
    }

    public int getTotalMethods() {
        return totalMethods;
    }

    public int getTotalFields() {
        return totalFields;
    }

    public int getModuleDescriptorCount() {
        return moduleDescriptorCount;
    }

    public int getPackageInfoCount() {
        return packageInfoCount;
    }
}
