package com.fasterxml.classmate;

import java.util.*;

import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.classmate.types.ResolvedRecursiveType;

/**
 * Tests for [classmate#132]: members inherited via self-referential
 * implemented interfaces must not be dropped.
 */
public class MemberResolver132Test extends BaseTest
{
    public interface Top { void top(); }
    public interface BaseI<X> extends Top { X get(); }
    public interface MidI<T> extends BaseI<OuterI> { }
    public interface OuterI extends MidI<String> { }

    // Same with class implementing interface
    static abstract class BaseC<X> implements BaseI<X> { }
    static abstract class MidC<T> extends BaseC<OuterC> { }
    static abstract class OuterC extends MidC<String> {
        public void own() { }
    }

    private final TypeResolver _typeResolver = new TypeResolver();
    private final MemberResolver _memberResolver = new MemberResolver(_typeResolver);

    public void testStandAloneInterface()
    {
        ResolvedType outer = _typeResolver.resolve(OuterI.class);
        assertEquals(Arrays.asList("get", "top"), _methodNames(outer));
    }

    public void testInterfaceViaTypeParameter()
    {
        ResolvedType outerInMid = _typeResolver.resolve(MidI.class, String.class)
                .getImplementedInterfaces().get(0).getTypeParameters().get(0);
        assertEquals(OuterI.class, outerInMid.getErasedType());
        assertEquals(Arrays.asList("get", "top"), _methodNames(outerInMid));
        // and `get()` should have proper return type
        ResolvedMethod get = _findMethod(outerInMid, "get");
        assertEquals(OuterI.class, get.getReturnType().getErasedType());
    }

    public void testImplementedInterfacesViaTypeParameter()
    {
        ResolvedType outerInMid = _typeResolver.resolve(MidI.class, String.class)
                .getImplementedInterfaces().get(0).getTypeParameters().get(0);
        // `MidI<String>` was still being resolved, but must not be exposed as self-reference
        List<ResolvedType> ifs = outerInMid.getImplementedInterfaces();
        assertEquals(1, ifs.size());
        ResolvedType mid = ifs.get(0);
        assertFalse(mid instanceof ResolvedRecursiveType);
        assertEquals(MidI.class, mid.getErasedType());
        assertEquals(String.class, mid.getTypeParameters().get(0).getErasedType());
        assertEquals(BaseI.class, mid.getImplementedInterfaces().get(0).getErasedType());

        // and supertypes reachable via it must be found
        ResolvedType base = outerInMid.findSupertype(BaseI.class);
        assertNotNull(base);
        assertEquals(OuterI.class, base.getTypeParameters().get(0).getErasedType());
        assertNotNull(outerInMid.findSupertype(Top.class));
    }

    public void testClassViaTypeParameter()
    {
        ResolvedType standAlone = _typeResolver.resolve(OuterC.class);
        assertEquals(Arrays.asList("get", "own", "top"), _methodNames(standAlone));

        ResolvedType outerInMid = _typeResolver.resolve(MidC.class, String.class)
                .getParentClass().getTypeParameters().get(0);
        assertEquals(OuterC.class, outerInMid.getErasedType());
        assertEquals(Arrays.asList("get", "own", "top"), _methodNames(outerInMid));
        assertNotNull(outerInMid.findSupertype(BaseI.class));
        assertNotNull(outerInMid.findSupertype(Top.class));
    }

    public void testSelfReferenceAsMainType()
    {
        // self-reference itself as the type to resolve members of
        ResolvedType outer = _typeResolver.resolve(OuterI.class);
        ResolvedType selfRef = outer.getImplementedInterfaces().get(0) // MidI<String>
                .getImplementedInterfaces().get(0) // BaseI<OuterI>
                .getTypeParameters().get(0);
        assertEquals(OuterI.class, selfRef.getErasedType());
        assertEquals(Arrays.asList("get", "top"), _methodNames(selfRef));
    }

    private List<String> _methodNames(ResolvedType type)
    {
        List<String> names = new ArrayList<String>();
        for (ResolvedMethod m : _memberResolver.resolve(type, null, null).getMemberMethods()) {
            names.add(m.getName());
        }
        Collections.sort(names);
        return names;
    }

    private ResolvedMethod _findMethod(ResolvedType type, String name)
    {
        for (ResolvedMethod m : _memberResolver.resolve(type, null, null).getMemberMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        fail("No method '"+name+"' found");
        return null;
    }
}
