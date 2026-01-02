package com.fasterxml.classmate;

import java.lang.reflect.Field;
import java.util.*;

import com.fasterxml.classmate.types.ResolvedObjectType;

import org.junit.Test;

import static junit.framework.Assert.*;

public class TypeBindingsTest
{
    @Test
    public void create() throws NoSuchFieldException, IllegalAccessException {
        Field typesField = TypeBindings.class.getDeclaredField("_types");
        typesField.setAccessible(true);

        // test null/empty arguments
        TypeBindings instance = TypeBindings.create(String.class, (List<ResolvedType>) null);
        assertEquals(0, ((ResolvedType[]) typesField.get(instance)).length);
        instance = TypeBindings.create(String.class, Collections.<ResolvedType>emptyList());
        assertEquals(0, ((ResolvedType[]) typesField.get(instance)).length);
    }

    @Test
    public void isEmpty() {
        TypeBindings instance = TypeBindings.create(String.class, (List<ResolvedType>) null);
        assertTrue(instance.isEmpty());
        List<ResolvedType> types = new ArrayList<ResolvedType>();
        types.add(ResolvedObjectType.create(Object.class, TypeBindings.emptyBindings(), null, null));
        instance = TypeBindings.create(Comparable.class, types);
        assertFalse(instance.isEmpty());
    }

    @Test
    public void getBoundName() {
        // test index bounds
        List<ResolvedType> types = new ArrayList<ResolvedType>();
        types.add(ResolvedObjectType.create(Object.class, TypeBindings.emptyBindings(), null, null));
        TypeBindings instance = TypeBindings.create(Comparable.class, types);

        assertNull(instance.getBoundName(-1));
        assertNull(instance.getBoundName(1));
        assertNull(instance.getBoundName(Integer.MIN_VALUE));
        assertNull(instance.getBoundName(Integer.MAX_VALUE));
        assertEquals("T", instance.getBoundName(0));

    }

    @Test
    public void getBoundType() {
        // test index bounds
        List<ResolvedType> types = new ArrayList<ResolvedType>();
        types.add(ResolvedObjectType.create(Object.class, TypeBindings.emptyBindings(), null, null));
        TypeBindings instance = TypeBindings.create(Comparable.class, types);

        assertNull(instance.getBoundType(-1));
        assertNull(instance.getBoundType(1));
        assertNull(instance.getBoundType(Integer.MIN_VALUE));
        assertNull(instance.getBoundType(Integer.MAX_VALUE));
        assertSame(types.get(0), instance.getBoundType(0));
    }

    @Test
    public void typeBindingsToString() {
        TypeBindings instance = TypeBindings.create(String.class, (List<ResolvedType>) null);
        assertEquals("", instance.toString());

        List<ResolvedType> types = new ArrayList<ResolvedType>();
        types.add(ResolvedObjectType.create(Object.class, TypeBindings.emptyBindings(), null, null));
        instance = TypeBindings.create(Comparable.class, types);
        assertEquals("<java.lang.Object>", instance.toString());

        types.add(ResolvedObjectType.create(String.class, TypeBindings.emptyBindings(), null, null));
        instance = TypeBindings.create(Map.class, types);
        assertEquals("<java.lang.Object,java.lang.String>", instance.toString());
    }

    @Test
    public void equals() {
        // test referential
        TypeBindings instance = TypeBindings.create(String.class, (List<ResolvedType>) null);
        assertTrue(instance.equals(instance));

        // test null
        assertFalse(instance.equals(null));

        // test different classes
        Object strKey = "not a TypeBindings";
        assertFalse(instance.equals(strKey));

        // test no types
        TypeBindings instance1 = TypeBindings.create(Object.class, (List<ResolvedType>) null);
        assertTrue(instance.equals(instance1));
        assertTrue(instance1.equals(instance));

        // test unequal types length
        List<ResolvedType> types = new ArrayList<ResolvedType>();
        types.add(ResolvedObjectType.create(Object.class, TypeBindings.emptyBindings(), null, null));
        TypeBindings instance2 = TypeBindings.create(Comparable.class, types);
        assertFalse(instance.equals(instance2));
        assertFalse(instance2.equals(instance));

        // test equal types length (unequal values)
        types = new ArrayList<ResolvedType>();
        types.add(ResolvedObjectType.create(String.class, TypeBindings.emptyBindings(), null, null));
        TypeBindings instance3 = TypeBindings.create(Comparable.class, types);
        assertFalse(instance2.equals(instance3));
        assertFalse(instance3.equals(instance2));

        // test equal types length (equal values)
        types = new ArrayList<ResolvedType>();
        types.add(ResolvedObjectType.create(String.class, TypeBindings.emptyBindings(), null, null));
        TypeBindings instance4 = TypeBindings.create(Comparable.class, types);
        assertTrue(instance3.equals(instance4));
        assertTrue(instance4.equals(instance3));
    }

}
