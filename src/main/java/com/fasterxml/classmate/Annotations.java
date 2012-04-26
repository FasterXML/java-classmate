package com.fasterxml.classmate;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;

/**
 * Container class used for storing set of annotations resolved for types (classes)
 * as members (methods, fields, constructors).
 * 
 * @author tatu
 */
@SuppressWarnings("serial")
public class Annotations implements Serializable
{
    protected HashMap<Class<? extends Annotation>,Annotation> _annotations;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    
    public Annotations() { }

    /**
     * Method for adding specified annotation, overriding existing value
     * for the annotation type.
     */
    public void add(Annotation override)
    {
        if (_annotations == null) {
            _annotations = new HashMap<Class<? extends Annotation>,Annotation>();
        }
        _annotations.put(override.annotationType(), override);
    }

    /**
     * Method for adding all annotations from specified set, as overrides
     * to annotations this set has
     */
    public void addAll(Annotations overrides)
    {
        if (_annotations == null) {
            _annotations = new HashMap<Class<? extends Annotation>,Annotation>();
        }
        for (Annotation override : overrides._annotations.values()) {
            _annotations.put(override.annotationType(), override);
        }
    }
    
    /**
     * Method for adding specified annotation if and only if no value
     * exists for the annotation type.
     */
    public void addAsDefault(Annotation defValue)
    {
        Class<? extends Annotation> type = defValue.annotationType();
        if (_annotations == null) {
            _annotations = new HashMap<Class<? extends Annotation>,Annotation>();
            _annotations.put(type, defValue);
        } else if (!_annotations.containsKey(type)) {
            _annotations.put(type, defValue);
        }
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */
    
    public int size() {
        return (_annotations == null) ? 0 : _annotations.size();
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A get(Class<A> cls)
    {
        if (_annotations == null) {
            return null;
        }
        return (A) _annotations.get(cls);
    }
    
    /*
    /**********************************************************************
    /* Standard method overrides
    /**********************************************************************
     */
    
    @Override public String toString()
    {
        if (_annotations == null) {
            return "[null]";
        }
        return _annotations.toString();
    }
}
