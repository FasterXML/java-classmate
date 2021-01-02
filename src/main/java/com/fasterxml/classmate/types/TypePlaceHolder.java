package com.fasterxml.classmate.types;

import java.util.*;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;

/**
 * Placeholder used for resolving type assignments to figure out
 * type parameters for subtypes.
 */
public class TypePlaceHolder extends ResolvedType
{
    protected final int _ordinal;

    /**
     * Type assigned during wildcard resolution (which follows type
     * structure resolution)
     */
    protected ResolvedType _actualType;
    
    public TypePlaceHolder(int ordinal)
    {
        super(Object.class, TypeBindings.emptyBindings());
        _ordinal = ordinal;
    }

    @Override
    public boolean canCreateSubtypes() { return false; }

    public ResolvedType actualType() { return _actualType; }
    public void actualType(ResolvedType t) { _actualType = t; }
    
    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */
    
    @Override
    public ResolvedType getParentClass() { return null; }

    @Override
    public ResolvedType getSelfReferencedType() { return null; }
    
    @Override
    public List<ResolvedType> getImplementedInterfaces() { return Collections.<ResolvedType>emptyList(); }
    
    @Override
    public ResolvedType getArrayElementType() { return null; }

    /*
    /**********************************************************************
    /* Simple property accessors
    /**********************************************************************
     */

    @Override
    public boolean isInterface() { return false; }

    @Override
    public boolean isAbstract() { return true; }

    @Override
    public boolean isArray() { return false; }

    @Override
    public boolean isPrimitive() { return false; }

    /*
    /**********************************************************************
    /* String representations
    /**********************************************************************
     */

    @Override
    public StringBuilder appendSignature(StringBuilder sb) {
        return _appendClassSignature(sb);
    }

    @Override
    public StringBuilder appendErasedSignature(StringBuilder sb) {
        return _appendErasedClassSignature(sb);
    }

    @Override
    public StringBuilder appendBriefDescription(StringBuilder sb) {
        sb.append('<').append(_ordinal).append('>');
        return sb;
    }

    @Override
    public StringBuilder appendFullDescription(StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    /*
    /**********************************************************************
    /* Other overrides
    /**********************************************************************
     */

    // Important: make sure to avoid matches for cache lookups; one way is
    // to handle it here; not the only one.
    @Override public boolean equals(Object o) { // since 1.3.1
        // should these ever match actually?
        return (o == this);
    }

    // Only for compliance purposes: lgtm.com complains if only equals overridden
    @Override public int hashCode() {
        return _ordinal;
    }
}
