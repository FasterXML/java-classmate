package com.fasterxml.classmate.types;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;

import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * User: blangel
 */
public class ResolvedArrayTypeTest {

    @Test
    public void getArrayElementType() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertNull(arrayType.getArrayElementType());

        ResolvedArrayType arrayType1 = new ResolvedArrayType(Object.class, null, arrayType);
        assertEquals(arrayType, arrayType1.getArrayElementType());
    }

    @Test
    public void canCreateSubtypes() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertFalse(arrayType.canCreateSubtypes());
    }

    @Test
    public void getParentClass() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        // With [classmate#51]:
        assertNotNull(arrayType.getParentClass());
        assertEquals(Object.class, arrayType.getParentClass().getErasedType());
    }

    @Test
    public void getSelfReferencedType() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertNull(arrayType.getSelfReferencedType());
    }

    @Test
    public void getImplementedInterfaces() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertEquals(0, arrayType.getImplementedInterfaces().size());

        arrayType = new ResolvedArrayType(Collection.class, TypeBindings.create(String.class, (ResolvedType[]) null), ResolvedObjectType.create(String.class, null, null, null));
        assertEquals(0, arrayType.getImplementedInterfaces().size());
    }

    @Test
    public void isAbstract() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertFalse(arrayType.isAbstract());
    }

    @Test
    public void isArray() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertTrue(arrayType.isArray());
    }

    @Test
    public void isPrimitive() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertFalse(arrayType.isPrimitive());
    }

    @Test
    public void isInterface() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertFalse(arrayType.isInterface());
    }

    @Test
    public void appendSignature() {
        ResolvedArrayType npeType = new ResolvedArrayType(Object.class, null, null);
        try {
            npeType.appendSignature(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            npeType.appendSignature(new StringBuilder());
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, ResolvedObjectType.create(Object.class, null, null, null));
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = arrayType.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("[Ljava/lang/Object;", returned.toString());
        buffer = new StringBuilder("Existing ");
        returned = arrayType.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing [Ljava/lang/Object;", returned.toString());
    }

    @Test
    public void appendErasedSignature() {
        ResolvedArrayType npeType = new ResolvedArrayType(Object.class, null, null);
        try {
            npeType.appendErasedSignature(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            npeType.appendErasedSignature(new StringBuilder());
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, ResolvedObjectType.create(Object.class, null, null, null));
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = arrayType.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("[Ljava/lang/Object;", returned.toString());
        buffer = new StringBuilder("Existing ");
        returned = arrayType.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing [Ljava/lang/Object;", returned.toString());
    }

    @Test
    public void appendBriefDescription() {
        ResolvedArrayType npeType = new ResolvedArrayType(Object.class, null, null);
        try {
            npeType.appendBriefDescription(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            npeType.appendBriefDescription(new StringBuilder());
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, ResolvedObjectType.create(Object.class, null, null, null));
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = arrayType.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("java.lang.Object[]", returned.toString());
        buffer = new StringBuilder("Existing ");
        returned = arrayType.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing java.lang.Object[]", returned.toString());
    }

    @Test
    public void appendFullDescription() {
        ResolvedArrayType npeType = new ResolvedArrayType(Object.class, null, null);
        try {
            npeType.appendFullDescription(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            npeType.appendFullDescription(new StringBuilder());
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, ResolvedObjectType.create(Object.class, null, null, null));
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = arrayType.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("java.lang.Object[]", returned.toString());
        buffer = new StringBuilder("Existing ");
        returned = arrayType.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing java.lang.Object[]", returned.toString());
    }
}
