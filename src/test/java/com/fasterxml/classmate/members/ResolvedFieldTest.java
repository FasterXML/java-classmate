package com.fasterxml.classmate.members;

import com.fasterxml.classmate.types.ResolvedObjectType;

import org.junit.Test;

import java.lang.reflect.Field;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings("deprecation")
public class ResolvedFieldTest {

    @SuppressWarnings("unused")
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
    public void isTransient() {
        ResolvedField rawField = new ResolvedField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), null, transientFieldField, null);
        ResolvedField rawField1 = new ResolvedField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), null, testField, null);

        assertTrue(rawField.isTransient());
        assertFalse(rawField1.isTransient());
    }

    @Test
    public void isVolatile() {
        ResolvedField rawField = new ResolvedField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), null, volatileFieldField, null);
        ResolvedField rawField1 = new ResolvedField(ResolvedObjectType.create(ModifiersClass.class, null, null, null), null, testField, null);

        assertTrue(rawField.isVolatile());
        assertFalse(rawField1.isVolatile());
    }

    @Test
    public void equals() {
        ResolvedField resolvedField = new ResolvedField(ResolvedObjectType.create(String.class, null, null, null), null, serialVersionUIDField, null);
        // referential equality
        assertTrue(resolvedField.equals(resolvedField));

        // null test
        assertFalse(resolvedField.equals(null));

        // invalid class
        assertFalse(resolvedField.equals("not a ResolvedField"));

        // test inequality of field
        ResolvedField resolvedField1 = new ResolvedField(ResolvedObjectType.create(String.class, null, null, null), null, testField, null);
        assertFalse(resolvedField.equals(resolvedField1));
        assertFalse(resolvedField1.equals(resolvedField));

        // test equality of field
        ResolvedField resolvedField2 = new ResolvedField(ResolvedObjectType.create(String.class, null, null, null), null, testField, null);
        ResolvedField resolvedField3 = new ResolvedField(ResolvedObjectType.create(String.class, null, null, null), null, serialVersionUIDField, null);

        assertTrue(resolvedField.equals(resolvedField3));
        assertTrue(resolvedField3.equals(resolvedField));

        assertTrue(resolvedField1.equals(resolvedField2));
        assertTrue(resolvedField2.equals(resolvedField1));

    }

}
