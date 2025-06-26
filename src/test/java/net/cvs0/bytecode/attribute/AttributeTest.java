package net.cvs0.bytecode.attribute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttributeTest {
    
    private Attribute attribute;
    
    @BeforeEach
    void setUp() {
        attribute = new Attribute("TestAttribute");
    }
    
    @Test
    void testConstructorWithName() {
        assertEquals("TestAttribute", attribute.getName());
        assertNull(attribute.getData());
        assertTrue(attribute.getProperties().isEmpty());
    }
    
    @Test
    void testConstructorWithNameAndData() {
        byte[] data = "test data".getBytes();
        Attribute attr = new Attribute("TestAttribute", data);
        
        assertEquals("TestAttribute", attr.getName());
        assertArrayEquals(data, attr.getData());
        assertTrue(attr.getProperties().isEmpty());
    }
    
    @Test
    void testSettersAndGetters() {
        attribute.setName("NewName");
        assertEquals("NewName", attribute.getName());
        
        byte[] data = "new data".getBytes();
        attribute.setData(data);
        assertArrayEquals(data, attribute.getData());
    }
    
    @Test
    void testProperties() {
        attribute.setProperty("key1", "value1");
        attribute.setProperty("key2", 42);
        attribute.setProperty("key3", true);
        
        assertEquals("value1", attribute.getProperty("key1"));
        assertEquals(42, attribute.getProperty("key2"));
        assertEquals(true, attribute.getProperty("key3"));
        
        assertTrue(attribute.hasProperty("key1"));
        assertFalse(attribute.hasProperty("nonexistent"));
        
        Map<String, Object> properties = attribute.getProperties();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey("key1"));
        assertTrue(properties.containsKey("key2"));
        assertTrue(properties.containsKey("key3"));
        
        attribute.removeProperty("key2");
        assertFalse(attribute.hasProperty("key2"));
        assertNull(attribute.getProperty("key2"));
    }
    
    @Test
    void testAttributeTypeChecks() {
        assertFalse(attribute.isAnnotation());
        assertFalse(attribute.isSignature());
        assertFalse(attribute.isSourceFile());
        
        Attribute annotationAttr = new Attribute("RuntimeVisibleAnnotations");
        assertTrue(annotationAttr.isAnnotation());
        
        Attribute invisibleAnnotationAttr = new Attribute("RuntimeInvisibleAnnotations");
        assertTrue(invisibleAnnotationAttr.isAnnotation());
        
        Attribute paramAnnotationAttr = new Attribute("RuntimeVisibleParameterAnnotations");
        assertTrue(paramAnnotationAttr.isAnnotation());
        
        Attribute invisibleParamAnnotationAttr = new Attribute("RuntimeInvisibleParameterAnnotations");
        assertTrue(invisibleParamAnnotationAttr.isAnnotation());
        
        Attribute signatureAttr = new Attribute("Signature");
        assertTrue(signatureAttr.isSignature());
        
        Attribute sourceFileAttr = new Attribute("SourceFile");
        assertTrue(sourceFileAttr.isSourceFile());
        
        Attribute sourceDebugAttr = new Attribute("SourceDebugExtension");
        assertTrue(sourceDebugAttr.isSourceDebug());
        
        Attribute lineNumberAttr = new Attribute("LineNumberTable");
        assertTrue(lineNumberAttr.isLineNumberTable());
        
        Attribute localVarAttr = new Attribute("LocalVariableTable");
        assertTrue(localVarAttr.isLocalVariableTable());
        
        Attribute localVarTypeAttr = new Attribute("LocalVariableTypeTable");
        assertTrue(localVarTypeAttr.isLocalVariableTypeTable());
        
        Attribute codeAttr = new Attribute("Code");
        assertTrue(codeAttr.isCode());
        
        Attribute exceptionsAttr = new Attribute("Exceptions");
        assertTrue(exceptionsAttr.isExceptions());
        
        Attribute innerClassesAttr = new Attribute("InnerClasses");
        assertTrue(innerClassesAttr.isInnerClasses());
        
        Attribute enclosingMethodAttr = new Attribute("EnclosingMethod");
        assertTrue(enclosingMethodAttr.isEnclosingMethod());
        
        Attribute syntheticAttr = new Attribute("Synthetic");
        assertTrue(syntheticAttr.isSynthetic());
        
        Attribute deprecatedAttr = new Attribute("Deprecated");
        assertTrue(deprecatedAttr.isDeprecated());
        
        Attribute bootstrapMethodsAttr = new Attribute("BootstrapMethods");
        assertTrue(bootstrapMethodsAttr.isBootstrapMethods());
        
        Attribute methodParametersAttr = new Attribute("MethodParameters");
        assertTrue(methodParametersAttr.isMethodParameters());
    }
    
    @Test
    void testToString() {
        String result = attribute.toString();
        assertTrue(result.contains("TestAttribute"));
        assertTrue(result.contains("dataLength=0"));
        assertTrue(result.contains("properties=0"));
        
        attribute.setData("test".getBytes());
        attribute.setProperty("key", "value");
        
        result = attribute.toString();
        assertTrue(result.contains("dataLength=4"));
        assertTrue(result.contains("properties=1"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        Attribute attr1 = new Attribute("Test");
        Attribute attr2 = new Attribute("Test");
        Attribute attr3 = new Attribute("Different");
        
        assertEquals(attr1, attr2);
        assertNotEquals(attr1, attr3);
        assertEquals(attr1.hashCode(), attr2.hashCode());
        
        attr1.setProperty("key", "value");
        assertNotEquals(attr1, attr2);
        
        attr2.setProperty("key", "value");
        assertEquals(attr1, attr2);
    }
    
    @Test
    void testDataCloning() {
        byte[] originalData = "test data".getBytes();
        attribute.setData(originalData);
        
        byte[] retrievedData = attribute.getData();
        assertArrayEquals(originalData, retrievedData);
        assertNotSame(originalData, retrievedData);
        
        retrievedData[0] = 'X';
        assertNotEquals(originalData[0], retrievedData[0]);
        assertEquals(originalData[0], attribute.getData()[0]);
    }
}