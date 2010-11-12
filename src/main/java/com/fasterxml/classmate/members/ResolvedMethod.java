package com.fasterxml.classmate.members;

import java.lang.reflect.Method;

import com.fasterxml.classmate.ResolvedType;

public class ResolvedMethod extends ResolvedMember
{
    protected final Method _method;

    protected final ResolvedType _returnType;
    
    public ResolvedMethod(ResolvedType context, Method method, ResolvedType returnType)
    {
        super(context);
        _method = method;
        _returnType = returnType;
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public Method getRawMember() {
        return _method;
    }

    public ResolvedType getType() { return _returnType; }
    
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
