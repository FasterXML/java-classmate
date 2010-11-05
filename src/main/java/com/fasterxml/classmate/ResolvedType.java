package com.fasterxml.classmate;

import java.util.*;

public abstract class ResolvedType
{
    protected final Class<?> _erasedType;

    /**
     * Type bindings active when resolving members (methods, fields,
     * constructors) of this type
     */
    protected final TypeBindings _typeBindings;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    protected ResolvedType(Class<?> cls, TypeBindings bindings)
    {
        _erasedType = cls;
        _typeBindings = (bindings == null) ? TypeBindings.emptyBindings() : bindings;
    }

    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */
    
    public Class<?> getErasedType() { return _erasedType; }

    public abstract ResolvedType getParentClass();

    /**
     * Method that can be used to access element type of array types; will return
     * null for non-array types, and non-null type for array types.
     */
    public abstract ResolvedType getArrayElementType();
    
    public abstract List<ResolvedType> getImplementedInterfaces();

    public List<ResolvedType> getTypeParameters() {
        return _typeBindings.getTypeParameters();
    }

    /**
     * Method for accessing bindings of type variables to resolved types in context
     * of this type.
     */
    public TypeBindings getBindings() { return _typeBindings; }
    
    /**
     * Method that will try to find type parameterization this type
     * has for specified super type
     * 
     * @return List of type parameters for specified supertype (which may
     *   be empty, if supertype is not a parametric type); null if specified
     *   type is not a super type of this type
     */
    public List<ResolvedType> typeParametersFor(Class<?> erasedSupertype)
    {
        if (erasedSupertype == _erasedType) {
            return getTypeParameters();
        }
        // first, check super-class
        ResolvedType pc = getParentClass();
        if (pc != null) {
            List<ResolvedType> typeParams = pc.typeParametersFor(erasedSupertype);
            if (typeParams != null) {
                return typeParams;
            }
        }
        // if not found, and we are looking for an interface, try implemented interfaces:
        if (erasedSupertype.isInterface()) {
            for (ResolvedType it : getImplementedInterfaces()) {
                List<ResolvedType> typeParams = it.typeParametersFor(erasedSupertype);
                if (typeParams != null) {
                    return typeParams;
                }
            }
        }
        // nope; doesn't look like we extend or implement super type in question
        return null;
    }

    /*
    /**********************************************************************
    /* Simple property accessors
    /**********************************************************************
     */
    
    public abstract boolean isInterface();
    public abstract boolean isConcrete();
    public final boolean isAbstract() { return !isConcrete(); }

    /**
     * Method that indicates whether this type is an array type.
     */
    public abstract boolean isArray();

    /**
     * Method that indicates whether this type is one of small number of primitive
     * Java types; not including array types of primitive types but just basic
     * primitive types.
     */
    public abstract boolean isPrimitive();
}
