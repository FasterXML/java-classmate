package com.fasterxml.classmate.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.types.ResolvedRecursiveType;

/**
 * Simple helper class used to keep track of 'call stack' for classes being referenced
 * (as well as unbound variables)
 */
public final class ClassStack
{
    protected final ClassStack _parent;
    protected final Class<?> _current;

    private ArrayList<ResolvedRecursiveType> _selfRefs;

    /**
     * Root of the stack, used for storing state shared by all frames.
     *
     * @since 1.8
     */
    private final ClassStack _root;

    /**
     * Whether types constructed within this frame (as part of the type it represents)
     * contain self-references to types outside of it (that is, to types represented by
     * enclosing frames): if so, the type is only valid within this resolution, so it
     * must not be cached. Never true for the root frame.
     *
     * @since 1.8
     */
    private boolean _hasOuterReferences;

    /**
     * Whether type parameters of the type this frame represents are being resolved
     * to their bounds (to avoid infinite recursion for raw self-references)
     *
     * @since 1.8
     */
    private boolean _resolvingBounds;

    /**
     * Incomplete types (see {@link ResolvedType}) constructed during this resolution,
     * by key (shared by all frames, so only used via root): reused within this resolution
     * (since self-references they contain are all resolved by its end), but not cached.
     *
     * @since 1.8
     */
    private HashMap<ResolvedTypeKey, ResolvedType> _incompleteTypes;

    public ClassStack(Class<?> rootType) {
        this(null, rootType);
    }

    private ClassStack(ClassStack parent, Class<?> curr) {
        _parent = parent;
        _current = curr;
        _root = (parent == null) ? this : parent._root;
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
     *
     * @deprecated Since 1.8 use {@link #resolveSelfReferences(ResolvedType, Function)}
     */
    @Deprecated
    public void resolveSelfReferences(ResolvedType resolved)
    {
        resolveSelfReferences(resolved, null);
    }

    /**
     * Method called when type that this stack frame represents is
     * fully resolved, allowing self-references to be completed
     * (if there are any). Self-references that represent a type different from the
     * resolved one (like ones with different type bindings) get the actual type they
     * represent from supplier given by function (null if self-reference represents the
     * resolved type), if any (see {@link ResolvedRecursiveType#setReference(ResolvedType, Supplier)}).
     *
     * @since 1.8
     */
    public void resolveSelfReferences(ResolvedType resolved,
            Function<ResolvedRecursiveType, Supplier<ResolvedType>> actualTypes)
    {
        if (_selfRefs != null) {
            for (ResolvedRecursiveType ref : _selfRefs) {
                ref.setReference(resolved, (actualTypes == null) ? null : actualTypes.apply(ref));
            }
        }
    }

    /**
     * @return True if there are self-references to the type this frame represents
     *    (see {@link #addSelfReference})
     *
     * @since 1.8
     */
    public boolean hasSelfReferences() {
        return (_selfRefs != null);
    }

    public ClassStack find(Class<?> cls)
    {
        if (_current == cls) return this;
        for (ClassStack curr = _parent; curr != null; curr = curr._parent) {
            if (curr._current == cls) {
                return curr;
            }
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Tracking of incomplete types [classmate#128]
    /**********************************************************************
     */

    /**
     * Method called (on the innermost frame) when a self-reference to type represented
     * by given (enclosing) frame has been created: types represented by frames in between
     * (including this one) then contain self-references to types outside of them.
     *
     * @since 1.8
     */
    public void selfReferenceCreated(ResolvedRecursiveType ref, ClassStack target)
    {
        target.addSelfReference(ref);
        for (ClassStack frame = this; frame != target; frame = frame._parent) {
            frame._hasOuterReferences = true;
        }
    }

    /**
     * @return True if types constructed within this frame contain self-references
     *    to types outside of it (see {@link #selfReferenceCreated})
     *
     * @since 1.8
     */
    public boolean hasOuterReferences() {
        return _hasOuterReferences;
    }

    /**
     * Method for finding incomplete type with given key constructed earlier during this
     * resolution, if any. Since it may contain self-references to any types being
     * resolved, types represented by this frame and enclosing frames (except for the
     * root) are considered to contain self-references to types outside of them.
     *
     * @return Incomplete type to reuse, if any; {@code null} if none
     *
     * @since 1.8
     */
    public ResolvedType findIncomplete(ResolvedTypeKey key)
    {
        if ((key == null) || (_root._incompleteTypes == null)) {
            return null;
        }
        ResolvedType type = _root._incompleteTypes.get(key);
        if (type != null) {
            for (ClassStack frame = this; frame != _root; frame = frame._parent) {
                frame._hasOuterReferences = true;
            }
        }
        return type;
    }

    /**
     * Method for registering incomplete type constructed during this resolution,
     * so that it may be reused (see {@link #findIncomplete}).
     *
     * @since 1.8
     */
    public void addIncomplete(ResolvedTypeKey key, ResolvedType type)
    {
        if (key != null) {
            if (_root._incompleteTypes == null) {
                _root._incompleteTypes = new HashMap<ResolvedTypeKey, ResolvedType>();
            }
            _root._incompleteTypes.put(key, type);
        }
    }

    /**
     * Accessor for checking whether type parameters of the type this frame represents
     * are being resolved to their bounds.
     *
     * @since 1.8
     */
    public boolean isResolvingBounds() {
        return _resolvingBounds;
    }

    /**
     * @since 1.8
     */
    public void setResolvingBounds(boolean state) {
        _resolvingBounds = state;
    }
}
