package com.fasterxml.classmate.types;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;

/**
 * Intermediate base class for all non-interface types
 */
public abstract class ResolvedClass extends ResolvedType
{
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    public ResolvedClass(Class<?> erased, TypeBindings bindings)
    {
        super(erased, bindings);
    }

  
    /*
    /**********************************************************************
    /* Simple property accessors
    /**********************************************************************
     */
    
    @Override
    public boolean isInterface() { return false; }

    @Override
    public abstract boolean isConcrete();
}

