package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.reflect.Field;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 12:29 PM
 */
public class ResolvedFieldTest {

    private static class ModifiersClass {
        private static String test;
        private transient String transientField;
        private volatile String volatileField;
    }

    private static final Field serialVersionUIDField;
    private static final Field testField;
    private static final Field transientFieldField;
    private static final Field volatileFieldField;
    static {
        try {
            serialVersionUIDField = String.class.getDeclaredField("serialVersionUID");
            testField = ModifiersClass.class.getDeclaredField("test");
            transientFieldField = ModifiersClass.class.getDeclaredField("transientField");
            volatileFieldField = ModifiersClass.class.getDeclaredField("volatileField");
        } catch (NoSuchFieldException nsfe) {
            throw new AssertionError(nsfe);
        }
    }

    @Test
    public void init() {
        try {
            new ResolvedField(null, null, null, null);
        } catch (NullPointerException npe) {
            fail(npe.getMessage());
        }
    }

    @Test
    public void isTransient() {
        ResolvedField rawField = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, transientFieldField, null);
        ResolvedField rawField1 = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, testField, null);

        assertTrue(rawField.isTransient());
        assertFalse(rawField1.isTransient());
    }

    @Test
    public void isVolatile() {
        ResolvedField rawField = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, volatileFieldField, null);
        ResolvedField rawField1 = new ResolvedField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), null, testField, null);

        assertTrue(rawField.isVolatile());
        assertFalse(rawField1.isVolatile());
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
