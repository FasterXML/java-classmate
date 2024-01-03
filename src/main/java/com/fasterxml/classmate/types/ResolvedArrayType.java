package com.fasterxml.classmate.types;

import java.util.*;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;

public final class ResolvedArrayType extends ResolvedType
{
    /**
     * All Java Arrays extend {@link java.lang.Object} so we need
     * a reference
     *<p>
     * Note that direct construction is used instead of construction via
     * {@link TypeResolver} due to complexity of doing latter: {@code java.lang.Object}
     * also does not implement any interfaces so this should be safe enough.
     *
     * @since 1.7
     */
    private final static ResolvedObjectType PARENT_TYPE =
            ResolvedObjectType.create(Object.class, null, null, null);
    
    protected final ResolvedType _elementType;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public ResolvedArrayType(Class<?> erased, TypeBindings bindings,
            ResolvedType elementType)
    {
        super(erased, bindings);
        _elementType = elementType;
    }
    
    @Override
    public boolean canCreateSubtypes() {
        return false;
    }
    
    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */
    
    @Override
    public ResolvedType getParentClass() { return PARENT_TYPE; }
    
    @Override
    public ResolvedType getSelfReferencedType() { return null; }
    
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
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAbstract() { return false; }

    @Override
    public ResolvedType getArrayElementType() { return _elementType; }

    @Override
    public boolean isArray() { return true; }

    @Override
    public boolean isPrimitive() { return false; }

    /*
    /**********************************************************************
    /* Accessors for raw (minimally processed) members
    /**********************************************************************
     */

    // defaults are fine (nothing to access)
    
    /*
    /**********************************************************************
    /* String representations
    /**********************************************************************
     */

    @Override
    public StringBuilder appendSignature(StringBuilder sb) {
        sb.append('[');
        return _elementType.appendSignature(sb);
    }

    @Override
    public StringBuilder appendErasedSignature(StringBuilder sb) {
        sb.append('[');
        return _elementType.appendErasedSignature(sb);
    }

    @Override
    public StringBuilder appendBriefDescription(StringBuilder sb)
    {
        sb = _elementType.appendBriefDescription(sb);
        sb.append("[]");
        return sb;
    }

    @Override
    public StringBuilder appendFullDescription(StringBuilder sb) {
        return appendBriefDescription(sb);
    }
}
