package com.fasterxml.classmate;

import java.util.*;

import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.classmate.types.ResolvedRecursiveType;

/**
 * Tests for [classmate#132]: supertypes (and members inherited via them) of
 * self-referential supertypes must not be dropped.
 */
public class MemberResolver132Test extends BaseTest
{
    public interface Top { void top(); }
    public interface BaseI<X> extends Top { X get(); }
    public interface Ref<R> { R ref(); }

    // Self-referential interface: `MidI<String>` within `OuterI`
    public interface MidI<T> extends BaseI<OuterI> { }
    public interface OuterI extends MidI<String> { }

    // Self-referential parent class
    static abstract class MidC<T> implements BaseI<OuterC> { }
    static abstract class OuterC extends MidC<String> { }

    // Class implementing self-referential interface
    public interface MidI2<T> extends BaseI<OuterC2> { }
    static abstract class OuterC2 implements MidI2<String> { }

    // Self-referential supertypes (`MidJ`, `MidK`) that also refer to a type still being
    // resolved (`OutJ`, `OutK`), so are only valid within their resolution context
    static abstract class OutJ implements BaseI<MidJ> { }
    public interface MidJ extends BaseI<OuterJ>, Ref<OutJ> { }
    public interface OuterJ extends MidJ { }

    static abstract class OutK implements BaseI<MidK> { }
    static abstract class MidK implements BaseI<OuterK>, Ref<OutK> { }
    static abstract class OuterK extends MidK { }

    // Generic variant, for `resolveSubtype()`
    public interface GMid<T> extends BaseI<GOuter<T>>, Ref<GSub<T>> { }
    public interface GOuter<T> extends GMid<T> { }
    static abstract class GSub<T> implements GMid<T> { }

    private final TypeResolver _typeResolver = new TypeResolver();
    private final MemberResolver _memberResolver = new MemberResolver(_typeResolver);

    public void testInterfaceViaTypeParameter()
    {
        ResolvedType outer = _verifyViaContext(_typeResolver.resolve(MidI.class, String.class),
                OuterI.class, "get", "top");
        ResolvedType mid = outer.getImplementedInterfaces().get(0);
        assertEquals(MidI.class, mid.getErasedType());
        assertEquals(String.class, mid.getTypeParameters().get(0).getErasedType());
        assertEquals(OuterI.class, _findMethod(outer, "get").getReturnType().getErasedType());
    }

    public void testClassViaTypeParameter()
    {
        _verifyViaContext(_typeResolver.resolve(MidC.class, String.class), OuterC.class, "get", "top");
        _verifyViaContext(_typeResolver.resolve(MidI2.class, String.class), OuterC2.class, "get", "top");
    }

    // Self-reference itself as the type to resolve members of
    public void testSelfReferenceAsMainType()
    {
        ResolvedType selfRef = _typeResolver.resolve(OuterI.class)
                .findSupertype(BaseI.class).getTypeParameters().get(0);
        assertTrue(selfRef instanceof ResolvedRecursiveType);
        assertEquals(Arrays.asList("get", "top"), _methodNames(selfRef));
    }

    // Self-referential supertypes only valid within their resolution context must
    // be replaced with stand-alone types
    public void testIncompleteSupertypes()
    {
        ResolvedType midJ = _param(_typeResolver.resolve(OutJ.class));
        ResolvedType outerJ = _verifyViaContext(midJ, OuterJ.class, "get", "ref", "top");
        assertSame(_typeResolver.resolve(MidJ.class), outerJ.getImplementedInterfaces().get(0));
        _verifyRef(outerJ, OutJ.class);

        ResolvedType midK = _param(_typeResolver.resolve(OutK.class));
        ResolvedType outerK = _verifyViaContext(midK, OuterK.class, "get", "ref", "top");
        assertSame(_typeResolver.resolve(MidK.class), outerK.getParentClass());
        _verifyRef(outerK, OutK.class);
    }

    public void testSubtypeOfIncompleteType()
    {
        ResolvedType midJ = _param(_typeResolver.resolve(OutJ.class));
        assertSame(_typeResolver.resolve(OuterJ.class), _typeResolver.resolveSubtype(midJ, OuterJ.class));

        // generic subtype, with self-reference to incomplete type (`GMid<T>` within
        // `GOuter<T>`, due to reference to `GSub<T>`)
        ResolvedType superType = _typeResolver.resolve(BaseI.class,
                _typeResolver.resolve(GOuter.class, String.class));
        ResolvedType sub = _typeResolver.resolveSubtype(superType, GSub.class);
        assertSame(_typeResolver.resolve(GSub.class, String.class), sub);
        assertEquals(_typeResolver.resolve(GMid.class, String.class),
                _param(sub).getImplementedInterfaces().get(0));
        assertEquals(_typeResolver.resolve(GSub.class, String.class),
                sub.findSupertype(Ref.class).getTypeParameters().get(0));
        assertEquals(Arrays.asList("get", "ref", "top"), _methodNames(sub));
    }

    /**
     * Verifies type obtained as type parameter of {@code BaseI} of given type: must have
     * same supertypes and members as stand-alone type, without self-references.
     */
    private ResolvedType _verifyViaContext(ResolvedType context, Class<?> expType,
            String... expMethods)
    {
        ResolvedType type = _param(context);
        assertEquals(expType, type.getErasedType());
        List<ResolvedType> supertypes = new ArrayList<>(type.getImplementedInterfaces());
        if (type.getParentClass() != null) {
            supertypes.add(type.getParentClass());
        }
        assertFalse(supertypes.isEmpty());
        for (ResolvedType t : supertypes) {
            assertFalse(t instanceof ResolvedRecursiveType);
        }
        assertNotNull(type.findSupertype(Top.class));
        List<String> methods = _methodNames(type);
        assertEquals(Arrays.asList(expMethods), methods);
        assertEquals(_methodNames(_typeResolver.resolve(expType)), methods);
        return type;
    }

    private void _verifyRef(ResolvedType type, Class<?> expParam)
    {
        ResolvedType param = type.findSupertype(Ref.class).getTypeParameters().get(0);
        assertFalse(param instanceof ResolvedRecursiveType);
        assertEquals(expParam, param.getErasedType());
    }

    private static ResolvedType _param(ResolvedType type) {
        return type.findSupertype(BaseI.class).getTypeParameters().get(0);
    }

    private List<String> _methodNames(ResolvedType type)
    {
        List<String> names = new ArrayList<>();
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
