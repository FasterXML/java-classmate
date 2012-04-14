package com.fasterxml.classmate.members;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
    public void isAbstract() {
        ResolvedMethod resolvedMethod = new ResolvedMethod(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, toStringMethod, null, null);
        ResolvedMethod resolvedMethod1 = new ResolvedMethod(new ResolvedObjectType(RawMember.class, null, null, ResolvedType.NO_TYPES), null, getRawMemberMethod, null, null);
        ResolvedField resolvedField = new ResolvedField(new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES), null, serialVersionUIDField, null);

        assertFalse(resolvedMethod.isAbstract());
        assertFalse(resolvedField.isAbstract());
        assertTrue(resolvedMethod1.isAbstract());
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

        assertEquals(toStringMethod.getName().hashCode(), resolvedMethod.hashCode());
        assertEquals(getRawMemberMethod.getName().hashCode(), resolvedMethod1.hashCode());
        assertEquals(getTestMethod.getName().hashCode(), resolvedStaticMethod.hashCode());

        assertEquals(serialVersionUIDField.getName().hashCode(), resolvedField.hashCode());
        assertEquals(testField.getName().hashCode(), resolvedStaticField.hashCode());
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

}
