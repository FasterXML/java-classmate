package com.fasterxml.classmate.members;

import java.lang.reflect.Method;

import com.fasterxml.classmate.ResolvedType;

public class ResolvedMethod extends ResolvedMember
{
    protected final Method _method;

    public ResolvedMethod(ResolvedType context, Method method)
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

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        RawMethod other = (RawMethod) o;
        return (other._method == _method);
    }

}
