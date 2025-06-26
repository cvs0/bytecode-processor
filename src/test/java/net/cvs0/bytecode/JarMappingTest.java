package net.cvs0.bytecode;

import net.cvs0.bytecode.clazz.ProgramClass;
import net.cvs0.bytecode.clazz.LibraryClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JarMappingTest {
    
    private JarMapping jarMapping;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        jarMapping = new JarMapping("test.jar");
    }
    
    @Test
    void testAddAndGetProgramClass() {
        ProgramClass clazz = new ProgramClass("com/example/TestClass");
        jarMapping.addClass(clazz);
        
        assertEquals(clazz, jarMapping.getProgramClass("com/example/TestClass"));
        assertEquals(1, jarMapping.getProgramClasses().size());
        assertTrue(jarMapping.containsClass("com/example/TestClass"));
    }
    
    @Test
    void testAddAndGetLibraryClass() {
        LibraryClass clazz = new LibraryClass("java/lang/String");
        jarMapping.addLibraryClass(clazz);
        
        assertEquals(clazz, jarMapping.getLibraryClass("java/lang/String"));
        assertEquals(1, jarMapping.getLibraryClasses().size());
        assertTrue(jarMapping.containsClass("java/lang/String"));
    }
    
    @Test
    void testAddAndGetResource() {
        byte[] data = "test resource content".getBytes();
        jarMapping.addResource("META-INF/MANIFEST.MF", data);
        
        assertArrayEquals(data, jarMapping.getResource("META-INF/MANIFEST.MF"));
        assertEquals(1, jarMapping.getResourceCount());
        assertTrue(jarMapping.getResourceNames().contains("META-INF/MANIFEST.MF"));
    }
    
    @Test
    void testRemoveClass() {
        ProgramClass clazz = new ProgramClass("com/example/TestClass");
        jarMapping.addClass(clazz);
        
        assertTrue(jarMapping.containsClass("com/example/TestClass"));
        
        jarMapping.removeClass("com/example/TestClass");
        
        assertFalse(jarMapping.containsClass("com/example/TestClass"));
        assertNull(jarMapping.getProgramClass("com/example/TestClass"));
    }
    
    @Test
    void testRenameClass() {
        ProgramClass clazz = new ProgramClass("com/example/OldName");
        jarMapping.addClass(clazz);
        
        jarMapping.renameClass("com/example/OldName", "com/example/NewName");
        
        assertNull(jarMapping.getProgramClass("com/example/OldName"));
        assertNotNull(jarMapping.getProgramClass("com/example/NewName"));
        assertEquals("com/example/NewName", jarMapping.getProgramClass("com/example/NewName").getName());
    }
    
    @Test
    void testGetAllClassNames() {
        jarMapping.addClass(new ProgramClass("com/example/Class1"));
        jarMapping.addClass(new ProgramClass("com/example/Class2"));
        jarMapping.addLibraryClass(new LibraryClass("java/lang/String"));
        
        assertEquals(3, jarMapping.getAllClassNames().size());
        assertTrue(jarMapping.getAllClassNames().contains("com/example/Class1"));
        assertTrue(jarMapping.getAllClassNames().contains("com/example/Class2"));
        assertTrue(jarMapping.getAllClassNames().contains("java/lang/String"));
    }
    
    @Test
    void testGetTotalClassCount() {
        jarMapping.addClass(new ProgramClass("com/example/Class1"));
        jarMapping.addClass(new ProgramClass("com/example/Class2"));
        jarMapping.addLibraryClass(new LibraryClass("java/lang/String"));
        
        assertEquals(3, jarMapping.getTotalClassCount());
    }
    
    @Test
    void testJarPath() {
        assertEquals("test.jar", jarMapping.getJarPath());
    }
}