package io.github.cvs0.bytecode.transform;

import io.github.cvs0.bytecode.util.BytecodeNames;

import java.util.Map;
import java.util.Set;

/**
 * Decides whether field/method renames are safe for a type: JVM runtime types, types used as annotation class literals,
 * and very high fan-in hubs (see {@link ProgramTypeReferenceFanIn}, {@link AnnotationReferencedProgramTypes}).
 */
public final class MemberRenamePolicy {

    private MemberRenamePolicy() {}

    public static boolean allowMemberRenamesOnType(
            String ownerInternalName,
            Map<String, Integer> fanInPerOwner,
            Set<String> typesUsedInAnnotationClassLiterals) {
        if (BytecodeNames.isJvmRuntimeType(ownerInternalName)) {
            return false;
        }
        if (typesUsedInAnnotationClassLiterals.contains(ownerInternalName)) {
            return false;
        }
        int n = fanInPerOwner.getOrDefault(ownerInternalName, 0);
        return n < ProgramTypeReferenceFanIn.MEMBER_RENAME_FAN_IN_THRESHOLD;
    }
}
