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
public abstract class ResolvedMember
{
    /**
     * {@link ResolvedType} (class with generic type parameters) that declared
     * this member
     */
    protected final ResolvedType _declaringType;

    protected final Annotations _annotations;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    protected ResolvedMember(ResolvedType context, Annotations ann)
    {
        _declaringType = context;
        _annotations = ann;
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
    public abstract ResolvedType getType();
 
    /**
     * Returns JDK object that represents member.
     */
    public abstract Member getRawMember();

    public String getName() {
        return getRawMember().getName();
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }
    
    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }
    
    /*
    /**********************************************************************
    /* Standard method overrides
    /**********************************************************************
     */
    
    @Override public int hashCode() {
        return getName().hashCode();
    }
    
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
