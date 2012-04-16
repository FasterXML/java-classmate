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

    private static class KeySubclass extends ResolvedTypeCache.Key {
        private KeySubclass(Class<?> simpleType) {
            super(simpleType);
        }
    }

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

    public void testKeyEquals() {

        try {
            new ResolvedTypeCache.Key(null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        ResolvedTypeCache.Key key = new ResolvedTypeCache.Key(String.class);

        // test referential equality
        assertTrue(key.equals(key));

        // test null
        assertFalse(key.equals(null));

        // test unequal class
        assertFalse(key.equals("test"));

        // test subclass
        assertFalse(key.equals(new KeySubclass(String.class)));

        // test unequal resolve-class
        ResolvedTypeCache.Key key1 = new ResolvedTypeCache.Key(Object.class);
        assertFalse(key.equals(key1));

        // test equal resolve-class
        ResolvedTypeCache.Key key2 = new ResolvedTypeCache.Key(String.class);
        assertTrue(key.equals(key2));

        // test equal, 0-length resolved-type array change to null
        ResolvedTypeCache.Key key3 = new ResolvedTypeCache.Key(String.class, new ResolvedType[] {  });
        assertTrue(key.equals(key3));

        // test unequal, null other type-parameters
        ResolvedTypeCache.Key key4 = new ResolvedTypeCache.Key(String.class, new ResolvedType[] { new ResolvedObjectType(String.class, null, null, (ResolvedType[]) null)} );
        assertFalse(key.equals(key4));
        assertFalse(key4.equals(key));

        // test unequal, type-parameters length
        ResolvedTypeCache.Key key5 = new ResolvedTypeCache.Key(String.class, new ResolvedType[] {
                new ResolvedObjectType(String.class, null, null, (ResolvedType[]) null),
                new ResolvedObjectType(Object.class, null, null, (ResolvedType[]) null)
        });
        assertFalse(key4.equals(key5));

        // test unequal type-parameters
        ResolvedTypeCache.Key key6 = new ResolvedTypeCache.Key(String.class, new ResolvedType[] {
                new ResolvedObjectType(Object.class, null, null, (ResolvedType[]) null),
                new ResolvedObjectType(String.class, null, null, (ResolvedType[]) null)
        });
        assertFalse(key5.equals(key6));

        // test equal type-parameters
        ResolvedTypeCache.Key key7 = new ResolvedTypeCache.Key(String.class, new ResolvedType[] {
                new ResolvedObjectType(Object.class, null, null, (ResolvedType[]) null),
                new ResolvedObjectType(String.class, null, null, (ResolvedType[]) null)
        });
        assertTrue(key6.equals(key7));

    }
}