package com.fasterxml.classmate;

import java.io.Serializable;
import java.util.*;

import com.fasterxml.classmate.members.*;
import com.fasterxml.classmate.util.ClassKey;

/**
 * Builder class used to completely resolve members (fields, methods,
 * constructors) of {@link ResolvedType}s (generics-aware classes).
 */
@SuppressWarnings("serial")
public class MemberResolver implements Serializable
{
    
    /**
     * Type resolved needed for resolving types of member objects
     * (method argument and return; field types; constructor argument types)
     */
    protected final TypeResolver _typeResolver;
    
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

    /**
     * Filter used for determining whether given
     * field (static or member)
     * is to be included in aggregation of all
     * fields.
     */
    protected Filter<RawField> _fieldFilter;

    /**
     * Filter used for determining whether given
     * method (static or member)
     * is to be included in aggregation of all
     * methods.
     */
    protected Filter<RawMethod> _methodFilter;

    /**
     * Filter used for determining whether given
     * constructor
     * is to be included in aggregation of all
     * constructors.
     */
    protected Filter<RawConstructor> _constructorFilter;
    
    /*
    /**********************************************************************
    /* Life cycle (construct and config)
    /**********************************************************************
     */

    /**
     * Constructor for resolver that does not include <code>java.lang.Object</code>
     * in type hierarchy
     */
    public MemberResolver(TypeResolver typeResolver)
    {
        _typeResolver = typeResolver;
    }

    /**
     * Configuration method for specifying whether members of <code>java.lang.Object</code>
     * are to be included in resolution; if false, no members from {@link java.lang.Object}
     * are to be included; if true, will be included.
     */
    public MemberResolver setIncludeLangObject(boolean state) {
        _cfgIncludeLangObject = state;
        return this;
    }

    public MemberResolver setFieldFilter(Filter<RawField> f) {
        _fieldFilter = f;
        return this;
    }

    public MemberResolver setMethodFilter(Filter<RawMethod> f) {
        _methodFilter = f;
        return this;
    }

    public MemberResolver setConstructorFilter(Filter<RawConstructor> f) {
        _constructorFilter = f;
        return this;
    }
    
    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    /**
     * Method for constructing hierarchy object needed to fully resolve
     * member information, including basic type flattening as well as
     * addition of mix-in types in appropriate positions.
     * 
     * @param mainType Resolved type that is the starting point (i.e. the leaf class)
     *    for member resolution.
     * @param annotationConfig Configuration of annotation types; which ones to include, how to inherit
     * @param annotationOverrides Definitions of annotation overrides to use, if any (may be null)
     */
    public ResolvedTypeWithMembers resolve(final ResolvedType mainType,
            AnnotationConfiguration annotationConfig,
            AnnotationOverrides annotationOverrides)
    {
        // First: flatten basic type hierarchy (highest to lowest precedence)
        HashSet<ClassKey> seenTypes = new HashSet<ClassKey>();
        ArrayList<ResolvedType> types = new ArrayList<ResolvedType>();
        _gatherTypes(mainType, seenTypes, types);

        // Second step: inject mix-ins (keeping order from highest to lowest)
        HierarchicType[] htypes;
        HierarchicType mainHierarchicType = null;

        // Third step: add mix-ins (if any), reverse order (lowest to highest precedence)
        if (annotationOverrides == null) { // just create hierarchic instances:
            int len = types.size();
            htypes = new HierarchicType[len];
            for (int i = 0; i < len; ++i) {
                // false -> not a mix-in
                htypes[i] = new HierarchicType(types.get(i), false, i);
            }
            mainHierarchicType = htypes[0];
        } else { // need to add mix-ins, reorder
            ArrayList<HierarchicType> typesWithMixins = new ArrayList<HierarchicType>();
            for (ResolvedType type : types) {
                // First add mix-ins (which override type itself)
                List<Class<?>> m = annotationOverrides.mixInsFor(type.getErasedType());
                if (m != null) {
                    for (Class<?> mixinClass : m) {
                        _addOverrides(typesWithMixins, seenTypes, mixinClass);
                    }
                }

                // Then actual type:
                HierarchicType ht = new HierarchicType(type, false, typesWithMixins.size());
                if (mainHierarchicType == null) {
                    mainHierarchicType = ht;
                }
                typesWithMixins.add(ht);
            }
            htypes = typesWithMixins.toArray(new HierarchicType[typesWithMixins.size()]);
        }
        // And that's about all we need to do; rest computed lazily
        return new ResolvedTypeWithMembers(_typeResolver, annotationConfig, mainHierarchicType, htypes,
                _constructorFilter, _fieldFilter, _methodFilter);
    }

    private void _addOverrides(List<HierarchicType> typesWithOverrides, Set<ClassKey> seenTypes, Class<?> override)
    {
        ClassKey key = new ClassKey(override);
        if (!seenTypes.contains(key)) {
            seenTypes.add(key);
            ResolvedType resolvedOverride = _typeResolver.resolve(override);
            typesWithOverrides.add(new HierarchicType(resolvedOverride, true, typesWithOverrides.size()));
            for (ResolvedType r : resolvedOverride.getImplementedInterfaces()) { // interfaces?
                _addOverrides(typesWithOverrides, seenTypes, r);
            }
            ResolvedType superClass = resolvedOverride.getParentClass();
            _addOverrides(typesWithOverrides, seenTypes, superClass);
        }
    }

    private void _addOverrides(List<HierarchicType> typesWithOverrides, Set<ClassKey> seenTypes, ResolvedType override)
    {
        if (override == null) return;
        // first: may need to exclude Object.class:
        Class<?> raw = override.getErasedType();
        if (!_cfgIncludeLangObject && Object.class == raw) return;
        ClassKey key = new ClassKey(raw);
        if (!seenTypes.contains(key)) {
            seenTypes.add(key);
            typesWithOverrides.add(new HierarchicType(override, true, typesWithOverrides.size()));
            for (ResolvedType r : override.getImplementedInterfaces()) { // interfaces?
                _addOverrides(typesWithOverrides, seenTypes, r);
            }
            ResolvedType superClass = override.getParentClass();
            if (superClass != null) {
                _addOverrides(typesWithOverrides, seenTypes, superClass);
            }
        }
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
