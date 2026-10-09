package com.fasterxml.classmate.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
     * Number of frames above this frame (0 for root)
     *
     * @since 1.8
     */
    private final int _depth;

    /**
     * Depth of the shallowest frame that types constructed within this frame (that is,
     * as part of the type this frame represents) contain self-references to (directly,
     * or via other incomplete types); {@code Integer.MAX_VALUE} if none, and -1 if
     * they contain incomplete types from outside of this resolution.
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
     * Whether the type this frame represents has been constructed (and frame is no
     * longer on the stack).
     *
     * @since 1.8
     */
    private boolean _completed;

    /**
     * For completed frames with incomplete type: shallowest (enclosing) frame that the
     * type contains self-references to; {@code null} if type is complete, or contains
     * incomplete types from outside of this resolution.
     *
     * @since 1.8
     */
    private ClassStack _dependency;

    /**
     * Incomplete types (see {@link ResolvedType}) constructed during this resolution
     * (shared by all frames, so only used via root), mapped to the shallowest frame
     * they contain self-references to (when constructed).
     *
     * @since 1.8
     */
    private IdentityHashMap<ResolvedType, ClassStack> _incompleteTypes;

    /**
     * Incomplete types constructed during this resolution by key (shared by all frames,
     * so only used via root): may be reused (but not cached) within this resolution, as
     * long as frames they contain self-references to are still being resolved.
     *
     * @since 1.8
     */
    private HashMap<ResolvedTypeKey, ResolvedType> _incompleteByKey;

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
        resolveSelfReferences(resolved, null);
    }

    /**
     * Method called when type that this stack frame represents is
     * fully resolved, allowing self-references to be completed
     * (if there are any). Self-references with type bindings different from those
     * of the resolved type get the actual type they represent from given function,
     * if any (see {@link ResolvedRecursiveType#setReference(ResolvedType, Supplier)}).
     *
     * @since 1.8
     */
    public void resolveSelfReferences(ResolvedType resolved,
            Function<ResolvedRecursiveType, ResolvedType> actualTypeResolver)
    {
        if (_selfRefs != null) {
            for (final ResolvedRecursiveType ref : _selfRefs) {
                if ((actualTypeResolver == null)
                        || ref.getTypeBindings().equals(resolved.getTypeBindings())) {
                    ref.setReference(resolved);
                } else {
                    ref.setReference(resolved, () -> actualTypeResolver.apply(ref));
                }
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
        _dependsOn(target._depth);
        _root._addIncomplete(null, ref, target);
    }

    /**
     * Method called (on the innermost frame) when given incomplete type, containing
     * other incomplete type with given depth (see {@link #incompleteDepth}; like array
     * element type), has been constructed.
     *
     * @since 1.8
     */
    public void containerConstructed(ResolvedTypeKey key, ResolvedType type, int depth)
    {
        _dependsOn(depth);
        _root._addIncomplete(key, type, _frameAt(depth));
    }

    /**
     * Method called when type that this stack frame represents has been
     * constructed (but not yet cached), with given depth for its type parameters
     * (see {@link #incompleteDepth}). Note that self-references to the type need
     * to be completed separately (see {@link #resolveSelfReferences}).
     *
     * @return True if type is complete (may be cached): that is, it does not contain
     *    self-references to types still being resolved (other than itself); false if
     *    not (in which case it is registered as incomplete type with given key)
     *
     * @since 1.8
     */
    public boolean typeConstructed(ResolvedTypeKey key, ResolvedType type, int paramDepth)
    {
        _completed = true;
        final int minDepth = Math.min(_minRefDepth, paramDepth);
        if (minDepth >= _depth) {
            return true;
        }
        if (_parent != null) {
            _parent._dependsOn(minDepth);
        }
        _dependency = _frameAt(minDepth);
        _root._addIncomplete(key, type, _dependency);
        return false;
    }

    /**
     * Method for finding incomplete type with given key, constructed earlier during
     * this resolution, if it may be reused (in the type this frame represents): that is,
     * frames it contains self-references to are still being resolved.
     *
     * @return Incomplete type to reuse, if any; {@code null} if none
     *
     * @since 1.8
     */
    public ResolvedType findIncomplete(ResolvedTypeKey key)
    {
        if ((key == null) || (_root._incompleteByKey == null)) {
            return null;
        }
        ResolvedType type = _root._incompleteByKey.get(key);
        if (type != null) {
            int depth = incompleteDepth(type);
            if (depth >= 0) {
                _dependsOn(depth);
                return type;
            }
        }
        return null;
    }

    /**
     * Accessor for finding depth of the shallowest frame still being resolved that
     * given incomplete type contains self-references to (directly, or via types
     * already completed).
     *
     * @return Depth of the frame; or -1 if there is no such frame (type was not
     *    constructed during this resolution, or only contains self-references to
     *    completed types), in which case type is not valid within this frame
     *
     * @since 1.8
     */
    public int incompleteDepth(ResolvedType type)
    {
        ClassStack frame = (_root._incompleteTypes == null) ? null
                : _root._incompleteTypes.get(type);
        while ((frame != null) && frame._completed) {
            frame = frame._dependency;
        }
        return (frame == null) ? -1 : frame._depth;
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

    private void _dependsOn(int depth) {
        _minRefDepth = Math.min(_minRefDepth, depth);
    }

    /**
     * @return Frame with given depth (this frame or one of its ancestors); {@code null}
     *    for negative depth
     */
    private ClassStack _frameAt(int depth)
    {
        ClassStack frame = (depth < 0) ? null : this;
        while ((frame != null) && (frame._depth > depth)) {
            frame = frame._parent;
        }
        return frame;
    }

    private void _addIncomplete(ResolvedTypeKey key, ResolvedType type, ClassStack dependency)
    {
        if (_incompleteTypes == null) {
            _incompleteTypes = new IdentityHashMap<ResolvedType, ClassStack>();
            _incompleteByKey = new HashMap<ResolvedTypeKey, ResolvedType>();
        }
        _incompleteTypes.put(type, dependency);
        if (key != null) {
            _incompleteByKey.put(key, type);
        }
    }
}
