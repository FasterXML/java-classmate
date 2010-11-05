package com.fasterxml.classmate;

import junit.framework.TestCase;

public class TestTypeResolver extends TestCase
{

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    protected TypeResolver typeResolver;
    
    protected void setUp()
    {
        // Let's use a single instance for all tests, to increase chance of seeing failures
        typeResolver = new TypeResolver();
    }

    public void testSimpleTypes()
    {
        // let's check some default java types
        ResolvedType objectType = _testSimpleConcrete(Object.class);
        ResolvedType stringType = _testSimpleConcrete(String.class);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private ResolvedType _testSimpleConcrete(Class<?> cls)
    {
        ResolvedType type = typeResolver.resolve(cls);
        assertTrue(type instanceof ResolvedConcreteClass);
        assertSame(cls, type.getErasedType());
        assertFalse(type.isAbstract());
        assertFalse(type.isArray());
        assertFalse(type.isInterface());
        assertTrue(type.isConcrete());
        assertFalse(type.isPrimitive());
        return type;
    }
}
