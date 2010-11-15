package com.fasterxml.classmate.members;

import java.lang.reflect.Constructor;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

public final class ResolvedConstructor extends ResolvedMember
{
    protected final Constructor<?> _constructor;

    protected final ResolvedType[] _argumentTypes;
    
    public ResolvedConstructor(ResolvedType context, Annotations ann, Constructor<?> constructor,
            ResolvedType[] argumentTypes)            
    {
        super(context, ann);
        _constructor = constructor;
        _argumentTypes = argumentTypes;
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public Constructor<?> getRawMember() {
        return _constructor;
    }

    public ResolvedType getType() { return null; }
    
    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    @Override public int hashCode() {
        return _constructor.getName().hashCode();
    }

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        RawConstructor other = (RawConstructor) o;
        return (other._constructor == _constructor);
    }

}
