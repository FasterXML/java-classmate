package com.fasterxml.classmate.util;

import java.util.ArrayList;
import java.util.IdentityHashMap;

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
     * Number of frames above this frame (0 for root)
     *
     * @since 1.8
     */
    private final int _depth;

    /**
     * Depth of the shallowest frame that self-references (created during resolution
     * of the type this frame represents, including types it contains) point to;
     * {@code Integer.MAX_VALUE} if none.
     *
     * @since 1.8
     */
    private int _minRefDepth = Integer.MAX_VALUE;

    /**
     * Whether type parameters of the type this frame represents are being resolved
     * to their bounds (to avoid infinite recursion for raw self-references)
     *
     * @since 1.8
     */
    private boolean _resolvingBounds;

    /**
     * Types (shared by all frames, so only used via root) that contain self-references
     * to types still being resolved, mapped to depth of the shallowest such frame:
     * such types are only valid within their resolution context.
     *
     * @since 1.8
     */
    private IdentityHashMap<ResolvedType, Integer> _incompleteTypes;

    public ClassStack(Class<?> rootType) {
        this(null, rootType);
    }

    private ClassStack(ClassStack parent, Class<?> curr) {
        _parent = parent;
        _current = curr;
        _root = (parent == null) ? this : parent._root;
        _depth = (parent == null) ? 0 : (parent._depth + 1);
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
     * by given (enclosing) frame has been created.
     *
     * @since 1.8
     */
    public void selfReferenceCreated(ResolvedRecursiveType ref, ClassStack target)
    {
        target.addSelfReference(ref);
        _addIncomplete(ref, target._depth);
    }

    /**
     * Method called (on the innermost frame) when given type, containing given element
     * type (like array element type), has been constructed: if element type is
     * incomplete, so is the type.
     *
     * @return True if type is complete (may be cached); false if not
     *
     * @since 1.8
     */
    public boolean containerConstructed(ResolvedType type, ResolvedType elementType)
    {
        int depth = _incompleteDepth(elementType);
        if (depth == Integer.MAX_VALUE) {
            return true;
        }
        _addIncomplete(type, depth);
        return false;
    }

    /**
     * Method called when type that this stack frame represents has been
     * constructed (but not yet cached), with given type parameters.
     * Completes self-references to the type (see {@link #resolveSelfReferences}).
     *
     * @return True if type is complete (may be cached): that is, it does not contain
     *    self-references to types still being resolved (other than itself); false if not
     *
     * @since 1.8
     */
    public boolean typeConstructed(ResolvedType type, ResolvedType[] typeParams)
    {
        resolveSelfReferences(type);
        // type parameters were resolved in context of the parent frame, so the parent
        // is already aware of their self-references (if any)
        int minDepth = _minRefDepth;
        for (ResolvedType param : typeParams) {
            minDepth = Math.min(minDepth, _incompleteDepth(param));
        }
        if (_parent != null) {
            _parent._minRefDepth = Math.min(_parent._minRefDepth, _minRefDepth);
        }
        if (minDepth < _depth) {
            _root._incompleteTypes.put(type, minDepth);
            return false;
        }
        return true;
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

    private void _addIncomplete(ResolvedType type, int depth)
    {
        _minRefDepth = Math.min(_minRefDepth, depth);
        if (_root._incompleteTypes == null) {
            _root._incompleteTypes = new IdentityHashMap<ResolvedType, Integer>();
        }
        _root._incompleteTypes.put(type, depth);
    }

    private int _incompleteDepth(ResolvedType type)
    {
        if (_root._incompleteTypes != null) {
            Integer depth = _root._incompleteTypes.get(type);
            if (depth != null) {
                return depth.intValue();
            }
        }
        return Integer.MAX_VALUE;
    }
}
