package com.fasterxml.classmate;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.classmate.members.*;
import com.fasterxml.classmate.util.MethodKey;

/**
 * Class that contains information about fully resolved members of a
 * type; resolution meaning that masking is handled for methods, and
 * all inheritable annotations are flattened using optional overrides
 * as well ("mix-in annotations").
 * Instances are created by {@link com.fasterxml.classmate.MemberResolver}.
 *<p>
 * Note that instances are not thread-safe, as the expectation is that instances
 * will not be shared (unlike raw members or resolved types)
 */
public class ResolvedTypeWithMembers
{
    private final static ResolvedType[] NO_RESOLVED_TYPES = new ResolvedType[0];

    private final static ResolvedMethod[] NO_RESOLVED_METHODS = new ResolvedMethod[0];
    private final static ResolvedField[] NO_RESOLVED_FIELDS = new ResolvedField[0];
    private final static ResolvedConstructor[] NO_RESOLVED_CONSTRUCTORS = new ResolvedConstructor[0];
    
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
     * Handler for resolving annotation information
     */
    protected final AnnotationHandler _annotationHandler;
    
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

    /**
     * Filter to use for selecting fields to include
     */
    protected Filter<RawField> _fieldFilter;

    /**
     * Filter to use for selecting constructors to include
     */
    protected Filter<RawConstructor> _constructorFilter;
    
    /**
     * Filter to use for selecting methods to include
     */
    protected Filter<RawMethod> _methodFilter;
    
    /*
    /**********************************************************************
    /* Lazily constructed members
    /**********************************************************************
     */

    protected ResolvedMethod[] _staticMethods = null;

    protected ResolvedField[] _staticFields = null;

    protected ResolvedMethod[] _memberMethods = null;

    protected ResolvedField[] _memberFields = null;

    protected ResolvedConstructor[] _constructors = null;

    /*
    /**********************************************************************
    /* Life cycle at this point
    /**********************************************************************
     */
    
    public ResolvedTypeWithMembers(TypeResolver typeResolver, AnnotationConfiguration annotationConfig,
            HierarchicType mainType, HierarchicType[] types,
            Filter<RawConstructor> constructorFilter, Filter<RawField> fieldFilter, Filter<RawMethod> methodFilter)
    {
        _typeResolver = typeResolver;
        _mainType = mainType;
        _types = types;
        if (annotationConfig == null) {
            annotationConfig = DEFAULT_ANNOTATION_CONFIG;
        }
        _annotationHandler = new AnnotationHandler(annotationConfig);
        _constructorFilter = constructorFilter;
        _fieldFilter = fieldFilter;
        _methodFilter = methodFilter;
    }
    
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
     * Accessor for getting subset of type hierarchy which only contains main type
     * and possible overrides (mix-ins) it has, but not supertypes or their overrides.
     */
    public List<HierarchicType> mainTypeAndOverrides()
    {
        List<HierarchicType> l = Arrays.asList(_types);
        int end = _mainType.getPriority() + 1;
        if (end < l.size()) {
            l = l.subList(0, end);
        }
        return l;
    }

    /**
     * Accessor for finding just overrides for the main type (if any).
     */
    public List<HierarchicType> overridesOnly()
    {
        int index = _mainType.getPriority();
        if (index == 0) {
            return Collections.emptyList();
        }
        List<HierarchicType> l = Arrays.asList(_types);
        return l.subList(0, index);
    }
    
    /*
    /**********************************************************************
    /* Public API, actual resolution of members
    /**********************************************************************
     */

    /**
     * Method for finding all static fields of the main type (except for ones
     * possibly filtered out by filter) and applying annotation overrides, if any,
     * to annotations.
     * 
     * @since 1.2.0
     */
    public ResolvedField[] getStaticFields()
    {
        if (_staticFields == null) {
            _staticFields = resolveStaticFields();
        }
        return _staticFields;
    }
    
    /**
     * Method for finding all static methods of the main type (except for ones
     * possibly filtered out by filter) and applying annotation overrides, if any,
     * to annotations.
     */
    public ResolvedMethod[] getStaticMethods()
    {
        if (_staticMethods == null) {
            _staticMethods = resolveStaticMethods();
        }
        return _staticMethods;
    }

    public ResolvedField[] getMemberFields()
    {
        if (_memberFields == null) {
            _memberFields = resolveMemberFields();
        }
        return _memberFields;
    }
    
    public ResolvedMethod[] getMemberMethods()
    {
        if (_memberMethods == null) {
            _memberMethods = resolveMemberMethods();
        }
        return _memberMethods;
    }

    public ResolvedConstructor[] getConstructors()
    {
        if (_constructors == null) {
            _constructors = resolveConstructors();
        }
        return _constructors;
    }
    
    /*
    /**********************************************************************
    /* Internal methods: actual resolution
    /**********************************************************************
     */
    
    /**
     * Method that will actually resolve full information (types, annotations)
     * for constructors of the main type.
     */
    protected ResolvedConstructor[] resolveConstructors()
    {
        // First get static methods for main type, filter
        LinkedHashMap<MethodKey, ResolvedConstructor> constructors = new LinkedHashMap<MethodKey, ResolvedConstructor>();
        for (RawConstructor constructor : _mainType.getType().getConstructors()) {
            // no filter for constructors (yet?)
            if (_constructorFilter == null || _constructorFilter.include(constructor)) {
                constructors.put(constructor.createKey(), resolveConstructor(constructor));
            }
        }
        // then apply overrides (mix-ins):
        for (HierarchicType type : overridesOnly()) {
            for (RawConstructor raw : type.getType().getConstructors()) {
                ResolvedConstructor constructor = constructors.get(raw.createKey()); 
                // must override something, otherwise to ignore
                if (constructor != null) {
                    for (Annotation ann : raw.getAnnotations()) {
                        if (_annotationHandler.includeMethodAnnotation(ann)) {
                            constructor.applyOverride(ann);
                        }
                    }

                    // and parameter annotations
                    Annotation[][] params = raw.getRawMember().getParameterAnnotations();
                    for (int i = 0; i < params.length; i++) {
                        for (Annotation annotation : params[i]) {
                            if (_annotationHandler.includeParameterAnnotation(annotation)) {
                                constructor.applyParamOverride(i, annotation);
                            }
                        }
                    }
                }
            }
        }
        if (constructors.size() == 0) {
            return NO_RESOLVED_CONSTRUCTORS;
        }
        return constructors.values().toArray(new ResolvedConstructor[constructors.size()]);
    }

    /**
     * Method for fully resolving field definitions and associated annotations.
     * Neither field definitions nor associated annotations inherit, but we may
     * still need to add annotation overrides, as well as filter out filters
     * and annotations that caller is not interested in.
     */
    protected ResolvedField[] resolveMemberFields()
    {
        LinkedHashMap<String, ResolvedField> fields = new LinkedHashMap<String, ResolvedField>();

        /* Fields need different handling: must start from bottom; and annotations only get added
         * as overrides, never as defaults. And sub-classes fully mask fields. This makes
         * handling bit simpler than that of member methods.
         */
        for (int typeIndex = _types.length; --typeIndex >= 0; ) {
            HierarchicType thisType = _types[typeIndex];
            // If it's just a mix-in, add annotations as overrides
            if (thisType.isMixin()) {
                for (RawField raw : thisType.getType().getMemberFields()) {
                    if ((_fieldFilter != null) && !_fieldFilter.include(raw)) {
                        continue;
                    }
                    ResolvedField field = fields.get(raw.getName());
                    if (field != null) {
                        for (Annotation ann : raw.getAnnotations()) {
                            if (_annotationHandler.includeMethodAnnotation(ann)) {
                                field.applyOverride(ann);
                            }
                        }
                    }
                }
            } else { // If actual type, add fields, masking whatever might have existed before:
                for (RawField field : thisType.getType().getMemberFields()) {
                    if ((_fieldFilter != null) && !_fieldFilter.include(field)) {
                        continue;
                    }
                    fields.put(field.getName(), resolveField(field));
                }
            }
        }
        // and that's it?
        if (fields.size() == 0) {
            return NO_RESOLVED_FIELDS;
        }
        return fields.values().toArray(new ResolvedField[fields.size()]);
    }

    protected ResolvedMethod[] resolveMemberMethods()
    {
        LinkedHashMap<MethodKey, ResolvedMethod> methods = new LinkedHashMap<MethodKey, ResolvedMethod>();
        LinkedHashMap<MethodKey, Annotations> overrides = new LinkedHashMap<MethodKey, Annotations>();
        LinkedHashMap<MethodKey, Annotations[]> paramOverrides = new LinkedHashMap<MethodKey, Annotations[]>();

        /* Member methods are handled from top to bottom; and annotations are tracked
         * alongside (for overrides), as well as "merged down" for inheritable
         * annotations.
         */
        for (HierarchicType type : allTypesAndOverrides()) {
            for (RawMethod method : type.getType().getMemberMethods()) {
                // First: ignore methods caller is not interested
                if (_methodFilter != null && !_methodFilter.include(method)) {
                    continue;
                }

                MethodKey key = method.createKey();
                ResolvedMethod old = methods.get(key);
                
                // Ok, now, mix-ins only contribute annotations; whereas 'real' types methods
                if (type.isMixin()) { // mix-in: only get annotations
                    for (Annotation ann : method.getAnnotations()) {
                        // If already have a method, must be inheritable to include
                        if (old != null) {
                            if (!methodCanInherit(ann)) {
                                continue;
                            }
                            // and if so, apply as default (i.e. do not override)
                            old.applyDefault(ann);
                        } else { // If no method, need to add to annotation override map
                            Annotations oldAnn = overrides.get(key);
                            if (oldAnn == null) {
                                oldAnn = new Annotations();
                                oldAnn.add(ann);
                                overrides.put(key, oldAnn);
                            } else {
                                oldAnn.addAsDefault(ann);
                            }
                        }
                    }

                    // override argument annotations
                    final Annotation[][] argAnnotations = method.getRawMember().getParameterAnnotations();
                    if (old == null) { // no method (yet), add argument annotations to override map
                        Annotations[] oldParamAnns = paramOverrides.get(key);
                        if (oldParamAnns == null) { // no existing argument annotations for method
                            oldParamAnns = new Annotations[argAnnotations.length];
                            for (int i = 0; i < argAnnotations.length; i++) {
                                oldParamAnns[i] = new Annotations();
                                for (final Annotation annotation : argAnnotations[i]) {
                                    if (parameterCanInherit(annotation)) {
                                        oldParamAnns[i].add(annotation);
                                    }
                                }
                            }
                            paramOverrides.put(key, oldParamAnns);
                        } else {
                            for (int i = 0; i < argAnnotations.length; i++) {
                                for (final Annotation annotation : argAnnotations[i]) {
                                    if (parameterCanInherit(annotation)) {
                                        oldParamAnns[i].addAsDefault(annotation);
                                    }
                                }
                            }
                        }
                    } else { // already have a method, apply argument annotations as defaults
                        for (int i = 0; i < argAnnotations.length; i++) {
                            for (final Annotation annotation : argAnnotations[i]) {
                                if (parameterCanInherit(annotation)) {
                                    old.applyParamDefault(i, annotation);
                                }
                            }
                        }
                    }
                } else { // "real" methods; add if not present, possibly add defaults as well
                    if (old == null) { // new one to add
                        ResolvedMethod newMethod = resolveMethod(method);
                        methods.put(key, newMethod);
                        // But we may also have annotation overrides, so:
                        Annotations overrideAnn = overrides.get(key);
                        if (overrideAnn != null) {
                            newMethod.applyOverrides(overrideAnn);
                        }
                        // and apply parameter annotation overrides
                        Annotations[] annotations = paramOverrides.get(key);
                        if (annotations != null) {
                            for (int i = 0; i < annotations.length; i++) {
                                newMethod.applyParamOverrides(i, annotations[i]);
                            }
                        }
                    } else { // method masked by something else? can only contribute annotations
                        for (Annotation ann : method.getAnnotations()) {
                            if (methodCanInherit(ann)) {
                                old.applyDefault(ann);
                            }
                        }
                        // and parameter annotations
                        final Annotation[][] parameterAnnotations = method.getRawMember().getParameterAnnotations();
                        for (int i = 0; i < parameterAnnotations.length; i++) {
                            for (final Annotation annotation : parameterAnnotations[i]) {
                                if (parameterCanInherit(annotation)) {
                                    old.applyParamDefault(i, annotation);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (methods.size() == 0) {
            return NO_RESOLVED_METHODS;
        }
        return methods.values().toArray(new ResolvedMethod[methods.size()]);
    }
    
    /**
     * Method for fully resolving static field definitions and associated annotations.
     * Neither field definitions nor associated annotations inherit, but we may
     * still need to add annotation overrides, as well as filter out filters
     * and annotations that caller is not interested in.
     * 
     * @since 1.2.0
     */
    protected ResolvedField[] resolveStaticFields()
    {
        // First get static methods for main type, filter
        LinkedHashMap<String, ResolvedField> fields = new LinkedHashMap<String, ResolvedField>();
        for (RawField field : _mainType.getType().getStaticFields()) {
            if (_fieldFilter == null || _fieldFilter.include(field)) {
                fields.put(field.getName(), resolveField(field));
            }
        }
        // then apply overrides (mix-ins):
        for (HierarchicType type : overridesOnly()) {
            for (RawField raw : type.getType().getStaticFields()) {
                ResolvedField field = fields.get(raw.getName()); 
                // must override something, otherwise to ignore
                if (field != null) {
                    for (Annotation ann : raw.getAnnotations()) {
                        if (_annotationHandler.includeFieldAnnotation(ann)) {
                            field.applyOverride(ann);
                        }
                    }
                }
            }
        }
        // and that's it?
        if (fields.isEmpty()) {
            return NO_RESOLVED_FIELDS;
        }
        return fields.values().toArray(new ResolvedField[ fields.size()]);
    }

    /**
     * Method that will actually resolve full information (types, annotations)
     * for static methods, using configured filter.
     */
    protected ResolvedMethod[] resolveStaticMethods()
    {
        // First get static methods for main type, filter
        LinkedHashMap<MethodKey, ResolvedMethod> methods = new LinkedHashMap<MethodKey, ResolvedMethod>();
        for (RawMethod method : _mainType.getType().getStaticMethods()) {
            if (_methodFilter == null || _methodFilter.include(method)) {
                methods.put(method.createKey(), resolveMethod(method));
            }
        }
        // then apply overrides (mix-ins):
        for (HierarchicType type : overridesOnly()) {
            for (RawMethod raw : type.getType().getStaticMethods()) {
                ResolvedMethod method = methods.get(raw.createKey()); 
                // must override something, otherwise to ignore
                if (method != null) {
                    for (Annotation ann : raw.getAnnotations()) {
                        if (_annotationHandler.includeMethodAnnotation(ann)) {
                            method.applyOverride(ann);
                        }
                    }
                }
            }
        }
        if (methods.size() == 0) {
            return NO_RESOLVED_METHODS;
        }
        return methods.values().toArray(new ResolvedMethod[methods.size()]);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    /**
     * Method for resolving individual constructor completely
     */
    protected ResolvedConstructor resolveConstructor(RawConstructor raw)
    {
        final ResolvedType context = raw.getDeclaringType();
        final TypeBindings bindings = context.getTypeBindings();
        Constructor<?> ctor = raw.getRawMember();
        Type[] rawTypes = ctor.getGenericParameterTypes();
        ResolvedType[] argTypes;
        if (rawTypes == null || rawTypes.length == 0) {
            argTypes = NO_RESOLVED_TYPES;
        } else {
            argTypes = new ResolvedType[rawTypes.length];
            for (int i = 0, len = rawTypes.length; i < len; ++i) {
                argTypes[i] = _typeResolver.resolve(bindings, rawTypes[i]);
            }
        }
        // And then annotations
        Annotations anns = new Annotations();
        for (Annotation ann : ctor.getAnnotations()) {
            if (_annotationHandler.includeConstructorAnnotation(ann)) {
                anns.add(ann);
            }
        }

        ResolvedConstructor constructor = new ResolvedConstructor(context, anns, ctor, argTypes);

        // and parameter annotations
        Annotation[][] annotations = ctor.getParameterAnnotations();
        for (int i = 0; i < argTypes.length; i++) {
            for (Annotation ann : annotations[i]) {
                constructor.applyParamOverride(i, ann);
            }
        }

        return constructor;
    }

    /**
     * Method for resolving individual field completely
     */
    protected ResolvedField resolveField(RawField raw)
    {
        final ResolvedType context = raw.getDeclaringType();
        Field field = raw.getRawMember();
        ResolvedType type = _typeResolver.resolve(context.getTypeBindings(), field.getGenericType());
        // And then annotations
        Annotations anns = new Annotations();
        for (Annotation ann : field.getAnnotations()) {
            if (_annotationHandler.includeFieldAnnotation(ann)) {
                anns.add(ann);
            }
        }
        return new ResolvedField(context, anns, field, type);
    }
    
    /**
     * Method for resolving individual method completely
     */
    protected ResolvedMethod resolveMethod(RawMethod raw)
    {
        final ResolvedType context = raw.getDeclaringType();
        final TypeBindings bindings = context.getTypeBindings();
        Method m = raw.getRawMember();
        Type rawType = m.getGenericReturnType();
        ResolvedType rt = (rawType == Void.TYPE) ? null : _typeResolver.resolve(bindings, rawType);
        Type[] rawTypes = m.getGenericParameterTypes();
        ResolvedType[] argTypes;
        if (rawTypes == null || rawTypes.length == 0) {
            argTypes = NO_RESOLVED_TYPES;
        } else {
            argTypes = new ResolvedType[rawTypes.length];
            for (int i = 0, len = rawTypes.length; i < len; ++i) {
                argTypes[i] = _typeResolver.resolve(bindings, rawTypes[i]);
            }
        }
        // And then annotations
        Annotations anns = new Annotations();
        for (Annotation ann : m.getAnnotations()) {
            if (_annotationHandler.includeMethodAnnotation(ann)) {
                anns.add(ann);
            }
        }

        ResolvedMethod method = new ResolvedMethod(context, anns, m, rt, argTypes);

        // and argument annotations
        Annotation[][] annotations = m.getParameterAnnotations();
        for (int i = 0; i < argTypes.length; i++) {
            for (Annotation ann : annotations[i]) {
                method.applyParamOverride(i, ann);
            }
        }
        return method;
    }

    protected boolean methodCanInherit(Annotation annotation) {
        AnnotationInclusion annotationInclusion = _annotationHandler.methodInclusion(annotation);
        if (annotationInclusion == AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED) {
            return annotation.annotationType().isAnnotationPresent(Inherited.class);
        }
        return (annotationInclusion == AnnotationInclusion.INCLUDE_AND_INHERIT);
    }

    protected boolean parameterCanInherit(Annotation annotation) {
        AnnotationInclusion annotationInclusion = _annotationHandler.parameterInclusion(annotation);
        if (annotationInclusion == AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED) {
            return annotation.annotationType().isAnnotationPresent(Inherited.class);
        }
        return (annotationInclusion == AnnotationInclusion.INCLUDE_AND_INHERIT);
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

        private HashMap<Class<? extends Annotation>, AnnotationInclusion> _fieldInclusions;
        private HashMap<Class<? extends Annotation>, AnnotationInclusion> _constructorInclusions;
        private HashMap<Class<? extends Annotation>, AnnotationInclusion> _methodInclusions;
        private HashMap<Class<? extends Annotation>, AnnotationInclusion> _parameterInclusions;

        public AnnotationHandler(AnnotationConfiguration annotationConfig) {
            _annotationConfig = annotationConfig;
        }

        public boolean includeConstructorAnnotation(Annotation ann)
        {
            Class<? extends Annotation> annType = ann.annotationType();
            if (_constructorInclusions == null) {
                _constructorInclusions = new HashMap<Class<? extends Annotation>, AnnotationInclusion>();
            } else {
                AnnotationInclusion incl = _constructorInclusions.get(annType);
                if (incl != null) {
                    return (incl != AnnotationInclusion.DONT_INCLUDE);
                }
            }
            AnnotationInclusion incl = _annotationConfig.getInclusionForConstructor(annType);
            _constructorInclusions.put(annType, incl);
            return (incl != AnnotationInclusion.DONT_INCLUDE);
        }
        
        public boolean includeFieldAnnotation(Annotation ann)
        {
            Class<? extends Annotation> annType = ann.annotationType();
            if (_fieldInclusions == null) {
                _fieldInclusions = new HashMap<Class<? extends Annotation>, AnnotationInclusion>();
            } else {
                AnnotationInclusion incl = _fieldInclusions.get(annType);
                if (incl != null) {
                    return (incl != AnnotationInclusion.DONT_INCLUDE);
                }
            }
            AnnotationInclusion incl = _annotationConfig.getInclusionForField(annType);
            _fieldInclusions.put(annType, incl);
            return (incl != AnnotationInclusion.DONT_INCLUDE);
        }

        public boolean includeMethodAnnotation(Annotation ann)
        {
            return methodInclusion(ann) != AnnotationInclusion.DONT_INCLUDE;
        }

        public AnnotationInclusion methodInclusion(Annotation ann)
        {
            Class<? extends Annotation> annType = ann.annotationType();
            if (_methodInclusions == null) {
                _methodInclusions = new HashMap<Class<? extends Annotation>, AnnotationInclusion>();
            } else {
                AnnotationInclusion incl = _methodInclusions.get(annType);
                if (incl != null) {
                    return incl;
                }
            }
            AnnotationInclusion incl = _annotationConfig.getInclusionForMethod(annType);
            _methodInclusions.put(annType, incl);
            return incl;
        }

        public boolean includeParameterAnnotation(Annotation ann)
        {
            return parameterInclusion(ann) != AnnotationInclusion.DONT_INCLUDE;
        }

        public AnnotationInclusion parameterInclusion(Annotation ann)
        {
            Class<? extends Annotation> annType = ann.annotationType();
            if (_parameterInclusions == null) {
                _parameterInclusions = new HashMap<Class<? extends Annotation>, AnnotationInclusion>();
            } else {
                AnnotationInclusion incl = _parameterInclusions.get(annType);
                if (incl != null) {
                    return incl;
                }
            }
            AnnotationInclusion incl = _annotationConfig.getInclusionForParameter(annType);
            _parameterInclusions.put(annType, incl);
            return incl;
        }
    }
}
