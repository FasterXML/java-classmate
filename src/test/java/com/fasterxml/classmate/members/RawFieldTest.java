package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 11:44 AM
 */
public class RawFieldTest {

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
            new RawField(null, null);
        } catch (NullPointerException npe) {
            fail(npe.getMessage());
        }
    }

    @Test
    public void isTransient() {
        RawField rawField = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), transientFieldField);
        RawField rawField1 = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);

        assertTrue(rawField.isTransient());
        assertFalse(rawField1.isTransient());
    }

    @Test
    public void isVolatile() {
        RawField rawField = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), volatileFieldField);
        RawField rawField1 = new RawField(new ResolvedObjectType(ModifiersClass.class, null, null, ResolvedType.NO_TYPES), testField);

        assertTrue(rawField.isVolatile());
        assertFalse(rawField1.isVolatile());
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
