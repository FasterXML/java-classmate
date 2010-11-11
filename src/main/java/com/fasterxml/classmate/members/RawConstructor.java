package com.fasterxml.classmate.members;

import java.lang.reflect.Constructor;

import com.fasterxml.classmate.ResolvedType;

public final class RawConstructor extends RawMember
{
    protected final Constructor<?> _constructor;

    public RawConstructor(ResolvedType context, Constructor<?> constructor)
    {
        super(context);
        _constructor = constructor;
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public Constructor<?> getRawMember() {
        return _constructor;
    }
    
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
