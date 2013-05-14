package com.fasterxml.classmate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.classmate.members.*;

public abstract class ResolvedType
    implements Type
{
    public final static ResolvedType[] NO_TYPES = new ResolvedType[0];

    protected final static RawConstructor[] NO_CONSTRUCTORS = new RawConstructor[0];
    protected final static RawField[] NO_FIELDS = new RawField[0];
    protected final static RawMethod[] NO_METHODS = new RawMethod[0];
    
    protected final Class<?> _erasedType;

    /**
     * Type bindings active when resolving members (methods, fields,
     * constructors) of this type
     */
    protected final TypeBindings _typeBindings;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    protected ResolvedType(Class<?> cls, TypeBindings bindings)
    {
        _erasedType = cls;
        _typeBindings = (bindings == null) ? TypeBindings.emptyBindings() : bindings;
    }
    
    /**
     * Method that can be used to check if call to {@link TypeResolver#resolveSubtype(ResolvedType, Class)}
     * may ever succeed; if false, it will fail with an exception, if true, it may succeed.
     */
    public abstract boolean canCreateSubtypes();

    /**
     * Method that can be used to check if call to {@link TypeResolver#resolveSubtype(ResolvedType, Class)}
     * will succeed for specific type; if false, it will fail with an exception; if tru it
     * will succeed.
     */
    public final boolean canCreateSubtype(Class<?> subtype) {
        return canCreateSubtypes() && _erasedType.isAssignableFrom(subtype);
    }
    
    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */
    
    /**
     * Returns type-erased Class<?> that this resolved type has.
     */
    public Class<?> getErasedType() { return _erasedType; }

    /**
     * Returns parent class of this type, if it has one; primitive types
     * and interfaces have no parent class, nor does Object type
     * {@link java.lang.Object}.
     * Also, placeholders for cyclic (recursive) types return null for
     * this method.
     */
    public abstract ResolvedType getParentClass();

    /**
     * Accessor that must be used to find out actual type in
     * case of "self-reference"; case where type refers
     * recursive to itself (like, <code>T implements Comparable&lt;T></code>).
     * For all other types returns null but for self-references "real" type.
     * Separate accessor is provided to avoid accidental infinite loops.
     */
    public abstract ResolvedType getSelfReferencedType();
    
    /**
     * Method that can be used to access element type of array types; will return
     * null for non-array types, and non-null type for array types.
     */
    public abstract ResolvedType getArrayElementType();

    /**
     * Returns ordered list of interfaces (in declaration order) that this type
     * implements.
     * 
     * @return List of interfaces this type implements, if any; empty list if none
     */
    public abstract List<ResolvedType> getImplementedInterfaces();

    /**
     * Returns list of generic type declarations for this type, in order they
     * are declared in class description.
     */
    public List<ResolvedType> getTypeParameters() {
        return _typeBindings.getTypeParameters();
    }

    /**
     * Method for accessing bindings of type variables to resolved types in context
     * of this type. It has same number of entries as return List of
     * {@link #getTypeParameters}, accessible using declared name to which they
     * bind; for example, {@link java.util.Map} has 2 type bindings; one for
     * key type (name "K", from Map.java) and one for value type
     * (name "V", from Map.java).
     */
    public TypeBindings getTypeBindings() { return _typeBindings; }
    
    /**
     * Method that will try to find type parameterization this type
     * has for specified super type
     * 
     * @return List of type parameters for specified supertype (which may
     *   be empty, if supertype is not a parametric type); null if specified
     *   type is not a super type of this type
     */
    public List<ResolvedType> typeParametersFor(Class<?> erasedSupertype)
    {
        ResolvedType type = findSupertype(erasedSupertype);
        if (type != null) {
            return type.getTypeParameters();
        }
        // nope; doesn't look like we extend or implement super type in question
        return null;
    }

    /**
     * Method for finding super type of this type that has specified type
     * erased signature. If supertype is an interface which is implemented
     * using multiple inheritance paths, preference is given to interfaces
     * implemented "highest up the stack" (directly implemented interfaces
     * over interfaces superclass implements).
     */
    public ResolvedType findSupertype(Class<?> erasedSupertype)
    {
        if (erasedSupertype == _erasedType) {
            return this;
        }
        // Check super interfaces first:
        if (erasedSupertype.isInterface()) {
            for (ResolvedType it : getImplementedInterfaces()) {
                ResolvedType type = it.findSupertype(erasedSupertype);
                if (type != null) {
                    return type;
                }
            }
        }
        // and if not found, super class and its supertypes
        ResolvedType pc = getParentClass();
        if (pc != null) {
            ResolvedType type = pc.findSupertype(erasedSupertype);
            if (type != null) {
                return type;
            }
        }
        // nope; doesn't look like we extend or implement super type in question
        return null;
    }
    
    /*
    /**********************************************************************
    /* Accessors for simple properties
    /**********************************************************************
     */
    
    public abstract boolean isInterface();
    public final boolean isConcrete() { return !isAbstract(); }
    public abstract boolean isAbstract();

    /**
     * Method that indicates whether this type is an array type.
     */
    public abstract boolean isArray();

    /**
     * Method that indicates whether this type is one of small number of primitive
     * Java types; not including array types of primitive types but just basic
     * primitive types.
     */
    public abstract boolean isPrimitive();

    public final boolean isInstanceOf(Class<?> type) {
        return type.isAssignableFrom(_erasedType);
    }

    /*
    /**********************************************************************
    /* Accessors for raw (minimally procesed) members
    /**********************************************************************
     */

    public List<RawConstructor> getConstructors() { return Collections.emptyList(); }
    public List<RawField> getMemberFields() { return Collections.emptyList(); }
    public List<RawMethod> getMemberMethods() { return Collections.emptyList(); }
    public List<RawField> getStaticFields() { return Collections.emptyList(); }
    public List<RawMethod> getStaticMethods() { return Collections.emptyList(); }

    /*
    /**********************************************************************
    /* String representations
    /**********************************************************************
     */

    /**
     * Method that returns full generic signature of the type; suitable
     * as signature for things like ASM package.
     */
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        return appendSignature(sb).toString();
    }

    /**
     * Method that returns type erased signature of the type; suitable
     * as non-generic signature some packages need
     */
    public String getErasedSignature() {
        StringBuilder sb = new StringBuilder();
        return appendErasedSignature(sb).toString();
    }

    /**
     * Human-readable full description of type, which includes specification
     * of super types (in brief format)
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        return appendFullDescription(sb).toString();
    }

    /**
     * Human-readable brief description of type, which does not include
     * information about super types.
     */
    public String getBriefDescription() {
        StringBuilder sb = new StringBuilder();
        return appendBriefDescription(sb).toString();
    }

    public abstract StringBuilder appendBriefDescription(StringBuilder sb);
    public abstract StringBuilder appendFullDescription(StringBuilder sb);
    public abstract StringBuilder appendSignature(StringBuilder sb);
    public abstract StringBuilder appendErasedSignature(StringBuilder sb);
    
    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    @Override public String toString() {
        return getBriefDescription();
    }

    @Override public int hashCode() {
        return _erasedType.getName().hashCode() + _typeBindings.hashCode();
    }

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        // sub-types must be same:
        if (o == null || o.getClass() != getClass()) return false;
        // Should be possible to actually implement here...
        ResolvedType other = (ResolvedType) o;
        if (other._erasedType != _erasedType) {
            return false;
        }
        // and type bindings must match as well
        return _typeBindings.equals(other._typeBindings);
    }
    
    /*
    /**********************************************************************
    /* Helper methods for sub-classes; string construction
    /**********************************************************************
     */
    
    protected StringBuilder _appendClassSignature(StringBuilder sb)
    {
        sb.append('L');
        sb = _appendClassName(sb);
        int count = _typeBindings.size();
        if (count > 0) {
            sb.append('<');
            for (int i = 0; i < count; ++i) {
                sb = _typeBindings.getBoundType(i).appendErasedSignature(sb);
            }
            sb.append('>');
        }
        sb.append(';');
        return sb;
    }

    protected StringBuilder _appendErasedClassSignature(StringBuilder sb)
    {
        sb.append('L');
        sb = _appendClassName(sb);
        sb.append(';');
        return sb;
    }

    protected StringBuilder _appendClassDescription(StringBuilder sb)
    {
        sb.append(_erasedType.getName());
        int count = _typeBindings.size();
        if (count > 0) {
            sb.append('<');
            for (int i = 0; i < count; ++i) {
                if (i > 0) {
                    sb.append(',');
                }
                sb = _typeBindings.getBoundType(i).appendBriefDescription(sb);
            }
            sb.append('>');
        }
        return sb;
    }
    
    protected StringBuilder _appendClassName(StringBuilder sb)
    {
        String name = _erasedType.getName();
        for (int i = 0, len = name.length(); i < len; ++i) {
            char c = name.charAt(i);
            if (c == '.') c = '/';
            sb.append(c);
        }
        return sb;
    }

    /*
    /**********************************************************************
    /* Helper methods for sub-classes; gathering members
    /**********************************************************************
     */
    
    /**
     * @param statics Whether to return static methods (true) or member methods (false)
     */
    protected RawField[] _getFields(boolean statics)
    {
        ArrayList<RawField> fields = new ArrayList<RawField>();
        for (Field f : _erasedType.getDeclaredFields()) {
            // Only skip synthetic fields, which should not really be exposed
            if (!f.isSynthetic()) {
                if (Modifier.isStatic(f.getModifiers()) == statics) {
                    fields.add(new RawField(this, f));
                }
            }
        }
        if (fields.isEmpty()) {
            return NO_FIELDS;
        }
        return fields.toArray(new RawField[fields.size()]);
    }

    /**
     * @param statics Whether to return static methods (true) or member methods (false)
     */
    protected RawMethod[] _getMethods(boolean statics)
    {
        ArrayList<RawMethod> methods = new ArrayList<RawMethod>();
        for (Method m : _erasedType.getDeclaredMethods()) {
            // Only skip synthetic fields, which should not really be exposed
            if (!m.isSynthetic()) {
                if (Modifier.isStatic(m.getModifiers()) == statics) {
                    methods.add(new RawMethod(this, m));
                }
            }
        }
        if (methods.isEmpty()) {
            return NO_METHODS;
        }
        return methods.toArray(new RawMethod[methods.size()]);
    }

    protected RawConstructor[] _getConstructors()
    {
        ArrayList<RawConstructor> ctors = new ArrayList<RawConstructor>();
        for (Constructor<?> c : _erasedType.getDeclaredConstructors()) {
            // Only skip synthetic fields, which should not really be exposed
            if (!c.isSynthetic()) {
                ctors.add(new RawConstructor(this, c));
            }
        }
        if (ctors.isEmpty()) {
            return NO_CONSTRUCTORS;
        }
        return ctors.toArray(new RawConstructor[ctors.size()]);
    }
}
