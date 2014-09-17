package com.fasterxml.classmate.types;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.members.RawConstructor;
import com.fasterxml.classmate.members.RawField;
import com.fasterxml.classmate.members.RawMethod;

/**
 * Type implementation for classes that do not represent interfaces,
 * primitive or array types.
 */
public class ResolvedObjectType extends ResolvedType
{
    /**
     * While fundamentally super class has to be {@link ResolvedObjectType}
     * (or null for {@link java.lang.Object}), we may need to hold on to
     * a {@link ResolvedRecursiveType} occasionally.
     */
    protected final ResolvedType _superClass;

    /**
     * List of interfaces this type implements; may be empty but never null
     */
    protected final ResolvedType[] _superInterfaces;

    /**
     * Modifiers of the underlying class.
     */
    protected final int _modifiers;

    /**
     * Constructors declared by the resolved Object class.
     */
    protected RawConstructor[] _constructors;

    protected RawField[] _memberFields;
    protected RawField[] _staticFields;

    protected RawMethod[] _memberMethods;
    protected RawMethod[] _staticMethods;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    public ResolvedObjectType(Class<?> erased, TypeBindings bindings,
            ResolvedType superClass, List<ResolvedType> interfaces)
    {
        this(erased, bindings, superClass,
                (interfaces == null || interfaces.isEmpty()) ? NO_TYPES :
                interfaces.toArray(new ResolvedType[interfaces.size()]));
    }

    public ResolvedObjectType(Class<?> erased, TypeBindings bindings,
            ResolvedType superClass, ResolvedType[] interfaces)
    {
        super(erased, bindings);
        /* 19-Aug-2014, tatu: bit unclean, but has to do for now.
         *   Problem is, there is no common super-type, nor can we yet
         *   force or coerce recursive types. Rather, they may only get
         *   resolved only slightly after construction. So... need to
         *   keep a reference.
         */
        if (superClass != null) {
            if (!(superClass instanceof ResolvedObjectType)
                    && !(superClass instanceof ResolvedRecursiveType)
                    ) {
                throw new IllegalArgumentException("Unexpected parent type for "
                        +erased.getName()+": "+superClass.getClass().getName());
            }
        }
        
        _superClass = superClass;
        _superInterfaces = (interfaces == null) ? NO_TYPES : interfaces;
        _modifiers = erased.getModifiers();
    }

    @Deprecated // since 1.1; removed from 1.2 -- kept for binary backwards compatibility
    public ResolvedObjectType(Class<?> erased, TypeBindings bindings,
            ResolvedObjectType superClass, List<ResolvedType> interfaces)
    {
        this(erased, bindings, (ResolvedType) superClass, interfaces);
    }

    @Deprecated // since 1.1; removed from 1.2 -- kept for binary backwards compatibility
    public ResolvedObjectType(Class<?> erased, TypeBindings bindings,
            ResolvedObjectType superClass, ResolvedType[] interfaces)
    {
        this(erased, bindings, (ResolvedType) superClass, interfaces);
    }

    public static ResolvedObjectType create(Class<?> erased, TypeBindings bindings,
            ResolvedType superClass, List<ResolvedType> interfaces)
    {
        return new ResolvedObjectType(erased, bindings, superClass, interfaces);
    }
    
    @Override
    public boolean canCreateSubtypes() {
        return true;
    }
    
    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */

    @Override
    public ResolvedObjectType getParentClass() {
        
        /* 19-Aug-2014, tatu: Ugly does it... sigh.
         *   But can't be helped because ResolvedRecursiveType is typically only
         *   resolved after instances of this type have been constructed.
         *   This means that resolution will need to be done somewhat dynamically.
         */
        if (_superClass == null) {
            return null;
        }
        if (_superClass instanceof ResolvedObjectType) {
            return (ResolvedObjectType) _superClass;
        }
        ResolvedType rt = ((ResolvedRecursiveType) _superClass).getSelfReferencedType();
        if (!(rt instanceof ResolvedObjectType)) {
            throw new IllegalStateException("Internal error: self-referential parent type ("
                    +_superClass+") does not resolve into proper ResolvedObjectType, but instead to: "
                    +rt);
        }
        return (ResolvedObjectType) rt;
    }

    @Override
    public ResolvedType getSelfReferencedType() { return null; }
    
    @Override
    public List<ResolvedType> getImplementedInterfaces() {
        return (_superInterfaces.length == 0) ?
                Collections.<ResolvedType>emptyList() : Arrays.asList(_superInterfaces);
    }

    /*
    /**********************************************************************
    /* Accessors for related types
    /**********************************************************************
     */
    
    @Override
    public final ResolvedType getArrayElementType() { return null; }
    
    /*
    /**********************************************************************
    /* Simple property accessors
    /**********************************************************************
     */
    
    @Override
    public final boolean isInterface() { return false; }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(_modifiers);
    }

    @Override
    public final boolean isArray() { return false; }

    @Override
    public final boolean isPrimitive() { return false; }

    /*
    /**********************************************************************
    /* Accessors for raw (minimally procesed) members
    /**********************************************************************
     */

    @Override
    public synchronized List<RawField> getMemberFields()
    {
        if (_memberFields == null) {
            _memberFields = _getFields(false);
        }
        if (_memberFields.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(_memberFields);
    }

    @Override
    public synchronized List<RawField> getStaticFields()
    {
        if (_staticFields == null) {
            _staticFields = _getFields(true);
        }
        if (_staticFields.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(_staticFields);
    }

    @Override
    public synchronized List<RawMethod> getMemberMethods()
    {
        if (_memberMethods == null) {
            _memberMethods = _getMethods(false);
        }
        if (_memberMethods.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(_memberMethods);
    } 

    @Override
    public synchronized List<RawMethod> getStaticMethods()
    {
        if (_staticMethods == null) {
            _staticMethods = _getMethods(true);
        }
        if (_staticMethods.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(_staticMethods);
    } 

    @Override
    public List<RawConstructor> getConstructors()
    {
        if (_constructors == null) {
            _constructors = _getConstructors();
        }
        if (_constructors.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(_constructors);
    }

    /*
    /**********************************************************************
    /* String representations
    /**********************************************************************
     */

    @Override
    public StringBuilder appendSignature(StringBuilder sb) {
        return _appendClassSignature(sb);
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
        sb = _appendClassDescription(sb);
        if (_superClass != null) {
            sb.append(" extends ");
            sb = _superClass.appendBriefDescription(sb);
        }
        // interfaces 'extend' other interfaces...
        int count = _superInterfaces.length;
        if (count > 0) {
            sb.append(" implements ");
            for (int i = 0; i < count; ++i) {
                if (i > 0) {
                    sb.append(",");
                }
                sb = _superInterfaces[i].appendBriefDescription(sb);
            }
        }
        return sb;
    }
}

