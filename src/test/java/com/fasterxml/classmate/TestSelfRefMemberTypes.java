package com.fasterxml.classmate;

import java.util.Arrays;

import com.fasterxml.classmate.members.ResolvedField;
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

    // From Jackson-databind, [Issue#543]
    static abstract class Animal { }

    static class ContainerWithField<T extends Animal> {
         public T animal;

         public ContainerWithField(T a) { animal = a; }
     }
    
    static class ContainerWithTwoAnimals<U extends Animal,V extends Animal> extends ContainerWithField<U>
    {
         public V animal2;
    
         public ContainerWithTwoAnimals(U a1, V a2) {
              super(a1);
              animal2 = a2;
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

    public void testSelfReferencesVaryingDimensions()
    {
          TypeResolver typeResolver = new TypeResolver();
          MemberResolver memberResolver = new MemberResolver(typeResolver);

          ResolvedType t = typeResolver.resolve(ContainerWithTwoAnimals.class);
          ResolvedField[] fields = memberResolver.resolve(t, null, null).getMemberFields();
          assertEquals(2, fields.length);
          Arrays.sort(fields);

          ResolvedField m = fields[0];
          assertEquals("animal", m.getName());
          assertEquals(Animal.class, m.getType().getErasedType());

          m = fields[1];
          assertEquals("animal2", m.getName());
          assertEquals(Animal.class, m.getType().getErasedType());

          // anything else worth asserting?
    }
}

