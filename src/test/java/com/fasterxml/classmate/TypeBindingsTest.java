package com.fasterxml.classmate;

import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/14/12
 * Time: 11:28 AM
 */
public class TypeBindingsTest {

    @Test
    public void construction() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException,
            InvocationTargetException, InstantiationException {

        Constructor constructor = TypeBindings.class.getDeclaredConstructor(String[].class, ResolvedType[].class);
        constructor.setAccessible(true);

        Field namesField = TypeBindings.class.getDeclaredField("_names");
        Field typesField = TypeBindings.class.getDeclaredField("_types");
        namesField.setAccessible(true);
        typesField.setAccessible(true);

        TypeBindings instance = (TypeBindings) constructor.newInstance(null, null);
        assertEquals(0, ((String[]) namesField.get(instance)).length);
        assertEquals(0, ((ResolvedType[]) typesField.get(instance)).length);

        // test incompatible lengths
        String[] names = new String[] { "one", "two" };
        ResolvedType[] types = new ResolvedType[] { };
        try {
            constructor.newInstance(names, types);
            fail("Expecting an IllegalArgumentException as names.length != types.length");
        } catch (InvocationTargetException ite) {
            assertEquals(IllegalArgumentException.class, ite.getTargetException().getClass());
        }
    }

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
        types.add(new ResolvedObjectType(Object.class, TypeBindings.emptyBindings(), null, ResolvedType.NO_TYPES));
        instance = TypeBindings.create(Comparable.class, types);
        assertFalse(instance.isEmpty());
    }

    @Test
    public void getBoundName() {
        // test index bounds
        List<ResolvedType> types = new ArrayList<ResolvedType>();
        types.add(new ResolvedObjectType(Object.class, TypeBindings.emptyBindings(), null, ResolvedType.NO_TYPES));
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
        types.add(new ResolvedObjectType(Object.class, TypeBindings.emptyBindings(), null, ResolvedType.NO_TYPES));
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
        types.add(new ResolvedObjectType(Object.class, TypeBindings.emptyBindings(), null, ResolvedType.NO_TYPES));
        instance = TypeBindings.create(Comparable.class, types);
        assertEquals("<java.lang.Object>", instance.toString());

        types.add(new ResolvedObjectType(String.class, TypeBindings.emptyBindings(), null, ResolvedType.NO_TYPES));
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
        assertFalse(instance.equals("not a TypeBindings"));

        // test no types
        TypeBindings instance1 = TypeBindings.create(Object.class, (List<ResolvedType>) null);
        assertTrue(instance.equals(instance1));
        assertTrue(instance1.equals(instance));

        // test unequal types length
        List<ResolvedType> types = new ArrayList<ResolvedType>();
        types.add(new ResolvedObjectType(Object.class, TypeBindings.emptyBindings(), null, ResolvedType.NO_TYPES));
        TypeBindings instance2 = TypeBindings.create(Comparable.class, types);
        assertFalse(instance.equals(instance2));
        assertFalse(instance2.equals(instance));

        // test equal types length (unequal values)
        types = new ArrayList<ResolvedType>();
        types.add(new ResolvedObjectType(String.class, TypeBindings.emptyBindings(), null, ResolvedType.NO_TYPES));
        TypeBindings instance3 = TypeBindings.create(Comparable.class, types);
        assertFalse(instance2.equals(instance3));
        assertFalse(instance3.equals(instance2));

        // test equal types length (equal values)
        types = new ArrayList<ResolvedType>();
        types.add(new ResolvedObjectType(String.class, TypeBindings.emptyBindings(), null, ResolvedType.NO_TYPES));
        TypeBindings instance4 = TypeBindings.create(Comparable.class, types);
        assertTrue(instance3.equals(instance4));
        assertTrue(instance4.equals(instance3));
    }

}
