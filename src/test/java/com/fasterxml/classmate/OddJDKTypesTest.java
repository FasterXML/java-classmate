package com.fasterxml.classmate;

import java.util.*;

public class OddJDKTypesTest extends BaseTest
{
    // Simple test, based on issues Jackson's type resolver had with inner
    // classes of JDK (Hash)Map
    public void testJDKMaps()
    {
        TypeResolver resolver = new TypeResolver();
        Map<Object,Object> map = new HashMap<Object,Object>();
        map.put("foo", "bar");

        // map itself is trivial
        ResolvedType type = resolver.resolve(map.getClass());
        assertEquals(HashMap.class, type.getErasedType());

        // but how about it's entry, key, value sets?
        type = resolver.resolve(map.entrySet().getClass());
        assertNotNull(type.getErasedType());
        assertTrue(type.isInstanceOf(Set.class));
        
        type = resolver.resolve(map.keySet().getClass());
        assertNotNull(type.getErasedType());
        assertTrue(type.isInstanceOf(Set.class));

        type = resolver.resolve(map.values().getClass());
        assertNotNull(type.getErasedType());
        assertTrue(type.isInstanceOf(Collection.class));
    }
}
