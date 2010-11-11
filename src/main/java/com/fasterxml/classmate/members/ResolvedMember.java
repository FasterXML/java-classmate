package com.fasterxml.classmate.members;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.ResolvedType;

/**
 * Fully type-resolved equivalent of {@link RawMember}. Only members "that matter" (ones not
 * overridden, or filtered out) are resolved, since resolution process can add non-trivial
 * overhead.
 */
public abstract class ResolvedMember
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
    
    protected ResolvedMember(ResolvedType context)
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
