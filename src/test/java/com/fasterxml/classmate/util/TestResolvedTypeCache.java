package com.fasterxml.classmate.util;

import java.util.*;

import junit.framework.TestCase;

import com.fasterxml.classmate.*;

public class TestResolvedTypeCache extends TestCase
{
    public void testSimpleTypesBasic()
    {
        ResolvedTypeCache cache = new ResolvedTypeCache(2);
        assertEquals(0, cache.size());
        // bogus, just needed for testing:
        ResolvedType type1 = new ResolvedInterface(Map.class, null, null);
        cache.add(type1);
        assertEquals(1, cache.size());
        // re-adding won't change anything:
        cache.add(type1);
        assertEquals(1, cache.size());
        ResolvedType type2 = new ResolvedInterface(Set.class, null, null);
        cache.add(type2);
        assertEquals(2, cache.size());
        ResolvedType type3 = new ResolvedInterface(List.class, null, null);
        cache.add(type3);
        assertEquals(2, cache.size());
        // should now have types 2 and 3 available

        ResolvedType found1 = cache.find(cache.key(Map.class));
        ResolvedType found2 = cache.find(cache.key(Set.class));
        ResolvedType found3 = cache.find(cache.key(List.class));
        // should now have types 2 and 3 available
        assertNull(found1);
        assertSame(type2, found2);
        assertSame(type3, found3);
    }
}