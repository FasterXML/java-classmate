package com.fasterxml.classmate.types;

import org.junit.Test;

import static junit.framework.Assert.*;

public class ResolvedRecursiveTypeTest
{
    private static abstract class AbstractClass { }

    @SuppressWarnings("unused")
    private static class Mock {
        private static final String staticMemberField = "test";
        private final String memberField = "test";

        private static void staticMethod() { }
        private void memberMethod() { }
    }

    @Test
    public void canCreateSubtypes() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        try {
            recursiveType.canCreateSubtypes();
            fail("Expected a NullPointerException; _referencedType is not yet set.");
        } catch (NullPointerException npe) {
            // expected : TODO - for now? likely shouldn't be expected
        }

        recursiveType.setReference(ResolvedObjectType.create(Object.class, null, null,null));
        assertTrue(recursiveType.canCreateSubtypes());

        // TODO - currently fails for final-classes, likely should not, need to ask cowtowncoder about intent of canCreateSubtypes()
//        recursiveType = new ResolvedRecursiveType(String.class, null);
//        recursiveType.setReference(ResolvedObjectType.create(String.class, null, null, (ResolvedType[]) null));
//        assertFalse(recursiveType.canCreateSubtypes()); // String is final
    }

    @Test
    public void setReference() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        recursiveType.setReference(ResolvedObjectType.create(Object.class, null, null, null));
        try {
            recursiveType.setReference(ResolvedObjectType.create(Object.class, null, null, null));
            fail("Expecting an IllegalStateException; reference was already set");
        } catch (IllegalStateException ise) {
            // expected
        }

        recursiveType = new ResolvedRecursiveType(String.class, null);
        recursiveType.setReference(null); // can continuously set to null
        recursiveType.setReference(null);
        recursiveType.setReference(ResolvedObjectType.create(Object.class, null, null, null));
        try {
            recursiveType.setReference(ResolvedObjectType.create(Object.class, null, null, null));
            fail("Expecting an IllegalStateException; reference was already set");
        } catch (IllegalStateException ise) {
            // expected
        }
    }

    @Test
    public void getImplementedInterfaces() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        assertEquals(0, recursiveType.getImplementedInterfaces().size());
    }

    @Test
    public void getArrayElementType() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        assertNull(recursiveType.getArrayElementType());
    }

    @Test
    public void isInterface() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        assertFalse(recursiveType.isInterface());
        recursiveType = new ResolvedRecursiveType(Comparable.class, null);
        assertTrue(recursiveType.isInterface());
    }

    @Test
    public void isAbstract() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        assertFalse(recursiveType.isAbstract());
        recursiveType = new ResolvedRecursiveType(AbstractClass.class, null);
        assertTrue(recursiveType.isAbstract());
    }

    @Test
    public void isArray() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        assertFalse(recursiveType.isArray());
        recursiveType = new ResolvedRecursiveType(String[].class, null);
        assertTrue(recursiveType.isArray());
        recursiveType = new ResolvedRecursiveType(Object[].class, null);
        assertTrue(recursiveType.isArray());
        recursiveType = new ResolvedRecursiveType(boolean[].class, null);
        assertTrue(recursiveType.isArray());
    }

    @Test
    public void isPrimitive() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        assertFalse(recursiveType.isPrimitive());
        recursiveType = new ResolvedRecursiveType(boolean.class, null);
        assertFalse(recursiveType.isPrimitive()); // is a primitive but passing boolean is incorrect usage and so this should return false
    }

    @Test
    public void getMemberFields() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        try {
            recursiveType.getMemberFields();
            fail("Reference not set, NullPointerException expected.");
        } catch (NullPointerException npe) {
            // expected
        }
        recursiveType.setReference(ResolvedObjectType.create(Mock.class, null, null, null));
        assertEquals(1, recursiveType.getMemberFields().size());
    }

    @Test
    public void getStaticFields() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        try {
            recursiveType.getStaticFields();
            fail("Reference not set, NullPointerException expected.");
        } catch (NullPointerException npe) {
            // expected
        }
        recursiveType.setReference(ResolvedObjectType.create(Mock.class, null, null, null));
        assertEquals(1, recursiveType.getStaticFields().size());
    }

    @Test
    public void getMemberMethods() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        try {
            recursiveType.getMemberMethods();
            fail("Reference not set, NullPointerException expected.");
        } catch (NullPointerException npe) {
            // expected
        }
        recursiveType.setReference(ResolvedObjectType.create(Mock.class, null, null, null));
        assertEquals(1, recursiveType.getMemberMethods().size());
    }

    @Test
    public void getStaticMethods() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        try {
            recursiveType.getStaticMethods();
            fail("Reference not set, NullPointerException expected.");
        } catch (NullPointerException npe) {
            // expected
        }
        recursiveType.setReference(ResolvedObjectType.create(Mock.class, null, null, null));
        assertEquals(1, recursiveType.getStaticMethods().size());
    }

    @Test
    public void getConstructors() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        try {
            recursiveType.getConstructors();
            fail("Reference not set, NullPointerException expected.");
        } catch (NullPointerException npe) {
            // expected
        }
        recursiveType.setReference(ResolvedObjectType.create(Mock.class, null, null, null));
        assertEquals(1, recursiveType.getConstructors().size());
    }

    @Test
    public void appendSignature() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = recursiveType.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Ljava/lang/String;", returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = recursiveType.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing Ljava/lang/String;", returned.toString());
    }

    @Test
    public void appendErasedSignature() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = recursiveType.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Ljava/lang/String;", returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = recursiveType.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing Ljava/lang/String;", returned.toString());
    }

    @Test
    public void appendFullDescription() {
        ResolvedRecursiveType recursiveType = new ResolvedRecursiveType(String.class, null);
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = recursiveType.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("java.lang.String", returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = recursiveType.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing java.lang.String", returned.toString());
    }

}
