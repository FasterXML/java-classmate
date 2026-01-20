package com.fasterxml.classmate;

import com.fasterxml.classmate.types.ResolvedArrayType;
import com.fasterxml.classmate.types.ResolvedPrimitiveType;
import com.fasterxml.classmate.types.ResolvedRecursiveType;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("serial")
public class TestResolvedTypeMapper extends BaseTest {
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

	// // Multi-level resolution needed

	static class MyStringLongMap extends MyStringKeyMap<Long> {
		public Map<String, Long> expectedType;
	}

	static class MyStringKeyMap<V> extends TreeMap<String, V> {
	}

	// // For verifying "jdk type" resolution

	static class ListWrapper<T> {
		public List<T> wrap() {
			return null;
		}
	}

	// Since using the regular java API we cannot get a parameterized type for List<String>, add the expected type to compare the returntype of the wrap method against
	static class StringListWrapper extends ListWrapper<String> {
		public long field;

		public List<String> expectedType;
	}

	// and recursive types...
	static abstract class SelfRefType implements Comparable<SelfRefType> {
	}

	// And arrays (ResolvedType resolves TypeVariables to the upperbound, in this case Object[] and List<Object>[])
	static abstract class GenericArray<E, T extends List<E>> {
		public E[] field;
		public Object[] expectedTypeForField;
		public T[] boundedField;
		public List<Object>[] expectedTypeForBoundedField;
		public List<? super E>[] wildcardAndSuperField;
	}

	// When using concrete types, we can resolve stricter bounds for the fields in the superclass
	static class StringArray extends GenericArray<String, List<String>> {
		public String[] expectedType;
		public List<String>[] expectedBoundedType;
		public List<Object>[] expectedWildcardSuperBoundedType;
	}

	static class StringMapArray {
		public Map<String, Long>[] field;
	}
    
    /*
    /**********************************************************************
    /* setup
    /**********************************************************************
     */

	// Let's use a single instance for all tests, to increase chance of seeing failures
	protected final ResolvedTypeMapper mapper = new ResolvedTypeMapper();

	// Let's use the type resolver to construct the ResolvedType used in the tests
	protected final TypeResolver resolver = new TypeResolver();

    /*
    /**********************************************************************
    /* Unit tests, normal operation
    /**********************************************************************
     */

	public void testMapWithRawType() {
		ResolvedType resolvedType = resolver.resolve(List.class);
		assertEquals(List.class, mapper.map(resolvedType));
	}

	public void testPrimitiveTypes() {
		for (ResolvedPrimitiveType primitiveType : ResolvedPrimitiveType.all()) {
			assertEquals(primitiveType._erasedType, mapper.map(primitiveType));
		}
	}

	public void testPrimitiveArrayTypes() {
		ResolvedType resolvedType = resolver.resolve(int[].class);
		assertEquals(int[].class, mapper.map(resolvedType));
	}

	public void testGenericUnboundedArrayTypes() throws NoSuchFieldException {
		Field f = GenericArray.class.getDeclaredField("field");
		ResolvedType genericArrayType = resolver.resolve(GenericArray.class);
		ResolvedType fieldType = resolver.resolve(genericArrayType.getTypeBindings(), f.getGenericType());
		Type mappedArrayType = mapper.map(fieldType);
		// Check it is a Object[]
		assertEquals(GenericArray.class.getDeclaredField("expectedTypeForField").getGenericType(), mappedArrayType);
		assertTrue(mappedArrayType instanceof Class<?>);
	}

	public void testGenericBoundedArrayTypes() throws NoSuchFieldException {
		Field f = GenericArray.class.getDeclaredField("boundedField");
		ResolvedType genericArrayType = resolver.resolve(GenericArray.class);
		ResolvedType fieldType = resolver.resolve(genericArrayType.getTypeBindings(), f.getGenericType());
		Type mappedArrayType = mapper.map(fieldType);
		// Check it is a List<Object>[]
		Type expectedGenericType = GenericArray.class.getDeclaredField("expectedTypeForBoundedField").getGenericType();
		assertEquals(expectedGenericType, mappedArrayType);
		assertTrue(mappedArrayType instanceof GenericArrayType);
		//Test equals is symmetric
		assertEquals(mappedArrayType, expectedGenericType);
	}

	public void testGenericWildcardWithSuperArrayTypes() throws NoSuchFieldException {
		Field f = GenericArray.class.getDeclaredField("wildcardAndSuperField");
		ResolvedType genericArrayType = resolver.resolve(GenericArray.class);
		ResolvedType fieldType = resolver.resolve(genericArrayType.getTypeBindings(), f.getGenericType());
		Type mappedArrayType = mapper.map(fieldType);
		// Check it is a List<Object>[]
		assertEquals(GenericArray.class.getDeclaredField("expectedTypeForBoundedField").getGenericType(), mappedArrayType);
		assertTrue(mappedArrayType instanceof GenericArrayType);

		// Even after resolving E, we would still get the same upper bound, since we are using super
		ResolvedType stringArrayType = resolver.resolve(StringArray.class);
		assertEquals(StringArray.class, mapper.map(stringArrayType));
		ResolvedType resolvedType = resolver.resolve(stringArrayType.getParentClass().getTypeBindings(), f.getGenericType());
		// Check it is a List<Object>[]
		assertEquals(StringArray.class.getDeclaredField("expectedWildcardSuperBoundedType").getGenericType(), mapper.map(resolvedType));
	}

	public void testConcreteArrayTypes() throws NoSuchFieldException {
		Field f = GenericArray.class.getDeclaredField("field");
		ResolvedType stringArrayType = resolver.resolve(StringArray.class);
		assertEquals(StringArray.class, mapper.map(stringArrayType));

		ResolvedType resolvedType = resolver.resolve(stringArrayType.getParentClass().getTypeBindings(), f.getGenericType());
		Type mappedArrayType = mapper.map(resolvedType);
		// Check it is a String[]
		assertEquals(StringArray.class.getDeclaredField("expectedType").getGenericType(), mappedArrayType);
		assertTrue(mappedArrayType instanceof Class<?>);
	}

	public void testConcreteParameterArrayTypes() throws NoSuchFieldException {
		Field f = GenericArray.class.getDeclaredField("boundedField");
		ResolvedType stringArrayType = resolver.resolve(StringArray.class);
		assertEquals(StringArray.class, mapper.map(stringArrayType));

		ResolvedType resolvedType = resolver.resolve(stringArrayType.getParentClass().getTypeBindings(), f.getGenericType());
		Type mappedArrayType = mapper.map(resolvedType);
		// Check it is a List<String>[]
		assertEquals(StringArray.class.getDeclaredField("expectedBoundedType").getGenericType(), mappedArrayType);
		assertTrue(mappedArrayType instanceof GenericArrayType);
	}

	public void testParameterizedArrayTypes() throws NoSuchFieldException {
		Field f = StringMapArray.class.getDeclaredField("field");
		ResolvedType stringMapArrayType = resolver.resolve(StringMapArray.class);
		assertEquals(StringMapArray.class, mapper.map(stringMapArrayType));

		ResolvedType resolvedType = resolver.resolve(stringMapArrayType.getTypeBindings(), f.getGenericType());
		// Check it is a Map<String, Long>[]
		Type mappedArrayType = mapper.map(resolvedType);
		assertEquals(f.getGenericType(), mappedArrayType);
		assertTrue(mappedArrayType instanceof GenericArrayType);

	}

	public void testGenericMap() {
		// First, direct ref
		GenericType<?> mapInput = new GenericType<Map<String, Long>>() {
		};
		ResolvedType mapType = resolver.resolve(mapInput);

		//Parameterized map type, since GenericType is skipped
		ParameterizedType parameterizedMapType = (ParameterizedType) ((ParameterizedType) mapInput.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		assertEquals(parameterizedMapType, mapper.map(mapType));
		// Test equals is symmetric for ParameterizedType
		assertEquals(mapper.map(mapType), parameterizedMapType);

		//String
		Type mappedString = mapper.map(mapType.getTypeParameters().get(0));
		assertEquals(parameterizedMapType.getActualTypeArguments()[0], mappedString);
		assertEquals(String.class, mappedString);
		//Long
		Type mappedLong = mapper.map(mapType.getTypeParameters().get(1));
		assertEquals(parameterizedMapType.getActualTypeArguments()[1], mappedLong);
		assertEquals(Long.class, mappedLong);
	}

	public void testParametricMap() throws NoSuchFieldException {
		ResolvedType mapType = resolver.resolve(MyStringLongMap.class);

		assertEquals(MyStringLongMap.class,  mapper.map(mapType));
		// Ensure we can find parameters for Map
		ResolvedType mapSuperType = mapType.findSupertype(Map.class);
		// Check if it is a Map<String, Long>
		assertEquals(MyStringLongMap.class.getDeclaredField("expectedType").getGenericType(), mapper.map(mapSuperType));
	}

	public void testJdkType() throws Exception {
		ResolvedType wrapperType = resolver.resolve(StringListWrapper.class);
		Field f = StringListWrapper.class.getDeclaredField("field");
		// first; field has no generic stuff, should be simple
		ResolvedType fieldType = resolver.resolve(wrapperType.getTypeBindings(), f.getGenericType());
		assertEquals(Long.TYPE, mapper.map(fieldType));

		// but method return type is templatized; and MUST be given correct type bindings!
		Method m = ListWrapper.class.getDeclaredMethod("wrap");
		ResolvedType superType = wrapperType.getParentClass();
		ResolvedType methodReturnType = resolver.resolve(superType.getTypeBindings(),
				m.getGenericReturnType());
		// should be List<String>, but we cannot access that from the method, so we compare it to a typed field from the java API
		Type expectedType = StringListWrapper.class.getField("expectedType").getGenericType();
		assertEquals(expectedType, mapper.map(methodReturnType));
	}

	public void testSimpleSelfRef() {
		ResolvedType type = resolver.resolve(SelfRefType.class);
		List<ResolvedType> interfaces = type.getImplementedInterfaces();
		assertEquals(1, interfaces.size());
		ResolvedType compType = interfaces.get(0);
		assertEquals(SelfRefType.class.getGenericInterfaces()[0], mapper.map(compType));
	}

	/*
    /**********************************************************************
    /* Unit tests, error cases
    /**********************************************************************
     */

	public void testNullabilityInResolvedRecursiveType() {
		ResolvedRecursiveType resolvedType = new ResolvedRecursiveType(Integer.class, TypeBindings.emptyBindings());
		//empty reference
		try {
			mapper.map(resolvedType);
			fail("Expected failure");
		} catch (IllegalStateException e) {
			verifyException(e, "Missing self referenced type");
		}
	}

	public void testNullabilityInResolvedArrayType() {
		ResolvedArrayType resolvedType = new ResolvedArrayType(Integer.class, TypeBindings.emptyBindings(), null);
		//empty reference
		try {
			mapper.map(resolvedType);
			fail("Expected failure");
		} catch (IllegalStateException e) {
			verifyException(e, "Missing array element type");
		}
	}
}
