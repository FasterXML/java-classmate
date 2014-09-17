package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;

import org.junit.Test;

import java.lang.reflect.Method;

import static junit.framework.Assert.*;

/**
 * @author blangel
 */
public class ResolvedMethodTest {
    private static abstract class ModifiersClass {
        private strictfp void strictfpMethod() { }
        private native void nativeMethod();
        protected abstract void abstractMethod();
        private synchronized void synchronizedMethod() { }
    }

    private static final Method toStringMethod;
    private static final Method abstractMethodMethod;
    @SuppressWarnings("unused")
    private static final Method getRawMemberMethod;
    private static final Method strictfpMethodMethod;
    private static final Method nativeMethodMethod;
    private static final Method synchronizedMethodMethod;
    static {
        try {
            toStringMethod = Object.class.getDeclaredMethod("toString");
            getRawMemberMethod = RawMember.class.getDeclaredMethod("getRawMember");
            strictfpMethodMethod = ModifiersClass.class.getDeclaredMethod("strictfpMethod");
            nativeMethodMethod = ModifiersClass.class.getDeclaredMethod("nativeMethod");
            synchronizedMethodMethod = ModifiersClass.class.getDeclaredMethod("synchronizedMethod");
            abstractMethodMethod = ModifiersClass.class.getDeclaredMethod("abstractMethod");
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        }
    }

    @Test
    public void isAbstract() {
        ResolvedMethod resolvedMethod = new ResolvedMethod(ResolvedObjectType.create(Object.class, null, null, null), null, toStringMethod, null, null);
        ResolvedMethod resolvedMethod1 = new ResolvedMethod(ResolvedObjectType.create(RawMember.class, null, null, null), null, abstractMethodMethod, null, null);

        assertFalse(resolvedMethod.isAbstract());
        assertTrue(resolvedMethod1.isAbstract());
    }

    @Test
    public void isStrict() {
        ResolvedMethod rawMethod = new ResolvedMethod(ResolvedObjectType.create(Object.class, null, null, null), null, toStringMethod, null, ResolvedType.NO_TYPES);
        ResolvedMethod rawMethod1 = new ResolvedMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), null, strictfpMethodMethod, null, ResolvedType.NO_TYPES);

        assertFalse(rawMethod.isStrict());
        assertTrue(rawMethod1.isStrict());
    }

    @Test
    public void isNative() {
        ResolvedMethod rawMethod = new ResolvedMethod(ResolvedObjectType.create(Object.class, null, null, null), null, toStringMethod, null, ResolvedType.NO_TYPES);
        ResolvedMethod rawMethod1 = new ResolvedMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), null, nativeMethodMethod, null, ResolvedType.NO_TYPES);

        assertFalse(rawMethod.isNative());
        assertTrue(rawMethod1.isNative());
    }

    @Test
    public void isSynchronized() {
        ResolvedMethod rawMethod = new ResolvedMethod(ResolvedObjectType.create(Object.class, null, null, null), null, toStringMethod, null, ResolvedType.NO_TYPES);
        ResolvedMethod rawMethod1 = new ResolvedMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), null, synchronizedMethodMethod, null, ResolvedType.NO_TYPES);

        assertFalse(rawMethod.isSynchronized());
        assertTrue(rawMethod1.isSynchronized());
    }

    @Test
    public void getReturnType() {
        ResolvedObjectType string = ResolvedObjectType.create(String.class, null, null, null);
        ResolvedMethod resolvedMethod = new ResolvedMethod(string, null, toStringMethod, string, null);
        assertEquals(string, resolvedMethod.getReturnType());
    }

    @Test
    public void getType() {
        ResolvedObjectType string = ResolvedObjectType.create(String.class, null, null, null);
        ResolvedMethod resolvedMethod = new ResolvedMethod(string, null, toStringMethod, string, null);
        assertEquals(string, resolvedMethod.getType());
        assertEquals(resolvedMethod.getReturnType(), resolvedMethod.getType());
    }

    @Test
    public void getArgumentType() {
        ResolvedObjectType string = ResolvedObjectType.create(String.class, null, null, null);
        ResolvedMethod resolvedMethod = new ResolvedMethod(string, null, toStringMethod, string, null);
        assertNull(resolvedMethod.getArgumentType(-1));
        assertNull(resolvedMethod.getArgumentType(0));
        assertNull(resolvedMethod.getArgumentType(1));
    }

    @Test
    public void equals() {
        ResolvedObjectType string = ResolvedObjectType.create(String.class, null, null, null);
        ResolvedMethod resolvedMethod = new ResolvedMethod(string, null, toStringMethod, string, null);

        // test referential equals
        assertTrue(resolvedMethod.equals(resolvedMethod));

        // test null
        assertFalse(resolvedMethod.equals(null));

        // test unequal class
        assertFalse(resolvedMethod.equals("not a ResolvedMethod"));

        ResolvedObjectType object = ResolvedObjectType.create(Object.class, null, null, null);
        ResolvedMethod resolvedMethod1 = new ResolvedMethod(object, null, toStringMethod, string, null);
        assertTrue(resolvedMethod.equals(resolvedMethod1));

        ResolvedMethod resolvedMethod2 = new ResolvedMethod(object, null, null, string, null);
        assertFalse(resolvedMethod.equals(resolvedMethod2));
    }

}
