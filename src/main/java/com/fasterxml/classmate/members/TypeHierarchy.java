package com.fasterxml.classmate.members;

import com.fasterxml.classmate.*;

/**
 * Class that contains information about full hierarchy of a type
 * resolved by {@link com.fasterxml.classmate.MemberResolver}, used for
 * resolving member information, including annotations.
 */
public class TypeHierarchy
{
    /**
     * Need to be able to resolve member types still
     */
    protected final TypeResolver _typeResolver;

    protected final AnnotationConfiguration _annotationConfig;
    
    /**
     * Leaf of the type hierarchy, i.e. type from which this hierarchy
     * was generated.
     */
    protected final HierarchicType _mainType;

    /**
     * All types that hierarchy contains, in order of increasing precedence
     * (that is, later entries override members of earlier members)
     */
    protected final HierarchicType[] _types;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    public TypeHierarchy(TypeResolver typeResolver, AnnotationConfiguration annotationConfig,
            HierarchicType mainType, HierarchicType[] types)
    {
        _typeResolver = typeResolver;
        _annotationConfig = annotationConfig;
        _mainType = mainType;
        _types = types;
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */
    
    public int size() { return _types.length; }


}
