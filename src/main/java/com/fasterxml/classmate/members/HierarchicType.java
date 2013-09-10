package com.fasterxml.classmate.members;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;

/**
 * Container class used to enclose information about a single {@link ResolvedType}
 * that is part of {@link ResolvedTypeWithMembers}.
 */
public final class HierarchicType
{
    /**
     * Whether this type instance represents a mix-in; if so, it can only result in
     * addition of annotations but not in addition of actual members.
     */
    protected final boolean _isMixin;

    protected final ResolvedType _type;
    
    /**
     * Relative priority of this type in hierarchy; higher priority members can override
     * lower priority members. Priority values are unique and are based on type index
     * (starting from 0), although they are not to be used for indexing.
     */
    protected final int _priority;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public HierarchicType(ResolvedType type, boolean mixin, int priority)
    {
        _type = type;
        _isMixin = mixin;
        _priority = priority;
    }
    
    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public ResolvedType getType() { return _type; }
    public Class<?> getErasedType() { return _type.getErasedType(); }
    public boolean isMixin() { return _isMixin; }
    public int getPriority() { return _priority; }
    
    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    @Override public String toString() { return _type.toString(); }
    @Override public int hashCode() { return _type.hashCode(); }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        HierarchicType other = (HierarchicType) o;
        return _type.equals(other._type);
    }
}
