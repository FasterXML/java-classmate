package com.fasterxml.classmate.members;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.ResolvedType;

/**
 * Base class for all "raw" member (field, method, constructor) types; raw means that
 * actual types are not yet resolved, but relationship to declaring type is
 * retained for eventual resolution.
 * Instances are typically created by {@link com.fasterxml.classmate.ResolvedType}
 * when requested, and form the input to eventual full flattening of type members.
 */
public abstract class RawMember
{
    /**
     * {@link ResolvedType} (class with generic type parameters) that declared
     * this member
     */
    protected final ResolvedType _declaringType;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    protected RawMember(ResolvedType context)
    {
        _declaringType = context;
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public final ResolvedType getDeclaringType() {
        return _declaringType;
    }
    
    public abstract Member getRawMember();

    public String getName() {
        return getRawMember().getName();
    }

    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    public boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(getModifiers());
    }

    public boolean isProtected() {
        return Modifier.isProtected(getModifiers());
    }

    public boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    public Annotation[] getAnnotations() {
        return ((AnnotatedElement) getRawMember()).getAnnotations();
    }
    
    /*
    /**********************************************************************
    /* Standard method overrides
    /**********************************************************************
     */

    // make abstract to force implementation by sub-class
    @Override public abstract boolean equals(Object o);

    @Override public abstract int hashCode();

    @Override public String toString() {
        return getName();
    }
    
    /*
    /**********************************************************************
    /* Package methods
    /**********************************************************************
     */

    protected final int getModifiers() { return getRawMember().getModifiers(); }
}
