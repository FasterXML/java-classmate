package com.fasterxml.classmate;

/**
 * Builder class used to completely resolve members (fields, methods,
 * constructors) of bean types (or more precisely, any POJOs); optionally
 * 
 */
public class BeanResolver
{
    /**
     * Type resolved needed for resolving types of member objects
     * (method argument and return; field types; constructor argument types)
     */
    protected final TypeResolver _typeResolver;

    /**
     * Configuration setting that determines whether members from
     * {@link java.lang.Object} are included or not; by default
     * false meaning that they are not.
     */
    protected boolean _cfgIncludeLangObject;
    
    /*
    /**********************************************************************
    /* Life cycle (construct and config)
    /**********************************************************************
     */

    public BeanResolver(TypeResolver typeResolver)
    {
        _typeResolver = typeResolver;
    }

    public void setIncludeLangObject(boolean state) {
        _cfgIncludeLangObject = state;
    }
    
    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    /*
    /**********************************************************************
    /* Pre-created instances
    /**********************************************************************
     */
}
