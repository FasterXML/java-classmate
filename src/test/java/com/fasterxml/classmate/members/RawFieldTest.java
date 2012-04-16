package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.reflect.Field;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 11:44 AM
 */
public class RawFieldTest {

    private static class HasStaticField {
        private static String test;
    }

    private static final Field serialVersionUIDField;
    private static final Field testField;
    static {
        try {
            serialVersionUIDField = String.class.getDeclaredField("serialVersionUID");
            testField = HasStaticField.class.getDeclaredField("test");
        } catch (NoSuchFieldException nsfe) {
            throw new AssertionError(nsfe);
        }
    }

    @Test
    public void equals() {
        // test referential equality
        RawField rawField = new RawField(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        assertTrue(rawField.equals(rawField));

        // test null
        assertFalse(rawField.equals(null));

        // test different class
        assertFalse(rawField.equals("not a RawField"));

        // test unequal fields
        RawField rawField1 = new RawField(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), testField);
        assertFalse(rawField.equals(rawField1));
        assertFalse(rawField1.equals(rawField));

        RawField rawField2 = new RawField(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), testField);
        RawField rawField3 = new RawField(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), serialVersionUIDField);
        assertTrue(rawField.equals(rawField3));
        assertTrue(rawField3.equals(rawField));
        assertTrue(rawField1.equals(rawField2));
        assertTrue(rawField2.equals(rawField1));
    }

}
