package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import com.fasterxml.classmate.util.MethodKey;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 11:10 AM
 */
public class RawMethodTest {

    private static class ModifiersClass {
        private strictfp void strictfpMethod() { }
        private native void nativeMethod();
        private synchronized void synchronizedMethod() { }
    }

    private static final Method toStringMethod;
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
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        }
    }

    @Test
    public void init() {
        try {
            new RawMethod(null, null);
        } catch (NullPointerException npe) {
            fail(npe.getMessage());
        }
    }

    @Test
    public void isAbstract() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);

        assertFalse(rawMethod.isAbstract());
        assertTrue(rawMethod1.isAbstract());
    }

    @Test
    public void isStrict() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), strictfpMethodMethod);

        assertFalse(rawMethod.isStrict());
        assertTrue(rawMethod1.isStrict());
    }

    @Test
    public void isNative() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), nativeMethodMethod);

        assertFalse(rawMethod.isNative());
        assertTrue(rawMethod1.isNative());
    }

    @Test
    public void isSynchronized() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), synchronizedMethodMethod);

        assertFalse(rawMethod.isSynchronized());
        assertTrue(rawMethod1.isSynchronized());
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
