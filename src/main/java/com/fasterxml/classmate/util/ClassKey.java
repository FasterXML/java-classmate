package com.fasterxml.classmate.util;

import java.io.Serializable;

/**
 * Helper class used as key when we need efficient Class-to-value lookups.
 */
@SuppressWarnings("serial")
public class ClassKey
    implements Comparable<ClassKey>, Serializable
{
    private final String _className;

    private final Class<?> _class;

    /**
     * Let's cache hash code straight away, since we are almost certain to need it.
     */
    private final int _hashCode;

    public ClassKey(Class<?> clz)
    {
        _class = clz;
        _className = clz.getName();
        _hashCode = _className.hashCode();
    }

    /*
    /**********************************************************************
    /* Comparable
    /**********************************************************************
     */

    @Override
    public int compareTo(ClassKey other)
    {
        // Just need to sort by name, ok to collide (unless used in TreeMap/Set!)
        return _className.compareTo(other._className);
    }

    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;
        ClassKey other = (ClassKey) o;

        /* Is it possible to have different Class object for same name + class loader combo?
         * Let's assume answer is no: if this is wrong, will need to uncomment following functionality
         */
        /*
        return (other._className.equals(_className))
            && (other._class.getClassLoader() == _class.getClassLoader());
        */
        return other._class == _class;
    }

    @Override public int hashCode() { return _hashCode; }

    @Override public String toString() { return _className; }
    
}
