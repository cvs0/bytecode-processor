package net.cvs0.bytecode.integration;

import net.cvs0.bytecode.JarMapping;
import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.member.ProgramField;
import net.cvs0.bytecode.member.ProgramMethod;
import net.cvs0.bytecode.attribute.Attribute;
import net.cvs0.bytecode.attribute.AnnotationAttribute;
import net.cvs0.bytecode.attribute.SignatureAttribute;
import net.cvs0.bytecode.plugin.PluginManager;
import net.cvs0.bytecode.plugin.impl.ObfuscationPlugin;
import net.cvs0.bytecode.plugin.impl.OptimizationPlugin;
import net.cvs0.bytecode.transform.ClassTransformer;
import net.cvs0.bytecode.analysis.DependencyAnalyzer;
import net.cvs0.bytecode.analysis.UnusedCodeAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeProcessorIntegrationTest {
    
    private JarMapping jarMapping;
    private PluginManager pluginManager;
    
    @BeforeEach
    void setUp() {
        jarMapping = new JarMapping("test-integration.jar");
        pluginManager = new PluginManager();
        setupTestClasses();
    }
    
    private void setupTestClasses() {
        ProgramClass mainClass = new ProgramClass("com/example/Main");
        mainClass.setAccess(0x0021);
        mainClass.setSuperName("java/lang/Object");
        
        ProgramMethod mainMethod = new ProgramMethod("main", "([Ljava/lang/String;)V", 0x0009);
        mainClass.addMethod(mainMethod);
        
        ProgramMethod helperMethod = new ProgramMethod("helper", "()V", 0x0002);
        mainClass.addMethod(helperMethod);
        
        ProgramField field = new ProgramField("counter", "I", 0x000A);
        mainClass.addField(field);
        
        jarMapping.addClass(mainClass);
        
        ProgramClass utilClass = new ProgramClass("com/example/Util");
        utilClass.setAccess(0x0021);
        utilClass.setSuperName("java/lang/Object");
        
        ProgramMethod utilMethod = new ProgramMethod("doSomething", "()V", 0x0009);
        utilClass.addMethod(utilMethod);
        
        ProgramMethod unusedMethod = new ProgramMethod("unusedMethod", "()V", 0x0001);
        utilClass.addMethod(unusedMethod);
        
        jarMapping.addClass(utilClass);
        
        ProgramClass unusedClass = new ProgramClass("com/example/UnusedClass");
        unusedClass.setAccess(0x0021);
        unusedClass.setSuperName("java/lang/Object");
        
        jarMapping.addClass(unusedClass);
    }
    
    @Test
    void testCompleteWorkflow() {
        assertEquals(3, jarMapping.getProgramClasses().size());
        
        Map<String, Set<String>> dependencyGraph = DependencyAnalyzer.buildDependencyGraph(jarMapping);
        assertNotNull(dependencyGraph);
        assertEquals(3, dependencyGraph.size());
        
        Set<String> unusedClasses = DependencyAnalyzer.findUnusedClasses(jarMapping);
        assertTrue(unusedClasses.contains("com/example/UnusedClass"));
        
        Set<String> unusedMethods = UnusedCodeAnalyzer.findUnusedMethods(jarMapping);
        assertFalse(unusedMethods.isEmpty());
        
        ClassTransformer transformer = new ClassTransformer(jarMapping);
        transformer.renameField("com/example/Main", "counter", "a");
        transformer.renameMethod("com/example/Main", "helper", "()V", "b");
        transformer.renameClass("com/example/Main", "com/example/ObfuscatedMain");
        transformer.applyTransformations();
        
        assertNull(jarMapping.getProgramClass("com/example/Main"));
        assertNotNull(jarMapping.getProgramClass("com/example/ObfuscatedMain"));
        
        ProgramClass obfuscatedMain = jarMapping.getProgramClass("com/example/ObfuscatedMain");
        assertNotNull(obfuscatedMain.getField("a"));
        assertNotNull(obfuscatedMain.getMethod("b", "()V"));
    }
    
    @Test
    void testPluginIntegration() {
        ObfuscationPlugin obfuscationPlugin = new ObfuscationPlugin();
        Map<String, Object> obfuscationConfig = new HashMap<>();
        obfuscationConfig.put("obfuscateClasses", true);
        obfuscationConfig.put("obfuscateMethods", true);
        obfuscationConfig.put("obfuscateFields", true);
        obfuscationConfig.put("namePrefix", "obf");
        obfuscationPlugin.configure(obfuscationConfig);
        
        OptimizationPlugin optimizationPlugin = new OptimizationPlugin();
        Map<String, Object> optimizationConfig = new HashMap<>();
        optimizationConfig.put("removeNops", true);
        optimizationConfig.put("optimizeConstants", true);
        optimizationPlugin.configure(optimizationConfig);
        
        pluginManager.registerPlugin(obfuscationPlugin);
        pluginManager.registerPlugin(optimizationPlugin);
        
        assertEquals(2, pluginManager.getPluginCount());
        assertEquals(2, pluginManager.getEnabledPluginCount());
        
        pluginManager.processWithPlugins(jarMapping);
        
        assertTrue(obfuscationPlugin.isEnabled());
        assertTrue(optimizationPlugin.isEnabled());
    }
    
    @Test
    void testAttributeHandling() {
        ProgramClass testClass = jarMapping.getProgramClass("com/example/Main");
        assertNotNull(testClass);
        
        SignatureAttribute signatureAttr = new SignatureAttribute("<T:Ljava/lang/Object;>Ljava/lang/Object;");
        testClass.addAttribute(signatureAttr);
        
        AnnotationAttribute annotationAttr = new AnnotationAttribute("RuntimeVisibleAnnotations", true);
        AnnotationAttribute.AnnotationInfo annotation = new AnnotationAttribute.AnnotationInfo("Ljava/lang/Deprecated;");
        annotationAttr.addAnnotation(annotation);
        testClass.addAttribute(annotationAttr);
        
        assertEquals(2, testClass.getAttributes().size());
        
        boolean hasSignature = testClass.getAttributes().stream()
                .anyMatch(attr -> attr instanceof SignatureAttribute);
        assertTrue(hasSignature);
        
        boolean hasAnnotation = testClass.getAttributes().stream()
                .anyMatch(attr -> attr instanceof AnnotationAttribute);
        assertTrue(hasAnnotation);
    }
    
    @Test
    void testMethodAnalysis() {
        Map<String, Integer> methodComplexity = UnusedCodeAnalyzer.getMethodComplexity(jarMapping);
        assertNotNull(methodComplexity);
        assertFalse(methodComplexity.isEmpty());
        
        for (Integer complexity : methodComplexity.values()) {
            assertTrue(complexity >= 0);
        }
        
        assertTrue(methodComplexity.containsKey("com/example/Main.main([Ljava/lang/String;)V"));
        assertTrue(methodComplexity.containsKey("com/example/Main.helper()V"));
    }
    
    @Test
    void testResourceHandling() {
        byte[] manifestData = "Manifest-Version: 1.0\nMain-Class: com.example.Main\n".getBytes();
        jarMapping.addResource("META-INF/MANIFEST.MF", manifestData);
        
        byte[] propertiesData = "app.name=Test Application\napp.version=1.0\n".getBytes();
        jarMapping.addResource("application.properties", propertiesData);
        
        assertEquals(2, jarMapping.getResourceCount());
        assertArrayEquals(manifestData, jarMapping.getResource("META-INF/MANIFEST.MF"));
        assertArrayEquals(propertiesData, jarMapping.getResource("application.properties"));
        
        assertTrue(jarMapping.getResourceNames().contains("META-INF/MANIFEST.MF"));
        assertTrue(jarMapping.getResourceNames().contains("application.properties"));
    }
    
    @Test
    void testClassHierarchyAnalysis() {
        ProgramClass baseClass = new ProgramClass("com/example/BaseClass");
        baseClass.setSuperName("java/lang/Object");
        jarMapping.addClass(baseClass);
        
        ProgramClass derivedClass = new ProgramClass("com/example/DerivedClass");
        derivedClass.setSuperName("com/example/BaseClass");
        jarMapping.addClass(derivedClass);
        
        Set<String> baseDependencies = DependencyAnalyzer.findClassDependencies(baseClass);
        assertTrue(baseDependencies.contains("java/lang/Object"));
        
        Set<String> derivedDependencies = DependencyAnalyzer.findClassDependencies(derivedClass);
        assertTrue(derivedDependencies.contains("com/example/BaseClass"));
        
        assertEquals(5, jarMapping.getTotalClassCount());
    }
}