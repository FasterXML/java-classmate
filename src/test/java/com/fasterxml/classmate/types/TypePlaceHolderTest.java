package com.fasterxml.classmate.types;

import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 10:30 AM
 */
public class TypePlaceHolderTest {

    @Test
    public void canCreateSubtypes() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        assertFalse(placeHolder.canCreateSubtypes());
    }

    @Test
    public void getParentClass() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        assertNull(placeHolder.getParentClass());
    }

    @Test
    public void getSelfReferencedType() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        assertNull(placeHolder.getSelfReferencedType());
    }

    @Test
    public void getImplementedInterfaces() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        assertEquals(0, placeHolder.getImplementedInterfaces().size());
    }

    @Test
    public void getArrayElementType() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        assertNull(placeHolder.getArrayElementType());
    }

    @Test
    public void isInterface() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        assertFalse(placeHolder.isInterface());
    }

    @Test
    public void isAbstract() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        assertFalse(placeHolder.isAbstract());
    }

    @Test
    public void isArray() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        assertFalse(placeHolder.isArray());
    }

    @Test
    public void isPrimitive() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        assertFalse(placeHolder.isPrimitive());
    }

    @Test
    public void appendSignature() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = placeHolder.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Ljava/lang/Object;", returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = placeHolder.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing Ljava/lang/Object;", returned.toString());
    }

    @Test
    public void appendErasedSignature() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = placeHolder.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Ljava/lang/Object;", returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = placeHolder.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing Ljava/lang/Object;", returned.toString());
    }

    @Test
    public void appendBriefDescription() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = placeHolder.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("<0>", returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = placeHolder.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing <0>", returned.toString());

        placeHolder = new TypePlaceHolder(Integer.MAX_VALUE);
        buffer = new StringBuilder();
        returned = placeHolder.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals(String.format("<%d>", Integer.MAX_VALUE), returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = placeHolder.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals(String.format("Existing <%d>", Integer.MAX_VALUE), returned.toString());

        placeHolder = new TypePlaceHolder(Integer.MIN_VALUE);
        buffer = new StringBuilder();
        returned = placeHolder.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals(String.format("<%d>", Integer.MIN_VALUE), returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = placeHolder.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals(String.format("Existing <%d>", Integer.MIN_VALUE), returned.toString());
    }

    @Test
    public void appendFullDescription() {
        TypePlaceHolder placeHolder = new TypePlaceHolder(0);
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = placeHolder.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("<0>", returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = placeHolder.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing <0>", returned.toString());

        placeHolder = new TypePlaceHolder(Integer.MAX_VALUE);
        buffer = new StringBuilder();
        returned = placeHolder.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals(String.format("<%d>", Integer.MAX_VALUE), returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = placeHolder.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals(String.format("Existing <%d>", Integer.MAX_VALUE), returned.toString());

        placeHolder = new TypePlaceHolder(Integer.MIN_VALUE);
        buffer = new StringBuilder();
        returned = placeHolder.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals(String.format("<%d>", Integer.MIN_VALUE), returned.toString());

        buffer = new StringBuilder("Existing ");
        returned = placeHolder.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals(String.format("Existing <%d>", Integer.MIN_VALUE), returned.toString());
    }

}
