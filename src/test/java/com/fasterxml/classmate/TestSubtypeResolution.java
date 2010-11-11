package com.fasterxml.classmate;

import java.util.*;

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
    
    /*
    /**********************************************************************
    /* setup
    /**********************************************************************
     */

    protected TypeResolver typeResolver;
    
    protected void setUp()
    {
        // Let's use a single instance for all tests, to increase chance of seeing failures
        typeResolver = new TypeResolver();
    }
    
    /*
    /**********************************************************************
    /* Unit tests
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

    public void testValidGenericSubtype()
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
    public void testIncompatibleTypeParameters()
    {
        ResolvedType supertype = typeResolver.resolve(ArrayList.class, String.class);
        try {
            typeResolver.resolveSubtype(supertype, IntArrayList.class);
            fail("Expected failure");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Type parameter #1/1 differs; expected java.lang.String");
        }
    }
    
}
