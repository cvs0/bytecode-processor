package net.cvs0.bytecode.analysis;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JarStatisticsTest {

    @Test
    void fromEmptyMappingIsZeroed() {
        JarStatistics s = JarStatistics.from(new JarMapping("empty.jar"));
        assertEquals(0, s.getProgramClassCount());
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
        assertEquals(1, s.getProgramClassCount());
        assertEquals(1, s.getTotalMethods());
        assertEquals(1, s.getTotalFields());
        assertEquals(0, s.getResourceCount());
    }
}
