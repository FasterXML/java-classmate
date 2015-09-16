package com.fasterxml.classmate.members;

import java.util.List;

import junit.framework.TestCase;

import com.fasterxml.classmate.*;

// for issue #28, "ghost" type parameter:
public class Issue28Test
    extends TestCase
{
    class A<T extends Number> {
        public void foo(T t) {
        }
    }

    public void testIssue28()
    {
        TypeResolver resolver = new TypeResolver();
        ResolvedType resolvedType = resolver.resolve(A.class);
        MemberResolver memberResolver = new MemberResolver(resolver);
        ResolvedTypeWithMembers resolvedTypeWithMembers = memberResolver.resolve(resolvedType, null, null);

        ResolvedMethod[] members = resolvedTypeWithMembers.getMemberMethods();
        assertEquals(1, members.length);

        ResolvedMethod fooMethod = members[0];
        assertEquals("foo", fooMethod.getName());
        assertEquals(1, fooMethod.getArgumentCount());
        ResolvedType arg = fooMethod.getArgumentType(0);

        assertEquals(Number.class, arg.getErasedType());
        List<ResolvedType> tps = arg.getTypeParameters();
        if (tps.size() != 0) {
            fail("Expected 0 type parameters, got "+tps.size()+": "+tps);
        }
    }
}
