package com.fasterxml.classmate.members;

import java.lang.reflect.Constructor;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

/**
 * Class that represents a constructor that has fully resolved generic
 * type information and annotation information.
 */
public final class ResolvedConstructor extends ResolvedParameterizedMember<Constructor<?>>
{
    public ResolvedConstructor(ResolvedType context, Annotations ann, Constructor<?> constructor,
            ResolvedType[] argumentTypes)
    {
        super(context, ann, constructor, null, argumentTypes);
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

}
