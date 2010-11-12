package com.fasterxml.classmate;

import java.util.*;

import com.fasterxml.classmate.util.ClassKey;

/**
 * Interface for object that can provide mix-ins to override annotations.
 */
public abstract class AnnotationOverrides
{
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
     * Simple implementation configured with explicit associations with
     * target class as key, and overrides as ordered list of classes
     * (with first entry having precedence over later ones).
     */
    public static class StdImpl extends AnnotationOverrides
    {
        protected HashMap<ClassKey,List<Class<?>>> _targetsToMixins
            = new HashMap<ClassKey,List<Class<?>>>();
        
        public StdImpl() { }

        @Override
        public List<Class<?>> mixInsFor(ClassKey target) {
            return _targetsToMixins.get(target);
        }

        public void addMixIn(Class<?> target, Class<?> mixin) {
            addMixIn(new ClassKey(target), mixin);
        }
        
        public void addMixIn(ClassKey target, Class<?> mixin) {
            List<Class<?>> mixins = _targetsToMixins.get(target);
            if (mixins == null) {
                mixins = new ArrayList<Class<?>>();
                _targetsToMixins.put(target, mixins);
            }
            mixins.add(mixin);
        }
    }
}
