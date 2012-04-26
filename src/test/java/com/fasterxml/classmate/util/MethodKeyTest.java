package com.fasterxml.classmate.util;

import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/12/12
 * Time: 2:50 PM
 */
public class MethodKeyTest {

    @SuppressWarnings("serial")
    private static class MethodKeySubclass extends MethodKey {
        private MethodKeySubclass(String name) {
            super(name);
        }
    }

    @Test
    public void equals() {

        // test referential equality
        MethodKey key1 = new MethodKey("test");
        assertTrue(key1.equals(key1));

        // test null
        assertFalse(key1.equals(null));

        // test unequal class
        assertFalse(key1.equals("test"));

        // test subclass
        MethodKeySubclass methodKeySubclass = new MethodKeySubclass("test");
        assertFalse(key1.equals(methodKeySubclass));

        // test name equals
        MethodKey key2 = new MethodKey("test");
        assertTrue(key1.equals(key2));

        // test name not equals
        MethodKey key3 = new MethodKey("test3");
        assertFalse(key1.equals(key3));

        // test arguments not equal
        MethodKey key4 = new MethodKey("test", new Class[] { String.class });
        assertFalse(key1.equals(key4));

        // test arguments equal (non-zero)
        MethodKey key5 = new MethodKey("test", new Class[] { String.class });
        assertTrue(key4.equals(key5));

        // test arguments not equal (length-wise)
        MethodKey key6 = new MethodKey("test", new Class[] { String.class, Object.class});
        assertFalse(key5.equals(key6));

        // test arguments not equal (type-wise)
        MethodKey key7 = new MethodKey("test", new Class[] { Object.class, String.class});
        assertFalse(key6.equals(key7));

        // test equals (with arguments)
        MethodKey key8 = new MethodKey("test", new Class[] { Object.class, String.class});
        assertEquals(key7, key8);

    }

    @Test
    public void methodKeyToString() {
        MethodKey key = new MethodKey("test");
        assertEquals("test()", key.toString());

        key = new MethodKey("test", new Class[] { String.class });
        assertEquals("test(java.lang.String)", key.toString());

        key = new MethodKey("test", new Class[] { String.class, Object[].class });
        assertEquals("test(java.lang.String,[Ljava.lang.Object;)", key.toString());

        key = new MethodKey("test", new Class[] { boolean.class });
        assertEquals("test(boolean)", key.toString());

        key = new MethodKey("test", new Class[] { void.class });
        assertEquals("test(void)", key.toString());
    }

}
