package com.fasterxml.classmate;

import com.fasterxml.classmate.members.ResolvedField;
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
    }

    public static class Sym<T extends Sym<T>> extends Base<T> { }

    // Raw self-reference
    public static class RA<T extends RB> { }
    public static class RB extends Base<RA> { }

    private final TypeResolver _typeResolver = new TypeResolver();
    private final MemberResolver _memberResolver = new MemberResolver(_typeResolver);

    public void testGenericSelfReference()
    {
        _verify(Sym.class, Sym.class, Base.class);
    }

    public void testRawSelfReference()
    {
        _verify(RB.class, RA.class, Object.class);
    }

    private void _verify(Class<?> type, Class<?> expValueType, Class<?> expParent)
    {
        ResolvedTypeWithMembers members = _memberResolver.resolve(_typeResolver.resolve(type),
                null, null);
        ResolvedType value = _findField(members, "value").getType();
        ResolvedType arr = _findField(members, "arr").getType();

        assertFalse("Should not be self-reference: "+value, value instanceof ResolvedRecursiveType);
        assertEquals(expValueType, value.getErasedType());
        assertNotNull(value.getParentClass());
        assertEquals(expParent, value.getParentClass().getErasedType());

        ResolvedType elem = arr.getArrayElementType();
        assertFalse("Should not be self-reference: "+elem, elem instanceof ResolvedRecursiveType);
        assertEquals(elem, value);
        assertEquals(elem.hashCode(), value.hashCode());
        assertEquals(elem.getFullDescription(), value.getFullDescription());
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
}
