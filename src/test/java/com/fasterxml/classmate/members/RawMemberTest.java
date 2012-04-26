package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 11:24 AM
 */
public class RawMemberTest {

    private static class ModifiersClass {
        private static String test;
        public final String finalField = "something";
        protected String protectedField;
        private static String getTest() {
            return test;
        }
        public final void finalMethod() { }
        protected void protectedMethod() { }
    }

    private static final Method toStringMethod;
    private static final Method getRawMemberMethod;
    private static final Method getTestMethod;
    private static final Method finalMethodMethod;
    private static final Method protectedMethodMethod;
    private static final Field serialVersionUIDField;
    private static final Field testField;
    private static final Field finalFieldField;
    private static final Field protectedFieldField;
    private static final Constructor objectConstructor;
    static {
        try {
            toStringMethod = Object.class.getDeclaredMethod("toString");
            getRawMemberMethod = RawMember.class.getDeclaredMethod("getRawMember");
            getTestMethod = ModifiersClass.class.getDeclaredMethod("getTest");
            finalMethodMethod = ModifiersClass.class.getDeclaredMethod("finalMethod");
            protectedMethodMethod = ModifiersClass.class.getDeclaredMethod("protectedMethod");
            objectConstructor = Object.class.getDeclaredConstructor();
            finalFieldField = ModifiersClass.class.getDeclaredField("finalField");
            protectedFieldField = ModifiersClass.class.getDeclaredField("protectedField");
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        } catch (NoSuchFieldException nsfe) {
            throw new AssertionError(nsfe);
        }
        try {
            serialVersionUIDField = String.class.getDeclaredField("serialVersionUID");
            testField = ModifiersClass.class.getDeclaredField("test");
        } catch (NoSuchFieldException nsfe) {
            throw new AssertionError(nsfe);
        }
    }

    @Test
    public void isStatic() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), getTestMethod);

        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        RawField rawStaticField = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);

        assertFalse(rawMethod.isStatic());
        assertFalse(rawMethod1.isStatic());

        assertTrue(rawField.isStatic());
        assertTrue(rawStaticMethod.isStatic());
        assertTrue(rawStaticField.isStatic());
    }

    @Test
    public void isFinal() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), finalMethodMethod);

        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        RawField rawField1 = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), finalFieldField);
        RawField rawField2 = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);

        assertFalse(rawMethod.isFinal());
        assertTrue(rawMethod1.isFinal());

        assertTrue(rawField.isFinal());
        assertTrue(rawField1.isFinal());
        assertFalse(rawField2.isFinal());
    }

    @Test
    public void isPrivate() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), getTestMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), finalMethodMethod);

        RawField rawField = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);
        RawField rawField1 = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), finalFieldField);

        assertTrue(rawMethod.isPrivate());
        assertFalse(rawMethod1.isPrivate());

        assertTrue(rawField.isPrivate());
        assertFalse(rawField1.isPrivate());
    }

    @Test
    public void isProtected() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), getTestMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), finalMethodMethod);
        RawMethod rawMethod2 = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), protectedMethodMethod);

        RawField rawField = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);
        RawField rawField1 = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), finalFieldField);
        RawField rawField2 = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), protectedFieldField);

        assertFalse(rawMethod.isProtected());
        assertFalse(rawMethod1.isProtected());
        assertTrue(rawMethod2.isProtected());

        assertFalse(rawField.isProtected());
        assertFalse(rawField1.isProtected());
        assertTrue(rawField2.isProtected());
    }

    @Test
    public void isPublic() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), getTestMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), finalMethodMethod);

        RawField rawField = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);
        RawField rawField1 = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), finalFieldField);

        assertFalse(rawMethod.isPublic());
        assertTrue(rawMethod1.isPublic());

        assertFalse(rawField.isPublic());
        assertTrue(rawField1.isPublic());
    }

    @Test
    public void rawMemberHashCode() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), getTestMethod);

        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        RawField rawStaticField = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);

        assertEquals(toStringMethod.hashCode(), rawMethod.hashCode());
        assertEquals(getRawMemberMethod.hashCode(), rawMethod1.hashCode());
        assertEquals(getTestMethod.hashCode(), rawStaticMethod.hashCode());

        assertEquals(serialVersionUIDField.hashCode(), rawField.hashCode());
        assertEquals(testField.hashCode(), rawStaticField.hashCode());
    }

    @Test
    public void getModifiers() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), getTestMethod);

        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        RawField rawStaticField = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);

        RawConstructor rawConstructor = new RawConstructor(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES),
                objectConstructor);

        assertEquals(toStringMethod.getModifiers(), rawMethod.getModifiers());
        assertEquals(getRawMemberMethod.getModifiers(), rawMethod1.getModifiers());
        assertEquals(getTestMethod.getModifiers(), rawStaticMethod.getModifiers());

        assertEquals(serialVersionUIDField.getModifiers(), rawField.getModifiers());
        assertEquals(testField.getModifiers(), rawStaticField.getModifiers());

        assertEquals(objectConstructor.hashCode(), rawConstructor.hashCode());
    }

    @Test
    public void rawMemberToString() {
        RawMethod rawMethod = new RawMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), getTestMethod);

        RawField rawField = new RawField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        RawField rawStaticField = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);

        assertEquals(toStringMethod.getName(), rawMethod.toString());
        assertEquals(getRawMemberMethod.getName(), rawMethod1.toString());
        assertEquals(getTestMethod.getName(), rawStaticMethod.toString());

        assertEquals(serialVersionUIDField.getName(), rawField.toString());
        assertEquals(testField.getName(), rawStaticField.toString());
    }

}
