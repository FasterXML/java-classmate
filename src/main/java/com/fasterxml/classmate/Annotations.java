package com.fasterxml.classmate;

import java.lang.annotation.Annotation;
import java.util.HashMap;

/**
 * Container class used for storing set of annotations resolved for types (classes)
 * as members (methods, fields, constructors).
 * 
 * @author tatu
 */
public class Annotations
{
    protected HashMap<Class<? extends Annotation>,Annotation> _annotations;

    public Annotations() { }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A get(Class<A> cls)
    {
        if (_annotations == null) {
            return null;
        }
        return (A) _annotations.get(cls);
    }

    public int size() {
        return (_annotations == null) ? 0 : _annotations.size();
    }

    /**
     * Method called to add specified annotation in the Map.
     */
    public void add(Annotation ann)
    {
        if (_annotations == null) {
            _annotations = new HashMap<Class<? extends Annotation>,Annotation>();
        }
        _annotations.put(ann.annotationType(), ann);
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

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

}
