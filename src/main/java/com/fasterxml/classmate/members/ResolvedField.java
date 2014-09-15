package com.fasterxml.classmate.members;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

public final class ResolvedField extends ResolvedMember<Field>
     implements Comparable<ResolvedField>
{
    public ResolvedField(ResolvedType context, Annotations ann,
            Field field, ResolvedType type)
    {
        super(context, ann, field, type);
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public boolean isTransient() {
        return Modifier.isTransient(getModifiers());
    }

    public boolean isVolatile() {
        return Modifier.isVolatile(getModifiers());
    }

    @Override
    public int compareTo(ResolvedField other) {
         return getName().compareTo(other.getName());
    }
}
