package com.fasterxml.classmate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fasterxml.classmate.BaseTest;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

// for [classmate#53]: Raw Comparator
public class TestTypeResolver53 extends BaseTest
{
    @SuppressWarnings("rawtypes")
    static abstract class Comparator53 implements Comparator { }

    @SuppressWarnings("rawtypes")
    static abstract class Map53 implements Map { }

    @SuppressWarnings("rawtypes")
    static abstract class BoundedComparable<T extends Number> implements Comparable { }

    @SuppressWarnings("rawtypes")
    static abstract class BoundedRaw extends BoundedComparable { }

    static abstract class NestedRaw extends java.util.ArrayList<Map> { }

    protected final TypeResolver RESOLVER = new TypeResolver();

    // [classmate#53] Problem with Raw types - single type parameter
    public void testResolvingRawType() {
        ResolvedType rt = RESOLVER.resolve(Comparator53.class);
        List<ResolvedType> params = rt.typeParametersFor(Comparator.class);
        assertEquals(Arrays.asList(RESOLVER.resolve(Object.class)),
                params);
    }

    // [classmate#53] Raw type with multiple type parameters (Map<K,V>)
    public void testRawTypeMultipleParameters() {
        ResolvedType rt = RESOLVER.resolve(Map53.class);
        List<ResolvedType> params = rt.typeParametersFor(Map.class);
        assertEquals(Arrays.asList(
                RESOLVER.resolve(Object.class),
                RESOLVER.resolve(Object.class)),
                params);
    }

    // [classmate#53] Raw type with bounded type parameter
    public void testRawTypeWithBoundedParameter() {
        ResolvedType rt = RESOLVER.resolve(BoundedRaw.class);
        List<ResolvedType> params = rt.typeParametersFor(Comparable.class);
        // BoundedComparable<T extends Number> implements Comparable<T>, used raw
        // Type parameter T has bound Number, so should resolve to Number
        assertEquals(Arrays.asList(RESOLVER.resolve(Number.class)),
                params);
    }

    // [classmate#53] Nested raw types (List<Map> where Map is raw)
    public void testNestedRawType() {
        ResolvedType rt = RESOLVER.resolve(NestedRaw.class);
        List<ResolvedType> params = rt.typeParametersFor(java.util.ArrayList.class);
        assertEquals(1, params.size());

        // The type parameter should be Map (raw), which should have its own parameters
        ResolvedType mapType = params.get(0);
        assertEquals(Map.class, mapType.getErasedType());

        // The raw Map should have Object,Object as its type parameters
        List<ResolvedType> mapParams = mapType.getTypeParameters();
        assertEquals(Arrays.asList(
                RESOLVER.resolve(Object.class),
                RESOLVER.resolve(Object.class)),
                mapParams);
    }
}
