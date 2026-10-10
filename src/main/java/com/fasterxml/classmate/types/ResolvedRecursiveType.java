package com.fasterxml.classmate.types;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.members.RawConstructor;
import com.fasterxml.classmate.members.RawField;
import com.fasterxml.classmate.members.RawMethod;

/**
 * Specialized type placeholder used in cases where type definition is
 * recursive; to avoid infinite loop, reference that would be "back" in
 * hierarchy is represented by an instance of this class.
 * Underlying information is achievable (for full resolution), but
 * not exposed using super type (parent) accessors; and has special
 * handling when used for constructing descriptions.
 */
public class ResolvedRecursiveType extends ResolvedType
{
    /**
     * Actual fully resolved type; assigned once resolution is complete
     */
    protected ResolvedType _referencedType;

    /**
     * For self-references with type bindings different from those of the referenced
     * type (like raw {@code Mid} within {@code Mid<String>}, or {@code N<N<T>>} within
     * {@code N<T>}): supplier of the actual type this self-reference represents.
     * Resolved lazily since doing so eagerly could lead to infinite recursion.
     *
     * @since 1.8
     */
    protected Supplier<ResolvedType> _actualTypeSupplier;

    /**
     * Actual type this self-reference represents, if differs from referenced type;
     * resolved lazily using {@link #_actualTypeSupplier}.
     *
     * @since 1.8
     */
    protected volatile ResolvedType _actualType;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    public ResolvedRecursiveType(Class<?> erased, TypeBindings bindings)
    {
        super(erased, bindings);
    }
    
    @Override
    public boolean canCreateSubtypes() {
        // only depends on erased type, so no need to resolve actual type
        return _referencedType.canCreateSubtypes();
    }
    
    public void setReference(ResolvedType ref)
    {
        setReference(ref, null);
    }

    /**
     * Alternative to {@link #setReference(ResolvedType)} used when type bindings of
     * this self-reference differ from those of the referenced type: in that case,
     * {@link #getActualType()} returns type obtained (lazily) from given supplier
     * (or referenced type, if equal).
     *
     * @since 1.8
     */
    public synchronized void setReference(ResolvedType ref, Supplier<ResolvedType> actualType)
    {
        // sanity check; should not be called multiple times
        if (_referencedType != null) {
            throw new IllegalStateException("Trying to re-set self reference; old value = "+_referencedType+", new = "+ref);
        }
        _referencedType = ref;
        _actualTypeSupplier = actualType;
    }

    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */
    
    /**
     * To avoid infinite loops, will return null;
     */
    @Override
    public ResolvedType getParentClass() {
        return null;
    }

    /**
     * Accessor for the type being resolved that this self-reference points to.
     * Note that type bindings of this self-reference may differ from those of the
     * referenced type (like for raw {@code Mid} within {@code Mid<String>}): if so,
     * {@link #getActualType()} returns the type self-reference actually represents.
     */
    @Override
    public ResolvedType getSelfReferencedType() { return _referencedType; }

    /**
     * Accessor for the type this self-reference represents: same as
     * {@link #getSelfReferencedType()}, unless type bindings of this self-reference
     * differ from those of the referenced type (like raw {@code Mid} within
     * {@code Mid<String>}, or {@code N<N<T>>} within {@code N<T>}), in which case
     * it is the type with bindings of this self-reference (resolved lazily).
     *<p>
     * NOTE: for expanding types (like {@code N<T>} above), following actual types
     * repeatedly yields ever deeper types.
     *
     * @return Type this self-reference represents; {@code null} if not yet resolved
     *
     * @since 1.8
     */
    public ResolvedType getActualType()
    {
        ResolvedType actual = _actualType;
        if (actual != null) {
            return actual;
        }
        final Supplier<ResolvedType> supplier;
        synchronized (this) {
            if (_actualType != null) {
                return _actualType;
            }
            supplier = _actualTypeSupplier;
            if (supplier == null) {
                return _referencedType;
            }
        }
        // resolved outside of lock, since resolution may access other self-references;
        // concurrent calls may resolve more than once, but only first one is retained
        actual = supplier.get();
        // retain identity if equal
        if (actual.equals(_referencedType)) {
            actual = _referencedType;
        }
        synchronized (this) {
            if (_actualType == null) {
                _actualType = actual;
                // no longer needed (and may hold on to `TypeResolver`)
                _actualTypeSupplier = null;
            }
            return _actualType;
        }
    }
    
    /**
     * To avoid infinite loops, will return empty list
     */
    @Override
    public List<ResolvedType> getImplementedInterfaces() {
        return Collections.<ResolvedType>emptyList();
    }
    
    /**
     * To avoid infinite loops, will return null type
     */
    @Override
    public ResolvedType getArrayElementType() { // interfaces are never arrays, so:
        return null;
    }

    /*
    /**********************************************************************
    /* Simple property accessors
    /**********************************************************************
     */

    @Override
    public boolean isInterface() { return _erasedType.isInterface(); }

    @Override
    public boolean isAbstract() { return Modifier.isAbstract(_erasedType.getModifiers()); }

    @Override
    public boolean isArray() { return _erasedType.isArray(); }

    @Override
    public boolean isPrimitive() { return false; }

    /*
    /**********************************************************************
    /* Accessors for raw (minimally procesed) members
    /**********************************************************************
     */

    @Override
    public List<RawField> getMemberFields() { return getActualType().getMemberFields(); }
    @Override
    public List<RawField> getStaticFields() { return getActualType().getStaticFields(); }
    @Override
    public List<RawMethod> getStaticMethods() { return getActualType().getStaticMethods(); }
    @Override
    public List<RawMethod> getMemberMethods() { return getActualType().getMemberMethods(); }
    @Override
    public List<RawConstructor> getConstructors() { return getActualType().getConstructors(); }
    
    /*
    /**********************************************************************
    /* String representations
    /**********************************************************************
     */

    @Override
    public StringBuilder appendSignature(StringBuilder sb) {
        // to avoid infinite recursion, only print type erased version
        return appendErasedSignature(sb);
    }

    @Override
    public StringBuilder appendErasedSignature(StringBuilder sb) {
        return _appendErasedClassSignature(sb);
    }

    @Override
    public StringBuilder appendBriefDescription(StringBuilder sb) {
        return _appendClassDescription(sb);
    }

    @Override
    public StringBuilder appendFullDescription(StringBuilder sb)
    {
        // should never get called, but just in case, only print brief description
        return appendBriefDescription(sb);
    }

    /*
    /**********************************************************************
    /* Other overrides
    /**********************************************************************
     */

    // 02-Jan-2026: [classmate#117]: Do NOT compare _referencedType to avoid infinite
    // recursion: super.equals() already compares class type, erased type, and type
    // bindings,  which is sufficient for determining equality of recursive types.
    // Comparing _referencedType causes StackOverflowError when comparing types
    // from different TypeResolver instances.
    /*
    @Override
    public boolean equals(Object o)
    {
        return super.equals(o);
    }
    */

    // Only for compliance purposes: lgtm.com complains if only equals overridden
    // 02-Jan-2026, tatu: No longer, base impl is fine
    // @Override public int hashCode() { return super.hashCode(); }
}
