package com.fasterxml.classmate.members;

import java.lang.reflect.Constructor;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.util.MethodKey;

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

    /**
     * Although constructors are different from other methods, we can use
     * {@link MethodKey} easily.
     */
    public MethodKey createKey()
    {
        String name = _constructor.getName();
        Class<?>[] argTypes = _constructor.getParameterTypes();
        if (argTypes == null) {
            return new MethodKey(name);
        }
        return new MethodKey(name, argTypes);
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
