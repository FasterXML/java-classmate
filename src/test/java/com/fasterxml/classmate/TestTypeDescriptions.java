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

    @Override
    protected void setUp()
    {
        // Let's use a single instance for all tests, to increase chance of seeing failures
        typeResolver = new TypeResolver();
    }

    public void testSimpleTypes()
    {
        // let's check some default java types
        ResolvedType objectType = typeResolver.resolve(Object.class);
        assertEquals("java.lang.Object", objectType.getFullDescription());
        assertEquals("Ljava/lang/Object;", objectType.getErasedSignature());
        assertEquals("Ljava/lang/Object;", objectType.getSignature());

        ResolvedType stringType = typeResolver.resolve(String.class);
        // Interesting thing with "simple" type like java.lang.String is that
        // it has recursive type self-reference (via Comparable<T>)

        final String stringDesc = stringType.getFullDescription();

        // 10-Oct-2023, tatu: With JDK 17, get even more stuff... Start with pre-17 desc:
        if (stringDesc.equals("java.lang.String extends java.lang.Object"
                +" implements java.io.Serializable,java.lang.Comparable<java.lang.String>,java.lang.CharSequence"
                )) {
            ;
            // But then allow JDK 17 variant
        } else if (stringDesc.equals("java.lang.String extends java.lang.Object"
                    +" implements java.io.Serializable,java.lang.Comparable<java.lang.String>,java.lang.CharSequence"
                    +",java.lang.constant.Constable,java.lang.constant.ConstantDesc"
                    )) {
        } else {
            fail("Full String description not matching one of expected signatures: "+stringDesc);
        }
        assertEquals("Ljava/lang/String;", stringType.getErasedSignature());
        assertEquals("Ljava/lang/String;", stringType.getSignature());
    }

    public void testPrimitiveTypes()
    {
        // let's check some default java types
        ResolvedType intType = typeResolver.resolve(Integer.TYPE);
        assertEquals("I", intType.getSignature());
        assertEquals("I", intType.getErasedSignature());
        assertEquals("int", intType.getFullDescription());

        ResolvedType boolType = typeResolver.resolve(Boolean.TYPE);
        assertEquals("Z", boolType.getSignature());
        assertEquals("Z", boolType.getErasedSignature());
        assertEquals("boolean", boolType.getFullDescription());
    }

    public void testGenericTypes()
    {
        ResolvedType mapType = typeResolver.resolve(new GenericType<Map<Long,Boolean>>() { });
        assertEquals("Ljava/util/Map;", mapType.getErasedSignature());
        assertEquals("Ljava/util/Map<Ljava/lang/Long;Ljava/lang/Boolean;>;", mapType.getSignature());
        assertEquals("java.util.Map<java.lang.Long,java.lang.Boolean>", mapType.getFullDescription());
    }
}
