package com.fasterxml.classmate.members;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.reflect.Method;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 10:50 AM
 */
public class ResolvedMethodTest {

    private static class ResolvedMethodSubclass extends ResolvedMethod {
        private ResolvedMethodSubclass() {
            super(null, null, null, null, null);
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
    public void getReturnType() {
        ResolvedObjectType string = new ResolvedObjectType(String.class, null, null, (ResolvedType[]) null);
        ResolvedMethod resolvedMethod = new ResolvedMethod(string, null, toStringMethod, string, null);
        assertEquals(string, resolvedMethod.getReturnType());
    }

    @Test
    public void getType() {
        ResolvedObjectType string = new ResolvedObjectType(String.class, null, null, (ResolvedType[]) null);
        ResolvedMethod resolvedMethod = new ResolvedMethod(string, null, toStringMethod, string, null);
        assertEquals(string, resolvedMethod.getType());
        assertEquals(resolvedMethod.getReturnType(), resolvedMethod.getType());
    }

    @Test
    public void getArgumentType() {
        ResolvedObjectType string = new ResolvedObjectType(String.class, null, null, (ResolvedType[]) null);
        ResolvedMethod resolvedMethod = new ResolvedMethod(string, null, toStringMethod, string, null);
        assertNull(resolvedMethod.getArgumentType(-1));
        assertNull(resolvedMethod.getArgumentType(0));
        assertNull(resolvedMethod.getArgumentType(1));
    }

    @Test
    public void equals() {
        ResolvedObjectType string = new ResolvedObjectType(String.class, null, null, (ResolvedType[]) null);
        ResolvedMethod resolvedMethod = new ResolvedMethod(string, null, toStringMethod, string, null);

        // test referential equals
        assertTrue(resolvedMethod.equals(resolvedMethod));

        // test null
        assertFalse(resolvedMethod.equals(null));

        // test unequal class
        assertFalse(resolvedMethod.equals("not a ResolvedMethod"));

        // test subclass unequal
        assertFalse(resolvedMethod.equals(new ResolvedMethodSubclass()));

        ResolvedObjectType object = new ResolvedObjectType(Object.class, null, null, (ResolvedType[]) null);
        ResolvedMethod resolvedMethod1 = new ResolvedMethod(object, null, toStringMethod, string, null);
        assertTrue(resolvedMethod.equals(resolvedMethod1));

        ResolvedMethod resolvedMethod2 = new ResolvedMethod(object, null, null, string, null);
        assertFalse(resolvedMethod.equals(resolvedMethod2));
    }

}
