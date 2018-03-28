package com.fasterxml.classmate.util;

import java.util.*;

import com.fasterxml.classmate.ResolvedType;

/**
 * Simple cache used for storing up to specified number of most recently accessed
 * {@link ResolvedType} instances. Uses "least-recently used" eviction algorithm
 * (via {@link LinkedHashMap} used internally) which optimized retention, but
 * requires full synchronization as read operation also has to modify internal state
 * to maintain LRU aspects.
 * This means that it works well in optimizing access patterns, by keeping most recently
 * accessed types in cache, but may not well work well for highly concurrent cases due
 * to synchronization overhead.
 *<p>
 * Like all {@link ResolvedTypeCache} implementations,
 * access is thread-safe and caller need not (and should not) use additional synchronization.
 *<p>
 *
 * @see ConcurrentTypeCache
 *
 * @since 1.4
 */
public class LRUTypeCache extends ResolvedTypeCache
{
    private static final long serialVersionUID = 1L;

    protected final int _maxEntries;

    protected final transient CacheMap _map;

    public LRUTypeCache(int maxEntries) {
        _map = new CacheMap(maxEntries);
        _maxEntries = maxEntries;
    }

    // For JDK serialization: have to re-construct backing Map since it is NOT serialized
    Object readResolve() {
        return new LRUTypeCache(_maxEntries);
    }

    @Override
    public synchronized ResolvedType find(ResolvedTypeKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Null key not allowed");
        }
        return _map.get(key);
    }

    @Override
    public synchronized int size() {
        return _map.size();
    }
    
    @Override
    public synchronized void put(ResolvedTypeKey key, ResolvedType type) {
        if (key == null) {
            throw new IllegalArgumentException("Null key not allowed");
        }
        _map.put(key, type);
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Simple sub-class to get LRU cache
     */
    @SuppressWarnings("serial")
    private final static class CacheMap
        extends LinkedHashMap<ResolvedTypeKey, ResolvedType>
    {
        protected final int _maxEntries;
        
        public CacheMap(int maxEntries) {
            _maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<ResolvedTypeKey, ResolvedType> eldest) {
            return size() > _maxEntries;
        }
    }
}
