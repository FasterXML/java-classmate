package com.fasterxml.classmate.util;

import java.util.*;

import junit.framework.TestCase;

import com.fasterxml.classmate.*;
import com.fasterxml.classmate.types.ResolvedInterfaceType;

public class TestResolvedTypeCache extends TestCase
{
    public void testSimpleCaching()
    {
        ResolvedTypeCache cache = new ResolvedTypeCache(2);
        assertEquals(0, cache.size());
        // bogus, just needed for testing:
        ResolvedType type1 = new ResolvedInterfaceType(Map.class, null, null);
        cache.add(type1);
        assertEquals(1, cache.size());
        // re-adding won't change anything:
        cache.add(type1);
        assertEquals(1, cache.size());
        ResolvedType type2 = new ResolvedInterfaceType(Set.class, null, null);
        cache.add(type2);
        assertEquals(2, cache.size());
        ResolvedType type3 = new ResolvedInterfaceType(List.class, null, null);
        cache.add(type3);
        assertEquals(2, cache.size());

        // should now only have types 2 and 3 available
        ResolvedType found1 = cache.find(cache.key(Map.class));
        ResolvedType found2 = cache.find(cache.key(Set.class));
        ResolvedType found3 = cache.find(cache.key(List.class));
        assertNull(found1);
        assertSame(type2, found2);
        assertSame(type3, found3);
    }
}