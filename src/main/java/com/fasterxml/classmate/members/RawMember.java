package com.fasterxml.classmate.members;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.ResolvedType;

/**
 * Base class for all "raw" member (field, method, constructor) types; raw means that
 * actual types are not yet resolved, but relationship to declaring type is
 * retained for eventual resolution.
 * Instances are typically created by {@link com.fasterxml.classmate.ResolvedType}
 * when requested, and form the input to eventual full flattening of type members.
 */
public abstract class RawMember
{
    /**
     * {@link ResolvedType} (class with generic type parameters) that declared
     * this member
     */
    protected final ResolvedType _declaringType;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    protected RawMember(ResolvedType context)
    {
        _declaringType = context;
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public final ResolvedType getDeclaringType() {
        return _declaringType;
    }
    
    public abstract Member getRawMember();

    public String getName() {
        return getRawMember().getName();
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }
    
    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }
    
    /*
    /**********************************************************************
    /* Package methods
    /**********************************************************************
     */

    protected final int getModifiers() { return getRawMember().getModifiers(); }
}
