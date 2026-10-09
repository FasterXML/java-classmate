package com.fasterxml.classmate;

import java.util.*;
import java.util.function.Supplier;

import com.fasterxml.classmate.members.ResolvedField;

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

    // Self-references via supertypes, escaping resolution via members, subtypes
    static class FieldBase<T> {
        public List<T> values;
        public T[] array;
    }
    static class FA extends FieldBase<FB> { }
    static class FB extends FieldBase<FA> { }
    static class FSub<T> extends FieldBase<T> { }

    static class Pair<A, B> { }
    static class Dup<E> extends Pair<E, E> { }
    enum Color { RED }
    static class SubPair<A, B> extends Pair<A, B> { }
    static class X extends Base<Y> { }
    static class Y extends Base<Z> { }
    static class Z extends Pair<X, List<Y>> { }

    // Cycle of 3 types via type parameters
    static class C1 extends Pair<C2, C3> { }
    static class C2 extends Pair<C3, C1> { }
    static class C3 extends Pair<C1, C2> { }

    // Many types referring to each other: incomplete types must be reused
    // within resolution (to avoid exponential resolution time)
    static class Base10<P0,P1,P2,P3,P4,P5,P6,P7,P8,P9> { }
    static class T0 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }
    static class T1 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }
    static class T2 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }
    static class T3 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }
    static class T4 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }
    static class T5 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }
    static class T6 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }
    static class T7 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }
    static class T8 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }
    static class T9 extends Base10<T0,T1,T2,T3,T4,T5,T6,T7,T8,T9> { }

    // Raw self-reference to generic type
    @SuppressWarnings("rawtypes")
    static class GMid<T> extends Base<GOuter> {
        public T value;
    }
    static class GOuter extends GMid { }

    // Self-reference to enclosing type within bound
    static class BMid<X> { }
    @SuppressWarnings("rawtypes")
    static class BOuter<T extends BMid<BOuter>> { }

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

    // Incomplete types obtained from resolved type must not be cached when used
    // for resolving other types later on
    public void testIncompleteTypeViaMembersNotCached()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedTypeWithMembers members = new MemberResolver(resolver)
                .resolve(resolver.resolve(FA.class), null, null);
        assertEquals(2, members.getMemberFields().length);
        // member types themselves must not contain incomplete types either
        for (ResolvedField field : members.getMemberFields()) {
            ResolvedType type = field.getType();
            ResolvedType b = type.isArray() ? type.getArrayElementType()
                    : type.getTypeParameters().get(0);
            _verifyFullyResolved(b.getParentClass().getTypeParameters().get(0), FA.class);
        }
        // and are cached
        ResolvedTypeWithMembers members2 = new MemberResolver(resolver)
                .resolve(resolver.resolve(FA.class), null, null);
        assertSame(members.getMemberFields()[0].getType(), members2.getMemberFields()[0].getType());

        ResolvedType listOfB = resolver.resolve(List.class, FB.class);
        _verifyFullyResolved(listOfB.getTypeParameters().get(0).getParentClass()
                .getTypeParameters().get(0), FA.class);
        ResolvedType arrayOfB = resolver.resolve(FB[].class);
        _verifyFullyResolved(arrayOfB.getArrayElementType().getParentClass()
                .getTypeParameters().get(0), FA.class);
    }

    public void testIncompleteTypeViaArrayTypeNotCached()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType b = resolver.resolve(FA.class).getParentClass().getTypeParameters().get(0);
        resolver.arrayType(b);
        _verifyFullyResolved(resolver.resolve(FB[].class).getArrayElementType()
                .getParentClass().getTypeParameters().get(0), FA.class);
    }

    public void testIncompleteTypeViaSubtypeNotCached()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType baseOfB = resolver.resolve(FA.class).getParentClass();
        ResolvedType sub = resolver.resolveSubtype(baseOfB, FSub.class);
        // incomplete type itself is not included in subtype either
        _verifyFullyResolved(sub.getTypeParameters().get(0).getParentClass()
                .getTypeParameters().get(0), FA.class);
        _verifyFullyResolved(resolver.resolve(FieldBase.class, FB.class).getTypeParameters()
                .get(0).getParentClass().getTypeParameters().get(0), FA.class);
        _verifyFullyResolved(resolver.resolve(FSub.class, FB.class).getTypeParameters()
                .get(0).getParentClass().getTypeParameters().get(0), FA.class);
    }

    public void testIncompleteReferencedTypeViaSubtypeNotCached()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType y = resolver.resolve(X.class).getParentClass().getTypeParameters().get(0);
        ResolvedType z = y.getParentClass().getTypeParameters().get(0);
        // `Pair<X, List<Y>>` with self-references to `X` and `Y`
        ResolvedType pair = z.getParentClass();
        assertTrue(TypeResolver.isSelfReference(pair.getTypeParameters().get(0)));
        resolver.resolveSubtype(pair, SubPair.class);
        ResolvedType listOfY = resolver.resolve(List.class, Y.class);
        ResolvedType z2 = listOfY.getTypeParameters().get(0).getParentClass()
                .getTypeParameters().get(0);
        _verifyFullyResolved(z2.getParentClass().getTypeParameters().get(0), X.class);
    }

    // Subtyping types with self-references to incomplete types, cyclic via
    // type parameters, must terminate and produce stand-alone types
    public void testSubtypeWithCyclicIncompleteTypes()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType c2 = resolver.resolve(C1.class).getParentClass().getTypeParameters().get(0);
        ResolvedType c3 = c2.getParentClass().getTypeParameters().get(0);
        // `Pair<C1, C2>` with self-references to both
        ResolvedType pair = c3.getParentClass();
        assertTrue(TypeResolver.isSelfReference(pair.getTypeParameters().get(0)));
        assertTrue(TypeResolver.isSelfReference(pair.getTypeParameters().get(1)));
        ResolvedType sub = resolver.resolveSubtype(pair, SubPair.class);
        assertSame(SubPair.class, sub.getErasedType());
        _verifyFullyResolved(sub.getTypeParameters().get(0), C1.class);
        _verifyFullyResolved(sub.getTypeParameters().get(1), C2.class);
        assertEquals(resolver.resolve(SubPair.class, C1.class, C2.class), sub);
    }

    // Incomplete types must be completed by all `resolve()` methods
    public void testIncompleteTypeResolvedDirectly()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType b = resolver.resolve(A.class).getParentClass().getTypeParameters().get(0);
        ResolvedType direct = resolver.resolve(b);
        assertNotSame(b, direct);
        _verifyFullyResolved(direct.getParentClass().getTypeParameters().get(0), A.class);
        assertSame(resolver.resolve(B.class), direct);
        assertSame(direct, resolver.resolve(TypeBindings.emptyBindings(), b));
    }

    // Self-references to enclosing types (within incomplete types) are valid as-is,
    // so subtyping to the type itself must not change it
    public void testSubtypeRetainsSelfReferenceToEnclosingType()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType outer = resolver.resolve(BOuter.class);
        ResolvedType mid = outer.getTypeParameters().get(0);
        assertTrue(TypeResolver.isSelfReference(mid.getTypeParameters().get(0)));
        ResolvedType sub = resolver.resolveSubtype(outer, BOuter.class);
        assertEquals(outer, sub);
        assertEquals(sub, outer);
        assertSame(sub, resolver.resolveSubtype(outer, BOuter.class));
    }

    public void testIncompleteTypesReusedWithinResolution()
    {
        TypeResolver resolver = new TypeResolver();
        List<ResolvedType> params = resolver.resolve(T0.class).getParentClass().getTypeParameters();
        assertTrue(TypeResolver.isSelfReference(params.get(0)));
        // `T2` first resolved as part of `T1`, then reused
        ResolvedType t2 = params.get(2);
        assertSame(T2.class, t2.getErasedType());
        assertSame(t2, params.get(1).getParentClass().getTypeParameters().get(2));
        // but not cached
        ResolvedType t2Direct = resolver.resolve(T2.class);
        assertNotSame(t2, t2Direct);
        assertTrue(TypeResolver.isSelfReference(t2Direct.getParentClass().getTypeParameters().get(2)));
        assertSame(t2Direct, resolver.resolve(T2.class));
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

    // Raw self-reference must refer to raw type, not to (differently parameterized)
    // type being resolved
    public void testRawSelfReferenceToGenericType()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType outer = resolver.resolve(GMid.class, String.class).getParentClass()
                .getTypeParameters().get(0);
        assertSame(GOuter.class, outer.getErasedType());
        ResolvedType parent = outer.getParentClass();
        assertEquals(resolver.resolve(GMid.class, Object.class), parent);

        ResolvedTypeWithMembers members = new MemberResolver(resolver).resolve(outer, null, null);
        assertEquals(1, members.getMemberFields().length);
        assertSame(Object.class, members.getMemberFields()[0].getType().getErasedType());
    }

    // Self-reference with nested bindings (like `N<N<T>>` within `N<T>`) must refer
    // to type with those bindings
    public void testNestedSelfReferenceReferencedType()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType selfRef = resolver.resolve(N.class, String.class).getParentClass()
                .getTypeParameters().get(0).getArrayElementType();
        assertTrue(TypeResolver.isSelfReference(selfRef));
        assertEquals(resolver.resolve(N.class, resolver.resolve(N.class, String.class)),
                selfRef.getSelfReferencedType());
    }

    // Self-reference (in raw `Enum`) must be merged with compatible type as the type
    // it represents
    public void testSubtypeMergingSelfReference()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType rawEnum = resolver.resolve(Enum.class);
        assertTrue(TypeResolver.isSelfReference(rawEnum.getTypeParameters().get(0)));
        ResolvedType enumOfColor = resolver.resolve(Enum.class,
                resolver.resolve(Enum.class, Color.class));
        ResolvedType sub = resolver.resolveSubtype(
                resolver.resolve(Pair.class, rawEnum, enumOfColor), Dup.class);
        assertEquals(resolver.resolve(Dup.class, enumOfColor), sub);
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
