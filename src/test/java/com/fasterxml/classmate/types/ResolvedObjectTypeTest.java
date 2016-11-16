package com.fasterxml.classmate.types;

import java.util.*;

import com.fasterxml.classmate.BaseTest;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.members.RawConstructor;
import com.fasterxml.classmate.members.RawField;

import org.junit.Test;

/**
 * @author blangel
 */
public class ResolvedObjectTypeTest extends BaseTest
{
    private static class NoExplicitConstructor { }

    private static abstract class AbstractClass { }

    private static interface NoConstructor { }

    @Test
    public void testConstructors()
    {
        ResolvedObjectType resolvedObjectType = ResolvedObjectType.create(String.class, null, null, (List<ResolvedType>) null);
        assertNotNull(resolvedObjectType._superInterfaces);

        resolvedObjectType = ResolvedObjectType.create(String.class, null, null, Collections.<ResolvedType>emptyList());
        assertNotNull(resolvedObjectType._superInterfaces);

        List<ResolvedType> types = new ArrayList<ResolvedType>();
        types.add(resolvedObjectType);
        ResolvedObjectType resolvedObjectType1 = ResolvedObjectType.create(String.class, null, null, types);
        assertNotNull(resolvedObjectType1._superInterfaces);
        assertSame(resolvedObjectType, resolvedObjectType1._superInterfaces[0]);
    }

    @Test
    public void testGetArrayElementType() {
        ResolvedObjectType resolvedObjectType = ResolvedObjectType.create(String.class, null, null, (List<ResolvedType>) null);
        assertNull(resolvedObjectType.getArrayElementType());

        ResolvedObjectType resolvedObjectType1 = ResolvedObjectType.create(String.class, TypeBindings.emptyBindings(), resolvedObjectType,
                Collections.<ResolvedType>emptyList());
        assertNull(resolvedObjectType1.getArrayElementType());
    }

    @Test
    public void testGetStaticFields() {
        ResolvedObjectType objectType = ResolvedObjectType.create(Object.class, null, null, Collections.<ResolvedType>emptyList());
        List<RawField> staticFields = objectType.getStaticFields();
        assertEquals(0, staticFields.size());

        ResolvedObjectType stringType = ResolvedObjectType.create(String.class, null, objectType, Collections.<ResolvedType>emptyList());
        staticFields = stringType.getStaticFields();
        // 13-May-2013, tatu: Looks like Java 7 will add fourth, "HASHING_SEED"?

        // serialVersionUID & serialPersistentFields & CASE_INSENSITIVE_ORDER
        int count = staticFields.size();

        switch (count) {
        case 3: // Java 6
            matchRawMembers(staticFields, new String[] {
                    "serialVersionUID", "serialPersistentFields", "CASE_INSENSITIVE_ORDER"
            });
            break;
        case 4: // Java 7/8
            matchRawMembers(staticFields, new String[] {
                    "serialVersionUID", "serialPersistentFields", "CASE_INSENSITIVE_ORDER", "HASHING_SEED"
            });
            break;
        case 6: // Java 9
            matchRawMembers(staticFields, new String[] {
                    "serialVersionUID", "serialPersistentFields", "CASE_INSENSITIVE_ORDER",
                    "COMPACT_STRINGS", "LATIN1", "UTF16"
            });
            break;
        default:
            fail("Expected 3 (JDK 1.6), 4 (1.7/1.8) or 6 (1.9) static fields, got "+count+"; fields: "+staticFields);
        }
    }

    @Test
    public void testGetConstructors() {
        ResolvedObjectType noExplicitConstructorType = ResolvedObjectType.create(NoExplicitConstructor.class, null, null, Collections.<ResolvedType>emptyList());
        List<RawConstructor> constructors = noExplicitConstructorType.getConstructors();

        assertEquals(1, constructors.size());

        // abstract classes have constructors...they just can't be instantiated except via a sub-class instantiation
        ResolvedObjectType abstractClass = ResolvedObjectType.create(AbstractClass.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = abstractClass.getConstructors();
        assertEquals(1, constructors.size());

        ResolvedObjectType primitiveType = ResolvedObjectType.create(boolean.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = ResolvedObjectType.create(void.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = ResolvedObjectType.create(int.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = ResolvedObjectType.create(long.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = ResolvedObjectType.create(short.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = ResolvedObjectType.create(double.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = ResolvedObjectType.create(float.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = ResolvedObjectType.create(byte.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());

        ResolvedObjectType arrayType = ResolvedObjectType.create(Object[].class, null, null, Collections.<ResolvedType>emptyList());
        constructors = arrayType.getConstructors();
        assertEquals(0, constructors.size());

        ResolvedObjectType interfaceClass = ResolvedObjectType.create(NoConstructor.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = interfaceClass.getConstructors();
        assertEquals(0, constructors.size());
    }

}
