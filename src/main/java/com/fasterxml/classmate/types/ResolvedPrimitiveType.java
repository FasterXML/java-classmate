package com.fasterxml.classmate.types;

import java.util.*;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;

/**
 * Type used for Java primitive types (which does not include arrays here).
 *<p>
 * Since set of primitive types is bounded, constructor is defined as protected,
 * and class final; that is, new primitive types are not to be constructed
 * by calling applications.
 */
public final class ResolvedPrimitiveType extends ResolvedClass
{
    private final static ResolvedPrimitiveType VOID = new ResolvedPrimitiveType(Void.TYPE, 'V');
    
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

    public static List<ResolvedPrimitiveType> all()
    {
        ArrayList<ResolvedPrimitiveType> all = new ArrayList<ResolvedPrimitiveType>();
        all.add(new ResolvedPrimitiveType(Boolean.TYPE, 'Z'));
        all.add(new ResolvedPrimitiveType(Byte.TYPE, 'B'));
        all.add(new ResolvedPrimitiveType(Short.TYPE, 'S'));
        all.add(new ResolvedPrimitiveType(Character.TYPE, 'C'));
        all.add(new ResolvedPrimitiveType(Integer.TYPE, 'I'));
        all.add(new ResolvedPrimitiveType(Long.TYPE, 'J'));
        all.add(new ResolvedPrimitiveType(Float.TYPE, 'F'));
        all.add(new ResolvedPrimitiveType(Double.TYPE, 'D'));
        return all;
    }

    public static ResolvedPrimitiveType voidType()
    {
        return VOID;
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
