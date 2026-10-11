package com.fasterxml.classmate.types;

import java.util.List;

import com.fasterxml.classmate.*;

/**
 * Tests for [classmate#137]: supertype lookups on a self-reference
 * ({@link ResolvedRecursiveType}) must give same results as for the type
 * it represents.
 */
public class ResolvedRecursiveType137Test extends BaseTest
{
    public interface Top { void top(); }
    public interface BaseI<X> extends Top { X get(); }
    public interface MidI<T> extends BaseI<OuterI> { }
    public interface OuterI extends MidI<String> { }

    public static class Sym<T extends Sym<T>> implements Comparable<T> {
        @Override
        public int compareTo(T o) { return 0; }
    }

    // Expanding types
    public static class Base<X> { }
    public static class C<T> extends Base<D<T>> { }
    public static class D<T> extends C<List<T>> { }

    private final TypeResolver _typeResolver = new TypeResolver();

    public void testSupertypesOfSelfReferenceInInterface()
    {
        ResolvedType outer = _typeResolver.resolve(OuterI.class);
        ResolvedType selfRef = outer.findSupertype(BaseI.class).getTypeParameters().get(0);
        assertTrue(TypeResolver.isSelfReference(selfRef));
        ResolvedType actual = ((ResolvedRecursiveType) selfRef).getActualType();

        ResolvedType top = selfRef.findSupertype(Top.class);
        assertNotNull(top);
        assertEquals(Top.class, top.getErasedType());
        assertEquals(actual.findSupertype(Top.class), top);

        List<ResolvedType> params = selfRef.typeParametersFor(BaseI.class);
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals(OuterI.class, params.get(0).getErasedType());

        params = selfRef.typeParametersFor(MidI.class);
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals(String.class, params.get(0).getErasedType());

        // type itself
        assertEquals(actual, selfRef.findSupertype(OuterI.class));
        assertEquals(0, selfRef.typeParametersFor(OuterI.class).size());

        // and non-supertypes
        assertNull(selfRef.findSupertype(Runnable.class));
        assertNull(selfRef.typeParametersFor(String.class));
    }

    public void testSupertypesOfGenericSelfReference()
    {
        ResolvedType sym = _typeResolver.resolve(Sym.class);
        ResolvedType selfRef = sym.getTypeParameters().get(0);
        assertTrue(TypeResolver.isSelfReference(selfRef));

        List<ResolvedType> params = selfRef.typeParametersFor(Comparable.class);
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals(Sym.class, params.get(0).getErasedType());

        assertEquals(Object.class, selfRef.findSupertype(Object.class).getErasedType());
    }

    public void testSupertypesOfExpandingSelfReference()
    {
        ResolvedType c = _typeResolver.resolve(new GenericType<C<String>>() { });
        ResolvedType selfRef = c.findSupertype(Base.class).getTypeParameters().get(0);
        assertEquals(D.class, selfRef.getErasedType());

        // `D<String>` -> `C<List<String>>` -> `Base<D<List<String>>>`
        ResolvedType base = selfRef.findSupertype(Base.class);
        assertNotNull(base);
        assertEquals("Base<D<List<String>>>", base.getBriefDescription()
                .replace(getClass().getName()+"$", "").replace("java.util.", "")
                .replace("java.lang.", ""));
        assertEquals(C.class, selfRef.findSupertype(C.class).getErasedType());
    }
}
