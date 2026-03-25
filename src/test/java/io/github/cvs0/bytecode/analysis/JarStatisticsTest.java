package io.github.cvs0.bytecode.analysis;

import io.github.cvs0.bytecode.JarMapping;
import io.github.cvs0.bytecode.clazz.LibraryClass;
import io.github.cvs0.bytecode.clazz.ProgramClass;
import io.github.cvs0.bytecode.member.ProgramField;
import io.github.cvs0.bytecode.member.ProgramMethod;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JarStatisticsTest {

    @Test
    void fromEmptyMappingIsZeroed() {
        JarStatistics s = JarStatistics.from(new JarMapping("empty.jar"));
        assertEquals(0, s.getApplicationClassCount());
        assertEquals(0, s.getTotalMethods());
        assertEquals(0, s.getResourceCount());
        assertEquals(0, s.getModuleDescriptorCount());
        assertEquals(0, s.getPackageInfoCount());
    }

    @Test
    void fromMappingAggregatesCounts() {
        JarMapping m = new JarMapping("x.jar");
        ProgramClass c = new ProgramClass("p/C");
        c.addMethod(new ProgramMethod("m", "()V", 0));
        c.addField(new ProgramField("f", "I", 0));
        m.addClass(c);

        JarStatistics s = JarStatistics.from(m);
        assertEquals(1, s.getApplicationClassCount());
        assertEquals(1, s.getTotalMethods());
        assertEquals(1, s.getTotalFields());
        assertEquals(0, s.getResourceCount());
    }

    @Test
    void applicationVsEmbeddedLibraryDistinction() {
        JarMapping m = new JarMapping("x.jar");

        ProgramClass app = new ProgramClass("app/Main");
        m.addClass(app);

        ProgramClass lib = new ProgramClass("shaded/Dep");
        lib.setApplicationClass(false);
        m.addClass(lib);

        JarStatistics s = JarStatistics.from(m);
        assertEquals(1, s.getApplicationClassCount());
        assertEquals(1, s.getEmbeddedLibraryClassCount());
        assertEquals(2, s.getTotalModeledClassCount());
    }

    @Test
    void flagCountsAreAccurate() {
        JarMapping m = new JarMapping("x.jar");

        ProgramClass iface = new ProgramClass("a/I");
        iface.setAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT);
        m.addClass(iface);

        ProgramClass abs = new ProgramClass("a/A");
        abs.setAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT);
        m.addClass(abs);

        ProgramClass fin = new ProgramClass("a/F");
        fin.setAccess(Opcodes.ACC_FINAL);
        m.addClass(fin);

        JarStatistics s = JarStatistics.from(m);
        assertEquals(1, s.getInterfaceCount());
        assertEquals(2, s.getAbstractClassCount());
        assertEquals(1, s.getFinalClassCount());
        assertEquals(2, s.getPublicClassCount());
    }

    @Test
    void resourceCountReflectsMapping() {
        JarMapping m = new JarMapping("x.jar");
        m.addResource("a.txt", new byte[0]);
        m.addResource("b.txt", new byte[0]);

        JarStatistics s = JarStatistics.from(m);
        assertEquals(2, s.getResourceCount());
    }
}
