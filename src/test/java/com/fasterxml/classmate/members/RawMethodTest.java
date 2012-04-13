package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import com.fasterxml.classmate.util.MethodKey;
import org.junit.Test;

import java.lang.reflect.Method;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 11:10 AM
 */
public class RawMethodTest {

    private static class RawMethodSubclass extends RawMethod {
        private RawMethodSubclass() {
            super(null, null);
        }
    }

    private static final Method toStringMethod;
    static {
        try {
            toStringMethod = Object.class.getDeclaredMethod("toString");
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        }
    }

    @Test
    public void createKey() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        MethodKey methodKey = rawMethod.createKey();
        assertNotNull(methodKey);
    }

    @Test
    public void equals() {
        // referential equality
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        assertTrue(rawMethod.equals(rawMethod));

        // null
        assertFalse(rawMethod.equals(null));

        // unequal class
        assertFalse(rawMethod.equals("not a RawMethod"));

        // unequal sub-classing
        assertFalse(rawMethod.equals(new RawMethodSubclass()));

        // equality via delegation to Method
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        assertTrue(rawMethod.equals(rawMethod1));
        assertTrue(rawMethod1.equals(rawMethod));

        RawMethod rawMethod2 = new RawMethod(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), null);
        assertFalse(rawMethod.equals(rawMethod2));
        assertFalse(rawMethod2.equals(rawMethod));
        assertFalse(rawMethod1.equals(rawMethod2));
        assertFalse(rawMethod2.equals(rawMethod1));
    }

}
