package com.fasterxml.classmate;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.*;

import com.fasterxml.classmate.util.ClassKey;

/**
 * Interface for object that determines handling of annotations in regards
 * to inheritance, overrides.
 */
@SuppressWarnings("serial")
public abstract class AnnotationConfiguration implements Serializable
{
    /**
     * Method called to figure out how to handle instances of specified annotation
     * type when used as class annotation.
     */
    public abstract AnnotationInclusion getInclusionForClass(Class<? extends Annotation> annotationType);

    /**
     * Method called to figure out how to handle instances of specified annotation
     * type when used as constructor annotation.
     *<p>
     * Note that constructor annotations can never be inherited so this just determines
     * between inclusion or non-inclusion.
     */
    public abstract AnnotationInclusion getInclusionForConstructor(Class<? extends Annotation> annotationType);
    
    /**
     * Method called to figure out how to handle instances of specified annotation
     * type when used as field annotation.
     *<p>
     * Note that field annotations can never be inherited so this just determines
     * between inclusion or non-inclusion.
     */
    public abstract AnnotationInclusion getInclusionForField(Class<? extends Annotation> annotationType);
    
    /**
     * Method called to figure out how to handle instances of specified annotation
     * type when used as method annotation.
     *<p>
     * Note that method annotations can be inherited for member methods, but not for static
     * methods; for static methods thereby this just determines between inclusion and
     * non-inclusion.
     */
    public abstract AnnotationInclusion getInclusionForMethod(Class<? extends Annotation> annotationType);

    /**
     * Method called to figure out how to handle instances of specified annotation
     * type when used as parameter annotation.
     *<p>
     * Note that parameter annotations can be inherited for member methods, but not for static
     * methods; for static methods thereby this just determines between inclusion and
     * non-inclusion.
     */
    public abstract AnnotationInclusion getInclusionForParameter(Class<? extends Annotation> annotationType);
    
    /**
     * Simple implementation that can be configured with default behavior
     * for unknown annotations, as well as explicit behaviors for
     * enumerated annotation types. Same default is used for both class and
     * member method annotations (constructor, field and static method
     * annotations are never inherited)
     */
    public static class StdConfiguration extends AnnotationConfiguration implements Serializable
    {
        protected final AnnotationInclusion _defaultInclusion;

        protected final HashMap<ClassKey,AnnotationInclusion> _inclusions = new HashMap<ClassKey,AnnotationInclusion>();
        
        public StdConfiguration(AnnotationInclusion defaultBehavior)
        {
            _defaultInclusion = defaultBehavior;
        }
        
        @Override
        public AnnotationInclusion getInclusionForClass(Class<? extends Annotation> annotationType) {
            return _inclusionFor(annotationType);
        }

        @Override
        public AnnotationInclusion getInclusionForConstructor(Class<? extends Annotation> annotationType) {
            return _inclusionFor(annotationType);
        }

        @Override
        public AnnotationInclusion getInclusionForField(Class<? extends Annotation> annotationType) {
            return getInclusionForClass(annotationType);
        }
        
        @Override
        public AnnotationInclusion getInclusionForMethod(Class<? extends Annotation> annotationType) {
            return getInclusionForClass(annotationType);
        }

        @Override
        public AnnotationInclusion getInclusionForParameter(Class<? extends Annotation> annotationType) {
            return getInclusionForClass(annotationType);
        }

        public void setInclusion(Class<? extends Annotation> annotationType, AnnotationInclusion incl)
        {
            _inclusions.put(new ClassKey(annotationType), incl);
        }

        protected AnnotationInclusion _inclusionFor(Class<? extends Annotation> annotationType)
        {
            ClassKey key = new ClassKey(annotationType);
            AnnotationInclusion beh = _inclusions.get(key);
            return (beh == null) ? _defaultInclusion : beh;
        }
    }
}
