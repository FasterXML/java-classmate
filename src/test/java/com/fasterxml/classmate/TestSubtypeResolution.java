package com.fasterxml.classmate;

import java.util.*;

public class TestSubtypeResolution extends BaseTest
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

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

    public void testValidSubtype()
    {
        // First, make a concrete type that extends specified generic interface:
        ResolvedType supertype = typeResolver.resolve(Map.class, String.class, Long.class);
        ResolvedType subtype = typeResolver.resolveSubtype(supertype, HashMap.class);
        assertSame(HashMap.class, subtype.getErasedType());

        // hmmh. Whether we can resolve type bindings is an open question..

        TypeBindings bindings = subtype.getTypeBindings();
        assertEquals(2, bindings.size());
        assertSame(String.class, bindings.getBoundType(0).getErasedType());
        assertSame(Long.class, bindings.getBoundType(1).getErasedType());
    }

    public void testInvalidSubtype()
    {
        ResolvedType supertype = typeResolver.resolve(List.class, Integer.class);
        try {
            typeResolver.resolveSubtype(supertype, HashMap.class);
            fail("Expected failure");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Can not sub-class java.util.List");
        }
    }

}
