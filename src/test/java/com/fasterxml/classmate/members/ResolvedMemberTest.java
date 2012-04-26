package com.fasterxml.classmate.members;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 12:01 PM
 */
public class ResolvedMemberTest {

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Decorate { }

    private static class HasStaticFieldMethod {
        private static String test;
        private static String getTest() {
            return test;
        }
    }

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
            finalMethodMethod = ModifiersClass.class.getDeclaredMethod("finalMethod");
            protectedMethodMethod = ModifiersClass.class.getDeclaredMethod("protectedMethod");
            getTestMethod = HasStaticFieldMethod.class.getDeclaredMethod("getTest");
            finalFieldField = ModifiersClass.class.getDeclaredField("finalField");
            protectedFieldField = ModifiersClass.class.getDeclaredField("protectedField");
            objectConstructor = Object.class.getDeclaredConstructor();
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        } catch (NoSuchFieldException nsfe) {
            throw new AssertionError(nsfe);
        }
        try {
            serialVersionUIDField = String.class.getDeclaredField("serialVersionUID");
            testField = HasStaticFieldMethod.class.getDeclaredField("test");
        } catch (NoSuchFieldException nsfe) {
            throw new AssertionError(nsfe);
        }
    }

    @Test
    public void isFinal() {
        ResolvedMethod rawMethod = new ResolvedMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, toStringMethod, null, ResolvedType.NO_TYPES);
        ResolvedMethod rawMethod1 = new ResolvedMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, finalMethodMethod, null, ResolvedType.NO_TYPES);

        ResolvedField rawField = new ResolvedField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, serialVersionUIDField, null);
        ResolvedField rawField1 = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, finalFieldField, null);
        ResolvedField rawField2 = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, testField, null);

        assertFalse(rawMethod.isFinal());
        assertTrue(rawMethod1.isFinal());

        assertTrue(rawField.isFinal());
        assertTrue(rawField1.isFinal());
        assertFalse(rawField2.isFinal());
    }

    @Test
    public void isPrivate() {
        ResolvedMethod rawMethod = new ResolvedMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, getTestMethod, null, ResolvedType.NO_TYPES);
        ResolvedMethod rawMethod1 = new ResolvedMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, finalMethodMethod, null, ResolvedType.NO_TYPES);

        ResolvedField rawField = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, testField, null);
        ResolvedField rawField1 = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, finalFieldField, null);

        assertTrue(rawMethod.isPrivate());
        assertFalse(rawMethod1.isPrivate());

        assertTrue(rawField.isPrivate());
        assertFalse(rawField1.isPrivate());
    }

    @Test
    public void isProtected() {
        ResolvedMethod rawMethod = new ResolvedMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, getTestMethod, null, ResolvedType.NO_TYPES);
        ResolvedMethod rawMethod1 = new ResolvedMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, finalMethodMethod, null, ResolvedType.NO_TYPES);
        ResolvedMethod rawMethod2 = new ResolvedMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, protectedMethodMethod, null, ResolvedType.NO_TYPES);

        ResolvedField rawField = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, testField, null);
        ResolvedField rawField1 = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, finalFieldField, null);
        ResolvedField rawField2 = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, protectedFieldField, null);

        assertFalse(rawMethod.isProtected());
        assertFalse(rawMethod1.isProtected());
        assertTrue(rawMethod2.isProtected());

        assertFalse(rawField.isProtected());
        assertFalse(rawField1.isProtected());
        assertTrue(rawField2.isProtected());
    }

    @Test
    public void isPublic() {
        ResolvedMethod rawMethod = new ResolvedMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, getTestMethod, null, ResolvedType.NO_TYPES);
        ResolvedMethod rawMethod1 = new ResolvedMethod(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, finalMethodMethod, null, ResolvedType.NO_TYPES);

        ResolvedField rawField = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, testField, null);
        ResolvedField rawField1 = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, finalFieldField, null);

        assertFalse(rawMethod.isPublic());
        assertTrue(rawMethod1.isPublic());

        assertFalse(rawField.isPublic());
        assertTrue(rawField1.isPublic());
    }

    @Test
    public void applyOverride() throws NoSuchMethodException {
        ResolvedObjectType string = new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES);
        Annotations annotations = new Annotations();
        ResolvedMethod resolvedMethod = new ResolvedMethod(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                                                           annotations, toStringMethod, string, ResolvedType.NO_TYPES);
        Method thisMethod = ResolvedMemberTest.class.getDeclaredMethod("applyOverride");
        Annotation testAnnotation = thisMethod.getAnnotation(Test.class);
        assertNull(annotations.get(Test.class));
        resolvedMethod.applyOverride(testAnnotation);
        assertNotNull(annotations.get(Test.class));
    }

    @Test @Decorate
    public void applyOverrides() throws NoSuchMethodException {
        ResolvedObjectType string = new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES);
        Annotations annotations = new Annotations();
        ResolvedMethod resolvedMethod = new ResolvedMethod(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                annotations, toStringMethod, string, ResolvedType.NO_TYPES);
        Method thisMethod = ResolvedMemberTest.class.getDeclaredMethod("applyOverrides");
        Annotation testAnnotation = thisMethod.getAnnotation(Test.class);
        Annotation decorateAnnotation = thisMethod.getAnnotation(Decorate.class);
        assertNull(annotations.get(Test.class));
        assertNull(annotations.get(Decorate.class));
        Annotations addAll = new Annotations();
        addAll.add(testAnnotation);
        addAll.add(decorateAnnotation);
        resolvedMethod.applyOverrides(addAll);
        assertNotNull(annotations.get(Test.class));
        assertNotNull(annotations.get(Decorate.class));
    }

    @Test
    public void applyDefault() throws NoSuchMethodException  {
        ResolvedObjectType string = new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES);
        Annotations annotations = new Annotations();
        ResolvedMethod resolvedMethod = new ResolvedMethod(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES),
                annotations, toStringMethod, string, ResolvedType.NO_TYPES);
        Method thisMethod = ResolvedMemberTest.class.getDeclaredMethod("applyDefault");
        Annotation testAnnotation = thisMethod.getAnnotation(Test.class);
        assertNull(annotations.get(Test.class));
        resolvedMethod.applyDefault(testAnnotation);
        assertNotNull(annotations.get(Test.class));
        // get different Test annotation and try to add
        Method otherMethod = ResolvedMemberTest.class.getDeclaredMethod("applyOverrides");
        Annotation otherTestAnnotation = otherMethod.getAnnotation(Test.class);
        assertNotSame(testAnnotation, otherTestAnnotation);
        resolvedMethod.applyDefault(otherTestAnnotation);
        assertSame(testAnnotation, annotations.get(Test.class));
    }

    @Test
    public void isStatic() {
        ResolvedMethod resolvedMethod = new ResolvedMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, toStringMethod, null, null);
        ResolvedMethod resolvedMethod1 = new ResolvedMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), null, getRawMemberMethod, null, null);
        ResolvedMethod resolvedStaticMethod = new ResolvedMethod(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), null, getTestMethod, null, null);

        ResolvedField resolvedField = new ResolvedField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, serialVersionUIDField, null);
        ResolvedField resolvedStaticField = new ResolvedField(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), null, testField, null);

        assertFalse(resolvedMethod.isStatic());
        assertFalse(resolvedMethod1.isStatic());

        assertTrue(resolvedField.isStatic());
        assertTrue(resolvedStaticMethod.isStatic());
        assertTrue(resolvedStaticField.isStatic());
    }

    @Test
    public void resolvedMemberHashCode() {
        ResolvedMethod resolvedMethod = new ResolvedMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, toStringMethod, null, null);
        ResolvedMethod resolvedMethod1 = new ResolvedMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), null, getRawMemberMethod, null, null);
        ResolvedMethod resolvedStaticMethod = new ResolvedMethod(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), null, getTestMethod, null, null);

        ResolvedField resolvedField = new ResolvedField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, serialVersionUIDField, null);
        ResolvedField resolvedStaticField = new ResolvedField(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), null, testField, null);

        ResolvedConstructor resolvedConstructor = new ResolvedConstructor(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, objectConstructor, ResolvedType.NO_TYPES);

        assertEquals(toStringMethod.hashCode(), resolvedMethod.hashCode());
        assertEquals(getRawMemberMethod.hashCode(), resolvedMethod1.hashCode());
        assertEquals(getTestMethod.hashCode(), resolvedStaticMethod.hashCode());

        assertEquals(serialVersionUIDField.hashCode(), resolvedField.hashCode());
        assertEquals(testField.hashCode(), resolvedStaticField.hashCode());

        assertEquals(objectConstructor.hashCode(), resolvedConstructor.hashCode());
    }

    @Test
    public void resolvedMemberToString() {
        ResolvedMethod resolvedMethod = new ResolvedMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, toStringMethod, null, null);
        ResolvedMethod resolvedMethod1 = new ResolvedMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), null, getRawMemberMethod, null, null);
        ResolvedMethod resolvedStaticMethod = new ResolvedMethod(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), null, getTestMethod, null, null);

        ResolvedField resolvedField = new ResolvedField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, serialVersionUIDField, null);
        ResolvedField resolvedStaticField = new ResolvedField(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), null, testField, null);

        assertEquals(toStringMethod.getName(), resolvedMethod.toString());
        assertEquals(getRawMemberMethod.getName(), resolvedMethod1.toString());
        assertEquals(getTestMethod.getName(), resolvedStaticMethod.toString());

        assertEquals(serialVersionUIDField.getName(), resolvedField.toString());
        assertEquals(testField.getName(), resolvedStaticField.toString());
    }

    @Test
    public void getModifiers() {
        ResolvedMethod resolvedMethod = new ResolvedMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, toStringMethod, null, null);
        ResolvedMethod resolvedMethod1 = new ResolvedMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), null, getRawMemberMethod, null, null);
        ResolvedMethod resolvedStaticMethod = new ResolvedMethod(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), null, getTestMethod, null, null);

        ResolvedField resolvedField = new ResolvedField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, serialVersionUIDField, null);
        ResolvedField resolvedStaticField = new ResolvedField(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), null, testField, null);

        assertEquals(toStringMethod.getModifiers(), resolvedMethod.getModifiers());
        assertEquals(getRawMemberMethod.getModifiers(), resolvedMethod1.getModifiers());
        assertEquals(getTestMethod.getModifiers(), resolvedStaticMethod.getModifiers());

        assertEquals(serialVersionUIDField.getModifiers(), resolvedField.getModifiers());
        assertEquals(testField.getModifiers(), resolvedStaticField.getModifiers());
    }

    @Test
    public void get() throws NoSuchMethodException {
        // test NPE first
        try {
            ResolvedMethod npeMethod = new ResolvedMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, toStringMethod, null, null);
            npeMethod.get(Test.class);
            fail("Expecting a NullPointerException; haven't passed in an Annotations object reference to the constructor.");
        } catch (NullPointerException npe) {
            // expected
        }

        Annotations annotations = new Annotations();
        ResolvedMethod resolvedMethod = new ResolvedMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), annotations, toStringMethod, null, null);
        ResolvedMethod resolvedMethod1 = new ResolvedMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), annotations, getRawMemberMethod, null, null);
        ResolvedMethod resolvedStaticMethod = new ResolvedMethod(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), annotations, getTestMethod, null, null);

        ResolvedField resolvedField = new ResolvedField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), annotations, serialVersionUIDField, null);
        ResolvedField resolvedStaticField = new ResolvedField(new ResolvedObjectType(HasStaticFieldMethod.class, null, null, ResolvedType.NO_TYPES), annotations, testField, null);

        assertNull(resolvedMethod.get(Test.class));
        assertNull(resolvedMethod1.get(Test.class));
        assertNull(resolvedStaticMethod.get(Test.class));
        assertNull(resolvedField.get(Test.class));
        assertNull(resolvedStaticField.get(Test.class));

        Method thisMethod = ResolvedMemberTest.class.getDeclaredMethod("get");
        Annotation testAnnotation = thisMethod.getAnnotation(Test.class);

        annotations.add(testAnnotation);

        assertSame(testAnnotation, resolvedMethod.get(Test.class));
        assertSame(testAnnotation, resolvedMethod1.get(Test.class));
        assertSame(testAnnotation, resolvedStaticMethod.get(Test.class));
        assertSame(testAnnotation, resolvedField.get(Test.class));
        assertSame(testAnnotation, resolvedStaticField.get(Test.class));
    }

}
