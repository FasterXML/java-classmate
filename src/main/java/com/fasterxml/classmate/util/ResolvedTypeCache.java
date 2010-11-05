package com.fasterxml.classmate.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.classmate.ResolvedType;

/**
 * Simple LRU cache used for storing up to specified number of most recently accessed
 * {@link ResolvedType} instances.
 * Since usage pattern is such that caller needs synchronization, cache access methods
 * are fully synchronized so that caller need not do explicit synchronization.
 */
@SuppressWarnings("serial")
public class ResolvedTypeCache
    extends LinkedHashMap<ResolvedTypeCache.Key, ResolvedType>
{
    protected final int _maxEntries;
    
    public ResolvedTypeCache(int maxEntries)
    {
        _maxEntries = maxEntries;
    }

    public synchronized ResolvedType find(Class<?> simpleType) {
        return get(new Key(simpleType));
    }

    public synchronized ResolvedType find(Class<?> erasedType, List<ResolvedType> tp) {
        return get(new Key(erasedType, tp));
    }

    @Override
    public synchronized int size() {
        return super.size();
    }
    
    public synchronized void add(ResolvedType type)
    {
        Key key = new Key(type.getErasedType(), type.getTypeParameters());
        put(key, type);
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<Key, ResolvedType> eldest) {
        return size() > _maxEntries;
    }
    
    /**
     * Key used for type entries.
     */
    public static class Key
    {
        private final Class<?> _erasedType;
        
        private final List<ResolvedType> _typeParameters;
        
        private final int _hashCode;
        
        public Key(Class<?> simpleType) {
            this(simpleType, null);
        }
        
        public Key(Class<?> erasedType, List<ResolvedType> tp) {
            if (tp != null && tp.isEmpty()) {
                tp = null;
            }
            _erasedType = erasedType;
            _typeParameters = tp;
            int h = erasedType.getName().hashCode();
            if (tp != null) {
                h += tp.size();
            }
            _hashCode = h;
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
            List<ResolvedType> otherTP = other._typeParameters;
            if (_typeParameters == null) {
                return (otherTP == null);
            }
            if (otherTP == null || otherTP.size() != _typeParameters.size()) {
                return false;
            }
            for (int i = 0, len = _typeParameters.size(); i < len; ++i) {
                if (!_typeParameters.get(i).equals(otherTP.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

}
