package com.fasterxml.classmate;

import com.fasterxml.classmate.members.ResolvedMethod;

/**
 * Unit tests to check issue #4, problems with local generic self-referential
 * generic types.
 */
public class TestSelfRefMemberTypes extends BaseTest
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    static class MyComparable {
        public <T extends Comparable<T>> T foobar() { return null; }
    }
    
    static class ComplexSelfType<T, V extends ComplexSelfType<T, V>> { }

    static class ClassUsingComplexSelfType {
      public <T, V extends ComplexSelfType<T, V>> V complexMap(V input) {
        return null;
      }
    }
    
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
    /* Unit tests
    /**********************************************************************
     */

    public void testSelfReferencesSimple()
    {
        TypeResolver typeResolver = new TypeResolver();
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        ResolvedType type = typeResolver.resolve(MyComparable.class);
        ResolvedMethod[] resolvedMethods = memberResolver.resolve(type, null, null).getMemberMethods();

        assertEquals(1, resolvedMethods.length);
        assertEquals(Comparable.class, resolvedMethods[0].getReturnType().getErasedType());
    }

    public void testSelfReferencesComplex()
    {
          TypeResolver typeResolver = new TypeResolver();
          MemberResolver memberResolver = new MemberResolver(typeResolver);

          ResolvedType t = typeResolver.resolve(ClassUsingComplexSelfType.class);
          ResolvedMethod[] resolvedMethods = memberResolver.resolve(t, null, null).getMemberMethods();
          assertEquals(1, resolvedMethods.length);
          ResolvedMethod m = resolvedMethods[0];
          assertEquals("complexMap", m.getName());

          assertEquals(1, m.getArgumentCount());
          ResolvedType argType = m.getArgumentType(0);

          ResolvedType returnType = m.getReturnType();

          // All right... hmmh. Actually, due to lack of bindings, they are just Objects
          assertEquals(ComplexSelfType.class, argType.getErasedType());
          assertEquals(ComplexSelfType.class, returnType.getErasedType());
      }
}
