package com.fasterxml.classmate.members;

import java.lang.reflect.Constructor;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

/**
 * Class that represents a constructor that has fully resolved generic
 * type information and annotation information.
 */
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

    @Override
    public Constructor<?> getRawMember() {
        return _constructor;
    }

    @Override
    public ResolvedType getType() { return null; }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

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
