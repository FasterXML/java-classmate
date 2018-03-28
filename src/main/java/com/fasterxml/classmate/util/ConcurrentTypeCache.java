package com.fasterxml.classmate.util;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.classmate.ResolvedType;

/**
 * Alternative {@link ResolvedTypeCache} implementation that uses
 * {@link ConcurrentHashMap} for efficient concurrent access and limits maximum
 * entry count to specified maximum. But instead of trying to optimize retention
 * by access (as {@link LRUTypeCache} does, will simply clear (remove all entries)
 * if maximum size is reached. This works well as long as maximum size is large enough
 * to cover most commonly resolved types, and works well for higher concurrency use
 * cases.
 * 
 * @see LRUTypeCache
 */
public class ConcurrentTypeCache
    extends ResolvedTypeCache
{
    private static final long serialVersionUID = 1L;

    protected final int _maxEntries;

    protected final transient ConcurrentHashMap<ResolvedTypeKey, ResolvedType> _map;

    public ConcurrentTypeCache(int maxEntries) {
        // We'll use concurrency level of 4, seems reasonable
        _map = new ConcurrentHashMap<ResolvedTypeKey, ResolvedType>(maxEntries,
                0.8f, 4);
        _maxEntries = maxEntries;
    }

    // For JDK serialization: have to re-construct backing Map since it is NOT serialized
    Object readResolve() {
        return new ConcurrentTypeCache(_maxEntries);
    }

    @Override
    public ResolvedType find(ResolvedTypeKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Null key not allowed");
        }
        return _map.get(key);
    }

    @Override
    public int size() {
        return _map.size();
    }
    
    @Override
    public void put(ResolvedTypeKey key, ResolvedType type) {
        if (key == null) {
            throw new IllegalArgumentException("Null key not allowed");
        }
        if (_map.size() >= _maxEntries) {
            // double-locking, yes, but safe here; trying to avoid "clear storms"
            // when multiple threads think they are to flush the cache
            synchronized (this) {
                if (_map.size() >= _maxEntries) {
                    _map.clear();
                }
            }
        }
        _map.put(key, type);
    }
}
