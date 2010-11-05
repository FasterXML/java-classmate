package com.fasterxml.classmate;

import java.util.Collections;
import java.util.List;

public class ResolvedArrayType  extends ResolvedClass
{
    protected final ResolvedType _elementType;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public ResolvedArrayType(Class<?> erased, TypeBindings bindings,
            ResolvedClass superclass, // must be java.lang.Object
            ResolvedType elementType)
    {
        super(erased, bindings);
        _elementType = elementType;
    }

    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */
    
    @Override
    public ResolvedType getParentClass() { return null; }
    
    @Override
    public List<ResolvedType> getImplementedInterfaces() {
        return Collections.emptyList();
    }
    
    /*
    /**********************************************************************
    /* Simple property accessors
    /**********************************************************************
     */

    @Override
    public boolean isConcrete() { return true; }

    @Override
    public ResolvedType getArrayElementType() { return _elementType; }

    @Override
    public boolean isArray() { return true; }

    @Override
    public boolean isPrimitive() { return false; }
}
