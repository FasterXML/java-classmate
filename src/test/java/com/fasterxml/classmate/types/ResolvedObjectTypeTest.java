package com.fasterxml.classmate.types;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.members.RawConstructor;
import com.fasterxml.classmate.members.RawField;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/12/12
 * Time: 4:59 PM
 */
public class ResolvedObjectTypeTest {

    private static class NoExplicitConstructor { }

    private static abstract class AbstractClass { }

    private static interface NoConstructor { }

    @Test
    public void constructors() {
        ResolvedObjectType resolvedObjectType = new ResolvedObjectType(String.class, null, null, (List<ResolvedType>) null);
        assertNotNull(resolvedObjectType._superInterfaces);

        resolvedObjectType = new ResolvedObjectType(String.class, null, null, Collections.<ResolvedType>emptyList());
        assertNotNull(resolvedObjectType._superInterfaces);

        List<ResolvedType> types = new ArrayList<ResolvedType>();
        types.add(resolvedObjectType);
        ResolvedObjectType resolvedObjectType1 = new ResolvedObjectType(String.class, null, null, types);
        assertNotNull(resolvedObjectType1._superInterfaces);
        assertSame(resolvedObjectType, resolvedObjectType1._superInterfaces[0]);
    }

    @Test
    public void getArrayElementType() {
        ResolvedObjectType resolvedObjectType = new ResolvedObjectType(String.class, null, null, (List<ResolvedType>) null);
        assertNull(resolvedObjectType.getArrayElementType());

        ResolvedObjectType resolvedObjectType1 = new ResolvedObjectType(String.class, TypeBindings.emptyBindings(), resolvedObjectType,
                Collections.<ResolvedType>emptyList());
        assertNull(resolvedObjectType1.getArrayElementType());
    }

    @Test
    public void getStaticFields() {
        ResolvedObjectType objectType = new ResolvedObjectType(Object.class, null, null, Collections.<ResolvedType>emptyList());
        List<RawField> staticFields = objectType.getStaticFields();
        assertEquals(0, staticFields.size());

        ResolvedObjectType stringType = new ResolvedObjectType(String.class, null, objectType, Collections.<ResolvedType>emptyList());
        staticFields = stringType.getStaticFields();
        // serialVersionUID & serialPersistentFields & CASE_INSENSITIVE_ORDER
        assertEquals(3, staticFields.size());
    }

    @Test
    public void getConstructors() {
        ResolvedObjectType noExplicitConstructorType = new ResolvedObjectType(NoExplicitConstructor.class, null, null, Collections.<ResolvedType>emptyList());
        List<RawConstructor> constructors = noExplicitConstructorType.getConstructors();

        assertEquals(1, constructors.size());

        // abstract classes have constructors...they just can't be instantiated except via a sub-class instantiation
        ResolvedObjectType abstractClass = new ResolvedObjectType(AbstractClass.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = abstractClass.getConstructors();
        assertEquals(1, constructors.size());

        ResolvedObjectType primitiveType = new ResolvedObjectType(boolean.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = new ResolvedObjectType(void.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = new ResolvedObjectType(int.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = new ResolvedObjectType(long.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = new ResolvedObjectType(short.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = new ResolvedObjectType(double.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = new ResolvedObjectType(float.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());
        primitiveType = new ResolvedObjectType(byte.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = primitiveType.getConstructors();
        assertEquals(0, constructors.size());

        ResolvedObjectType arrayType = new ResolvedObjectType(Object[].class, null, null, Collections.<ResolvedType>emptyList());
        constructors = arrayType.getConstructors();
        assertEquals(0, constructors.size());

        ResolvedObjectType interfaceClass = new ResolvedObjectType(NoConstructor.class, null, null, Collections.<ResolvedType>emptyList());
        constructors = interfaceClass.getConstructors();
        assertEquals(0, constructors.size());
    }

}
