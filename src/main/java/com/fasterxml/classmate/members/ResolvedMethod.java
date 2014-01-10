package com.fasterxml.classmate.members;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

public final class ResolvedMethod extends ResolvedMember
{
    protected final Method _method;

    protected final ResolvedType _returnType;

    protected final ResolvedType[] _argumentTypes;

    protected final Annotations[] _argumentAnnotations;

    protected final int _hashCode;
    
    public ResolvedMethod(ResolvedType context, Annotations ann, Method method,
            ResolvedType returnType, ResolvedType[] argumentTypes)
    {
        super(context, ann);
        _method = method;
        _returnType = returnType;
        _argumentTypes = (argumentTypes == null ? ResolvedType.NO_TYPES : argumentTypes);
        _argumentAnnotations = new Annotations[_argumentTypes.length];
        _hashCode = (_method == null ? 0 : _method.hashCode());
    }

    public void applyArgumentOverride(int index, Annotation override)
    {
        if (index >= _argumentAnnotations.length)
            return;

        getParameterAnnotations(index).add(override);
    }

    public void applyArgumentOverrides(int index, Annotations overrides)
    {
        if (index >= _argumentAnnotations.length)
            return;

        getParameterAnnotations(index).addAll(overrides);
    }

    public void applyArgumentDefault(int index, Annotation override)
    {
        if (index >= _argumentAnnotations.length)
            return;

        getParameterAnnotations(index).addAsDefault(override);
    }

    private Annotations getParameterAnnotations(int index) {
        if (_argumentAnnotations[index] == null) {
            _argumentAnnotations[index] = new Annotations();
        }
        return _argumentAnnotations[index];
    }

    public <A extends Annotation> A getArgument(int index, Class<A> cls)
    {
        if (index >= _argumentAnnotations.length)
            return null;

        return _argumentAnnotations[index].get(cls);
    }

    /*
    /**********************************************************************
    /* Simple accessors from base class
    /**********************************************************************
     */

    @Override
    public Method getRawMember() {
        return _method;
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    public boolean isStrict() {
        return Modifier.isStrict(getModifiers());
    }

    public boolean isNative() {
        return Modifier.isNative(getModifiers());
    }

    public boolean isSynchronized() {
        return Modifier.isSynchronized(getModifiers());
    }

    @Override
    public ResolvedType getType() { return _returnType; }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    public ResolvedType getReturnType() { return _returnType; }

    /**
     * Returns number of arguments method takes.
     */
    public int getArgumentCount() {
        return _argumentTypes.length;
    }
    
    public ResolvedType getArgumentType(int index)
    {
        if (index < 0 || index >= _argumentTypes.length) {
            return null;
        }
        return _argumentTypes[index];
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
        ResolvedMethod other = (ResolvedMethod) o;
        return (other._method == _method);
    }

}
