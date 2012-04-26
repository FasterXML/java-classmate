package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import com.fasterxml.classmate.util.MethodKey;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 11:50 AM
 */
public class RawConstructorTest {

    private static final Constructor<String> stringConstructor;
    static {
        try {
            stringConstructor = String.class.getDeclaredConstructor();
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        }
    }

    @Test
    public void init() {
        try {
            new RawConstructor(null, null);
        } catch (NullPointerException npe) {
            fail(npe.getMessage());
        }
    }

    @Test
    public void createKey() {
        RawConstructor rawConstructor = new RawConstructor(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), stringConstructor);
        MethodKey methodKey = rawConstructor.createKey();
        assertNotNull(methodKey);
        assertEquals("<init>()", methodKey.toString());
    }

    @Test
    public void equals() {
        // referential equality
        RawConstructor rawConstructor = new RawConstructor(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), stringConstructor);
        assertTrue(rawConstructor.equals(rawConstructor));

        // null
        assertFalse(rawConstructor.equals(null));

        // unequal class
        assertFalse(rawConstructor.equals("not a RawConstructor"));

        // equality via delegation to Method
        RawConstructor rawConstructor1 = new RawConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), stringConstructor);
        assertTrue(rawConstructor.equals(rawConstructor1));
        assertTrue(rawConstructor1.equals(rawConstructor));

        RawConstructor rawConstructor2 = new RawConstructor(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), null);
        assertFalse(rawConstructor.equals(rawConstructor2));
        assertFalse(rawConstructor2.equals(rawConstructor));
        assertFalse(rawConstructor1.equals(rawConstructor2));
        assertFalse(rawConstructor2.equals(rawConstructor1));
    }

    @Test
    public void rawConstructorHashCode() {
        RawConstructor rawConstructor = new RawConstructor(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), stringConstructor);
        assertEquals(stringConstructor.getName().hashCode(), rawConstructor.hashCode());
    }

}
