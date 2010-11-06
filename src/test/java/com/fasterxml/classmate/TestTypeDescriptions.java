package com.fasterxml.classmate;

import java.util.*;

/**
 * Unit tests for verifying that resolved types give expected string description
 * and signatures.
 */
public class TestTypeDescriptions extends BaseTest
{
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
        ResolvedType objectType = typeResolver.resolve(Object.class);
        assertEquals("java.lang.Object", objectType.getDescription());
        assertEquals("Ljava/lang/Object;", objectType.getErasedSignature());
        assertEquals("Ljava/lang/Object;", objectType.getSignature());
    }

    public void testPrimitiveTypes()
    {
        // let's check some default java types
        ResolvedType intType = typeResolver.resolve(Integer.TYPE);
        assertEquals("I", intType.getSignature());
        assertEquals("I", intType.getErasedSignature());
        assertEquals("int", intType.getDescription());

        ResolvedType boolType = typeResolver.resolve(Boolean.TYPE);
        assertEquals("Z", boolType.getSignature());
        assertEquals("Z", boolType.getErasedSignature());
        assertEquals("boolean", boolType.getDescription());
    }

    public void testGenericTypes()
    {
        ResolvedType mapType = typeResolver.resolve(new GenericType<Map<Long,Boolean>>() { });
        assertEquals("Ljava/util/Map;", mapType.getErasedSignature());
        assertEquals("Ljava/util/Map<Ljava/lang/Long;Ljava/lang/Boolean;>;", mapType.getSignature());
        assertEquals("java.util.Map<java.lang.Long,java.lang.Boolean>", mapType.getDescription());
    }
}
