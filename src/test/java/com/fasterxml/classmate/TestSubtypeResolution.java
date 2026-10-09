package com.fasterxml.classmate;

import com.fasterxml.classmate.util.ResolvedTypeCache;

import java.util.*;

/**
 * Unit tests to verify that {@link TypeResolver#resolveSubtype(ResolvedType, Class)}
 * works as expected.
 */
@SuppressWarnings("serial")
public class TestSubtypeResolution extends BaseTest
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    static class IntArrayList extends ArrayList<Integer> { }

    static class StringIntMap extends HashMap<String,Integer> { }

    interface StringKeyMap<VT> extends Map<String,VT> { }

    interface StringLongMap extends StringKeyMap<Long> { }

    static class Wrapper<T> {
        T value;
    }

    static class ListWrapper<E> extends Wrapper<List<E>> { }

    // [classmate#127]
    static class ArrayWrapper<E> extends Wrapper<E[]> { }

    static class IntListArrayWrapper extends Wrapper<List<Integer>[]> { }

    static class IntArrayWrapper extends Wrapper<int[]> { }

    static class Array2Wrapper<E> extends Wrapper<E[][]> { }

    static class ListOfArrayWrapper<E> extends Wrapper<List<E[]>> { }

    static class Pair<A, B> { }

    static class DupPair<E> extends Pair<E[], E> { }

    static class RawSelfArray<T> extends Wrapper<RawSelfArray[]> { }

    static class RawSelfArraySub extends RawSelfArray<String> { }

    static class RawSelf<T> extends Wrapper<RawSelf> { }

    static class RawSelfSub extends RawSelf<String> { }

    static class RawSelfOther extends Wrapper<RawSelf> { }

    static class SubWrapper<E> extends Wrapper<E> { }

    static class SelfInList extends Wrapper<List<SelfInList>> { }

    static class SamePair<E> extends Pair<E, E> { }

    static class RecInList<T> extends Wrapper<List<RecInList<T>>> { }

    static class EnumPair<E extends Enum<E>> extends Pair<Enum<E>, E> { }

    static class IntOnlyWrapper<E extends Integer> extends Wrapper<E> { }

    static class Builder<B extends Builder<B>> { }

    static class NPair<A extends Number, B> { }

    static class NSamePair<E extends Number> extends NPair<E, E> { }

    static class NumArrayWrapper<E extends Number> extends Wrapper<E[]> { }

    static class ComparableNumWrapper<E extends Number & Comparable<E>> extends Wrapper<E> { }

    static class BuilderWrapper<B extends Builder<B>> extends Wrapper<B> { }

    static class MyBuilder extends Builder<MyBuilder> { }

    static class KVPair<K, V extends K> extends Pair<K, V> { }

    static class EnumHolder<E extends Enum<E>> extends Wrapper<Enum<E>> { }

    abstract static class OuterType<K, V> extends AbstractMap<K, Collection<V>>
    {
        public abstract class Inner extends AbstractMap<K, Collection<V>> {
        }
    }
    
    /*
    /**********************************************************************
    /* setup
    /**********************************************************************
     */

    protected TypeResolver typeResolver;

    @Override
    protected void setUp()
    {
        // Let's use a single instance for all tests, to increase chance of seeing failures
        typeResolver = new TypeResolver();
    }
 
    /*
    /**********************************************************************
    /* Unit tests, success, simple
    /**********************************************************************
     */

    // Types with nested placeholders (like `Wrapper<List<P0>>`) are not to be cached
    public void testSubtypeWithNestedPlaceholderNotCached()
    {
        ResolvedTypeCache cache = ResolvedTypeCache.lruCache(200);
        TypeResolver resolver = new TypeResolver(cache);
        ResolvedType supertype = resolver.resolve(Wrapper.class,
                resolver.resolve(List.class, String.class));
        ResolvedType subtype = resolver.resolveSubtype(supertype, ListWrapper.class);
        assertEquals(supertype, subtype.getParentClass());
        int size = cache.size();

        for (int i = 0; i < 3; ++i) {
            resolver.resolveSubtype(supertype, ListWrapper.class);
        }
        assertEquals(size, cache.size());
    }

    // [classmate#127]: type variables within array types must be resolved
    public void testSubtypeWithArrayOfTypeVariable()
    {
        ResolvedType supertype = typeResolver.resolve(Wrapper.class, String[].class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, ArrayWrapper.class);
        assertSame(ArrayWrapper.class, subtype.getErasedType());
        List<ResolvedType> params = subtype.getTypeParameters();
        assertEquals(1, params.size());
        assertSame(String.class, params.get(0).getErasedType());

        // and same with generic element type
        supertype = typeResolver.resolve(Wrapper.class,
                typeResolver.arrayType(typeResolver.resolve(List.class, Long.class)));
        subtype = typeResolver.resolveSubtype(supertype, ArrayWrapper.class);
        params = subtype.getTypeParameters();
        assertEquals(1, params.size());
        assertSame(List.class, params.get(0).getErasedType());
        assertSame(Long.class, params.get(0).getTypeParameters().get(0).getErasedType());
    }

    // [classmate#127]: matching generic array element types are accepted
    public void testSubtypeWithMatchingGenericArray()
    {
        ResolvedType supertype = typeResolver.resolve(Wrapper.class,
                typeResolver.arrayType(typeResolver.resolve(List.class, Integer.class)));
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, IntListArrayWrapper.class);
        assertSame(IntListArrayWrapper.class, subtype.getErasedType());
        assertEquals(supertype, subtype.getParentClass());

        supertype = typeResolver.resolve(Wrapper.class, int[].class);
        subtype = typeResolver.resolveSubtype(supertype, IntArrayWrapper.class);
        assertEquals(supertype, subtype.getParentClass());
    }

    // [classmate#127]: same type variable bound consistently is fine
    public void testSubtypeWithRepeatedTypeVariable()
    {
        ResolvedType supertype = typeResolver.resolve(Pair.class, String[].class, String.class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, DupPair.class);
        assertSame(DupPair.class, subtype.getErasedType());
        assertSame(String.class, subtype.getTypeParameters().get(0).getErasedType());
        assertEquals(supertype, subtype.getParentClass());
    }

    // [classmate#127]: raw self-reference as (array element) type parameter
    public void testSubtypeWithRawSelfReference()
    {
        ResolvedType supertype = typeResolver.resolve(Wrapper.class, RawSelfArray[].class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, RawSelfArraySub.class);
        assertSame(RawSelfArraySub.class, subtype.getErasedType());

        supertype = typeResolver.resolve(Wrapper.class, RawSelf.class);
        subtype = typeResolver.resolveSubtype(supertype, RawSelfSub.class);
        assertSame(RawSelfSub.class, subtype.getErasedType());
    }

    // [classmate#127]: self-reference in supertype must not leak into subtype
    public void testSubtypeWithSelfReferenceInSupertype()
    {
        ResolvedType supertype = typeResolver.resolve(RawSelfArray.class).getParentClass();
        assertTrue(TypeResolver.isSelfReference(supertype.getTypeParameters().get(0).getArrayElementType()));
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, ArrayWrapper.class);
        ResolvedType param = subtype.getTypeParameters().get(0);
        assertFalse(TypeResolver.isSelfReference(param));
        assertSame(RawSelfArray.class, param.getErasedType());
        assertNotNull(param.getParentClass());
        assertSame(Wrapper.class, param.getParentClass().getErasedType());
    }

    // [classmate#127]: self-references nested within bound type must not leak into subtype
    public void testSubtypeWithNestedSelfReferenceInSupertype()
    {
        ResolvedType supertype = typeResolver.resolve(RawSelfArray.class).getParentClass();
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, SubWrapper.class);
        ResolvedType param = subtype.getTypeParameters().get(0);
        assertTrue(param.isArray());
        _verifyNoSelfReference(param.getArrayElementType(), RawSelfArray.class);

        supertype = typeResolver.resolve(SelfInList.class).getParentClass();
        subtype = typeResolver.resolveSubtype(supertype, SubWrapper.class);
        param = subtype.getTypeParameters().get(0);
        assertSame(List.class, param.getErasedType());
        _verifyNoSelfReference(param.getTypeParameters().get(0), SelfInList.class);
    }

    // [classmate#127]: same type with and without self-reference must bind consistently
    public void testSubtypeWithSelfReferenceAndRepeatedTypeVariable()
    {
        ResolvedType withSelfRef = typeResolver.resolve(SelfInList.class).getParentClass()
                .getTypeParameters().get(0);
        ResolvedType withoutSelfRef = typeResolver.resolve(List.class, SelfInList.class);
        ResolvedType supertype = typeResolver.resolve(Pair.class, withSelfRef, withoutSelfRef);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, SamePair.class);
        assertEquals(withoutSelfRef, subtype.getTypeParameters().get(0));
    }

    // [classmate#127]: self-reference as supertype is resolved to referenced type
    public void testSubtypeOfSelfReference()
    {
        ResolvedType rawEnum = typeResolver.resolve(Enum.class);
        ResolvedType supertype = rawEnum.getTypeParameters().get(0);
        assertTrue(TypeResolver.isSelfReference(supertype));
        assertEquals(rawEnum, typeResolver.resolveSubtype(supertype, Enum.class));
    }

    // [classmate#127]: self-reference to another instance of the same class (from
    // different resolution context) must not be retained
    public void testSubtypeWithSelfReferenceFromOtherContext()
    {
        ResolvedType listOfSelfRef = typeResolver.resolve(RecInList.class, String.class)
                .getParentClass().getTypeParameters().get(0);
        assertTrue(TypeResolver.isSelfReference(listOfSelfRef.getTypeParameters().get(0)));
        ResolvedType supertype = typeResolver.resolve(Wrapper.class,
                typeResolver.resolve(RecInList.class, listOfSelfRef));
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, SubWrapper.class);
        // SubWrapper<RecInList<List<RecInList<String>>>>
        ResolvedType inner = subtype.getTypeParameters().get(0) // RecInList<...>
                .getTypeParameters().get(0) // List<...>
                .getTypeParameters().get(0); // RecInList<String>
        assertFalse(TypeResolver.isSelfReference(inner));
        assertEquals(typeResolver.resolve(RecInList.class, String.class), inner);
    }

    // [classmate#127]: raw `Enum` (with self-reference) bound consistently
    public void testSubtypeWithRawEnumAndRepeatedTypeVariable()
    {
        ResolvedType rawEnum = typeResolver.resolve(Enum.class);
        ResolvedType subtype = typeResolver.resolveSubtype(
                typeResolver.resolve(Pair.class, Enum.class, Enum.class), EnumPair.class);
        assertEquals(rawEnum, subtype.getTypeParameters().get(0));

        subtype = typeResolver.resolveSubtype(
                typeResolver.resolve(Pair.class, rawEnum, rawEnum.getTypeParameters().get(0)),
                SamePair.class);
        assertEquals(rawEnum, subtype.getTypeParameters().get(0));
    }

    // [classmate#127]: self-references valid as-is (like in raw `Enum`) must be retained
    public void testSubtypeWithSelfBoundedTypeParameter()
    {
        for (Class<?> selfBounded : new Class<?>[] { Enum.class, Builder.class }) {
            ResolvedType supertype = typeResolver.resolve(Wrapper.class, selfBounded);
            ResolvedType subtype = typeResolver.resolveSubtype(supertype, SubWrapper.class);
            assertEquals(typeResolver.resolve(SubWrapper.class, selfBounded), subtype);
            assertEquals(supertype, subtype.getParentClass());
        }
    }

    // [classmate#127]: `Object` (from wildcard or raw type) compatible with more specific binding
    public void testSubtypeWithWildcardAndRepeatedTypeVariable()
    {
        ResolvedType supertype = typeResolver.resolve(new GenericType<java.util.function.Function<String,?>>() { });
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, java.util.function.UnaryOperator.class);
        assertEquals(typeResolver.resolve(java.util.function.UnaryOperator.class, String.class), subtype);

        supertype = typeResolver.resolve(new GenericType<java.util.function.Function<?,String>>() { });
        subtype = typeResolver.resolveSubtype(supertype, java.util.function.UnaryOperator.class);
        assertEquals(typeResolver.resolve(java.util.function.UnaryOperator.class, String.class), subtype);

        // raw `NPair` resolves to `NPair<Number,Object>`
        supertype = typeResolver.resolve(NPair.class);
        subtype = typeResolver.resolveSubtype(supertype, NSamePair.class);
        assertEquals(typeResolver.resolve(NSamePair.class, Number.class), subtype);
    }

    // [classmate#127]: no self-references in result even if no actual sub-classing done
    public void testSubtypeSameAsSupertypeWithSelfReference()
    {
        ResolvedType supertype = typeResolver.resolve(RawSelfArray.class).getParentClass();
        ResolvedType result = typeResolver.resolveSubtype(supertype, Wrapper.class);
        assertSame(Wrapper.class, result.getErasedType());
        _verifyNoSelfReference(result.getTypeParameters().get(0).getArrayElementType(),
                RawSelfArray.class);
    }

    // [classmate#127]: `Object` type parameters (possibly from wildcard or raw type)
    // are not verified against bounds
    public void testSubtypeWithObjectNotVerifiedAgainstBounds()
    {
        // `V extends K`
        ResolvedType supertype = typeResolver.resolve(new GenericType<Pair<String,?>>() { });
        assertEquals(typeResolver.resolve(KVPair.class, String.class, Object.class),
                typeResolver.resolveSubtype(supertype, KVPair.class));
        // multiple bounds
        for (ResolvedType wrapper : new ResolvedType[] {
                typeResolver.resolve(Wrapper.class),
                typeResolver.resolve(new GenericType<Wrapper<?>>() { })
        }) {
            assertEquals(typeResolver.resolve(ComparableNumWrapper.class, Object.class),
                    typeResolver.resolveSubtype(wrapper, ComparableNumWrapper.class));
        }
    }

    // [classmate#127]: self-reference (valid within enclosing type) must not become
    // stand-alone type parameter of subtype
    public void testSubtypeBindingSelfReference()
    {
        ResolvedType supertype = typeResolver.resolve(Wrapper.class, Enum.class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, EnumHolder.class);
        ResolvedType param = subtype.getTypeParameters().get(0);
        assertFalse(TypeResolver.isSelfReference(param));
        assertEquals(typeResolver.resolve(Enum.class), param);
        assertNotNull(param.getParentClass());
    }

    // [classmate#127]: `Object` (from wildcard or raw type) nested within type
    // compatible with more specific binding
    public void testSubtypeWithNestedWildcardAndRepeatedTypeVariable()
    {
        ResolvedType listOfString = typeResolver.resolve(List.class, String.class);
        ResolvedType supertype = typeResolver.resolve(
                new GenericType<java.util.function.Function<? extends List<?>, List<String>>>() { });
        assertEquals(typeResolver.resolve(java.util.function.UnaryOperator.class, listOfString),
                typeResolver.resolveSubtype(supertype, java.util.function.UnaryOperator.class));

        supertype = typeResolver.resolve(Pair.class, List.class, listOfString);
        assertEquals(listOfString,
                typeResolver.resolveSubtype(supertype, SamePair.class).getTypeParameters().get(0));

        supertype = typeResolver.resolve(Pair.class, Object[].class, String[].class);
        assertEquals(typeResolver.resolve(String[].class),
                typeResolver.resolveSubtype(supertype, SamePair.class).getTypeParameters().get(0));

        // and merged from both
        supertype = typeResolver.resolve(new GenericType<Pair<Map<String,?>, Map<?,Integer>>>() { });
        assertEquals(typeResolver.resolve(Map.class, String.class, Integer.class),
                typeResolver.resolveSubtype(supertype, SamePair.class).getTypeParameters().get(0));
    }

    // [classmate#127]: bounds of type variables are not verified (wildcard upper bound
    // can not be distinguished from actual type; see [classmate#130])
    public void testSubtypeOfBoundedWildcard()
    {
        ResolvedType supertype = typeResolver.resolve(new GenericType<Wrapper<? extends Number>>() { });
        assertSame(IntOnlyWrapper.class,
                typeResolver.resolveSubtype(supertype, IntOnlyWrapper.class).getErasedType());
    }

    // [classmate#127]: bounded type variables bound to types satisfying bounds
    public void testSubtypeSatisfyingBounds()
    {
        assertEquals(typeResolver.resolve(NumArrayWrapper.class, Integer.class),
                typeResolver.resolveSubtype(typeResolver.resolve(Wrapper.class, Integer[].class),
                        NumArrayWrapper.class));
        assertEquals(typeResolver.resolve(ComparableNumWrapper.class, Long.class),
                typeResolver.resolveSubtype(typeResolver.resolve(Wrapper.class, Long.class),
                        ComparableNumWrapper.class));
        assertEquals(typeResolver.resolve(BuilderWrapper.class, MyBuilder.class),
                typeResolver.resolveSubtype(typeResolver.resolve(Wrapper.class, MyBuilder.class),
                        BuilderWrapper.class));
    }

    private void _verifyNoSelfReference(ResolvedType type, Class<?> expRaw)
    {
        assertFalse(TypeResolver.isSelfReference(type));
        assertSame(expRaw, type.getErasedType());
        assertNotNull(type.getParentClass());
        assertSame(Wrapper.class, type.getParentClass().getErasedType());
    }

    /**
     * Test to ensure a properly parameterized {@link List} can be be made
     * more specific while still keeping parameterization.
     */
    public void testMoreSpecificListType()
    {
        ResolvedType supertype = typeResolver.resolve(List.class, Integer.class);
        // First verify original bindings are correct
        List<ResolvedType> bindings = supertype.typeParametersFor(List.class);
        assertEquals(1, bindings.size());
        assertSame(Integer.class, bindings.get(0).getErasedType());
        bindings = supertype.typeParametersFor(Collection.class);
        assertEquals(1, bindings.size());
        assertSame(Integer.class, bindings.get(0).getErasedType());

        ResolvedType subtype = typeResolver.resolveSubtype(supertype, ArrayList.class);
        // and then with specialization too
        bindings = subtype.typeParametersFor(List.class);
        assertEquals(1, bindings.size());
        assertSame(Integer.class, bindings.get(0).getErasedType());
        bindings = supertype.typeParametersFor(Collection.class);
        assertEquals(1, bindings.size());
        assertSame(Integer.class, bindings.get(0).getErasedType());
    }

    // Similar to above, but via Collection, not List
    public void testMoreSpecificCollectionType()
    {
        final Class<?> elemType = String.class;
        
        List<ResolvedType> bindings;
        ResolvedType supertype = typeResolver.resolve(Collection.class, elemType);
        bindings = supertype.typeParametersFor(Collection.class);
        assertEquals(1, bindings.size());
        assertSame(elemType, bindings.get(0).getErasedType());

        ResolvedType subtype;

        // and then with specialization too
        subtype = typeResolver.resolveSubtype(supertype, ArrayList.class);
        bindings = subtype.typeParametersFor(List.class);
        assertEquals(1, bindings.size());
        assertSame(elemType, bindings.get(0).getErasedType());
        bindings = supertype.typeParametersFor(Collection.class);
        assertEquals(1, bindings.size());
        assertSame(elemType, bindings.get(0).getErasedType());

        // and once more, but now to a generic type
        // 25-Oct-2015, tatu: Seems like there's some caching issue here...
        subtype = typeResolver.resolveSubtype(supertype, List.class);
        bindings = subtype.typeParametersFor(List.class);
        assertEquals(1, bindings.size());
        assertSame(elemType, bindings.get(0).getErasedType());
        bindings = supertype.typeParametersFor(Collection.class);
        assertEquals(1, bindings.size());
        assertSame(elemType, bindings.get(0).getErasedType());
    }

    /*
    /**********************************************************************
    /* Unit tests, success, untyped/incomplete
    /**********************************************************************
     */

    public void testValidUntypedSubtype()
    {
        // First, make a concrete type that extends specified generic interface:
        ResolvedType supertype = typeResolver.resolve(HashMap.class, String.class, Integer.class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, StringIntMap.class);
        assertSame(StringIntMap.class, subtype.getErasedType());

        // but resolution can't cheat; we must be able to find parameterization...
        List<ResolvedType> bindings = subtype.typeParametersFor(HashMap.class);
        assertEquals(2, bindings.size());
        assertSame(String.class, bindings.get(0).getErasedType());
        assertSame(Integer.class, bindings.get(1).getErasedType());
    }

    /**
     * Let's test that we can also resolve to incomplete types; might
     * be useful occasionally
     */
    public void testValidIncompleteSubtype()
    {
        ResolvedType supertype = typeResolver.resolve(Map.class, String.class, Long.class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, StringKeyMap.class);
        assertSame(StringKeyMap.class, subtype.getErasedType());

        TypeBindings bindings = subtype.getTypeBindings();
        assertEquals(1, bindings.size());
        assertSame(Long.class, bindings.getBoundType(0).getErasedType());

        // And should see full types for Map
        ResolvedType actualSupertype = subtype.findSupertype(Map.class);
        assertSame(Map.class, actualSupertype.getErasedType());
        bindings = actualSupertype.getTypeBindings();
        assertEquals(2, bindings.size());
        assertSame(String.class, bindings.getBoundType(0).getErasedType());
        assertSame(Long.class, bindings.getBoundType(1).getErasedType());
    }

    /*
    /**********************************************************************
    /* Unit tests, success, generic
    /**********************************************************************
     */

    public void testValidGenericSubClass()
    {
        // First, make a concrete type that extends specified generic interface:
        ResolvedType supertype = typeResolver.resolve(Map.class, String.class, Long.class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, HashMap.class);
        assertSame(HashMap.class, subtype.getErasedType());

        // in this case it's direct class, so we do have bindings
        TypeBindings bindings = subtype.getTypeBindings();
        assertEquals(2, bindings.size());
        assertSame(String.class, bindings.getBoundType(0).getErasedType());
        assertSame(Long.class, bindings.getBoundType(1).getErasedType());

        // and must look the same in other respects too:
        assertEquals("Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/Long;>;", subtype.getSignature());
        assertEquals("java.util.HashMap<java.lang.String,java.lang.Long> extends java.util.AbstractMap<java.lang.String,java.lang.Long> implements java.util.Map<java.lang.String,java.lang.Long>,java.lang.Cloneable,java.io.Serializable",
                subtype.getFullDescription());
    }

    /**
     * Unit test for verifying that we can "sub-class" from rather low-level secondary
     * interfaces, too
     */
    public void testValidGenericSubInterface()
    {
        ResolvedType supertype = typeResolver.resolve(Iterable.class, Byte.class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, LinkedHashSet.class);
        assertSame(LinkedHashSet.class, subtype.getErasedType());
        assertEquals("java.util.LinkedHashSet<java.lang.Byte>", subtype.getBriefDescription());

        ResolvedType collectionType = subtype.findSupertype(Collection.class);
        assertNotNull(collectionType);
        assertEquals("java.util.Collection<java.lang.Byte>", collectionType.getBriefDescription());
        ResolvedType setType = subtype.findSupertype(Set.class);
        assertNotNull(setType);
        assertEquals("java.util.Set<java.lang.Byte>", setType.getBriefDescription());    
    }

    public void testValidGenericSubInterfaceWithMap()
    {
        ResolvedType supertype = typeResolver.resolve(Map.class, String.class, Long.class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, StringLongMap.class);
        assertSame(StringLongMap.class, subtype.getErasedType());
        ResolvedType match = subtype.findSupertype(Map.class);
        TypeBindings tb = match.getTypeBindings();
        assertEquals(2, tb.size());
        assertSame(String.class, tb.getBoundType(0).getErasedType());
        assertSame(Long.class, tb.getBoundType(1).getErasedType());
    }

    public void testValidNestedType()
    {
        // Let's try to get to ListWrapper<String>, from Wrapper<List<String>>
        ResolvedType elemType = typeResolver.resolve(List.class, String.class);
        ResolvedType wrapperType = typeResolver.resolve(Wrapper.class, elemType);
        ResolvedType subtype = typeResolver.resolveSubtype(wrapperType, ListWrapper.class);
        assertSame(ListWrapper.class, subtype.getErasedType());
        ResolvedType match = subtype.findSupertype(Wrapper.class);
        TypeBindings tb = match.getTypeBindings();
        assertEquals(1, tb.size());
        ResolvedType listType = tb.getBoundType(0);
        assertSame(List.class, listType.getErasedType());
        tb = listType.getTypeBindings();
        assertEquals(1, tb.size());
        assertSame(String.class, tb.getBoundType(0).getErasedType());
    }

    // inspired by [JACKSON-677]
    public void testValidInnerType()
    {
        ResolvedType type = typeResolver.resolve(OuterType.Inner.class);
        assertSame(OuterType.Inner.class, type.getErasedType());
        ResolvedType mapType = type.findSupertype(Map.class);
        assertSame(Map.class, mapType.getErasedType());
        TypeBindings bindings = mapType.getTypeBindings();
        assertEquals(2, bindings.size());
        assertSame(Object.class, bindings.getBoundType(0).getErasedType());
        // value should be "Collection<V>", which resolves to "Collection<Object>"
        ResolvedType valueType = bindings.getBoundType(1);
        assertSame(Collection.class, valueType.getErasedType());
        // directly Collection, no need to find, just get:
        bindings = valueType.getTypeBindings();
        assertEquals(1, bindings.size());
        assertSame(Object.class, bindings.getBoundType(0).getErasedType());
    }
    
    /*
    /**********************************************************************
    /* Unit tests, failure cases
    /**********************************************************************
     */
    
    // Test to verify that type erasures are compatible
    public void testInvalidSubClass()
    {
        ResolvedType supertype = typeResolver.resolve(List.class, Integer.class);
        try {
            typeResolver.resolveSubtype(supertype, HashMap.class);
            fail("Expected failure");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Can not sub-class java.util.List");
        }
    }

    // Test to further verify that type parameters are compatible
    public void testIncompatibleTypeParametersList()
    {
        ResolvedType supertype = typeResolver.resolve(ArrayList.class, String.class);
        try {
            typeResolver.resolveSubtype(supertype, IntArrayList.class);
            fail("Expected failure");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Type parameter #1/1 differs; expected java.lang.String");
        }
    }

    public void testIncompatibleTypeParametersMap()
    {
        ResolvedType supertype = typeResolver.resolve(Map.class, String.class, Integer.class);
        try {
            ResolvedType t = typeResolver.resolveSubtype(supertype, StringLongMap.class);
            fail("Expected failure, got: "+t);
        } catch (IllegalArgumentException e) {
            verifyException(e, "Type parameter #2/2 differs; expected java.lang.Integer");
        }
    }

    // [classmate#127]: array element types must be verified
    public void testIncompatibleGenericArrayElementType()
    {
        ResolvedType supertype = typeResolver.resolve(Wrapper.class,
                typeResolver.arrayType(typeResolver.resolve(List.class, String.class)));
        try {
            ResolvedType t = typeResolver.resolveSubtype(supertype, IntListArrayWrapper.class);
            fail("Expected failure, got: "+t);
        } catch (IllegalArgumentException e) {
            verifyException(e, "Type parameter #1/1 differs");
        }
    }

    public void testIncompatibleArrayElementType()
    {
        ResolvedType supertype = typeResolver.resolve(Wrapper.class, long[].class);
        try {
            ResolvedType t = typeResolver.resolveSubtype(supertype, IntArrayWrapper.class);
            fail("Expected failure, got: "+t);
        } catch (IllegalArgumentException e) {
            verifyException(e, "Type parameter #1/1 differs");
        }

        supertype = typeResolver.resolve(Wrapper.class, String.class);
        try {
            ResolvedType t = typeResolver.resolveSubtype(supertype, ArrayWrapper.class);
            fail("Expected failure, got: "+t);
        } catch (IllegalArgumentException e) {
            verifyException(e, "Type parameter #1/1 differs");
        }
    }

    // [classmate#127]: primitive types can not be bound to type variables
    public void testPrimitiveArrayElementForTypeVariable()
    {
        _verifyIncompatible(typeResolver.resolve(Wrapper.class, int[].class), ArrayWrapper.class);
        _verifyIncompatible(typeResolver.resolve(Wrapper.class, int[][].class), Array2Wrapper.class);
        _verifyIncompatible(typeResolver.resolve(Wrapper.class,
                typeResolver.resolve(List.class, int[].class)), ListOfArrayWrapper.class);
    }

    // [classmate#127]: type variable must not be bound to conflicting types
    public void testConflictingTypeVariableBindings()
    {
        ResolvedType supertype = typeResolver.resolve(Pair.class, String[].class, Integer.class);
        try {
            ResolvedType t = typeResolver.resolveSubtype(supertype, DupPair.class);
            fail("Expected failure, got: "+t.getFullDescription());
        } catch (IllegalArgumentException e) {
            verifyException(e, "Type parameter #2/2 differs; conflicting bindings for type variable `E` of "
                    +DupPair.class.getName()+": java.lang.String vs java.lang.Integer");
        }
        // also nested, and for primitive arrays
        _verifyIncompatible(typeResolver.resolve(new GenericType<Pair<Map<String,?>, Map<Long,?>>>() { }),
                SamePair.class);
        _verifyIncompatible(typeResolver.resolve(Pair.class, Object[].class, int[].class),
                SamePair.class);
    }

    private void _verifyIncompatible(ResolvedType supertype, Class<?> subtype)
    {
        try {
            ResolvedType t = typeResolver.resolveSubtype(supertype, subtype);
            fail("Expected failure, got: "+t.getFullDescription());
        } catch (IllegalArgumentException e) {
            verifyException(e, "differs");
        }
    }

    // [classmate#127]: raw self-reference resolves to bounds, same as other raw types
    public void testRawSelfReferenceVerifiedAsRaw()
    {
        ResolvedType supertype = typeResolver.resolve(Wrapper.class,
                typeResolver.resolve(RawSelf.class, String.class));
        _verifyIncompatible(supertype, RawSelfSub.class);
        _verifyIncompatible(supertype, RawSelfOther.class);
    }

    // [classmate#127]: `Object[]` is not compatible with primitive array
    public void testObjectArrayNotCompatibleWithPrimitiveArray()
    {
        _verifyIncompatible(typeResolver.resolve(Wrapper.class, Object[].class), IntArrayWrapper.class);
    }
}
