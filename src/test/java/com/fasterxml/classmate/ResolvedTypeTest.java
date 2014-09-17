package com.fasterxml.classmate;

import com.fasterxml.classmate.types.ResolvedArrayType;
import com.fasterxml.classmate.types.ResolvedInterfaceType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.io.Serializable;
import java.nio.CharBuffer;
import java.util.List;

import static junit.framework.Assert.*;

public class ResolvedTypeTest
{
    // For [Issue#16]

    private static class Foo16 extends Bar16 { }

    private static class Bar16 extends Zen16<Bar16, Foo16> { }

    private static class Zen16<A, B extends A>  { }
    
    @Test
    public void canCreateSubtype() {
        ResolvedObjectType stringType = ResolvedObjectType.create(String.class, null, null, null);
        assertTrue(stringType.canCreateSubtype(String.class));
        assertFalse(stringType.canCreateSubtype(CharBuffer.class));

        ResolvedObjectType objectType = ResolvedObjectType.create(Object.class, null, null, null);
        assertTrue(objectType.canCreateSubtype(Object.class));
        assertTrue(objectType.canCreateSubtype(String.class));
        assertTrue(objectType.canCreateSubtype(CharBuffer.class));

        ResolvedArrayType arrayType = new ResolvedArrayType(String[].class, null, stringType);
        assertFalse(arrayType.canCreateSubtype(String[].class));
        assertFalse(arrayType.canCreateSubtype(CharBuffer[].class));
        assertFalse(arrayType.canCreateSubtype(String.class));

    }

    @Test
    public void typeParametersFor() {
        ResolvedObjectType stringType = ResolvedObjectType.create(String.class, null, null, null);
        assertNull(stringType.typeParametersFor(CharBuffer.class));
    }

    @Test
    public void findSupertype() {
        ResolvedInterfaceType comparableType = new ResolvedInterfaceType(Comparable.class, TypeBindings.create(String.class, ResolvedType.NO_TYPES), null);
        ResolvedObjectType stringType = new ResolvedObjectType(String.class, null, (ResolvedType) null, new ResolvedType[] { comparableType });
        assertNull(stringType.findSupertype(CharBuffer.class));

        assertNull(stringType.findSupertype(Serializable.class));

    }

    @Test
    public void isConcrete() {
        ResolvedObjectType stringType = ResolvedObjectType.create(String.class, null, null, null);
        assertTrue(stringType.isConcrete());
        ResolvedObjectType charBufferType = ResolvedObjectType.create(CharBuffer.class, null, null, null);
        assertFalse(charBufferType.isConcrete());
    }

    @Test
    public void accessors() {
        // the default accessor implementation is to return an empty-list; check for a new subtype
        ResolvedType type = new ResolvedType(String.class, null) {
            @Override public boolean canCreateSubtypes() {
                return false;
            }
            @Override public ResolvedType getParentClass() {
                return null;
            }
            @Override public ResolvedType getSelfReferencedType() {
                return null;
            }
            @Override public ResolvedType getArrayElementType() {
                return null;
            }
            @Override public List<ResolvedType> getImplementedInterfaces() {
                return null;
            }
            @Override public boolean isInterface() {
                return false;
            }
            @Override public boolean isAbstract() {
                return false;
            }
            @Override public boolean isArray() {
                return false;
            }
            @Override public boolean isPrimitive() {
                return false;
            }
            @Override public StringBuilder appendBriefDescription(StringBuilder sb) {
                return null;
            }
            @Override public StringBuilder appendFullDescription(StringBuilder sb) {
                return null;
            }
            @Override public StringBuilder appendSignature(StringBuilder sb) {
                return null;
            }
            @Override public StringBuilder appendErasedSignature(StringBuilder sb) {
                return null;
            }
        };
        assertEquals(0, type.getConstructors().size());
        assertEquals(0, type.getMemberFields().size());
        assertEquals(0, type.getMemberMethods().size());
        assertEquals(0, type.getStaticMethods().size());
        assertEquals(0, type.getStaticFields().size());
    }

    // For [Issue#16]
    @Test
    public void testIssue16()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType type = resolver.resolve(Bar16.class);
        assertNotNull(type);

        // We'll have the "default" constructor so
        assertEquals(1, type.getConstructors().size());
        assertEquals(0, type.getMemberFields().size());
        assertEquals(0, type.getMemberMethods().size());
        assertEquals(0, type.getStaticMethods().size());
        assertEquals(0, type.getStaticFields().size());

        assertEquals(Bar16.class, type.getErasedType());
        ResolvedType parent = type.getParentClass();
        assertNotNull(parent);
        assertEquals(Zen16.class, parent.getErasedType());

        List<ResolvedType> params = parent.getTypeParameters();
        assertNotNull(params);
        assertEquals(2, params.size());

        assertEquals(Bar16.class, params.get(0).getErasedType());
        assertEquals(Foo16.class, params.get(1).getErasedType());
    }
}
