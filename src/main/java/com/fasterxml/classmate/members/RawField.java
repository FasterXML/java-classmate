package com.fasterxml.classmate.members;

import java.lang.reflect.Field;

import com.fasterxml.classmate.ResolvedType;

public final class RawField extends RawMember
{
    protected final Field _field;

    public RawField(ResolvedType context, Field field)
    {
        super(context);
        _field = field;
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public Field getRawMember() {
        return _field;
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
}
