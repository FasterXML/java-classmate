package com.fasterxml.classmate.members;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

public final class ResolvedMethod extends ResolvedParameterizedMember<Method>
     implements Comparable<ResolvedMethod>
{
    public ResolvedMethod(ResolvedType context, Annotations ann, Method method,
            ResolvedType returnType, ResolvedType[] argumentTypes)
    {
        super(context, ann, method, returnType, argumentTypes);
    }

    /*
    /**********************************************************************
    /* Simple accessors from base class
    /**********************************************************************
     */

    public boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    public boolean isStrict() {
        return Modifier.isStrict(getModifiers());
    }

    public boolean isNative() {
        return Modifier.isNative(getModifiers());
    }

    public boolean isSynchronized() {
        return Modifier.isSynchronized(getModifiers());
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    public ResolvedType getReturnType() { return getType(); }

    /*
    /**********************************************************************
    /* Standard method override
    /**********************************************************************
     */

    @Override
    public int compareTo(ResolvedMethod other)
    {
         // primary sort by name (alphabetic); secondary by arg count (ascending)
         int diff = getName().compareTo(other.getName());
         if (diff == 0) {
              // subtract fine, no fear of overflow here
              diff = getArgumentCount() - other.getArgumentCount();
         }
         return diff;
    }
}
