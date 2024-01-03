package com.fasterxml.classmate.types;

import com.fasterxml.classmate.BaseTest;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

public class ResolvedArrayType51Test extends BaseTest
{
    protected final TypeResolver RESOLVER = new TypeResolver();

    // [classmate#51]: parent type for Array classes
    public void testResolvingRawType() {
        ResolvedType rt = RESOLVER.resolve(Long[].class);
        ResolvedType parent = rt.getParentClass();
        assertNotNull(parent);
        assertEquals(Object.class, parent.getErasedType());
    }
}
