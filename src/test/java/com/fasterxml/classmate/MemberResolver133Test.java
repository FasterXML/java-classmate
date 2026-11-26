package com.fasterxml.classmate;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.classmate.types.ResolvedRecursiveType;

/**
 * Tests for [classmate#133]: member type of bare type variable bound to self-reference
 * must be a stand-alone (fully resolved) type, same as when used within
 * other types (like {@code T[]}).
 */
public class MemberResolver133Test extends BaseTest
{
    public static class Base<X> {
        public X value;
        public X[] arr;

        public X get() { return null; }
        public void set(X x) { }
    }

    public static class Sym<T extends Sym<T>> extends Base<T> { }

    // Raw self-reference within (parameterized) type parameter
    public static class RA<T extends RB> { }
    public static class RB extends Base<RA> { }

    // Bare raw self-reference (`T` bound to raw `RawSelf` while resolving its bounds)
    @SuppressWarnings("rawtypes")
    public static class RawSelf<T extends RawSelf> extends Base<T> { }

    private final TypeResolver _typeResolver = new TypeResolver();
    private final MemberResolver _memberResolver = new MemberResolver(_typeResolver);

    @Test
    public void testGenericSelfReference()
    {
        _verify(Sym.class, Sym.class, Base.class);
    }

    @Test
    public void testRawSelfReferenceInTypeParameter()
    {
        _verify(RB.class, RA.class, Object.class);
    }

    @Test
    public void testBareRawSelfReference()
    {
        // sanity check: type parameter is a raw self-reference
        ResolvedType param = _typeResolver.resolve(RawSelf.class).getTypeParameters().get(0);
        assertTrue(TypeResolver.isSelfReference(param));
        assertTrue(param.getTypeBindings().isEmpty());

        _verify(RawSelf.class, RawSelf.class, Base.class);
    }

    // Self-reference passed directly to public `resolve()` methods
    @Test
    public void testResolveSelfReference()
    {
        ResolvedType rawEnum = _typeResolver.resolve(Enum.class);
        ResolvedType param = rawEnum.getTypeParameters().get(0);
        assertTrue(TypeResolver.isSelfReference(param));

        ResolvedType type = _typeResolver.resolve(param);
        assertFalse(TypeResolver.isSelfReference(type));
        assertEquals(Enum.class, type.getErasedType());
        assertNotNull(type.getParentClass());
        assertEquals(rawEnum, type);

        ResolvedType type2 = _typeResolver.resolve(TypeBindings.emptyBindings(), param);
        assertFalse(TypeResolver.isSelfReference(type2));
        assertEquals(type, type2);

        ResolvedType elem = _typeResolver.arrayType(param).getArrayElementType();
        assertFalse(TypeResolver.isSelfReference(elem));
        assertEquals(type, elem);
    }

    private void _verify(Class<?> type, Class<?> expValueType, Class<?> expParent)
    {
        ResolvedTypeWithMembers members = _memberResolver.resolve(_typeResolver.resolve(type),
                null, null);
        ResolvedType value = _findField(members, "value").getType();
        ResolvedType arr = _findField(members, "arr").getType();

        _verifyStandalone(value, expValueType, expParent);

        ResolvedType elem = arr.getArrayElementType();
        assertFalse(elem instanceof ResolvedRecursiveType, "Should not be self-reference: "+elem);
        assertEquals(elem, value);
        assertEquals(elem.hashCode(), value.hashCode());
        assertEquals(elem.getFullDescription(), value.getFullDescription());

        // and same for methods
        ResolvedType getter = _findMethod(members, "get").getReturnType();
        _verifyStandalone(getter, expValueType, expParent);
        assertEquals(value, getter);

        ResolvedType setter = _findMethod(members, "set").getArgumentType(0);
        _verifyStandalone(setter, expValueType, expParent);
        assertEquals(value, setter);
    }

    private void _verifyStandalone(ResolvedType type, Class<?> expType, Class<?> expParent)
    {
        assertFalse(type instanceof ResolvedRecursiveType, "Should not be self-reference: "+type);
        assertEquals(expType, type.getErasedType());
        assertNotNull(type.getParentClass());
        assertEquals(expParent, type.getParentClass().getErasedType());
    }

    private static ResolvedField _findField(ResolvedTypeWithMembers members, String name)
    {
        for (ResolvedField f : members.getMemberFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        fail("No field '"+name+"' found");
        return null;
    }

    private static ResolvedMethod _findMethod(ResolvedTypeWithMembers members, String name)
    {
        for (ResolvedMethod m : members.getMemberMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        fail("No method '"+name+"' found");
        return null;
    }
}
