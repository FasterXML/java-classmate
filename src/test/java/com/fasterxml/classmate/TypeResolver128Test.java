package com.fasterxml.classmate;

import java.util.*;
import java.util.function.Supplier;

/**
 * Tests for [classmate#128]: caching of types with self-references via supertypes,
 * and equality of self-references with fully resolved types.
 */
public class TypeResolver128Test extends BaseTest
{
    static class Base<T> { }

    // Self-references via supertypes
    static class A extends Base<B> { }
    static class B extends Base<A> { }

    static class Mid extends Base<Outer> { }
    static class Outer extends Mid { }

    static class ArrA extends Base<ArrB[]> { }
    static class ArrB extends Base<ArrA> { }

    static class ListA extends Base<List<ListB>> { }
    static class ListB extends Base<ListA> { }

    static class SelfComparable implements Comparable<SelfComparable>, Supplier<String> {
        @Override
        public int compareTo(SelfComparable o) { return 0; }
        @Override
        public String get() { return null; }
    }

    // Self-references nested within other types
    static class LNode extends Base<List<LNode>> { }

    static class N<T> extends Base<N<N<T>>[]> { }

    // Raw self-reference with raw bound
    @SuppressWarnings("rawtypes")
    static class RawBound<T extends RawBound> extends Base<RawBound> { }

    /*
    /**********************************************************************
    /* Caching
    /**********************************************************************
     */

    public void testSelfReferenceViaSupertypeNotCached()
    {
        TypeResolver resolver = new TypeResolver();
        resolver.resolve(A.class);
        _verifyFullyResolved(resolver.resolve(B.class).getParentClass()
                .getTypeParameters().get(0), A.class);
        // but types themselves are cached
        assertSame(resolver.resolve(A.class), resolver.resolve(A.class));
        assertSame(resolver.resolve(B.class), resolver.resolve(B.class));
    }

    public void testSelfReferenceViaSuperclassNotCached()
    {
        TypeResolver resolver = new TypeResolver();
        resolver.resolve(Outer.class);
        _verifyFullyResolved(resolver.resolve(Mid.class).getParentClass()
                .getTypeParameters().get(0), Outer.class);
    }

    public void testSelfReferenceViaSupertypeInArrayNotCached()
    {
        TypeResolver resolver = new TypeResolver();
        resolver.resolve(ArrA.class);
        _verifyFullyResolved(resolver.resolve(ArrB.class).getParentClass()
                .getTypeParameters().get(0), ArrA.class);
        ResolvedType elem = resolver.resolve(ArrB[].class).getArrayElementType();
        _verifyFullyResolved(elem.getParentClass().getTypeParameters().get(0), ArrA.class);
    }

    public void testSelfReferenceViaSupertypeInTypeParameterNotCached()
    {
        TypeResolver resolver = new TypeResolver();
        resolver.resolve(ListA.class);
        ResolvedType listOfB = resolver.resolve(List.class, ListB.class);
        ResolvedType b = listOfB.getTypeParameters().get(0);
        _verifyFullyResolved(b.getParentClass().getTypeParameters().get(0), ListA.class);
    }

    // Types not containing self-references to types being resolved are still cached
    public void testTypesWithoutSelfReferencesCached()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType type = resolver.resolve(SelfComparable.class);
        ResolvedType supplier = type.findSupertype(Supplier.class);
        assertSame(supplier, resolver.resolve(Supplier.class, String.class));
        assertSame(type, resolver.resolve(SelfComparable.class));
    }

    /*
    /**********************************************************************
    /* Equality
    /**********************************************************************
     */

    public void testSelfReferenceInTypeParameterEquality()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType viaParent = resolver.resolve(LNode.class).getParentClass();
        ResolvedType direct = resolver.resolve(Base.class,
                resolver.resolve(List.class, LNode.class));
        assertTrue(TypeResolver.isSelfReference(viaParent.getTypeParameters().get(0)
                .getTypeParameters().get(0)));
        _verifyEqual(direct, viaParent);
    }

    public void testNestedSelfReferenceEquality()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType viaParent = resolver.resolve(N.class, String.class).getParentClass()
                .getTypeParameters().get(0);
        ResolvedType direct = resolver.arrayType(resolver.resolve(N.class,
                resolver.resolve(N.class, String.class)));
        assertTrue(TypeResolver.isSelfReference(viaParent.getArrayElementType()));
        _verifyEqual(direct, viaParent);

        // but not equal to differently parameterized type
        ResolvedType other = resolver.arrayType(resolver.resolve(N.class,
                resolver.resolve(N.class, Integer.class)));
        assertFalse(viaParent.equals(other));
        assertFalse(other.equals(viaParent));
    }

    public void testSelfReferenceResolvedForSubtype()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType viaParent = resolver.resolve(N.class, String.class).getParentClass();
        ResolvedType subtype = resolver.resolveSubtype(viaParent, N.class);
        assertEquals(resolver.resolve(N.class, String.class), subtype);
    }

    public void testRawSelfReferenceWithRawBound()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType type = resolver.resolve(RawBound.class);
        ResolvedType param = type.getParentClass().getTypeParameters().get(0);
        assertTrue(TypeResolver.isSelfReference(param));
        assertSame(RawBound.class, param.getErasedType());
        assertEquals(1, param.getTypeParameters().size());
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _verifyFullyResolved(ResolvedType type, Class<?> expType)
    {
        assertSame(expType, type.getErasedType());
        assertFalse(TypeResolver.isSelfReference(type));
        assertNotNull(type.getParentClass());
        assertSame(expType.getSuperclass(), type.getParentClass().getErasedType());
    }

    private void _verifyEqual(ResolvedType t1, ResolvedType t2)
    {
        assertEquals(t1, t2);
        assertEquals(t2, t1);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertEquals(1, new HashSet<ResolvedType>(Arrays.asList(t1, t2)).size());
    }
}
