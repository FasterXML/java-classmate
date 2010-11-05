package com.fasterxml.classmate;

import java.util.*;

import junit.framework.TestCase;

public class TestTypeResolver extends TestCase
{
    /*
    /**********************************************************************
    /* Helper type
    /**********************************************************************
     */

    static class IndirectRef
        extends GenericType<HashMap<String,Long>> { }

    static class IndirectIndirectRef
        extends IndirectRef { }
    
    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    protected TypeResolver typeResolver;
    
    protected void setUp()
    {
        // Let's use a single instance for all tests, to increase chance of seeing failures
        typeResolver = new TypeResolver();
    }

    public void testSimpleTypes()
    {
        // let's check some default java types
        ResolvedType objectType = _testSimpleConcrete(Object.class);
        ResolvedType stringType = _testSimpleConcrete(String.class);

        // minor other thing; see that caching works
        assertSame(stringType, _testSimpleConcrete(String.class));

        ResolvedType stringParent = stringType.getParentClass();
        assertNotNull(stringParent);
        // actually, should also resolve to same as direct lookup:
        assertSame(objectType, stringParent);
    }

    public void testArrayTypes()
    {
        ResolvedType arrayType = typeResolver.resolve(int[].class);
        assertTrue(arrayType.isArray());
        ResolvedType elemType = arrayType.getArrayElementType();
        assertNotNull(elemType);
        assertTrue(elemType.isPrimitive());
        assertSame(Integer.TYPE, elemType.getErasedType());

        // And then alternate ways to construct
        ResolvedType arrayType2 = typeResolver.resolve(new GenericType<int[]>() { });
        assertTrue(arrayType2.isArray());
        assertSame(elemType, arrayType2.getArrayElementType());

        ResolvedType arrayType3 = typeResolver.arrayType(typeResolver.resolve(Integer.TYPE));
        assertTrue(arrayType3.isArray());
        assertSame(elemType, arrayType3.getArrayElementType());
    }    

    public void testGenericMap()
    {
        // First, direct ref
        GenericType<?> mapInput = new GenericType<Map<String,Long>>() { };
        ResolvedType mapType = typeResolver.resolve(mapInput);
        assertSame(Map.class, mapType.getErasedType());
        assertTrue(mapType.isAbstract());
        assertTrue(mapType.isInterface());

        // Which should have proper parameterization:
        List<ResolvedType> params = mapType.typeParametersFor(Map.class);
        assertEquals(2, params.size());
        assertSame(String.class, params.get(0).getErasedType());
        assertSame(Long.class, params.get(1).getErasedType());

        // Then alternative methods
        ResolvedType mapType2 = typeResolver.resolve(Map.class, Character.class, Boolean.class);
        assertSame(Map.class, mapType.getErasedType());
        List<ResolvedType> params2 = mapType2.typeParametersFor(Map.class);
        assertTrue(mapType2.isAbstract());
        assertTrue(mapType2.isInterface());
        assertEquals(2, params2.size());
        assertSame(Character.class, params2.get(0).getErasedType());
        assertSame(Boolean.class, params2.get(1).getErasedType());
    }

    /**
     * Unit test for verifying that it is ok to sub-class {@link GenericType}
     */
    public void testIndirectGeneric()
    {
        ResolvedType type = typeResolver.resolve(new IndirectRef());
        assertSame(HashMap.class, type.getErasedType());
        List<ResolvedType> mapParams = type.typeParametersFor(HashMap.class);
        assertNotNull(mapParams);
        assertEquals(2, mapParams.size());
        assertSame(String.class, mapParams.get(0).getErasedType());
        assertSame(Long.class, mapParams.get(1).getErasedType());
        // ditto when looking deeper:
        mapParams = type.typeParametersFor(Map.class);
        assertNotNull(mapParams);
        assertEquals(2, mapParams.size());
        assertSame(String.class, mapParams.get(0).getErasedType());
        assertSame(Long.class, mapParams.get(1).getErasedType());

        // and same with even more indirection...
        type = typeResolver.resolve(new IndirectIndirectRef());
        assertSame(HashMap.class, type.getErasedType());
        mapParams = type.typeParametersFor(HashMap.class);
        assertNotNull(mapParams);
        assertEquals(2, mapParams.size());
        assertSame(String.class, mapParams.get(0).getErasedType());
        assertSame(Long.class, mapParams.get(1).getErasedType());
        mapParams = type.typeParametersFor(Map.class);
        assertNotNull(mapParams);
        assertEquals(2, mapParams.size());
        assertSame(String.class, mapParams.get(0).getErasedType());
        assertSame(Long.class, mapParams.get(1).getErasedType());
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private ResolvedType _testSimpleConcrete(Class<?> cls)
    {
        ResolvedType type = typeResolver.resolve(cls);
        assertTrue(type instanceof ResolvedConcreteClass);
        assertSame(cls, type.getErasedType());
        assertFalse(type.isAbstract());
        assertFalse(type.isArray());
        assertFalse(type.isInterface());
        assertTrue(type.isConcrete());
        assertFalse(type.isPrimitive());
        return type;
    }
}
