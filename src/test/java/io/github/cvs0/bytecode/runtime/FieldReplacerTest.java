package io.github.cvs0.bytecode.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldReplacerTest {

    // ========================================================================
    //  Test fixtures — simple singleton-with-delegate pattern
    // ========================================================================

    /** Simulates a singleton registry class with a private static instance field. */
    static class ServiceRegistry {
        @SuppressWarnings("unused")
        private static ServiceRegistry instance = new ServiceRegistry();
        @SuppressWarnings("unused")
        private Object service = "original";
    }

    /** Two levels of indirection: Manager.instance → ManagerImpl.delegate → the service */
    static class Manager {
        @SuppressWarnings("unused")
        private static Manager instance = new ManagerImpl();
    }

    static class ManagerImpl extends Manager {
        @SuppressWarnings("unused")
        private Object delegate = "deepOriginal";
    }

    /** Three levels of indirection */
    static class Root {
        @SuppressWarnings("unused")
        private static Root singleton = new Root();
        @SuppressWarnings("unused")
        private Middle middle = new Middle();
    }

    static class Middle {
        @SuppressWarnings("unused")
        private Leaf leaf = new Leaf();
    }

    static class Leaf {
        @SuppressWarnings("unused")
        private String value = "leafValue";
    }

    /** Simple class with just a static field */
    static class holder {
        @SuppressWarnings("unused")
        private static String INSTANCE = "staticVal";
    }

    /** Superclass field lookup test */
    static class Base {
        @SuppressWarnings("unused")
        private static Base ref = new Derived();
        @SuppressWarnings("unused")
        private String baseField = "fromBase";
    }

    static class Derived extends Base {
        @SuppressWarnings("unused")
        private String derivedField = "fromDerived";
    }

    // ========================================================================
    //  Tests
    // ========================================================================

    @Test
    void getSingleStaticField() throws Exception {
        Object val = FieldReplacer.on(holder.class).field("INSTANCE").get();
        assertEquals("staticVal", val);
    }

    @Test
    void setSingleStaticField() throws Exception {
        // save original
        String original = holder.INSTANCE;
        try {
            FieldReplacer.on(holder.class).field("INSTANCE").set("newStaticVal");
            assertEquals("newStaticVal", holder.INSTANCE);
        } finally {
            holder.INSTANCE = original;
        }
    }

    @Test
    void getTwoLevelChain() throws Exception {
        Object val = FieldReplacer.on(ServiceRegistry.class)
                .field("instance")
                .field("service")
                .get();
        assertEquals("original", val);
    }

    @Test
    void setTwoLevelChain() throws Exception {
        // Save and restore
        Object original = FieldReplacer.on(ServiceRegistry.class)
                .field("instance").field("service").get();
        try {
            FieldReplacer.on(ServiceRegistry.class)
                    .field("instance")
                    .field("service")
                    .set("replaced");

            Object after = FieldReplacer.on(ServiceRegistry.class)
                    .field("instance").field("service").get();
            assertEquals("replaced", after);
        } finally {
            FieldReplacer.on(ServiceRegistry.class)
                    .field("instance").field("service").set(original);
        }
    }

    @Test
    void getAndSetReturnsPreviousValue() throws Exception {
        Object original = FieldReplacer.on(ServiceRegistry.class)
                .field("instance").field("service").get();
        try {
            Object previous = FieldReplacer.on(ServiceRegistry.class)
                    .field("instance")
                    .field("service")
                    .getAndSet("swapped");
            assertEquals(original, previous);

            Object current = FieldReplacer.on(ServiceRegistry.class)
                    .field("instance").field("service").get();
            assertEquals("swapped", current);
        } finally {
            FieldReplacer.on(ServiceRegistry.class)
                    .field("instance").field("service").set(original);
        }
    }

    @Test
    void twoLevelWithSubclass() throws Exception {
        // Manager.instance is actually a ManagerImpl — field lookup should resolve on runtime class
        Object val = FieldReplacer.on(Manager.class)
                .field("instance")
                .field("delegate")
                .get();
        assertEquals("deepOriginal", val);
    }

    @Test
    void setTwoLevelWithSubclass() throws Exception {
        Object original = FieldReplacer.on(Manager.class)
                .field("instance").field("delegate").get();
        try {
            FieldReplacer.on(Manager.class)
                    .field("instance")
                    .field("delegate")
                    .set("newDelegate");

            Object after = FieldReplacer.on(Manager.class)
                    .field("instance").field("delegate").get();
            assertEquals("newDelegate", after);
        } finally {
            FieldReplacer.on(Manager.class)
                    .field("instance").field("delegate").set(original);
        }
    }

    @Test
    void threeLevelChain() throws Exception {
        Object val = FieldReplacer.on(Root.class)
                .field("singleton")
                .field("middle")
                .field("leaf")
                .get();
        assertInstanceOf(Leaf.class, val);
    }

    @Test
    void fourLevelChainGetAndSet() throws Exception {
        Object original = FieldReplacer.on(Root.class)
                .field("singleton")
                .field("middle")
                .field("leaf")
                .field("value")
                .get();
        assertEquals("leafValue", original);

        try {
            FieldReplacer.on(Root.class)
                    .field("singleton")
                    .field("middle")
                    .field("leaf")
                    .field("value")
                    .set("newLeaf");

            Object after = FieldReplacer.on(Root.class)
                    .field("singleton")
                    .field("middle")
                    .field("leaf")
                    .field("value")
                    .get();
            assertEquals("newLeaf", after);
        } finally {
            FieldReplacer.on(Root.class)
                    .field("singleton")
                    .field("middle")
                    .field("leaf")
                    .field("value")
                    .set(original);
        }
    }

    @Test
    void superclassFieldLookup() throws Exception {
        // Base.ref is actually a Derived — reading baseField should find it on Base
        Object val = FieldReplacer.on(Base.class)
                .field("ref")
                .field("baseField")
                .get();
        assertEquals("fromBase", val);
    }

    @Test
    void derivedFieldOnRuntimeType() throws Exception {
        // Base.ref is a Derived — derivedField should be found on the runtime class
        Object val = FieldReplacer.on(Base.class)
                .field("ref")
                .field("derivedField")
                .get();
        assertEquals("fromDerived", val);
    }

    @Test
    void noFieldsThrowsIllegalState() {
        assertThrows(IllegalStateException.class, () ->
                FieldReplacer.on(ServiceRegistry.class).get());
    }

    @Test
    void noFieldsSetThrowsIllegalState() {
        assertThrows(IllegalStateException.class, () ->
                FieldReplacer.on(ServiceRegistry.class).set("x"));
    }

    @Test
    void nonExistentFieldThrows() {
        assertThrows(NoSuchFieldException.class, () ->
                FieldReplacer.on(ServiceRegistry.class)
                        .field("instance")
                        .field("nonexistent")
                        .get());
    }

    @Test
    void nullRootClassThrows() {
        assertThrows(NullPointerException.class, () ->
                FieldReplacer.on(null));
    }

    @Test
    void nullFieldNameThrows() {
        assertThrows(NullPointerException.class, () ->
                FieldReplacer.on(ServiceRegistry.class).field(null));
    }

    @Test
    void nullIntermediateFieldThrowsNullPointer() {
        // Set service to null so traversal through it would fail
        Object original;
        try {
            original = FieldReplacer.on(ServiceRegistry.class)
                    .field("instance").field("service").get();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        try {
            FieldReplacer.on(ServiceRegistry.class)
                    .field("instance").field("service").set(null);

            // Now try to traverse through the null service
            // We need a class where 2nd level is an object we then read a 3rd field from
            // Let's use Root and null out middle
            Object origMiddle = FieldReplacer.on(Root.class)
                    .field("singleton").field("middle").get();
            try {
                FieldReplacer.on(Root.class)
                        .field("singleton").field("middle").set(null);

                assertThrows(NullPointerException.class, () ->
                        FieldReplacer.on(Root.class)
                                .field("singleton")
                                .field("middle")
                                .field("leaf")
                                .get());
            } finally {
                FieldReplacer.on(Root.class)
                        .field("singleton").field("middle").set(origMiddle);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                FieldReplacer.on(ServiceRegistry.class)
                        .field("instance").field("service").set(original);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void setNullValue() throws Exception {
        Object original = FieldReplacer.on(ServiceRegistry.class)
                .field("instance").field("service").get();
        try {
            FieldReplacer.on(ServiceRegistry.class)
                    .field("instance").field("service").set(null);
            assertNull(FieldReplacer.on(ServiceRegistry.class)
                    .field("instance").field("service").get());
        } finally {
            FieldReplacer.on(ServiceRegistry.class)
                    .field("instance").field("service").set(original);
        }
    }
}
