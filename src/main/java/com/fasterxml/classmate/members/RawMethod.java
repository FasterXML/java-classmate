package com.fasterxml.classmate.members;

import java.lang.reflect.Method;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.util.MethodKey;

public final class RawMethod extends RawMember
{
    protected final Method _method;
    
    public RawMethod(ResolvedType context, Method method)
    {
        super(context);
        _method = method;
    }

    
    
    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public Method getRawMember() {
        return _method;
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

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        RawMethod other = (RawMethod) o;
        return (other._method == _method);
    }    
}
