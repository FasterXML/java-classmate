package com.fasterxml.classmate.members;

import java.lang.reflect.Method;

import com.fasterxml.classmate.ResolvedType;

public class RawMethod extends RawMember
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
    
    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    @Override public String toString() {
        return _method.getName();
    }

    @Override public int hashCode() {
        return _method.getName().hashCode();
    }

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        RawMethod other = (RawMethod) o;
        return (other._method == _method);
    }
    
}
