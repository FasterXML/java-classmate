package com.fasterxml.classmate.util;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.TypePlaceHolder;

/**
 * Simple LRU cache used for storing up to specified number of most recently accessed
 * {@link ResolvedType} instances.
 * Since usage pattern is such that caller needs synchronization, cache access methods
 * are fully synchronized so that caller need not do explicit synchronization.
 */
@SuppressWarnings("serial")
public abstract class ResolvedTypeCache implements Serializable
{
    /**
     * @since 1.4
     */
    public static ResolvedTypeCache lruCache(int maxEntries) {
        return new LRUTypeCache(maxEntries);
    }

    /**
     * @since 1.4
     */
    public static ResolvedTypeCache concurrentCache(int maxEntries) {
        return new ConcurrentTypeCache(maxEntries);
    }

    /**
     * Helper method for constructing reusable cache keys
     */
    public ResolvedTypeKey key(Class<?> simpleType) {
        return new ResolvedTypeKey(simpleType);
    }

    /**
     * Helper method for constructing reusable cache keys
     */
    public ResolvedTypeKey key(Class<?> simpleType, ResolvedType[] tp) {
        int len = (tp == null) ? 0 : tp.length;
        if (len == 0) {
            return new ResolvedTypeKey(simpleType);
        }
        // 25-Oct-2015, tatu: Need to prevent caching of anything with TypePlaceHolder;
        //   can cause problems otherwise as those are ephemeral/mutable containers
        for (int i = 0; i < len; ++i) {
            if (tp[i] instanceof TypePlaceHolder) {
                return null;
            }
        }
        return new ResolvedTypeKey(simpleType, tp);
    }

    public abstract ResolvedType find(ResolvedTypeKey key);

    public abstract int size();

    public abstract void put(ResolvedTypeKey key, ResolvedType type);

    // // // Methods for unit tests

    /**
     * Method only used by test code: do not use otherwise.
     */
    protected void _addForTest(ResolvedType type)
    {
        List<ResolvedType> tp = type.getTypeParameters();
        ResolvedType[] tpa = tp.toArray(new ResolvedType[tp.size()]);
        put(key(type.getErasedType(), tpa), type);
    }
}
