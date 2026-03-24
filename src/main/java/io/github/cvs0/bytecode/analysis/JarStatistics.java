package io.github.cvs0.bytecode.analysis;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

/**
 * Aggregated counts for a {@link JarMapping}: application vs embedded library classes, members, resources, and flags.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JarStatistics {
    private final int applicationClassCount;
    private final int embeddedLibraryClassCount;
    private final int externalLibraryStubCount;
    private final int resourceCount;
    private final int interfaceCount;
    private final int abstractClassCount;
    private final int finalClassCount;
    private final int publicClassCount;
    private final int totalMethods;
    private final int totalFields;
    private final int moduleDescriptorCount;
    private final int packageInfoCount;

    /**
     * All modeled {@code .class} entries ({@link ProgramClass} plus optional {@link io.github.cvs0.bytecode.clazz.LibraryClass} stubs).
     */
    public int getTotalModeledClassCount() {
        return applicationClassCount + embeddedLibraryClassCount + externalLibraryStubCount;
    }

    /**
     * Same as {@link #getApplicationClassCount()} (legacy name when everything was treated as one bucket).
     *
     * @deprecated prefer {@link #getApplicationClassCount()}
     */
    @Deprecated
    public int getProgramClassCount() {
        return applicationClassCount;
    }

    /**
     * Embedded {@link ProgramClass} entries plus manual {@link io.github.cvs0.bytecode.clazz.LibraryClass} stubs.
     */
    public int getLibraryClassCount() {
        return embeddedLibraryClassCount + externalLibraryStubCount;
    }

    /**
     * Computes statistics from the given mapping.
     */
    public static JarStatistics from(JarMapping mapping) {
        Collection<ProgramClass> classes = mapping.getProgramClasses();
        int application = 0;
        int embedded = 0;
        int interfaces = 0;
        int abstractClasses = 0;
        int finals = 0;
        int publics = 0;
        int methods = 0;
        int fields = 0;
        for (ProgramClass clazz : classes) {
            if (clazz.isEmbeddedLibrary()) {
                embedded++;
            } else {
                application++;
            }
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
                application,
                embedded,
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
}
