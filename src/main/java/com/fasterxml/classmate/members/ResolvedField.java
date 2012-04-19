package com.fasterxml.classmate.members;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

public final class ResolvedField extends ResolvedMember
{
    protected final Field _field;

    protected final ResolvedType _type;

    protected final int _hashCode;
    
    public ResolvedField(ResolvedType context, Annotations ann,
            Field field, ResolvedType type)
    {
        super(context, ann);
        _field = field;
        _type = type;
        _hashCode = (_field == null ? 0 : _field.hashCode());
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public Field getRawMember() { return _field; }
    public ResolvedType getType() { return _type; }

    public boolean isTransient() {
        return Modifier.isTransient(getModifiers());
    }

    public boolean isVolatile() {
        return Modifier.isVolatile(getModifiers());
    }
    
    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    @Override public int hashCode()
    {
        return _hashCode;
    }

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        ResolvedField other = (ResolvedField) o;
        return (other._field == _field);
    }
}
