package com.fasterxml.classmate.types;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.classmate.BaseTest;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

public class ResolvedArrayType51Test extends BaseTest
{
    protected final TypeResolver RESOLVER = new TypeResolver();

    // [classmate#51]: parent type for Array classes
    @Test
    public void testResolvingRawType() {
        ResolvedType rt = RESOLVER.resolve(Long[].class);
        ResolvedType parent = rt.getParentClass();
        assertNotNull(parent);
        assertEquals(Object.class, parent.getErasedType());
    }
}
