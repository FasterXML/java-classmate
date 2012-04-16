package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 12:56 PM
 */
public class HierarchicTypeTest {

    @Test
    public void hierarchicTypeToString() {
        HierarchicType hierarchicType = new HierarchicType(null, false, 0);
        try {
            hierarchicType.toString();
            fail("Expected a NullPointerException as the type was not specified.");
        } catch (NullPointerException npe) {
            // expected
        }
        ResolvedObjectType stringType = new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES);
        hierarchicType = new HierarchicType(stringType, false, 0);
        assertEquals(stringType.toString(), hierarchicType.toString());
    }

    @Test
    public void hierarchicTypeHashCode() {
        HierarchicType hierarchicType = new HierarchicType(null, false, 0);
        try {
            int hashCode = hierarchicType.hashCode();
            fail(String.format("Expected a NullPointerException as the type was not specified [ got %d ].", hashCode));
        } catch (NullPointerException npe) {
            // expected
        }
        ResolvedObjectType stringType = new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES);
        hierarchicType = new HierarchicType(stringType, false, 0);
        assertEquals(stringType.hashCode(), hierarchicType.hashCode());
    }

    @Test
    public void equals() {
        HierarchicType hierarchicType = new HierarchicType(null, false, 0);
        // test referential
        assertTrue(hierarchicType.equals(hierarchicType));

        // test null
        assertFalse(hierarchicType.equals(null));

        // test different class
        assertFalse(hierarchicType.equals("not a HierarchicType"));

        // test NPE
        HierarchicType hierarchicType1 = new HierarchicType(null, false, 0);
        try {
            boolean result = hierarchicType.equals(hierarchicType1);
            fail(String.format("Expected a NullPointerException as the type was not specified [ got %b ].", result));
        } catch (NullPointerException npe) {
            // expected
        }

        // test unequal types
        ResolvedObjectType stringType = new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES);
        hierarchicType = new HierarchicType(stringType, false, 0);
        ResolvedObjectType objectType = new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES);
        hierarchicType1 = new HierarchicType(objectType, false, 0);
        assertFalse(hierarchicType.equals(hierarchicType1));
        assertFalse(hierarchicType1.equals(hierarchicType));

        // test equal
        HierarchicType hierarchicType2 = new HierarchicType(stringType, false, 0);
        HierarchicType hierarchicType3 = new HierarchicType(objectType, false, 0);

        assertTrue(hierarchicType.equals(hierarchicType2));
        assertTrue(hierarchicType2.equals(hierarchicType));

        assertTrue(hierarchicType1.equals(hierarchicType3));
        assertTrue(hierarchicType3.equals(hierarchicType1));

        // test equal even though mixin and priority are different
        hierarchicType2 = new HierarchicType(stringType, true, 0);
        assertTrue(hierarchicType.equals(hierarchicType2));
        assertTrue(hierarchicType2.equals(hierarchicType));
        hierarchicType2 = new HierarchicType(stringType, false, 1);
        assertTrue(hierarchicType.equals(hierarchicType2));
        assertTrue(hierarchicType2.equals(hierarchicType));
        hierarchicType2 = new HierarchicType(stringType, false, 1);
        assertTrue(hierarchicType.equals(hierarchicType2));
        assertTrue(hierarchicType2.equals(hierarchicType));
    }

}
