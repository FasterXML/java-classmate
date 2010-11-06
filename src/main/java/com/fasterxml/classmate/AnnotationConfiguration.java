package com.fasterxml.classmate;

import java.lang.annotation.Annotation;
import java.util.*;

import com.fasterxml.classmate.util.ClassKey;

/**
 * Interface for object that determines 
 * 
 * @author tsaloranta
 */
public abstract class AnnotationConfiguration
{
    /**
     * Enumeration that defines different settings for handling behavior
     * of individual annotations
     */
    public enum Inclusion
    {
        /**
         * Value that indicates that annotation is to be ignored, not included
         * in resolved bean information.
         */
        DONT_INCLUDE,

        /**
         * Value that indicates that annotation is to be included in results, but
         * only if directly associated with included member (or attached mix-in);
         * will not inherit from supertypes.
         */
        INCLUDE_BUT_DONT_INHERIT,

        /**
         * Value that indicates that annotation is to be included in results; and
         * values from overridden members are also inherited if not overridden
         * by members of subtypes.
         */
        INCLUDE_AND_INHERIT
        ;
    }

    /**
     * Method called to figure out how to handle instances of specified annotation
     * type.
     */
    public abstract Inclusion getInclusion(Class<Annotation> annotationType);

    /**
     * Simple implementation that can be configured with default behavior
     * for unknown annotations, as well as explicit behaviors for
     * enumerated annotation types.
     */
    public static class StdConfiguration extends AnnotationConfiguration
    {
        protected final Inclusion _defaultInclusion;

        protected HashMap<ClassKey,Inclusion> _inclusions = new HashMap<ClassKey,Inclusion>();
        
        public StdConfiguration(Inclusion defaultBehavior)
        {
            _defaultInclusion = defaultBehavior;
        }
        
        @Override
        public Inclusion getInclusion(Class<Annotation> annotationType)
        {
            ClassKey key = new ClassKey(annotationType);
            Inclusion beh = _inclusions.get(key);
            return (beh == null) ? _defaultInclusion : beh;
        }

        public void setInclusion(Class<Annotation> annotationType, Inclusion incl)
        {
            _inclusions.put(new ClassKey(annotationType), incl);
        }
    }
}
