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
 * Time: 12:29 PM
 */
public class ResolvedFieldTest {

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
        ResolvedField resolvedField = new ResolvedField(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), null, serialVersionUIDField, null);
        // referential equality
        assertTrue(resolvedField.equals(resolvedField));

        // null test
        assertFalse(resolvedField.equals(null));

        // invalid class
        assertFalse(resolvedField.equals("not a ResolvedField"));

        // test inequality of field
        ResolvedField resolvedField1 = new ResolvedField(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), null, testField, null);
        assertFalse(resolvedField.equals(resolvedField1));
        assertFalse(resolvedField1.equals(resolvedField));

        // test equality of field
        ResolvedField resolvedField2 = new ResolvedField(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), null, testField, null);
        ResolvedField resolvedField3 = new ResolvedField(new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES), null, serialVersionUIDField, null);

        assertTrue(resolvedField.equals(resolvedField3));
        assertTrue(resolvedField3.equals(resolvedField));

        assertTrue(resolvedField1.equals(resolvedField2));
        assertTrue(resolvedField2.equals(resolvedField1));

    }

}
