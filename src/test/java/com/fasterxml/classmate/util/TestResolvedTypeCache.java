package com.fasterxml.classmate.util;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedInterfaceType;
import com.fasterxml.classmate.types.ResolvedObjectType;

import junit.framework.TestCase;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestResolvedTypeCache extends TestCase
{
    private static class KeySubclass extends ResolvedTypeKey {
        private KeySubclass(Class<?> simpleType) {
            super(simpleType);
        }
    }

    public void testSimpleLRUCaching()
    {
        LRUTypeCache cache = (LRUTypeCache) ResolvedTypeCache.lruCache(2);
        _testSimple(cache, true);
    }

    public void testSimpleConcurrentCaching()
    {
        ConcurrentTypeCache cache = (ConcurrentTypeCache) ResolvedTypeCache.concurrentCache(2);
        _testSimple(cache, false);
    }

    private void _testSimple(ResolvedTypeCache cache, boolean lru) {
        assertEquals(0, cache.size());
        // bogus, just needed for testing:
        ResolvedType type1 = new ResolvedInterfaceType(Map.class, null, null);
        cache._addForTest(type1);
        assertEquals(1, cache.size());
        // re-adding won't change anything:
        cache._addForTest(type1);
        assertEquals(1, cache.size());
        ResolvedType type2 = new ResolvedInterfaceType(Set.class, null, null);
        cache._addForTest(type2);
        assertEquals(2, cache.size());
        ResolvedType type3 = new ResolvedInterfaceType(List.class, null, null);
        cache._addForTest(type3);

        // when full, behavior varies
        if (lru) {
            // will just replace oldest
            assertEquals(2, cache.size());
        } else {
            assertEquals(1, cache.size());
        }

        // should now only have types 2 and 3 available
        ResolvedType found1 = cache.find(cache.key(Map.class));
        ResolvedType found2 = cache.find(cache.key(Set.class));
        ResolvedType found3 = cache.find(cache.key(List.class));

        assertNull(found1);
        if (lru) {
            assertSame(type2, found2);
            assertSame(type3, found3);
        } else {
            assertNull(found2);
            assertSame(type3, found3);
        }
    }
    
    @SuppressWarnings("unused")
    public void testKeyEquals()
    {
        try {
            new ResolvedTypeKey(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedTypeKey key = new ResolvedTypeKey(String.class);

        // test referential equality
        assertTrue(key.equals(key));

        // test null
        assertFalse(key.equals(null));

        // test unequal class
        Object strKey = "test";
        assertFalse(key.equals(strKey));

        // test subclass
        assertFalse(key.equals(new KeySubclass(String.class)));

        // test unequal resolve-class
        ResolvedTypeKey key1 = new ResolvedTypeKey(Object.class);
        assertFalse(key.equals(key1));

        // test equal resolve-class
        ResolvedTypeKey key2 = new ResolvedTypeKey(String.class);
        assertTrue(key.equals(key2));

        // test equal, 0-length resolved-type array change to null
        ResolvedTypeKey key3 = new ResolvedTypeKey(String.class, new ResolvedType[] {  });
        assertTrue(key.equals(key3));

        // test unequal, null other type-parameters
        ResolvedTypeKey key4 = new ResolvedTypeKey(String.class, new ResolvedType[] { ResolvedObjectType.create(String.class, null, null, null)} );
        assertFalse(key.equals(key4));
        assertFalse(key4.equals(key));

        // test unequal, type-parameters length
        ResolvedTypeKey key5 = new ResolvedTypeKey(String.class, new ResolvedType[] {
                ResolvedObjectType.create(String.class, null, null, null),
                ResolvedObjectType.create(Object.class, null, null, null)
        });
        assertFalse(key4.equals(key5));

        // test unequal type-parameters
        ResolvedTypeKey key6 = new ResolvedTypeKey(String.class, new ResolvedType[] {
                ResolvedObjectType.create(Object.class, null, null, null),
                ResolvedObjectType.create(String.class, null, null, null)
        });
        assertFalse(key5.equals(key6));

        // test equal type-parameters
        ResolvedTypeKey key7 = new ResolvedTypeKey(String.class, new ResolvedType[] {
                ResolvedObjectType.create(Object.class, null, null, null),
                ResolvedObjectType.create(String.class, null, null, null)
        });
        assertTrue(key6.equals(key7));
    }
}
