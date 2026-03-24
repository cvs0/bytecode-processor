package io.github.cvs0.bytecode.transform;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects program types that appear as {@link Type} values inside runtime annotations. Frameworks often read those class
 * literals reflectively; skipping member renames on those types avoids subtle breakage without naming specific libraries.
 */
public final class AnnotationReferencedProgramTypes {

    private AnnotationReferencedProgramTypes() {}

    public static Set<String> typesUsedAsAnnotationClassLiterals(JarMapping mapping) {
        Set<String> inJar = new HashSet<>();
        for (ProgramClass pc : mapping.getProgramClasses()) {
            inJar.add(pc.getName());
        }
        Set<String> refs = new HashSet<>();
        for (ProgramClass pc : mapping.getProgramClasses()) {
            ClassNode cn = pc.getClassNode();
            if (cn == null) {
                continue;
            }
            scanList(cn.visibleAnnotations, inJar, refs);
            scanList(cn.invisibleAnnotations, inJar, refs);
            if (cn.fields != null) {
                for (FieldNode f : cn.fields) {
                    scanList(f.visibleAnnotations, inJar, refs);
                    scanList(f.invisibleAnnotations, inJar, refs);
                }
            }
            if (cn.methods != null) {
                for (MethodNode m : cn.methods) {
                    scanList(m.visibleAnnotations, inJar, refs);
                    scanList(m.invisibleAnnotations, inJar, refs);
                }
            }
            if (cn.recordComponents != null) {
                for (RecordComponentNode r : cn.recordComponents) {
                    scanList(r.visibleAnnotations, inJar, refs);
                    scanList(r.invisibleAnnotations, inJar, refs);
                }
            }
        }
        return refs;
    }

    private static void scanList(List<AnnotationNode> list, Set<String> inJar, Set<String> refs) {
        if (list == null) {
            return;
        }
        for (AnnotationNode an : list) {
            scanAnnotation(an, inJar, refs);
        }
    }

    @SuppressWarnings("unchecked")
    private static void scanAnnotation(AnnotationNode an, Set<String> inJar, Set<String> refs) {
        if (an == null || an.values == null) {
            return;
        }
        for (int i = 1; i < an.values.size(); i += 2) {
            scanValue(an.values.get(i), inJar, refs);
        }
    }

    private static void scanValue(Object v, Set<String> inJar, Set<String> refs) {
        if (v instanceof Type t) {
            if (t.getSort() == Type.OBJECT) {
                String in = t.getInternalName();
                if (inJar.contains(in)) {
                    refs.add(in);
                }
            } else if (t.getSort() == Type.ARRAY) {
                Type el = t.getElementType();
                if (el != null && el.getSort() == Type.OBJECT && inJar.contains(el.getInternalName())) {
                    refs.add(el.getInternalName());
                }
            }
            return;
        }
        if (v instanceof String[] arr && arr.length >= 2 && arr[0] != null) {
            String d = arr[0];
            if (d.length() > 2 && d.charAt(0) == 'L' && d.charAt(d.length() - 1) == ';') {
                String in = d.substring(1, d.length() - 1);
                if (inJar.contains(in)) {
                    refs.add(in);
                }
            }
            return;
        }
        if (v instanceof AnnotationNode nested) {
            scanAnnotation(nested, inJar, refs);
            return;
        }
        if (v instanceof List<?> list) {
            for (Object o : list) {
                scanValue(o, inJar, refs);
            }
        }
    }
}
