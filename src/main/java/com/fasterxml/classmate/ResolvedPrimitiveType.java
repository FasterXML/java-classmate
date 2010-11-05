package com.fasterxml.classmate;

import java.util.Collections;
import java.util.List;

/**
 * Type used for Java primitive types (which does not include arrays here).
 *<p>
 * Since set of primitive types is bounded, constructor is defined as protected,
 * and class final; that is, new primitive types are not to be constructed
 * by calling applications.
 */
public final class ResolvedPrimitiveType extends ResolvedClass
{
    /**
     * Primitive types have single-character Signature, easy and efficient
     * to just store here
     */
    protected final String _signature;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    protected ResolvedPrimitiveType(Class<?> erased, char sig)
    {
        super(erased, TypeBindings.emptyBindings());
        _signature = String.valueOf(sig);
    }

    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */
    
    @Override
    public ResolvedType getParentClass() { return null; }
    
    /*
    /**********************************************************************
    /* Simple property accessors
    /**********************************************************************
     */

    @Override
    public boolean isConcrete() { return true; }

    @Override
    public ResolvedType getArrayElementType() { return null; }

    @Override
    public boolean isArray() { return false; }

    @Override
    public boolean isPrimitive() { return true; }

    @Override
    public List<ResolvedType> getImplementedInterfaces() {
        return Collections.emptyList();
    }

}
