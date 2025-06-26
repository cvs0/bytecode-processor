package net.cvs0.bytecode.analysis;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DependencyAnalyzerTest {
    
    private JarMapping jarMapping;
    private ProgramClass classA;
    private ProgramClass classB;
    private ProgramClass classC;
    
    @BeforeEach
    void setUp() {
        jarMapping = new JarMapping("test.jar");
        
        classA = new ProgramClass("com/example/ClassA");
        classA.setSuperName("java/lang/Object");
        classA.setInterfaces(Arrays.asList("java/io/Serializable"));
        
        classB = new ProgramClass("com/example/ClassB");
        classB.setSuperName("com/example/ClassA");
        
        classC = new ProgramClass("com/example/ClassC");
        classC.setSuperName("java/lang/Object");
        
        jarMapping.addClass(classA);
        jarMapping.addClass(classB);
        jarMapping.addClass(classC);
    }
    
    @Test
    void testFindClassDependencies() {
        Set<String> dependencies = DependencyAnalyzer.findClassDependencies(classA);
        
        assertTrue(dependencies.contains("java/lang/Object"));
        assertTrue(dependencies.contains("java/io/Serializable"));
        assertEquals(2, dependencies.size());
    }
    
    @Test
    void testFindClassDependenciesWithInheritance() {
        Set<String> dependencies = DependencyAnalyzer.findClassDependencies(classB);
        
        assertTrue(dependencies.contains("com/example/ClassA"));
        assertEquals(1, dependencies.size());
    }
    
    @Test
    void testBuildDependencyGraph() {
        Map<String, Set<String>> dependencyGraph = DependencyAnalyzer.buildDependencyGraph(jarMapping);
        
        assertEquals(3, dependencyGraph.size());
        assertTrue(dependencyGraph.containsKey("com/example/ClassA"));
        assertTrue(dependencyGraph.containsKey("com/example/ClassB"));
        assertTrue(dependencyGraph.containsKey("com/example/ClassC"));
        
        Set<String> classADeps = dependencyGraph.get("com/example/ClassA");
        assertTrue(classADeps.contains("java/lang/Object"));
        assertTrue(classADeps.contains("java/io/Serializable"));
        
        Set<String> classBDeps = dependencyGraph.get("com/example/ClassB");
        assertTrue(classBDeps.contains("com/example/ClassA"));
    }
    
    @Test
    void testFindUnusedClasses() {
        Set<String> unusedClasses = DependencyAnalyzer.findUnusedClasses(jarMapping);
        
        assertTrue(unusedClasses.contains("com/example/ClassC"));
        assertFalse(unusedClasses.contains("com/example/ClassA"));
    }
    
    @Test
    void testGetTopologicalOrder() {
        Map<String, Set<String>> depGraph = DependencyAnalyzer.buildDependencyGraph(jarMapping);
        System.out.println("Dependency Graph: " + depGraph);
        
        List<String> topologicalOrder = DependencyAnalyzer.getTopologicalOrder(jarMapping);
        System.out.println("Topological Order: " + topologicalOrder);
        
        assertEquals(3, topologicalOrder.size());
        
        int indexA = topologicalOrder.indexOf("com/example/ClassA");
        int indexB = topologicalOrder.indexOf("com/example/ClassB");
        
        assertTrue(indexA < indexB, "ClassA should come before ClassB in topological order since ClassB depends on ClassA. Order: " + topologicalOrder);
    }
    
    @Test
    void testFindCircularDependencies() {
        ProgramClass classD = new ProgramClass("com/example/ClassD");
        classD.setSuperName("com/example/ClassE");
        
        ProgramClass classE = new ProgramClass("com/example/ClassE");
        classE.setSuperName("com/example/ClassD");
        
        jarMapping.addClass(classD);
        jarMapping.addClass(classE);
        
        Set<String> circularDeps = DependencyAnalyzer.findCircularDependencies(jarMapping);
        
        assertTrue(circularDeps.contains("com/example/ClassD"));
        assertTrue(circularDeps.contains("com/example/ClassE"));
    }
    
    @Test
    void testFindMethodDependencies() {
        ProgramMethod method = new ProgramMethod("testMethod", "()V", 0x0001);
        classA.addMethod(method);
        
        Set<String> dependencies = DependencyAnalyzer.findMethodDependencies(method);
        
        assertTrue(dependencies.isEmpty());
    }
}