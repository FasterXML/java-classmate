package com.fasterxml.classmate.util;

import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * @author blangel
 */
public class ClassKeyTest {

    @SuppressWarnings("serial")
    private static class ClassKeySubclass extends ClassKey {
        private ClassKeySubclass(Class<?> clz) {
            super(clz);
        }
    }

    @SuppressWarnings("unused")
    @Test
    public void nullClass() {
        try {
            new ClassKey(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test
    public void equals() {
        ClassKey key = new ClassKey(String.class);

        // test referential equality
        assertTrue(key.equals(key));

        // test null
        assertFalse(key.equals(null));

        // test unequal class
        assertFalse(key.equals("test"));

        // test subclass
        assertFalse(key.equals(new ClassKeySubclass(String.class)));

        // test not equals classes
        assertFalse(key.equals(new ClassKey(Object.class)));

        // test equal classes
        assertTrue(key.equals(new ClassKey(String.class)));

        // classes loaded by different class-loaders are not equal!
        // TODO
    }

    @Test
    public void classKeyToString() {
        ClassKey key = new ClassKey(String.class);
        assertEquals(String.class.getName(), key.toString());
    }

    @Test
    public void compareTo() {
        ClassKey key = new ClassKey(String.class);
        ClassKey key1 = new ClassKey(Object.class);

        assertEquals(0, key.compareTo(key));

        assertTrue(key.compareTo(key1) > 0);

        assertTrue(key1.compareTo(key) < 0);
    }

}
