package com.fasterxml.classmate.util;

import java.util.ArrayList;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedRecursiveType;

/**
 * Simple helper class used to keep track of 'call stack' for classes being referenced
 * (as well as unbound variables)
 */
public final class ClassStack
{
    private final ClassStack _parent;
    private final Class<?> _current;

    private ArrayList<ResolvedRecursiveType> _selfRefs;

    public ClassStack(Class<?> rootType) {
        this(null, rootType);
    }

    private ClassStack(ClassStack parent, Class<?> curr) {
        _parent = parent;
        _current = curr;
    }

    /**
     * @return New stack frame, if addition is ok; null if not
     */
    public ClassStack child(Class<?> cls)
    {
        return new ClassStack(this, cls);
    }

    /**
     * Method called to indicate that there is a self-reference from
     * deeper down in stack pointing into type this stack frame represents.
     */
    public void addSelfReference(ResolvedRecursiveType ref)
    {
        if (_selfRefs == null) {
            _selfRefs = new ArrayList<ResolvedRecursiveType>();
        }
        _selfRefs.add(ref);
    }

    /**
     * Method called when type that this stack frame represents is
     * fully resolved, allowing self-references to be completed
     * (if there are any)
     */
    public void resolveSelfReferences(ResolvedType resolved)
    {
        if (_selfRefs != null) {
            for (ResolvedRecursiveType ref : _selfRefs) {
                ref.setReference(resolved);
            }
        }
    }

    public ClassStack find(Class<?> cls)
    {
        if (_current == cls) return this;
        if (_parent != null) {
            return _parent.find(cls);
        }
        return null;
    }
}