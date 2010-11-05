package com.fasterxml.classmate;

import java.util.*;

public class ResolvedConcreteClass extends ResolvedClass
{
    protected final ResolvedClass _superClass;

    /**
     * List of interfaces this type implements; may be empty but never null
     */
    protected final List<ResolvedType> _interfaces;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public ResolvedConcreteClass(Class<?> erased, TypeBindings bindings,
            ResolvedClass superClass, List<ResolvedType> interfaces)
    {
        super(erased, bindings);
        _superClass = superClass;
        _interfaces = (interfaces == null) ? Collections.<ResolvedType>emptyList() : interfaces;
    }

    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */
    
    @Override
    public ResolvedType getParentClass() { return _superClass; }

    @Override
    public List<ResolvedType> getImplementedInterfaces() {
        return _interfaces;
    }
    
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
    public boolean isPrimitive() { return false; }

}
