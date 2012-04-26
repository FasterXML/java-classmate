package com.fasterxml.classmate.members;

import java.lang.reflect.Constructor;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.util.MethodKey;

public final class RawConstructor extends RawMember
{
    protected final Constructor<?> _constructor;

    protected final int _hashCode;

    public RawConstructor(ResolvedType context, Constructor<?> constructor)
    {
        super(context);
        _constructor = constructor;
        _hashCode = (_constructor == null ? 0 : _constructor.hashCode());
    }

    /**
     * Although constructors are different from other methods, we can use
     * {@link MethodKey} easily.
     */
    public MethodKey createKey()
    {
        String name = "<init>"; // do not use _constructor.getName() to allow for 'mix-ins'
        Class<?>[] argTypes = _constructor.getParameterTypes();  // return of Constructor#getParameterTypes will never be null
        return new MethodKey(name, argTypes);
    }
    
    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    @Override
    public Constructor<?> getRawMember() {
        return _constructor;
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
        RawConstructor other = (RawConstructor) o;
        return (other._constructor == _constructor);
    }
}
