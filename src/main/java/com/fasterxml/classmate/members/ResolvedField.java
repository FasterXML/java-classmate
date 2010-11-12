package com.fasterxml.classmate.members;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

import com.fasterxml.classmate.TypeResolver;

public class ResolvedField extends ResolvedMember
{
    protected final Field _field;

    protected final ResolvedType _type;

    protected final Annotations _annotations;
    
    private ResolvedField(ResolvedType context, Field field, ResolvedType type,
            Annotations annotations)
    {
        super(context);
        _field = field;
        _type = type;
        _annotations = annotations;
    }

    public static ResolvedField construct(RawField field, TypeResolver typeResolver,
            Annotations annotations)
    {
        ResolvedType context = field.getDeclaringType();
        Field rawField = field.getRawMember();
        ResolvedType type = typeResolver.resolve(rawField.getGenericType(), context.getTypeBindings());
        // Ok, after basics, 
        return new ResolvedField(context, rawField, type, annotations);
    }

    public void addAnnotation(Annotation ann) {
        _annotations.add(ann);
    }
    
    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public Field getRawMember() { return _field; }
    public ResolvedType getType() { return _type; }
    
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
