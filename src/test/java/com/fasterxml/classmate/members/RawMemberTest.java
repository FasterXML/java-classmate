package com.fasterxml.classmate.members;

import com.fasterxml.classmate.types.ResolvedObjectType;

import org.junit.Test;

import java.lang.reflect.*;

import junit.framework.TestCase;

/**
 * @author blangel
 */
public class RawMemberTest
    extends TestCase
{

    @SuppressWarnings("unused")
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
    private static final Constructor<?> objectConstructor;
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

    public void testIsStatic() {
        RawMethod rawMethod = new RawMethod(ResolvedObjectType.create(Object.class, null, null, null), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(ResolvedObjectType.create(RawMember.class, null, null, null), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), getTestMethod);

        RawField rawField = new RawField(ResolvedObjectType.create(Object.class, null, null, null), serialVersionUIDField);
        RawField rawStaticField = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), testField);

        assertFalse(rawMethod.isStatic());
        assertFalse(rawMethod1.isStatic());

        assertTrue(rawField.isStatic());
        assertTrue(rawStaticMethod.isStatic());
        assertTrue(rawStaticField.isStatic());
    }

    public void testIsFinal() {
        RawMethod rawMethod = new RawMethod(ResolvedObjectType.create(Object.class, null, null, null), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), finalMethodMethod);

        RawField rawField = new RawField(ResolvedObjectType.create(Object.class, null, null, null), serialVersionUIDField);
        RawField rawField1 = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), finalFieldField);
        RawField rawField2 = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), testField);

        assertFalse(rawMethod.isFinal());
        assertTrue(rawMethod1.isFinal());

        assertTrue(rawField.isFinal());
        assertTrue(rawField1.isFinal());
        assertFalse(rawField2.isFinal());
    }

    public void testIsPrivate() {
        RawMethod rawMethod = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), getTestMethod);
        RawMethod rawMethod1 = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), finalMethodMethod);

        RawField rawField = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), testField);
        RawField rawField1 = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), finalFieldField);

        assertTrue(rawMethod.isPrivate());
        assertFalse(rawMethod1.isPrivate());

        assertTrue(rawField.isPrivate());
        assertFalse(rawField1.isPrivate());
    }

    public void testIsProtected() {
        RawMethod rawMethod = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), getTestMethod);
        RawMethod rawMethod1 = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), finalMethodMethod);
        RawMethod rawMethod2 = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), protectedMethodMethod);

        RawField rawField = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), testField);
        RawField rawField1 = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), finalFieldField);
        RawField rawField2 = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), protectedFieldField);

        assertFalse(rawMethod.isProtected());
        assertFalse(rawMethod1.isProtected());
        assertTrue(rawMethod2.isProtected());

        assertFalse(rawField.isProtected());
        assertFalse(rawField1.isProtected());
        assertTrue(rawField2.isProtected());
    }

    public void testIsPublic() {
        RawMethod rawMethod = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), getTestMethod);
        RawMethod rawMethod1 = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), finalMethodMethod);

        RawField rawField = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), testField);
        RawField rawField1 = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), finalFieldField);

        assertFalse(rawMethod.isPublic());
        assertTrue(rawMethod1.isPublic());

        assertFalse(rawField.isPublic());
        assertTrue(rawField1.isPublic());
    }

    public void testRawMemberHashCode() {
        RawMethod rawMethod = new RawMethod(ResolvedObjectType.create(Object.class, null, null, null), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(ResolvedObjectType.create(RawMember.class, null, null, null), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), getTestMethod);

        RawField rawField = new RawField(ResolvedObjectType.create(Object.class, null, null, null), serialVersionUIDField);
        RawField rawStaticField = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), testField);

        assertEquals(toStringMethod.hashCode(), rawMethod.hashCode());
        assertEquals(getRawMemberMethod.hashCode(), rawMethod1.hashCode());
        assertEquals(getTestMethod.hashCode(), rawStaticMethod.hashCode());

        assertEquals(serialVersionUIDField.hashCode(), rawField.hashCode());
        assertEquals(testField.hashCode(), rawStaticField.hashCode());
    }

    public void testGetModifiers() {
        RawMethod rawMethod = new RawMethod(ResolvedObjectType.create(Object.class, null, null, null), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(ResolvedObjectType.create(RawMember.class, null, null, null), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), getTestMethod);

        RawField rawField = new RawField(ResolvedObjectType.create(Object.class, null, null, null), serialVersionUIDField);
        RawField rawStaticField = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), testField);

        RawConstructor rawConstructor = new RawConstructor(ResolvedObjectType.create(Object.class, null, null, null),
                objectConstructor);

        assertEquals(toStringMethod.getModifiers(), rawMethod.getModifiers());
        assertEquals(getRawMemberMethod.getModifiers(), rawMethod1.getModifiers());
        assertEquals(getTestMethod.getModifiers(), rawStaticMethod.getModifiers());

        assertEquals(serialVersionUIDField.getModifiers(), rawField.getModifiers());
        assertEquals(testField.getModifiers(), rawStaticField.getModifiers());

        assertEquals(objectConstructor.hashCode(), rawConstructor.hashCode());
    }

    public void testRawMemberToString() {
        RawMethod rawMethod = new RawMethod(ResolvedObjectType.create(Object.class, null, null, null), toStringMethod);
        RawMethod rawMethod1 = new RawMethod(ResolvedObjectType.create(RawMember.class, null, null, null), getRawMemberMethod);
        RawMethod rawStaticMethod = new RawMethod(ResolvedObjectType.create(ModifiersClass.class, null, null, null), getTestMethod);

        RawField rawField = new RawField(ResolvedObjectType.create(Object.class, null, null, null), serialVersionUIDField);
        RawField rawStaticField = new RawField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), testField);

        assertEquals(toStringMethod.getName(), rawMethod.toString());
        assertEquals(getRawMemberMethod.getName(), rawMethod1.toString());
        assertEquals(getTestMethod.getName(), rawStaticMethod.toString());

        assertEquals(serialVersionUIDField.getName(), rawField.toString());
        assertEquals(testField.getName(), rawStaticField.toString());
    }
}
