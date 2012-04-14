package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 11:24 AM
 */
public class RawMemberTest {

    private static class HasStaticFieldMethod {
        private static String test;
        private static String getTest() {
            return test;
        }
    }

    private static final Method toStringMethod;
    private static final Method getRawMemberMethod;
    private static final Method getTestMethod;
    private static final Field serialVersionUIDField;
    private static final Field testField;
    static {
        try {
            toStringMethod = Object.class.getDeclaredMethod("toString");
            getRawMemberMethod = RawMember.class.getDeclaredMethod("getRawMember");
            getTestMethod = HasStaticFieldMethod.class.getDeclaredMethod("getTest");
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        }
        try {
            serialVersionUIDField = String.class.getDeclaredField("serialVersionUID");
            testField = HasStaticFieldMethod.class.getDeclaredField("test");
        } catch (NoSuchFieldException nsfe) {
            throw new AssertionError(nsfe);
        }
    }

    @Test
    public void isAbstract() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);
        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);

        assertFalse(rawMethod.isAbstract());
        assertFalse(rawField.isAbstract());
        assertTrue(rawMethod1.isAbstract());
    }

    @Test
    public void isStatic() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), getTestMethod);

        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        RawField rawStaticField = new RawField(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), testField);

        assertFalse(rawMethod.isStatic());
        assertFalse(rawMethod1.isStatic());

        assertTrue(rawField.isStatic());
        assertTrue(rawStaticMethod.isStatic());
        assertTrue(rawStaticField.isStatic());
    }

    @Test
    public void rawMemberHashCode() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), getTestMethod);

        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        RawField rawStaticField = new RawField(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), testField);

        assertEquals(toStringMethod.getName().hashCode(), rawMethod.hashCode());
        assertEquals(getRawMemberMethod.getName().hashCode(), rawMethod1.hashCode());
        assertEquals(getTestMethod.getName().hashCode(), rawStaticMethod.hashCode());

        assertEquals(serialVersionUIDField.getName().hashCode(), rawField.hashCode());
        assertEquals(testField.getName().hashCode(), rawStaticField.hashCode());
    }

    @Test
    public void getModifiers() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), getTestMethod);

        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        RawField rawStaticField = new RawField(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), testField);

        assertEquals(toStringMethod.getModifiers(), rawMethod.getModifiers());
        assertEquals(getRawMemberMethod.getModifiers(), rawMethod1.getModifiers());
        assertEquals(getTestMethod.getModifiers(), rawStaticMethod.getModifiers());

        assertEquals(serialVersionUIDField.getModifiers(), rawField.getModifiers());
        assertEquals(testField.getModifiers(), rawStaticField.getModifiers());
    }

    @Test
    public void rawMemberToString() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), getTestMethod);

        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        RawField rawStaticField = new RawField(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), testField);

        assertEquals(toStringMethod.getName(), rawMethod.toString());
        assertEquals(getRawMemberMethod.getName(), rawMethod1.toString());
        assertEquals(getTestMethod.getName(), rawStaticMethod.toString());

        assertEquals(serialVersionUIDField.getName(), rawField.toString());
        assertEquals(testField.getName(), rawStaticField.toString());
    }

}
