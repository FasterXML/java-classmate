package com.fasterxml.classmate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.classmate.BaseTest;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

// for [classmate#53]: Raw Comparator
public class TestTypeResolver53 extends BaseTest
{
    @SuppressWarnings("rawtypes")
    static abstract class Comparator53 implements Comparator { }

    protected final TypeResolver RESOLVER = new TypeResolver();

    // [classmate#53] Problem with Raw types
    public void testResolvingRawType() {
        ResolvedType rt = RESOLVER.resolve(Comparator53.class);
        List<ResolvedType> params = rt.typeParametersFor(Comparator.class);
        assertEquals(Arrays.asList(RESOLVER.resolve(Object.class)),
                params);
    }
}
