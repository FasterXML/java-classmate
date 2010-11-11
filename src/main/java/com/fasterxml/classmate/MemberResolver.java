package com.fasterxml.classmate;

import java.util.*;

import com.fasterxml.classmate.members.*;
import com.fasterxml.classmate.util.ClassKey;

/**
 * Builder class used to completely resolve members (fields, methods,
 * constructors) of {@link ResolvedType}s (generics-aware classes).
 */
public class MemberResolver
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

    protected final AnnotationConfiguration _annotationConfig;
    
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
    public MemberResolver(TypeResolver typeResolver, ResolvedType beanType)
    {
        this(typeResolver, beanType, null);
    }

    /**
     * Constructor that will use defaults mix-ins (no mix-ins added)
     */
    public MemberResolver(TypeResolver typeResolver, ResolvedType beanType,
            AnnotationConfiguration annotationConfig)
    {
        _typeResolver = typeResolver;
        _beanType = beanType;
        if (annotationConfig == null) {
            annotationConfig = DEFAULT_ANNOTATION_CONFIG;
        }
        _annotationConfig = annotationConfig;
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

    /**
     * Method used to find out full type hierarchy for given starting point.
     * Resulting will contain passed type as the first element, followed by
     * supertypes in order; starting with most immediate super-interfaces
     * (and their super-interfaces)
     * followed by super-classes, recursively.
     */
    public List<ResolvedType> flattenTypes(ResolvedType mainType)
    {
        HashSet<ClassKey> seenTypes = new HashSet<ClassKey>();
        ArrayList<ResolvedType> types = new ArrayList<ResolvedType>();
        _gatherTypes(mainType, seenTypes, types);
        return types;
    }
    
    /**
     * Method for constructing hierarchy object needed to fully resolve
     * member information, including basic type flattening as well as
     * addition of mix-in types in appropriate positions.
     * 
     * @param mainType Resolved type that is the starting point (i.e. the leaf class)
     *    for member resolution.
     */
    public TypeHierarchy resolveTypeHierarchy(final ResolvedType mainType, final MixInProvider mixins)
    {
        // First: flatten basic type hierarchy
        HashSet<ClassKey> seenTypes = new HashSet<ClassKey>();
        ArrayList<ResolvedType> types = new ArrayList<ResolvedType>();
        _gatherTypes(mainType, seenTypes, types);

        // Second step: inject mix-ins (keeping order from highest to lowest)
        HierarchicType[] htypes;
        HierarchicType mainHierarchicType = null;

        // Third step: add mix-ins (if any), reverse order (lowest to highest precedence)
        if (mixins == null) { // just copy, reorder
            int len = types.size();
            htypes = new HierarchicType[len];
            for (int i = 0; i < len; ++i) {
                // false -> not a mix-in
                htypes[i] = new HierarchicType(types.get(len-i), false, i);
            }
            mainHierarchicType = htypes[len-1];
        } else { // need to add mix-ins, reorder
            int len = types.size();
            ArrayList<HierarchicType> typesWithMixins = new ArrayList<HierarchicType>();
            for (int i = 0; i < len; ++i) {
                // First add type itself (lower precedence)
                ResolvedType type = types.get(len-i);
                HierarchicType ht = new HierarchicType(type, false, typesWithMixins.size());
                typesWithMixins.add(ht);
                mainHierarchicType = ht;
                List<Class<?>> m = mixins.mixInsFor(type.getErasedType());
                if (m != null) {
                    for (Class<?> mixinClass : m) {
                        ClassKey key = new ClassKey(mixinClass);
                        if (!seenTypes.contains(key)) {
                            ResolvedType mixinType = _typeResolver.resolve(mixinClass);
                            typesWithMixins.add(new HierarchicType(mixinType, true, typesWithMixins.size()));
                        }
                    }
                }
            }
            htypes = typesWithMixins.toArray(new HierarchicType[typesWithMixins.size()]);
        }
        // And that's about all we need at this point
        return new TypeHierarchy(_typeResolver, _annotationConfig, mainHierarchicType, htypes);
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */
    
    protected void _gatherTypes(ResolvedType currentType, Set<ClassKey> seenTypes, List<ResolvedType> types)
    {
        // may get called with null if no parent type
        if (currentType == null) {
            return;
        }
        Class<?> raw = currentType.getErasedType();
        // Also, don't include Object.class unless that's ok
        if (!_cfgIncludeLangObject && raw == Object.class) {
            return;
        }
        // Finally, only include first instance of an interface, so:
        ClassKey key = new ClassKey(currentType.getErasedType());
        if (seenTypes.contains(key)) {
            return;
        }
        // If all good so far, append
        seenTypes.add(key);
        types.add(currentType);
        /* and check supertypes; starting with interfaces. Why interfaces?
         * So that "highest" interfaces get priority; otherwise we'd recurse
         * super-class stack and actually start with the bottom. Usually makes
         * little difference, but in cases where it does this seems like the
         * correct order.
         */
        for (ResolvedType t : currentType.getImplementedInterfaces()) {
            _gatherTypes(t, seenTypes, types);
        }
        // and then superclass
        _gatherTypes(currentType.getParentClass(), seenTypes, types);
    }
}
