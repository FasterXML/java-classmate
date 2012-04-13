package com.fasterxml.classmate.types;

import com.fasterxml.classmate.ResolvedType;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/12/12
 * Time: 6:58 PM
 */
public class ResolvedInterfaceTypeTest {

    private static interface Parent { }

    private static interface Child extends Parent {
        static final String test = "test";
        void getTest();
    }

    protected static interface GrandChild extends Child { }

    @Test
    public void getParentClass() {
        ResolvedInterfaceType interfaceType = new ResolvedInterfaceType(Comparable.class, null, null);
        assertNull(interfaceType.getParentClass());
    }

    @Test
    public void getImplementedInterfaces() {
        ResolvedInterfaceType interfaceType = new ResolvedInterfaceType(Parent.class, null, null);
        assertEquals(0, interfaceType.getImplementedInterfaces().size());

        interfaceType = new ResolvedInterfaceType(Parent.class, null, new ResolvedType[] { });
        assertEquals(0, interfaceType.getImplementedInterfaces().size());

        ResolvedInterfaceType childInterfaceType = new ResolvedInterfaceType(Child.class, null, new ResolvedType[] { interfaceType });
        assertEquals(1, childInterfaceType.getImplementedInterfaces().size());
        assertEquals(Parent.class, childInterfaceType.getImplementedInterfaces().get(0).getErasedType());
    }

    @Test
    public void getArrayElementType() {
        ResolvedInterfaceType interfaceType = new ResolvedInterfaceType(Comparable.class, null, null);
        assertNull(interfaceType.getArrayElementType());
    }

    @Test
    public void isArray() {
        ResolvedInterfaceType interfaceType = new ResolvedInterfaceType(Comparable.class, null, null);
        assertFalse(interfaceType.isArray());
    }

    @Test
    public void isPrimitive() {
        ResolvedInterfaceType interfaceType = new ResolvedInterfaceType(Comparable.class, null, null);
        assertFalse(interfaceType.isPrimitive());
    }

    @Test
    public void getStaticFields() {
        ResolvedInterfaceType interfaceType = new ResolvedInterfaceType(Parent.class, null, null);
        assertEquals(0, interfaceType.getStaticFields().size());
        assertEquals(0, interfaceType.getStaticFields().size()); // cached copy should be the same

        interfaceType = new ResolvedInterfaceType(Child.class, null, null);
        assertEquals(1, interfaceType.getStaticFields().size());
        assertEquals(1, interfaceType.getStaticFields().size()); // cached copy should be the same
    }

    @Test
    public void getMemberMethods() {
        ResolvedInterfaceType interfaceType = new ResolvedInterfaceType(Parent.class, null, null);
        assertEquals(0, interfaceType.getMemberMethods().size());
        assertEquals(0, interfaceType.getMemberMethods().size()); // cached copy should be the same

        interfaceType = new ResolvedInterfaceType(Child.class, null, null);
        assertEquals(1, interfaceType.getMemberMethods().size());
        assertEquals(1, interfaceType.getMemberMethods().size()); // cached copy should be the same
    }

    @Test
    public void appendFullDescription() {

        ResolvedInterfaceType interfaceType = new ResolvedInterfaceType(Parent.class, null, null);
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = interfaceType.appendFullDescription(buffer);
        assertSame(returned, buffer);
        assertEquals("com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Parent", buffer.toString());
        buffer = new StringBuilder("Interface = "); // existing data
        returned = interfaceType.appendFullDescription(buffer);
        assertSame(returned, buffer);
        assertEquals("Interface = com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Parent", buffer.toString());

        ResolvedInterfaceType childInterfaceType = new ResolvedInterfaceType(Child.class, null, new ResolvedType[] { interfaceType });
        buffer = new StringBuilder();
        returned = childInterfaceType.appendFullDescription(buffer);
        assertSame(returned, buffer);
        assertEquals("com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Child extends com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Parent", buffer.toString());
        buffer = new StringBuilder("Interface = "); // existing data
        returned = childInterfaceType.appendFullDescription(buffer);
        assertSame(returned, buffer);
        assertEquals("Interface = com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Child extends com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Parent", buffer.toString());

        ResolvedInterfaceType grandChildInterfaceType = new ResolvedInterfaceType(GrandChild.class, null, new ResolvedType[] { interfaceType, childInterfaceType });
        buffer = new StringBuilder();
        returned = grandChildInterfaceType.appendFullDescription(buffer);
        assertSame(returned, buffer);
        assertEquals("com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$GrandChild extends com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Parent,com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Child", buffer.toString());
        buffer = new StringBuilder("Interface = "); // existing data
        returned = grandChildInterfaceType.appendFullDescription(buffer);
        assertSame(returned, buffer);
        assertEquals("Interface = com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$GrandChild extends com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Parent,com.fasterxml.classmate.types.ResolvedInterfaceTypeTest$Child", buffer.toString());
    }

}
