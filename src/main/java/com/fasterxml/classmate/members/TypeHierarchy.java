package com.fasterxml.classmate.members;

import java.lang.annotation.Annotation;
import java.util.*;

import com.fasterxml.classmate.*;

/**
 * Class that contains information about full hierarchy of a type
 * resolved by {@link com.fasterxml.classmate.MemberResolver}, used for
 * resolving member information, including annotations.
 */
public class TypeHierarchy
{
    /**
     * Default annotation configuration is to ignore all annotations types.
     */
    protected final static AnnotationConfiguration DEFAULT_ANNOTATION_CONFIG
        = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.DONT_INCLUDE);

    /**
     * Need to be able to resolve member types still
     */
    protected final TypeResolver _typeResolver;
    
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
        _mainType = mainType;
        _types = types;
    }


//    protected final AnnotationConfiguration _annotationConfig;
//    _annotationConfig = annotationConfig;
    
    /*
    /**********************************************************************
    /* Public API, access to component types
    /**********************************************************************
     */
    
    public int size() { return _types.length; }

    /**
     * Accessor for getting full type hierarchy as priority-ordered list, from
     * the lowest precedence to highest precedence (main type, its mix-in overrides)
     */
    public List<HierarchicType> allTypesAndOverrides() {
        return Arrays.asList(_types);
    }

    /**
     * Access for getting subset of type hierarchy which only contains main type
     * and possible overrides (mix-ins) it has.
     */
    public List<HierarchicType> mainTypeAndOverrides()
    {
        List<HierarchicType> l = Arrays.asList(_types);
        int index = _mainType.getPriority();
        if (index > 0) {
            l = l.subList(index, l.size());
        }
        return l;
    }

    /*
    /**********************************************************************
    /* Public API, actual resolution of members
    /**********************************************************************
     */

    /**
     * Method for fully resolving field definitions and associated annotations.
     * Neither field definitions nor associated annotations inherit, but we may
     * still need to add annotation overrides, as well as filter out filters
     * and annotations that caller is not interested in.
     */
    public Collection<ResolvedField> resolveMemberFields(AnnotationConfiguration annotationConfig,
            Filter<RawField> fieldFilter)
    {
        if (annotationConfig == null) {
            annotationConfig = DEFAULT_ANNOTATION_CONFIG;
        }
        final AnnotationHandler annotationHandler = new AnnotationHandler(annotationConfig);
        
        // Fields are easy; find fields that main type has; check for annotation overrides
        LinkedHashMap<String, ResolvedField> fields = new LinkedHashMap<String, ResolvedField>();
        List<HierarchicType> types = mainTypeAndOverrides();
        // First actual fields from main type:
        for (RawField f : types.get(0).getType().getMemberFields()) {
            // want to filter it out?
            if (fieldFilter != null && !fieldFilter.include(f)) {
                continue;
            }
            Annotations annotations = new Annotations();
            for (Annotation ann : f.getRawMember().getAnnotations()) {
                if (annotationHandler.includeFieldAnnotation(ann)) {
                    annotations.add(ann);
                }
            }
            fields.put(f.getName(), ResolvedField.construct(f, _typeResolver, annotations));
        }
        // then annotation overrides, if any
        for (int i = 1, len = types.size(); i < len; ++i) {
            for (RawField f : types.get(i).getType().getMemberFields()) {
                ResolvedField base = fields.get(f.getName());
                if (base != null) {
                    for (Annotation ann : f.getRawMember().getAnnotations()) {
                        if (annotationHandler.includeFieldAnnotation(ann)) {
                            base.addAnnotation(ann);
                        }
                    }
                }
            }            
        }
        // And that's it!
        return new ArrayList<ResolvedField>(fields.values());
    }

    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    /**
     * Helper class we use to reduce number of calls to {@link AnnotationConfiguration};
     * mostly because determination may be expensive.
     */
    private final static class AnnotationHandler
    {
        private final AnnotationConfiguration _annotationConfig;

        private HashMap<Class<? extends Annotation>, AnnotationInclusion> _inclusions;
        
        public AnnotationHandler(AnnotationConfiguration annotationConfig) {
            _annotationConfig = annotationConfig;
        }

        public boolean includeFieldAnnotation(Annotation ann)
        {
            Class<? extends Annotation> annType = ann.annotationType();
            if (_inclusions == null) {
                _inclusions = new HashMap<Class<? extends Annotation>, AnnotationInclusion>();
            } else {
                AnnotationInclusion incl = _inclusions.get(annType);
                if (incl != null) {
                    return (incl != AnnotationInclusion.DONT_INCLUDE);
                }
            }
            AnnotationInclusion incl = _annotationConfig.getInclusionForField(annType);
            _inclusions.put(annType, incl);
            return (incl != AnnotationInclusion.DONT_INCLUDE);
        }
    }
}
