package com.fasterxml.classmate.members;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.util.MethodKey;

public final class RawMethod extends RawMember
{
    protected final Method _method;

    protected final int _hashCode;
    
    public RawMethod(ResolvedType context, Method method)
    {
        super(context);
        _method = method;
        _hashCode = (_method == null ? 0 : _method.hashCode());
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    @Override
    public Method getRawMember() {
        return _method;
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    public boolean isStrict() {
        return Modifier.isStrict(getModifiers());
    }

    public boolean isNative() {
        return Modifier.isNative(getModifiers());
    }

    public boolean isSynchronized() {
        return Modifier.isSynchronized(getModifiers());
    }

    public MethodKey createKey()
    {
        String name = _method.getName();
        Class<?>[] argTypes = _method.getParameterTypes(); // return of Method#getParameterTypes will never be null
        return new MethodKey(name, argTypes);
    }
    
    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    @Override public int hashCode()
    {
        return _hashCode;
    }

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        RawMethod other = (RawMethod) o;
        return (other._method == _method);
    }
}
