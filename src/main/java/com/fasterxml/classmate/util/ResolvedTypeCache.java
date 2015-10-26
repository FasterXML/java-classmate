package com.fasterxml.classmate.util;

import java.io.Serializable;
import java.util.*;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.TypePlaceHolder;

/**
 * Simple LRU cache used for storing up to specified number of most recently accessed
 * {@link ResolvedType} instances.
 * Since usage pattern is such that caller needs synchronization, cache access methods
 * are fully synchronized so that caller need not do explicit synchronization.
 */
@SuppressWarnings("serial")
public class ResolvedTypeCache implements Serializable
{
    protected final CacheMap _map;
    
    public ResolvedTypeCache(int maxEntries) {
        _map = new CacheMap(maxEntries);
    }

    /**
     * Helper method for constructing reusable cache keys
     */
    public Key key(Class<?> simpleType) {
        return new Key(simpleType);
    }

    /**
     * Helper method for constructing reusable cache keys
     */
    public Key key(Class<?> simpleType, ResolvedType[] tp) {
        int len = (tp == null) ? 0 : tp.length;
        if (len == 0) {
            return new Key(simpleType);
        }
        // 25-Oct-2015, tatu: Need to prevent caching of anything with TypePlaceHolder;
        //   can cause problems otherwise as those are ephemeral/mutable containers
        for (int i = 0; i < len; ++i) {
            if (tp[i] instanceof TypePlaceHolder) {
                return null;
            }
        }
        return new Key(simpleType, tp);
    }
    
    public synchronized ResolvedType find(Key key) {
        if (key == null) {
            throw new IllegalArgumentException("Null key not allowed");
        }
        return _map.get(key);
    }

    public synchronized int size() {
        return _map.size();
    }
    
    public synchronized void put(Key key, ResolvedType type) {
        if (key == null) {
            throw new IllegalArgumentException("Null key not allowed");
        }
        _map.put(key, type);
    }

    /*
    /**********************************************************************
    /* Methods for unit tests
    /**********************************************************************
     */

    /**
     * Method only used by test code: do not use otherwise.
     */
    protected void addForTest(ResolvedType type)
    {
        List<ResolvedType> tp = type.getTypeParameters();
        ResolvedType[] tpa = tp.toArray(new ResolvedType[tp.size()]);
        put(key(type.getErasedType(), tpa), type);
    }
    
    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */
    
    /**
     * Key used for type entries.
     */
    public static class Key
    {
        private final Class<?> _erasedType;
        private final ResolvedType[] _typeParameters;
        private final int _hashCode;

        public Key(Class<?> simpleType) {
            this(simpleType, null);
        }
        
        public Key(Class<?> erasedType, ResolvedType[] tp)
        {
            // let's not hold on type empty arrays
            if (tp != null && tp.length == 0) {
                tp = null;
            }
            _erasedType = erasedType;
            _typeParameters = tp;
            int h = erasedType.getName().hashCode();
            if (tp != null) {
                h += tp.length;
            }
            _hashCode = h;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[CacheKey: ");
            sb.append(_erasedType.getName())
                .append('(');
            if (_typeParameters != null) {
                for (int i = 0; i < _typeParameters.length; ++i) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(_typeParameters[i]);
                }
            }
            sb.append(")]");
            return sb.toString();
        }
        
        @Override
        public int hashCode() { return _hashCode; }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) return true;
            if (o == null || o.getClass() != getClass()) return false;
            Key other = (Key) o;
            if (other._erasedType != _erasedType) return false;
            ResolvedType[] otherTP = other._typeParameters;
            if (_typeParameters == null) {
                return (otherTP == null);
            }
            if (otherTP == null || otherTP.length != _typeParameters.length) {
                return false;
            }
            for (int i = 0, len = _typeParameters.length; i < len; ++i) {
                if (!_typeParameters[i].equals(otherTP[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Simple sub-class to get LRU cache
     */
    private final static class CacheMap
        extends LinkedHashMap<ResolvedTypeCache.Key, ResolvedType>
    {
        protected final int _maxEntries;
        
        public CacheMap(int maxEntries) {
            _maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, ResolvedType> eldest) {
            return size() > _maxEntries;
        }
    }
}
