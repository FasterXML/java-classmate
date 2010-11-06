package com.fasterxml.classmate;

/**
 * Builder class used to completely resolve members (fields, methods,
 * constructors) of bean types (or more precisely, any POJOs); optionally
 * 
 */
public class BeanResolver
{
    /**
     * Default annotation configuration is to ignore all annotations types.
     */
    protected final static AnnotationConfiguration DEFAULT_ANNOTATION_CONFIG
        = new AnnotationConfiguration.StdConfiguration(AnnotationConfiguration.Inclusion.DONT_INCLUDE);
    
    /**
     * Type resolved needed for resolving types of member objects
     * (method argument and return; field types; constructor argument types)
     */
    protected final TypeResolver _typeResolver;

    /**
     * Type of bean to resolve.
     */
    protected final ResolvedType _beanType;

    protected final MixInProvider _mixinProvider;
    
    /*
    /**********************************************************************
    /* Modifiable configuration
    /**********************************************************************
     */
    
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

    /**
     * Constructor that will use defaults for annotation configuration
     * (which is to ignore all annotations) and mix-ins (no mix-ins added)
     */
    public BeanResolver(TypeResolver typeResolver, ResolvedType beanType)
    {
        this(typeResolver, beanType, null);
    }

    /**
     * Constructor that will use defaults mix-ins (no mix-ins added)
     */
    public BeanResolver(TypeResolver typeResolver, ResolvedType beanType,
            AnnotationConfiguration config)
    {
        this(typeResolver, beanType, config, null);
    }

    public BeanResolver(TypeResolver typeResolver, ResolvedType beanType,
            AnnotationConfiguration config,
            MixInProvider mixins)
    {
        _typeResolver = typeResolver;
        _beanType = beanType;
        if (config == null) {
            config = DEFAULT_ANNOTATION_CONFIG;
        }
        _mixinProvider = mixins;
    }

    /**
     * Configuration method for specifying whether members of <code>java.lang.Object</code>
     * are to be included in resolution; if false, no members from {@link java.lang.Object}
     * are to be included; if true, will be included.
     */
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
