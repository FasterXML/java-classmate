package com.fasterxml.classmate.types;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;

public final class ResolvedAbstractClass extends ResolvedClass
{
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public ResolvedAbstractClass(Class<?> erased, TypeBindings bindings,
            ResolvedClass superClass, ResolvedType[] interfaces)
    {
        super(erased, bindings, superClass, interfaces);
    }
    
    /*
    /**********************************************************************
    /* Simple property accessors
    /**********************************************************************
     */

    @Override
    public boolean isConcrete() { return false; }
}
