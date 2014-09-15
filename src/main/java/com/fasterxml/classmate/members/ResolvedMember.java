package com.fasterxml.classmate.members;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

/**
 * Fully type-resolved equivalent of {@link RawMember}. Only members "that matter" (ones not
 * overridden, or filtered out) are resolved, since resolution process can add non-trivial
 * overhead.
 */
public abstract class ResolvedMember<T extends Member>
{
    /**
     * {@link ResolvedType} (class with generic type parameters) that declared
     * this member
     */
    protected final ResolvedType _declaringType;

    protected final Annotations _annotations;

    protected final T _member;

    protected final ResolvedType _type;

    protected final int _hashCode;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    protected ResolvedMember(ResolvedType context, Annotations ann, T member, ResolvedType type)
    {
        _declaringType = context;
        _annotations = ann;
        _member = member;
        _type = type;
        _hashCode = (_member == null ? 0 : _member.hashCode());
    }

    public void applyOverride(Annotation override)
    {
        _annotations.add(override);
    }

    public void applyOverrides(Annotations overrides)
    {
        _annotations.addAll(overrides);
    }

    public void applyDefault(Annotation override)
    {
        _annotations.addAsDefault(override);
    }

    public <A extends Annotation> A get(Class<A> cls)
    {
        return _annotations.get(cls);
    }

    public Annotations getAnnotations()
    {
        return _annotations;
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public final ResolvedType getDeclaringType() {
        return _declaringType;
    }

    /**
     * Returns type of this member; if it has one, for methods this is the
     * return type, for fields field type, and for constructors null.
     */
    public ResolvedType getType() {
        return _type;
    }
 
    /**
     * Returns JDK object that represents member.
     */
    public T getRawMember() {
        return _member;
    }

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
    
    /*
    /**********************************************************************
    /* Standard method overrides
    /**********************************************************************
     */
    
    @Override public String toString() {
        return getName();
    }
    
    /*
    /**********************************************************************
    /* Package methods
    /**********************************************************************
     */

    protected final int getModifiers() { return getRawMember().getModifiers(); }

    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    @Override public int hashCode() {
        return _hashCode;
    }

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        ResolvedMember<?> other = (ResolvedMember<?>) o;
        return (other._member == _member);
    }
    
}
