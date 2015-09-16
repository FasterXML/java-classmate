package com.fasterxml.classmate.members;

import com.fasterxml.classmate.types.ResolvedObjectType;
import com.fasterxml.classmate.util.MethodKey;

import java.lang.reflect.Constructor;

import junit.framework.TestCase;

/**
 * @author blangel
 */
public class RawConstructorTest
    extends TestCase
{

    private static final Constructor<String> stringConstructor;
    static {
        try {
            stringConstructor = String.class.getDeclaredConstructor();
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        }
    }

    public void testCreateKey() {
        RawConstructor rawConstructor = new RawConstructor(ResolvedObjectType.create(Object.class, null, null, null), stringConstructor);
        MethodKey methodKey = rawConstructor.createKey();
        assertNotNull(methodKey);
        assertEquals("<init>()", methodKey.toString());
    }

    public void testEquals() {
        // referential equality
        RawConstructor rawConstructor = new RawConstructor(ResolvedObjectType.create(Object.class, null, null, null), stringConstructor);
        assertTrue(rawConstructor.equals(rawConstructor));

        // null
        assertFalse(rawConstructor.equals(null));

        // unequal class
        assertFalse(rawConstructor.equals("not a RawConstructor"));

        // equality via delegation to Method
        RawConstructor rawConstructor1 = new RawConstructor(ResolvedObjectType.create(String.class, null, null, null), stringConstructor);
        assertTrue(rawConstructor.equals(rawConstructor1));
        assertTrue(rawConstructor1.equals(rawConstructor));

        RawConstructor rawConstructor2 = new RawConstructor(ResolvedObjectType.create(String.class, null, null, null), null);
        assertFalse(rawConstructor.equals(rawConstructor2));
        assertFalse(rawConstructor2.equals(rawConstructor));
        assertFalse(rawConstructor1.equals(rawConstructor2));
        assertFalse(rawConstructor2.equals(rawConstructor1));
    }

    public void testRawConstructorHashCode() {
        RawConstructor rawConstructor = new RawConstructor(ResolvedObjectType.create(Object.class, null, null, null), stringConstructor);
        assertEquals(stringConstructor.getName().hashCode(), rawConstructor.hashCode());
    }

}
