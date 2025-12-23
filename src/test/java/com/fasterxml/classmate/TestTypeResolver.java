package com.fasterxml.classmate;

import java.util.*;
import java.lang.reflect.*;

import com.fasterxml.classmate.members.RawMethod;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.classmate.types.*;
import com.fasterxml.classmate.util.ClassKey;
import com.fasterxml.classmate.util.ResolvedTypeKey;

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

    // Also: need to ensure that fields and methods get resolved

    public static class LongValuedMap<K> extends HashMap<K, Long> { }

    static class StringLongMapBean {
        public LongValuedMap<String> value;
    }

    abstract static class IntermediateList<E> implements List<E> { }
    static class StringListBean {
        public IntermediateList<String> value;
    }

    // For testing failure in subtype resolution with generic parameters

    static class Params<T> { }

    // For testing wildcards

    @SuppressWarnings("rawtypes")
    static interface Wild<T extends List<? extends Collection>, S extends List<? super Collection>> { }

    // For testing type-parameters matching

    static interface MatchA<T extends Collection<?>, S extends Comparator<T>> { }

    static interface MatchB<T extends List<?>> { }

    // From [https://github.com/FasterXML/jackson-databind/issues/76]
    static class HashTree<K, V> extends HashMap<K, HashTree<K, V>> { }
    
    /*
    /**********************************************************************
    /* setup
    /**********************************************************************
     */

    // Let's use a single instance for all tests, to increase chance of seeing failures
    protected final TypeResolver typeResolver = new TypeResolver();

    /*
    /**********************************************************************
    /* Unit tests, normal operation
    /**********************************************************************
     */

    public void testResolveWithEmptyTypeParameters()
    {
        ResolvedType reference = typeResolver.resolve(List.class);

        ResolvedType type = typeResolver.resolve(List.class, (Class[]) null);
        assertSame(reference, type);

        Class<?>[] typeParameters = new Class[0];
        type = typeResolver.resolve(List.class, typeParameters);
        assertSame(reference, type);

        type = typeResolver.resolve(List.class, (ResolvedType[]) null);
        assertSame(reference, type);

        ResolvedType[] resolvedTypeParameters = new ResolvedType[0];
        type = typeResolver.resolve(List.class, resolvedTypeParameters);
        assertSame(reference, type);
    }

    public void testResolveGenericWithFailures()
    {
        final GenericType<String> type = new GenericType<String>() { };
        // force failure
        TypeResolver._primitiveTypes.put(new ClassKey(type.getClass()), new ResolvedObjectType(type.getClass(), null, (ResolvedType) null, ResolvedType.NO_TYPES) {
            @Override public ResolvedType findSupertype(Class<?> erasedSupertype) {
                return null;
            }
        });
        try {
            typeResolver.resolve(type);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // now force failure on getting generic's parameterized type
        TypeResolver._primitiveTypes.put(new ClassKey(type.getClass()), new ResolvedObjectType(type.getClass(), null, (ResolvedType) null, ResolvedType.NO_TYPES) {
            @Override public ResolvedType findSupertype(Class<?> erasedSupertype) {
                return ResolvedObjectType.create(type.getClass(), null, null, null);
            }
        });
        try {
            typeResolver.resolve(type);
            fail("Should have thrown an IllegalArgumentException when getting the generic's parameterized types.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
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

        ResolvedType wildcardMap = typeResolver.resolve(HashTree.class);
        assertSame(HashTree.class, wildcardMap.getErasedType());
        // K becomes Object (unbound), V has lower bound of HashTree, so:
        params = wildcardMap.typeParametersFor(Map.class);
        assertEquals(2, params.size());
        assertSame(Object.class, params.get(0).getErasedType());
        assertSame(HashTree.class, params.get(1).getErasedType());
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
        ResolvedType fieldType = typeResolver.resolve(wrapperType.getTypeBindings(), f.getGenericType());
        assertEquals(Long.TYPE, fieldType.getErasedType());
        assertTrue(fieldType.isPrimitive());
        // but method return type is templatized; and MUST be given correct type bindings!
        Method m = ListWrapper.class.getDeclaredMethod("wrap");
        ResolvedType superType = wrapperType.getParentClass();
        assertSame(ListWrapper.class, superType.getErasedType());
        ResolvedType methodReturnType = typeResolver.resolve(superType.getTypeBindings(),
                m.getGenericReturnType());
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

    public void testTypesFromMapField() throws Exception
    {
        ResolvedType type = typeResolver.resolve(StringLongMapBean.class);
        Field field = StringLongMapBean.class.getDeclaredField("value");
        ResolvedType fieldType = typeResolver.resolve(type.getTypeBindings(), field.getGenericType());
        assertSame(LongValuedMap.class, fieldType.getErasedType());
        List<ResolvedType> mapTypes = fieldType.typeParametersFor(Map.class);
        assertEquals(2, mapTypes.size());
        assertSame(String.class, mapTypes.get(0).getErasedType());
        assertSame(Long.class, mapTypes.get(1).getErasedType());
    }

    public void testTypesFromListField() throws Exception
    {
        ResolvedType type = typeResolver.resolve(StringListBean.class);
        Field field = StringListBean.class.getDeclaredField("value");
        ResolvedType fieldType = typeResolver.resolve(type.getTypeBindings(), field.getGenericType());
        assertSame(IntermediateList.class, fieldType.getErasedType());
        List<ResolvedType> listType = fieldType.typeParametersFor(List.class);
        assertEquals(1, listType.size());
        assertSame(String.class, listType.get(0).getErasedType());
    }

    public void testResolvedTypeAsType()
    {
        ResolvedType t1 = typeResolver.resolve(getClass());
        ResolvedType t2 = typeResolver.resolve(t1);
        assertSame(t1, t2);
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
        supertype = typeResolver.resolve(boolean.class);
        try {
            typeResolver.resolveSubtype(supertype, Boolean.class); // autoboxing, clearly, isn't sub-classing
            fail("Expecting an UnsupportedOperationException as boolean.class cannot be subclassed.");
        } catch (UnsupportedOperationException uoe) {
            verify(uoe, "Can not subtype primitive or array types (type %s)", supertype.getFullDescription());
        }
        // add a mock class to force 'internal-error' case
        Object subclass = new Object() { };
        typeResolver._resolvedTypes.put(new ResolvedTypeKey(subclass.getClass()), new ResolvedObjectType(subclass.getClass(), null, (ResolvedType) null, ResolvedType.NO_TYPES) {
            @Override public ResolvedType findSupertype(Class<?> erasedSupertype) {
                return null;
            }
        });
        supertype = typeResolver.resolve(Object.class);
        try {
            typeResolver.resolveSubtype(supertype, subclass.getClass());
            fail("Expecting an IllegalArgumentException as supertype should not have been found for subtype.");
        } catch (IllegalArgumentException iae) {
            verify(iae, "Internal error: unable to locate supertype (%s) for type %s", subclass.getClass().getName(),
                    supertype.getBriefDescription());
        }
        // add a mock class to force a failure of finding a type's parameters
        subclass = new Params<Object>();
        final ResolvedType finalSuperType = supertype;
        TypeBindings typeBindings = TypeBindings.emptyBindings(); // force failure of parameter resolution
        typeResolver._resolvedTypes.put(new ResolvedTypeKey(subclass.getClass()), new ResolvedObjectType(subclass.getClass(),
                typeBindings, (ResolvedType) null, ResolvedType.NO_TYPES) {
            @Override public ResolvedType findSupertype(Class<?> erasedSupertype) {
                return finalSuperType;
            }
        });
        try {
            typeResolver.resolveSubtype(supertype, subclass.getClass());
            fail("Expecting an IllegalArgumentException as boolean.class cannot be subclassed.");
        } catch (IllegalArgumentException iae) {
            verify(iae, "Failed to find type parameter #1/1 for %s", subclass.getClass().getName());
        }
    }

    public void testResolveOfSelfReferencedType()
    {
        ResolvedType supertype = typeResolver.resolve(SelfRefType.class);
        ResolvedType self = typeResolver.resolveSubtype(supertype, SelfRefType.class);
        assertSame(self, supertype);

        assertFalse(TypeResolver.isSelfReference(self));
        assertFalse(TypeResolver.isSelfReference(supertype));

        ResolvedRecursiveType selfSuperType = new ResolvedRecursiveType(SelfRefType.class, null);
        selfSuperType.setReference(supertype);
        self = typeResolver.resolveSubtype(selfSuperType, SelfRefType.class);
        assertSame(self, supertype);

        assertTrue(TypeResolver.isSelfReference(selfSuperType));
    }

    public void testWildcardType()
    {
        @SuppressWarnings("rawtypes")
        GenericType<Wild<List<? extends Collection>, List<? super Collection>>> genericType =
                new GenericType<Wild<List<? extends Collection>, List<? super Collection>>>() { };
        ResolvedType resolvedType = typeResolver.resolve(genericType);
        List<ResolvedType> typeParameters = resolvedType.getTypeParameters();
        assertEquals(2, typeParameters.size());
        // the wildcard gets collapsed to it's upper-bound
        assertEquals(List.class, typeParameters.get(0).getErasedType());
        assertEquals(List.class, typeParameters.get(1).getErasedType());
    }

    public void testUnknownJdkType()
    {
        Type type = new Type() { };
        try {
            typeResolver.resolve(type);
            fail("Expected an IllegalArgumentException as concrete type of Type is unknown.");
        } catch (IllegalArgumentException iae) {
            verify(iae, "Unrecognized type class: %s", type.getClass().getName());
        }
    }

    public void testMissingSuperclass() throws IllegalAccessException, InvocationTargetException
    {
        ResolvedType resolvedType = typeResolver.resolve(TypeResolver.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "_resolveSuperClass".equals(element.getName());
            }
        });
        ResolvedTypeWithMembers resolvedTypeWithMembers = memberResolver.resolve(resolvedType, null, null);
        ResolvedMethod resolveSuperClassResolvedMethod = resolvedTypeWithMembers.getMemberMethods()[0];
        Method resolveSuperClassMethod = resolveSuperClassResolvedMethod.getRawMember();
        resolveSuperClassMethod.setAccessible(true);
        assertNull(resolveSuperClassMethod.invoke(typeResolver, null, Comparator.class, null));
    }

    public void testTypesMatch() throws IllegalAccessException, InvocationTargetException
    {
        ResolvedType resolvedType = typeResolver.resolve(TypeResolver.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            // 26-Oct-2015, tatu: Quite fragile, probably shouldn't use
            @Override public boolean include(RawMethod element) {
                return "_verifyAndResolve".equals(element.getName());
            }
        });
        ResolvedTypeWithMembers resolvedTypeWithMembers = memberResolver.resolve(resolvedType, null, null);
        ResolvedMethod[] methods = resolvedTypeWithMembers.getMemberMethods();
        if (methods.length == 0) {
            fail("Missing methods: should find one");
        }
        ResolvedMethod typesMatchResolvedMethod = methods[0];
        Method typesMatchMethod = typesMatchResolvedMethod.getRawMember();
        typesMatchMethod.setAccessible(true);

        // first test equality
        GenericType<MatchB<List<?>>> matchBList = new GenericType<MatchB<List<?>>>() { };
        ResolvedType matchBListResolved = typeResolver.resolve(matchBList);
        assertTrue((Boolean) typesMatchMethod.invoke(typeResolver, matchBListResolved, matchBListResolved));
        GenericType<MatchA<Set<?>, Comparator<Set<?>>>> matchASet = new GenericType<MatchA<Set<?>, Comparator<Set<?>>>>() { };
        GenericType<MatchA<Set<?>, Comparator<Set<?>>>> matchASet1 = new GenericType<MatchA<Set<?>, Comparator<Set<?>>>>() { };
        ResolvedType matchASetResolved = typeResolver.resolve(matchASet);
        ResolvedType matchASetResolved1 = typeResolver.resolve(matchASet1);
        assertTrue((Boolean) typesMatchMethod.invoke(typeResolver, matchASetResolved, matchASetResolved1));

        // now check inequality
        GenericType<MatchA<List<?>, Comparator<List<?>>>> matchAList = new GenericType<MatchA<List<?>, Comparator<List<?>>>>() { };
        ResolvedType matchAListResolved = typeResolver.resolve(matchAList);
        assertFalse((Boolean) typesMatchMethod.invoke(typeResolver, matchAListResolved, matchASetResolved));

        // now ensure different number of type-parameters are handled correctly
        assertFalse((Boolean) typesMatchMethod.invoke(typeResolver, matchAListResolved, matchBListResolved));
        assertFalse((Boolean) typesMatchMethod.invoke(typeResolver, matchBListResolved, matchAListResolved));
    }

    public void testMultiDimensionalGenericArrays() throws Exception
    {
        ResolvedType resolvedType = typeResolver.resolve(new GenericType<List<String>[][]>() { });
        assertEquals(List[][].class, resolvedType.getErasedType());
        assertEquals(Collections.emptyList(), resolvedType.getTypeParameters());
        assertTrue(resolvedType.isArray());
        ResolvedArrayType arrayType = (ResolvedArrayType) resolvedType;
        ResolvedType inner1 = arrayType.getArrayElementType();
        assertEquals(List[].class, inner1.getErasedType());
        assertTrue(inner1.isArray());
        ResolvedArrayType arrayType2 = (ResolvedArrayType) inner1;
        ResolvedType inner2 = arrayType2.getArrayElementType();
        assertEquals(List.class, inner2.getErasedType());
        List<ResolvedType> inner2TypeParams = inner2.getTypeParameters();
        assertEquals(1, inner2TypeParams.size());
        assertEquals(String.class, inner2TypeParams.get(0).getErasedType());

        assertEquals("java.util.List<java.lang.String>[][]", resolvedType.toString());
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
