package com.fasterxml.classmate.util;

import java.io.Serializable;

/**
 * Helper class needed when storing methods in maps.
 * Immutable.
 */
@SuppressWarnings("serial")
public class MethodKey implements Serializable
{
    private static final Class<?>[] NO_CLASSES = new Class[0];

    private final String _name;
    
    private final Class<?>[] _argumentTypes;

    private final int _hashCode;
    
    public MethodKey(String name)
    {
        _name = name;
        _argumentTypes = NO_CLASSES;
        _hashCode = name.hashCode();
    }

    public MethodKey(String name, Class<?>[] argTypes)
    {
        _name = name;
        _argumentTypes = argTypes;
        _hashCode = name.hashCode() + argTypes.length;
    }
    
    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    /**
     * Equality means name is the same and argument type erasures as well.
     */
    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        MethodKey other = (MethodKey) o;
        Class<?>[] otherArgs = other._argumentTypes;
        int len = _argumentTypes.length;
        if (otherArgs.length != len) return false;
        for (int i = 0; i < len; ++i) {
            if (otherArgs[i] != _argumentTypes[i]) return false;
        }
        return _name.equals(other._name);
    }    

    @Override public int hashCode() { return _hashCode; }

    @Override public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(_name);
        sb.append('(');
        for (int i = 0, len = _argumentTypes.length; i < len; ++i) {
            if (i > 0) sb.append(',');
            sb.append(_argumentTypes[i].getName());
        }
        sb.append(')');
        return sb.toString();
    }
}
