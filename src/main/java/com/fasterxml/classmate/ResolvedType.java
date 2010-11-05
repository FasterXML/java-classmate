package com.fasterxml.classmate;

import java.util.Collections;
import java.util.List;

public abstract class ResolvedType
{
    protected final Class<?> _erasedType;

    /**
     * List of interfaces this type implements; may be empty but never null
     */
    protected final List<ResolvedType> _interfaces;

    protected final List<ResolvedType> _typeParameters;
    
    protected ResolvedType(Class<?> cls, List<ResolvedType> typeParameters, List<ResolvedType> interfaces)
    {
        _erasedType = cls;
        if (typeParameters == null) {
            typeParameters = Collections.emptyList();
        }
        _typeParameters = typeParameters;
        if (interfaces == null) {
            interfaces = Collections.emptyList();
        }
        _interfaces = interfaces;
    }

    public Class<?> getErasedType() { return _erasedType; }

    public abstract ResolvedType getParentClass();

    public List<ResolvedType> getImplementedInterfaces() {
        return _interfaces;
    }

    public List<ResolvedType> getTypeParameters() { return _typeParameters; }
    
    public abstract boolean isInterface();
    public abstract boolean isConcrete();
    public final boolean isAbstract() { return !isConcrete(); }

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
}

