package com.fasterxml.classmate;

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
        assertEquals("java.util.HashMap<java.lang.String,java.lang.Long> extends java.util.AbstractMap<java.lang.String,java.lang.Long> implements java.util.Map<java.lang.String,java.lang.Long>,java.lang.Cloneable<java.lang.String,java.lang.Long>,java.io.Serializable<java.lang.String,java.lang.Long>",
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
}
