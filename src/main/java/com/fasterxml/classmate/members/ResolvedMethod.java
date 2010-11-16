package com.fasterxml.classmate.members;

import java.lang.reflect.Method;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

public class ResolvedMethod extends ResolvedMember
{
    protected final Method _method;

    protected final ResolvedType _returnType;

    protected final ResolvedType[] _argumentTypes;
    
    public ResolvedMethod(ResolvedType context, Annotations ann, Method method,
            ResolvedType returnType, ResolvedType[] argumentTypes)
    {
        super(context, ann);
        _method = method;
        _returnType = returnType;
        _argumentTypes = argumentTypes;
    }
    
    /*
    /**********************************************************************
    /* Simple accessors from base class
    /**********************************************************************
     */

    @Override
    public Method getRawMember() {
        return _method;
    }

    @Override
    public ResolvedType getType() { return _returnType; }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    public ResolvedType getReturnType() { return _returnType; }

    /**
     * Returns number of arguments method takes.
     */
    public int getArgumentCount() {
        return _argumentTypes.length;
    }
    
    public ResolvedType getArgumentType(int index)
    {
        if (index < 0 || index >= _argumentTypes.length) {
            return null;
        }
        return _argumentTypes[index];
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
