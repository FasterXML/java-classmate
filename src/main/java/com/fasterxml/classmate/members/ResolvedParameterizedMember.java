package com.fasterxml.classmate.members;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

/**
 * Base type for resolved members that take some parameters (e.g. methods and constructors).
 */
public abstract class ResolvedParameterizedMember<T extends Member> extends ResolvedMember<T> {

    protected final ResolvedType[] _paramTypes;

    protected final Annotations[] _paramAnnotations;

    protected ResolvedParameterizedMember(ResolvedType context, Annotations ann,
                                          T member, ResolvedType type, ResolvedType[] argumentTypes) {
        super(context, ann, member, type);
        _paramTypes = argumentTypes == null ? ResolvedType.NO_TYPES : argumentTypes;
        _paramAnnotations = new Annotations[_paramTypes.length];
    }

    public Annotations getParameterAnnotations(int index) {
        if (index >= _paramTypes.length)
            throw new IndexOutOfBoundsException("No parameter at index " + index + ", this is greater than the total number of parameters");

        if (_paramAnnotations[index] == null) {
            _paramAnnotations[index] = new Annotations();
        }
        return _paramAnnotations[index];
    }

    public void applyParamOverride(int index, Annotation override)
    {
        if (index >= _paramAnnotations.length)
            return;

        getParameterAnnotations(index).add(override);
    }

    public void applyParamOverrides(int index, Annotations overrides)
    {
        if (index >= _paramAnnotations.length)
            return;

        getParameterAnnotations(index).addAll(overrides);
    }

    public void applyParamDefault(int index, Annotation defaultValue)
    {
        if (index >= _paramAnnotations.length)
            return;

        getParameterAnnotations(index).addAsDefault(defaultValue);
    }

    public <A extends Annotation> A getParam(int index, Class<A> cls)
    {
        if (index >= _paramAnnotations.length)
            return null;

        return _paramAnnotations[index].get(cls);
    }

    /**
     * Returns number of arguments method takes.
     */
    public int getArgumentCount() {
        return _paramTypes.length;
    }

    public ResolvedType getArgumentType(int index)
    {
        if (index < 0 || index >= _paramTypes.length) {
            return null;
        }
        return _paramTypes[index];
    }
}
