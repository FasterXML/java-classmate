package com.fasterxml.classmate;

import java.io.Serializable;
import java.util.*;

import com.fasterxml.classmate.util.ClassKey;

/**
 * Interface for object that can provide mix-ins to override annotations.
 */
@SuppressWarnings("serial")
public abstract class AnnotationOverrides implements Serializable
{
    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    /**
     * Method called to find out which class(es) are to be used as source
     * for annotations to mix in for given type.
     * 
     * @return List of mix-in sources (starting with  highest priority); 
     *   can be null or empty list if no mix-ins are to be used.
     */
    public List<Class<?>> mixInsFor(Class<?> beanClass) {
        return mixInsFor(new ClassKey(beanClass));
    }

    public abstract List<Class<?>> mixInsFor(ClassKey beanClass);

    /**
     * Method for constructing builder for creating simple overrides provider
     * that just uses direct assignments (target-to-override classes)
     */
    public static StdBuilder builder() {
        return new StdBuilder();
    }

    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */
    
    /**
     * To make it easy to use simple override implementation (where overrides
     * are direct and explicit), here is a build that allow constructing
     * such override instance.
     */
    public static class StdBuilder
    {
        protected final HashMap<ClassKey,List<Class<?>>> _targetsToOverrides = new HashMap<ClassKey,List<Class<?>>>();

        public StdBuilder() { }

        public StdBuilder add(Class<?> target, Class<?> mixin) {
            return add(new ClassKey(target), mixin);
        }

        public StdBuilder add(ClassKey target, Class<?> mixin)
        {
            List<Class<?>> mixins = _targetsToOverrides.get(target);
            if (mixins == null) {
                mixins = new ArrayList<Class<?>>();
                _targetsToOverrides.put(target, mixins);
            }
            mixins.add(mixin);
            return this;
        }
        
        /**
         * Method that will construct a {@link AnnotationOverrides} instance using
         * mappings that have been added using this builder
         */
        public AnnotationOverrides build() {
            return new StdImpl(_targetsToOverrides);
        }
    }
    
    /**
     * Simple implementation configured with explicit associations with
     * target class as key, and overrides as ordered list of classes
     * (with first entry having precedence over later ones).
     */
    public static class StdImpl extends AnnotationOverrides
    {
        protected final HashMap<ClassKey,List<Class<?>>> _targetsToOverrides;
        
        public StdImpl(HashMap<ClassKey,List<Class<?>>> overrides) {
            _targetsToOverrides = new HashMap<ClassKey,List<Class<?>>>(overrides);
        }

        @Override
        public List<Class<?>> mixInsFor(ClassKey target) {
            return _targetsToOverrides.get(target);
        }
    }
}
