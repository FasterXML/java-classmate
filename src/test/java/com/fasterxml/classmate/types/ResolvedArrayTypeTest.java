package com.fasterxml.classmate.types;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.util.ResolvedTypeCache;

import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * User: blangel
 */
public class ResolvedArrayTypeTest {

    @Test
    public void equalityIncludesGenericElementType() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType strings = resolver.arrayType(resolver.resolve(List.class, String.class));
        ResolvedType integers = resolver.arrayType(resolver.resolve(List.class, Integer.class));
        ResolvedType sameStrings = resolver.arrayType(resolver.resolve(List.class, String.class));

        assertFalse(strings.equals(integers));
        assertFalse(integers.equals(strings));
        assertEquals(strings, sameStrings);
        assertEquals(strings.hashCode(), sameStrings.hashCode());
        Set<ResolvedType> types = new HashSet<ResolvedType>();
        types.add(strings);
        types.add(integers);
        types.add(sameStrings);
        assertEquals(2, types.size());
    }

    @Test
    public void cacheDistinguishesGenericArrayParameters() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType strings = resolver.arrayType(resolver.resolve(List.class, String.class));
        ResolvedType integers = resolver.arrayType(resolver.resolve(List.class, Integer.class));

        resolver.resolve(List.class, strings);
        ResolvedType result = resolver.resolve(List.class, integers);

        assertEquals(Integer.class, result.getTypeParameters().get(0).getArrayElementType()
                .getTypeParameters().get(0).getErasedType());
    }

    static class ArrayHolder<T> {
        public T[] typed;
        public String[] strings;
        @SuppressWarnings("rawtypes")
        public Map[] rawMaps;
        @SuppressWarnings("rawtypes")
        public Map rawMap;
        @SuppressWarnings("rawtypes")
        public List rawList;
    }

    @SuppressWarnings({ "rawtypes", "serial" })
    static class RawSuper<T> extends java.util.HashMap { }

    static class Base<T> { }

    static class Node extends Base<Node[]> { }

    static class Rec<T> extends Base<Rec<T>[]> { }

    static class Node2 extends Base<Node2[][]> { }

    @SuppressWarnings("rawtypes")
    static class GNode<T> extends Base<GNode[]> { }

    // Types containing self-references must not be cached, even if nested, since
    // they would be found by lookups for equal (but fully resolved) types
    @Test
    public void typeWithSelfReferenceNotCached() {
        TypeResolver resolver = new TypeResolver();
        resolver.resolve(Node.class);
        ResolvedType base = resolver.resolve(Base.class, Node[].class);
        ResolvedType elem = base.getTypeParameters().get(0).getArrayElementType();

        assertFalse(TypeResolver.isSelfReference(elem));
        assertEquals(Base.class, elem.getParentClass().getErasedType());
    }

    @Test
    public void multiDimArrayOfSelfReferenceNotCached() {
        TypeResolver resolver = new TypeResolver();
        resolver.resolve(Node2.class);
        ResolvedType direct = resolver.resolve(Node2[][].class);
        ResolvedType elem = direct.getArrayElementType().getArrayElementType();

        assertFalse(TypeResolver.isSelfReference(elem));
        assertEquals(Base.class, elem.getParentClass().getErasedType());
    }

    // Raw self-reference has no bindings, so it must not be equal to any parameterization
    // (would break transitivity, and with it Sets/Maps); known limitation: also not equal
    // to fully resolved raw type (`GNode<Object>[]`)
    @Test
    public void arrayOfRawRecursiveTypeEqualityIsTransitive() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType viaParent = resolver.resolve(GNode.class).getParentClass()
                .getTypeParameters().get(0);
        ResolvedType strings = resolver.arrayType(resolver.resolve(GNode.class, String.class));
        ResolvedType integers = resolver.arrayType(resolver.resolve(GNode.class, Integer.class));

        assertTrue(TypeResolver.isSelfReference(viaParent.getArrayElementType()));
        assertFalse(viaParent.equals(strings));
        assertFalse(viaParent.equals(integers));
        Set<ResolvedType> types = new HashSet<ResolvedType>();
        types.add(viaParent);
        types.add(strings);
        types.add(integers);
        assertEquals(3, types.size());
    }

    // Arrays with self-referential element type must equal ones with fully resolved
    // element type (needed for detecting overrides, f.ex `Node.foo(Node[])` vs `Base.foo(T)`)
    @Test
    public void arrayOfRecursiveTypeEqualsResolvedArray() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType viaParent = resolver.resolve(Node.class).getParentClass()
                .getTypeParameters().get(0);
        ResolvedType direct = resolver.resolve(Node[].class);

        assertTrue(TypeResolver.isSelfReference(viaParent.getArrayElementType()));
        assertFalse(TypeResolver.isSelfReference(direct.getArrayElementType()));
        assertEquals(direct, viaParent);
        assertEquals(viaParent, direct);
        assertEquals(direct.hashCode(), viaParent.hashCode());
        assertEquals(resolver.resolve(Node[][].class),
                resolver.arrayType(viaParent));
    }

    @Test
    public void arrayOfGenericRecursiveTypeComparesTypeParameters() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType viaParent = resolver.resolve(Rec.class, String.class).getParentClass()
                .getTypeParameters().get(0);
        ResolvedType strings = resolver.arrayType(resolver.resolve(Rec.class, String.class));
        ResolvedType integers = resolver.arrayType(resolver.resolve(Rec.class, Integer.class));

        assertTrue(TypeResolver.isSelfReference(viaParent.getArrayElementType()));
        assertEquals(strings, viaParent);
        assertEquals(viaParent, strings);
        assertEquals(strings.hashCode(), viaParent.hashCode());
        assertFalse(integers.equals(viaParent));
        assertFalse(viaParent.equals(integers));
    }

    // [classmate#125]: array types must not retain bindings of the context they are resolved in
    @Test
    public void arrayTypesIgnoreResolutionContext() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType direct = resolver.resolve(String[].class);
        ResolvedType viaFactory = resolver.arrayType(String.class);
        ResolvedType typed = _field(resolver, "typed");
        ResolvedType strings = _field(resolver, "strings");

        for (ResolvedType type : new ResolvedType[] { direct, viaFactory, typed, strings }) {
            assertEquals(String[].class, type.getErasedType());
            assertTrue(type.getTypeBindings().isEmpty());
            assertEquals(0, type.getTypeParameters().size());
            assertEquals(direct, type);
            assertEquals(type, direct);
            assertEquals(direct.hashCode(), type.hashCode());
        }
    }

    // Array types are cached (keyed by element type), regardless of how constructed
    @Test
    public void arrayTypesAreCached() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType direct = resolver.resolve(String[].class);

        assertSame(direct, resolver.resolve(String[].class));
        assertSame(direct, resolver.arrayType(String.class));
        assertSame(direct, _field(resolver, "strings"));
        assertSame(direct, _field(resolver, "typed"));
        // and multi-dimensional arrays share element types
        ResolvedType nested = resolver.resolve(String[][].class);
        assertSame(nested, resolver.arrayType(String[].class));
        assertSame(direct, nested.getArrayElementType());

        ResolvedType listStrings = resolver.arrayType(resolver.resolve(List.class, String.class));
        assertSame(listStrings, resolver.arrayType(resolver.resolve(List.class, String.class)));
        assertNotSame(listStrings, resolver.arrayType(resolver.resolve(List.class, Integer.class)));
    }

    @Test
    public void rawGenericArrayIgnoresResolutionContext() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType rawMaps = _field(resolver, "rawMaps");
        ResolvedType direct = resolver.resolve(Map[].class);

        assertEquals(direct, rawMaps);
        // raw Map resolves to bounds of both type parameters, not to `Map<String>`
        List<ResolvedType> params = rawMaps.getArrayElementType().getTypeParameters();
        assertEquals(2, params.size());
        assertEquals(Object.class, params.get(0).getErasedType());
        assertEquals(Object.class, params.get(1).getErasedType());
    }

    // [classmate#125]: same for non-array raw types
    @Test
    public void rawGenericTypeIgnoresResolutionContext() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType rawMap = _field(resolver, "rawMap");

        assertEquals(resolver.resolve(Map.class), rawMap);
        List<ResolvedType> params = rawMap.getTypeParameters();
        assertEquals(2, params.size());
        assertEquals(Object.class, params.get(0).getErasedType());
        assertEquals(Object.class, params.get(1).getErasedType());
    }

    @Test
    public void rawGenericTypeDoesNotPoisonCache() {
        TypeResolver resolver = new TypeResolver();
        _field(resolver, "rawList");
        ResolvedType listType = resolver.resolve(List.class, String.class);

        assertNotNull(listType.getTypeBindings().findBoundType("E"));
        ResolvedType collType = listType.findSupertype(Collection.class);
        assertEquals(String.class, collType.getTypeParameters().get(0).getErasedType());
    }

    static class StringHolder<T> {
        public String value;
    }

    // [classmate#125]: non-generic member types must be cached once, not once per
    // parameterization of the enclosing type
    @Test
    public void nonGenericMemberTypeCachedOnce() {
        ResolvedTypeCache cache = ResolvedTypeCache.lruCache(200);
        TypeResolver resolver = new TypeResolver(cache);
        MemberResolver memberResolver = new MemberResolver(resolver);
        // pre-resolve type parameters so that only the holder types are added below
        resolver.resolve(Integer.class);
        resolver.resolve(Long.class);

        ResolvedType intValue = memberResolver.resolve(resolver.resolve(StringHolder.class, Integer.class),
                null, null).getMemberFields()[0].getType();
        int size = cache.size();
        ResolvedType longValue = memberResolver.resolve(resolver.resolve(StringHolder.class, Long.class),
                null, null).getMemberFields()[0].getType();

        // only `StringHolder<Long>` itself is new
        assertEquals(size + 1, cache.size());
        assertSame(intValue, longValue);
        assertSame(resolver.resolve(String.class), longValue);
    }

    @Test
    public void rawSuperClassIgnoresResolutionContext() {
        TypeResolver resolver = new TypeResolver();
        ResolvedType type = resolver.resolve(RawSuper.class, Integer.class);
        ResolvedType parent = type.getParentClass();

        assertEquals(java.util.HashMap.class, parent.getErasedType());
        List<ResolvedType> params = parent.getTypeParameters();
        assertEquals(2, params.size());
        assertEquals(Object.class, params.get(0).getErasedType());
        assertEquals(Object.class, params.get(1).getErasedType());
    }

    @Test
    public void cachedArrayTypeParametersDoNotDependOnResolutionOrder() {
        TypeResolver resolver = new TypeResolver();
        resolver.resolve(List.class, _field(resolver, "typed"));
        ResolvedType listType = resolver.resolve(List.class, String[].class);

        ResolvedType arrayType = listType.getTypeParameters().get(0);
        assertEquals(String[].class, arrayType.getErasedType());
        assertEquals(0, arrayType.getTypeParameters().size());
    }

    @Test
    public void arrayOfRecursiveTypeNotCachedWithSelfReference() {
        TypeResolver resolver = new TypeResolver();
        resolver.resolve(Node.class);
        ResolvedType direct = resolver.resolve(Node[].class);

        assertFalse(TypeResolver.isSelfReference(direct.getArrayElementType()));
        assertEquals(Base.class, direct.getArrayElementType().getParentClass().getErasedType());
        assertEquals(resolver.arrayType(Node.class), direct);
        assertEquals(new TypeResolver().resolve(Node[].class), direct);
    }

    private ResolvedType _field(TypeResolver resolver, String name) {
        ResolvedTypeWithMembers members = new MemberResolver(resolver)
                .resolve(resolver.resolve(ArrayHolder.class, String.class), null, null);
        for (ResolvedField field : members.getMemberFields()) {
            if (field.getName().equals(name)) {
                return field.getType();
            }
        }
        throw new IllegalArgumentException("No field '"+name+"'");
    }

    @Test
    public void getArrayElementType() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertNull(arrayType.getArrayElementType());

        ResolvedArrayType arrayType1 = new ResolvedArrayType(Object.class, null, arrayType);
        assertEquals(arrayType, arrayType1.getArrayElementType());
    }

    @Test
    public void canCreateSubtypes() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertFalse(arrayType.canCreateSubtypes());
    }

    @Test
    public void getParentClass() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        // With [classmate#51]:
        assertNotNull(arrayType.getParentClass());
        assertEquals(Object.class, arrayType.getParentClass().getErasedType());
    }

    @Test
    public void getSelfReferencedType() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertNull(arrayType.getSelfReferencedType());
    }

    @Test
    public void getImplementedInterfaces() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertEquals(0, arrayType.getImplementedInterfaces().size());

        arrayType = new ResolvedArrayType(Collection.class, TypeBindings.create(String.class, (ResolvedType[]) null), ResolvedObjectType.create(String.class, null, null, null));
        assertEquals(0, arrayType.getImplementedInterfaces().size());
    }

    @Test
    public void isAbstract() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertFalse(arrayType.isAbstract());
    }

    @Test
    public void isArray() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertTrue(arrayType.isArray());
    }

    @Test
    public void isPrimitive() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertFalse(arrayType.isPrimitive());
    }

    @Test
    public void isInterface() {
        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, null);
        assertFalse(arrayType.isInterface());
    }

    @Test
    public void appendSignature() {
        ResolvedArrayType npeType = new ResolvedArrayType(Object.class, null, null);
        try {
            npeType.appendSignature(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            npeType.appendSignature(new StringBuilder());
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, ResolvedObjectType.create(Object.class, null, null, null));
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = arrayType.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("[Ljava/lang/Object;", returned.toString());
        buffer = new StringBuilder("Existing ");
        returned = arrayType.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing [Ljava/lang/Object;", returned.toString());
    }

    @Test
    public void appendErasedSignature() {
        ResolvedArrayType npeType = new ResolvedArrayType(Object.class, null, null);
        try {
            npeType.appendErasedSignature(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            npeType.appendErasedSignature(new StringBuilder());
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, ResolvedObjectType.create(Object.class, null, null, null));
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = arrayType.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("[Ljava/lang/Object;", returned.toString());
        buffer = new StringBuilder("Existing ");
        returned = arrayType.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing [Ljava/lang/Object;", returned.toString());
    }

    @Test
    public void appendBriefDescription() {
        ResolvedArrayType npeType = new ResolvedArrayType(Object.class, null, null);
        try {
            npeType.appendBriefDescription(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            npeType.appendBriefDescription(new StringBuilder());
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, ResolvedObjectType.create(Object.class, null, null, null));
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = arrayType.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("java.lang.Object[]", returned.toString());
        buffer = new StringBuilder("Existing ");
        returned = arrayType.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing java.lang.Object[]", returned.toString());
    }

    @Test
    public void appendFullDescription() {
        ResolvedArrayType npeType = new ResolvedArrayType(Object.class, null, null);
        try {
            npeType.appendFullDescription(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            npeType.appendFullDescription(new StringBuilder());
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedArrayType arrayType = new ResolvedArrayType(Object.class, null, ResolvedObjectType.create(Object.class, null, null, null));
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = arrayType.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("java.lang.Object[]", returned.toString());
        buffer = new StringBuilder("Existing ");
        returned = arrayType.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("Existing java.lang.Object[]", returned.toString());
    }
}
