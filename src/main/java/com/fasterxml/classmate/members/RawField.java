package com.fasterxml.classmate.members;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.ResolvedType;

public final class RawField extends RawMember
{
    protected final Field _field;

    private final int _hashCode;

    public RawField(ResolvedType context, Field field)
    {
        super(context);
        _field = field;
        _hashCode = (_field == null ? 0 : _field.hashCode());
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    @Override
    public Field getRawMember() {
        return _field;
    }

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

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        RawField other = (RawField) o;
        return (other._field == _field);
    }

    @Override public int hashCode()
    {
        return _hashCode;
    }
}
