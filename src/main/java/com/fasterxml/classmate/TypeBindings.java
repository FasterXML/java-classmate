package com.fasterxml.classmate;

import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * Helper class used for storing binding of local type variables to
 * matching resolved types.
 */
public class TypeBindings
{
    private final static String[] NO_STRINGS = new String[0];

    private final static ResolvedType[] NO_TYPES = new ResolvedType[0];

    private final static TypeBindings EMPTY = new TypeBindings(null, null);

    /**
     * Array of type (type variable) names.
     */
    private final String[] _names;

    private final ResolvedType[] _types;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */
    
    private TypeBindings(String[] names, ResolvedType[] types)
    {
        _names = (names == null) ? NO_STRINGS : names;
        _types = (types == null) ? NO_TYPES : types;
        if (_names.length != _types.length) {
            throw new IllegalArgumentException("Mismatching names ("+_names.length+"), types ("+_types.length+")");
        }
    }

    public static TypeBindings emptyBindings() {
        return EMPTY;
    }

    /**
     * Factory method for constructing bindings for given class using specified type
     * parameters.
     */
    public static TypeBindings create(Class<?> erasedType, List<ResolvedType> typeList)
    {
        ResolvedType[] types = (typeList == null || typeList.isEmpty()) ?
                NO_TYPES : typeList.toArray(new ResolvedType[typeList.size()]);
        return create(erasedType, types);
    }
        
    public static TypeBindings create(Class<?> erasedType, ResolvedType[] types)
    {
        if (types == null) {
            types = NO_TYPES;
        }
        TypeVariable<?>[] vars = erasedType.getTypeParameters();
        String[] names;
        if (vars == null || vars.length == 0) {
            names = NO_STRINGS;
        } else {
            int len = vars.length;
            names = new String[len];
            for (int i = 0; i < len; ++i) {
                names[i] = vars[i].getName();
            }
        }
        // Check here to give better error message
        if (names.length != types.length) {
            throw new IllegalArgumentException("Can not create TypeBinding for class "+erasedType.getName()
                   +" with "+types.length+" type parameters: class expects "+names.length);
        }
        return new TypeBindings(names, types);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */
    
    /**
     * Find type bound to specified name, if there is one; returns bound type if so, null if not.
     */
    public ResolvedType findBoundType(String name)
    {
        for (int i = 0, len = _names.length; i < len; ++i) {
            if (name.equals(_names[i])) {
                return _types[i];
            }
        }
        return null;
    }

    /**
     * Returns number of bindings contained
     */
    public int size() { 
        return _types.length;
    }

    public String getBoundName(int index)
    {
        if (index < 0 || index >= _names.length) {
            return null;
        }
        return _names[index];
    }

    public ResolvedType getBoundType(int index)
    {
        if (index < 0 || index >= _types.length) {
            return null;
        }
        return _types[index];
    }

    /**
     * Accessor for getting bound types in declaration order
     */
    public List<ResolvedType> getTypeParameters()
    {
        if (_types.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(_types);
    }

    /*
    /**********************************************************************
    /* Package accessible methods
    /**********************************************************************
     */

    protected ResolvedType[] typeParameterArray() {
        return _types;
    }
}
