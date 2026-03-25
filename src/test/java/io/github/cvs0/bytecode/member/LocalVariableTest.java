package io.github.cvs0.bytecode.member;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalVariableTest {

    @Test
    void constructorWithoutSignature() {
        LocalVariable lv = new LocalVariable("x", "I", 0, 10, 1);
        assertEquals("x", lv.getName());
        assertEquals("I", lv.getDescriptor());
        assertNull(lv.getSignature());
        assertEquals(0, lv.getStartPc());
        assertEquals(10, lv.getLength());
        assertEquals(1, lv.getIndex());
    }

    @Test
    void constructorWithSignature() {
        LocalVariable lv = new LocalVariable("list", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/String;>;", 5, 20, 2);
        assertEquals("list", lv.getName());
        assertEquals("Ljava/util/List;", lv.getDescriptor());
        assertEquals("Ljava/util/List<Ljava/lang/String;>;", lv.getSignature());
        assertEquals(5, lv.getStartPc());
        assertEquals(20, lv.getLength());
        assertEquals(2, lv.getIndex());
    }

    @Test
    void getEndPc() {
        LocalVariable lv = new LocalVariable("v", "I", 10, 5, 0);
        assertEquals(15, lv.getEndPc());
    }

    @Test
    void isActiveWithinScope() {
        LocalVariable lv = new LocalVariable("v", "I", 10, 5, 0);
        assertFalse(lv.isActive(9));
        assertTrue(lv.isActive(10));
        assertTrue(lv.isActive(14));
        assertFalse(lv.isActive(15));
    }

    @Test
    void hasSignature() {
        LocalVariable noSig = new LocalVariable("a", "I", 0, 1, 0);
        assertFalse(noSig.hasSignature());

        LocalVariable withSig = new LocalVariable("b", "I", "TT;", 0, 1, 0);
        assertTrue(withSig.hasSignature());
    }

    @Test
    void getTypeAliasesDescriptor() {
        LocalVariable lv = new LocalVariable("v", "Ljava/lang/String;", 0, 1, 0);
        assertEquals(lv.getDescriptor(), lv.getType());
    }

    @Test
    void isPrimitive() {
        assertTrue(new LocalVariable("i", "I", 0, 1, 0).isPrimitive());
        assertTrue(new LocalVariable("d", "D", 0, 1, 0).isPrimitive());
        assertTrue(new LocalVariable("z", "Z", 0, 1, 0).isPrimitive());
        assertFalse(new LocalVariable("s", "Ljava/lang/String;", 0, 1, 0).isPrimitive());
        assertFalse(new LocalVariable("a", "[I", 0, 1, 0).isPrimitive());
    }

    @Test
    void isObject() {
        assertTrue(new LocalVariable("s", "Ljava/lang/String;", 0, 1, 0).isObject());
        assertFalse(new LocalVariable("i", "I", 0, 1, 0).isObject());
        assertFalse(new LocalVariable("a", "[I", 0, 1, 0).isObject());
    }

    @Test
    void isArray() {
        assertTrue(new LocalVariable("a", "[I", 0, 1, 0).isArray());
        assertTrue(new LocalVariable("b", "[[Ljava/lang/String;", 0, 1, 0).isArray());
        assertFalse(new LocalVariable("i", "I", 0, 1, 0).isArray());
        assertFalse(new LocalVariable("s", "Ljava/lang/String;", 0, 1, 0).isArray());
    }

    @Test
    void equalsSameFields() {
        LocalVariable a = new LocalVariable("x", "I", "TT;", 0, 10, 1);
        LocalVariable b = new LocalVariable("x", "I", "TT;", 0, 10, 1);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqualsDifferentName() {
        LocalVariable a = new LocalVariable("x", "I", 0, 10, 1);
        LocalVariable b = new LocalVariable("y", "I", 0, 10, 1);
        assertNotEquals(a, b);
    }

    @Test
    void notEqualsDifferentDescriptor() {
        LocalVariable a = new LocalVariable("x", "I", 0, 10, 1);
        LocalVariable b = new LocalVariable("x", "J", 0, 10, 1);
        assertNotEquals(a, b);
    }

    @Test
    void notEqualsDifferentIndex() {
        LocalVariable a = new LocalVariable("x", "I", 0, 10, 1);
        LocalVariable b = new LocalVariable("x", "I", 0, 10, 2);
        assertNotEquals(a, b);
    }

    @Test
    void equalsHandlesNullSignature() {
        LocalVariable a = new LocalVariable("x", "I", 0, 10, 1);
        LocalVariable b = new LocalVariable("x", "I", 0, 10, 1);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalsHandlesNullVsNonNullSignature() {
        LocalVariable a = new LocalVariable("x", "I", null, 0, 10, 1);
        LocalVariable b = new LocalVariable("x", "I", "TT;", 0, 10, 1);
        assertNotEquals(a, b);
    }

    @Test
    void equalsReflexiveAndNull() {
        LocalVariable a = new LocalVariable("x", "I", 0, 10, 1);
        assertEquals(a, a);
        assertNotEquals(null, a);
    }
}
