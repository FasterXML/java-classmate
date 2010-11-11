package com.fasterxml.classmate;

import java.util.*;
import java.lang.reflect.*;

import com.fasterxml.classmate.types.*;

@SuppressWarnings("serial")
public class TestTypeResolver extends BaseTest
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    // // Multi-level resolution needed

    static class MyStringLongMap extends MyStringKeyMap<Long> { }
    static class MyStringKeyMap<V> extends TreeMap<String, V> { }
    
    // // Playing with GenericType using inheritance
    
    static class IndirectRef
        extends GenericType<HashMap<String,Long>> { }

    static class IndirectIndirectRef
        extends IndirectRef { }

    // // For verifying "jdk type" resolution

    static class ListWrapper<T> {
        public List<T> wrap() { return null; }
    }

    static class StringListWrapper extends ListWrapper<String> {
        public long field;
    }

    // and recursive types...
    static abstract class SelfRefType implements Comparable<SelfRefType> { }
    
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
    /* Unit tests, normal operation
    /**********************************************************************
     */
    
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
        assertTrue(mapType.isInstanceOf(Map.class));
        
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

    public void testParametricMap()
    {
        ResolvedType mapType = typeResolver.resolve(MyStringLongMap.class);
        assertSame(MyStringLongMap.class, mapType.getErasedType());
        // Ensure we can find parameters for Map
        List<ResolvedType> params = mapType.typeParametersFor(Map.class);
        assertEquals(2, params.size());
        assertSame(String.class, params.get(0).getErasedType());
        assertSame(Long.class, params.get(1).getErasedType());
    }

    /**
     * Unit test for verifying that it is ok to sub-class {@link GenericType}
     */
    public void testIndirectGeneric()
    {
        ResolvedType type = typeResolver.resolve(new IndirectRef());
        assertSame(HashMap.class, type.getErasedType());
        assertTrue(type.isInstanceOf(Map.class));
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

    public void testJdkType() throws Exception
    {
        ResolvedType wrapperType = typeResolver.resolve(StringListWrapper.class);
        assertTrue(wrapperType instanceof ResolvedObjectType);
        Field f = StringListWrapper.class.getDeclaredField("field");
        assertNotNull(f);
        // first; field has no generic stuff, should be simple
        ResolvedType fieldType = typeResolver.resolve(f.getGenericType(), wrapperType.getTypeBindings());
        assertEquals(Long.TYPE, fieldType.getErasedType());
        assertTrue(fieldType.isPrimitive());
        // but method return type is templatized; and MUST be given correct type bindings!
        Method m = ListWrapper.class.getDeclaredMethod("wrap");
        ResolvedType superType = wrapperType.getParentClass();
        assertSame(ListWrapper.class, superType.getErasedType());
        ResolvedType methodReturnType = typeResolver.resolve(m.getGenericReturnType(), superType.getTypeBindings());
        // should be List<String>
        assertSame(List.class, methodReturnType.getErasedType());
        List<ResolvedType> tp = methodReturnType.getTypeParameters();
        assertEquals(1, tp.size());
        assertSame(String.class, tp.get(0).getErasedType());
    }

    public void testSimpleSelfRef()
    {
        ResolvedType type = typeResolver.resolve(SelfRefType.class);
        assertSame(SelfRefType.class, type.getErasedType());
        List<ResolvedType> interfaces = type.getImplementedInterfaces();
        assertEquals(1, interfaces.size());
        ResolvedType compType = interfaces.get(0);
        assertSame(Comparable.class, compType.getErasedType());
        List<ResolvedType> pts = compType.getTypeParameters();
        assertEquals(1, pts.size());
        ResolvedType compParam = pts.get(0);
        // ok this ought to be self-ref
        assertSame(ResolvedRecursiveType.class, compParam.getClass());
        assertSame(SelfRefType.class, compParam.getErasedType());
        assertNull(compParam.getParentClass());
        // but we should be able to find what it really is, too:
        assertSame(type, compParam.getSelfReferencedType());
    }
    
    /*
    /**********************************************************************
    /* Unit tests, error cases
    /**********************************************************************
     */

    /**
     * Unit test to verify that discrepancies are properly detected
     */
    public void testGenericParamMismatch()
    {
        // Maps require 2 type params:
        try {
            typeResolver.resolve(Map.class, Long.class);
            fail("Expected failure");
        } catch (IllegalArgumentException e) {
            verifyException(e, "1 type parameter: class expects 2");
        }

        // And Lists just 1
        try {
            typeResolver.resolve(List.class, Integer.class, Long.class);
            fail("Expected failure");
        } catch (IllegalArgumentException e) {
            verifyException(e, "2 type parameters: class expects 1");
        }
    }

    public void testInvalidSubtype()
    {
        ResolvedType supertype = typeResolver.resolve(String.class);
        try {
            // can't do equivalent of "ArrayType extends String"
            typeResolver.resolveSubtype(supertype, ArrayList.class);
        } catch (IllegalArgumentException e) {
            verifyException(e, "Can not sub-class java.lang.String into java.util.ArrayList");
        }
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private ResolvedType _testSimpleConcrete(Class<?> cls)
    {
        ResolvedType type = typeResolver.resolve(cls);
        assertTrue(type instanceof ResolvedObjectType);
        assertSame(cls, type.getErasedType());
        assertFalse(type.isAbstract());
        assertFalse(type.isArray());
        assertFalse(type.isInterface());
        assertTrue(type.isConcrete());
        assertFalse(type.isPrimitive());
        return type;
    }
}
