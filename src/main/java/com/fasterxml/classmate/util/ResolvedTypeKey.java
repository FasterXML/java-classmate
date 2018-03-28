package com.fasterxml.classmate.util;

import com.fasterxml.classmate.ResolvedType;

/**
 * Key used for entries cached in a {@link ResolvedTypeCache}.
 */
public class ResolvedTypeKey
{
    private final Class<?> _erasedType;
    private final ResolvedType[] _typeParameters;
    private final int _hashCode;

    public ResolvedTypeKey(Class<?> simpleType) {
        this(simpleType, null);
    }
    
    public ResolvedTypeKey(Class<?> erasedType, ResolvedType[] tp)
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
        ResolvedTypeKey other = (ResolvedTypeKey) o;
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