package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 12:45 PM
 */
public class ResolvedConstructorTest {

    private static final Constructor<String> stringConstructor;
    private static final Constructor<Object> objectConstructor;
    static {
        try {
            stringConstructor = String.class.getDeclaredConstructor();
            objectConstructor = Object.class.getDeclaredConstructor();
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        }
    }

    @Test
    public void getRawMember() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                                                                          null, stringConstructor, ResolvedType.NO_TYPES);
        assertSame(stringConstructor, resolvedConstructor.getRawMember());
    }

    @Test
    public void getType() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                null, stringConstructor, ResolvedType.NO_TYPES);
        assertNull(resolvedConstructor.getType());
    }

    @Test
    public void getArgumentType() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                null, stringConstructor, ResolvedType.NO_TYPES);

        assertNull(resolvedConstructor.getArgumentType(Integer.MIN_VALUE));
        assertNull(resolvedConstructor.getArgumentType(-1));
        assertNull(resolvedConstructor.getArgumentType(0));
        assertNull(resolvedConstructor.getArgumentType(1));
        assertNull(resolvedConstructor.getArgumentType(Integer.MAX_VALUE));
    }

    @Test
    public void resolvedConstructorHashCode() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                null, stringConstructor, ResolvedType.NO_TYPES);
        assertEquals(stringConstructor.getName().hashCode(), resolvedConstructor.hashCode());
    }

    @Test
    public void equals() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                null, stringConstructor, ResolvedType.NO_TYPES);
        // referential equality
        assertTrue(resolvedConstructor.equals(resolvedConstructor));

        // null
        assertFalse(resolvedConstructor.equals(null));

        // different class
        assertFalse(resolvedConstructor.equals("not a ResolvedConstructor"));

        // unequal constructors
        ResolvedConstructor resolvedConstructor1 = new ResolvedConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                null, objectConstructor, ResolvedType.NO_TYPES);
        assertFalse(resolvedConstructor.equals(resolvedConstructor1));
        assertFalse(resolvedConstructor1.equals(resolvedConstructor));

        // equal constructors
        ResolvedConstructor resolvedConstructor2 = new ResolvedConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                null, objectConstructor, ResolvedType.NO_TYPES);
        ResolvedConstructor resolvedConstructor3 = new ResolvedConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                null, stringConstructor, ResolvedType.NO_TYPES);

        assertTrue(resolvedConstructor1.equals(resolvedConstructor2));
        assertTrue(resolvedConstructor2.equals(resolvedConstructor1));

        assertTrue(resolvedConstructor.equals(resolvedConstructor3));
        assertTrue(resolvedConstructor3.equals(resolvedConstructor));
    }
}
