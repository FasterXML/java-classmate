package com.fasterxml.classmate.members;

import com.fasterxml.classmate.types.ResolvedObjectType;

import org.junit.Test;

import java.lang.reflect.Constructor;

import static junit.framework.Assert.*;

/**
 * @author blangel
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

    @SuppressWarnings("unused")
    @Test
    public void init() {
        try {
            new ResolvedConstructor(null, null, null, null);
        } catch (NullPointerException npe) {
            fail(npe.getMessage());
        }
    }

    @Test
    public void getRawMember() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(ResolvedObjectType.create(String.class, null, null, null),
                                                                          null, stringConstructor, null);
        assertSame(stringConstructor, resolvedConstructor.getRawMember());
    }

    @Test
    public void getType() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(ResolvedObjectType.create(String.class, null, null, null),
                null, stringConstructor, null);
        assertNull(resolvedConstructor.getType());
    }

    @Test
    public void getArgumentType() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(ResolvedObjectType.create(String.class, null, null, null),
                null, stringConstructor, null);

        assertNull(resolvedConstructor.getArgumentType(Integer.MIN_VALUE));
        assertNull(resolvedConstructor.getArgumentType(-1));
        assertNull(resolvedConstructor.getArgumentType(0));
        assertNull(resolvedConstructor.getArgumentType(1));
        assertNull(resolvedConstructor.getArgumentType(Integer.MAX_VALUE));
    }

    @Test
    public void resolvedConstructorHashCode() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(ResolvedObjectType.create(String.class, null, null, null),
                null, stringConstructor, null);
        assertEquals(stringConstructor.getName().hashCode(), resolvedConstructor.hashCode());
    }

    @Test
    public void equals() {
        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(ResolvedObjectType.create(String.class, null, null, null),
                null, stringConstructor, null);
        // referential equality
        assertTrue(resolvedConstructor.equals(resolvedConstructor));

        // null
        assertFalse(resolvedConstructor.equals(null));

        // different class
        assertFalse(resolvedConstructor.equals("not a ResolvedConstructor"));

        // unequal constructors
        ResolvedConstructor resolvedConstructor1 = new ResolvedConstructor(ResolvedObjectType.create(String.class, null, null, null),
                null, objectConstructor, null);
        assertFalse(resolvedConstructor.equals(resolvedConstructor1));
        assertFalse(resolvedConstructor1.equals(resolvedConstructor));

        // equal constructors
        ResolvedConstructor resolvedConstructor2 = new ResolvedConstructor(ResolvedObjectType.create(String.class, null, null, null),
                null, objectConstructor, null);
        ResolvedConstructor resolvedConstructor3 = new ResolvedConstructor(ResolvedObjectType.create(String.class, null, null, null),
                null, stringConstructor, null);

        assertTrue(resolvedConstructor1.equals(resolvedConstructor2));
        assertTrue(resolvedConstructor2.equals(resolvedConstructor1));

        assertTrue(resolvedConstructor.equals(resolvedConstructor3));
        assertTrue(resolvedConstructor3.equals(resolvedConstructor));
    }
}
