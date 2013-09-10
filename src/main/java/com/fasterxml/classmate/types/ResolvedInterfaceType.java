package com.fasterxml.classmate.types;

import java.util.*;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.members.RawField;
import com.fasterxml.classmate.members.RawMethod;

public class ResolvedInterfaceType extends ResolvedType
{

    /**
     * List of interfaces this type implements; may be empty but never null
     */
    protected final ResolvedType[] _superInterfaces;

    /**
     * Interfaces can have static final (constant) fields.
     */
    protected RawField[] _constantFields;

    /**
     * Interface methods are all public and abstract.
     */
    protected RawMethod[] _memberMethods;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public ResolvedInterfaceType(Class<?> erased, TypeBindings bindings,
            ResolvedType[] superInterfaces)
    {
        super(erased, bindings);
        _superInterfaces = (superInterfaces == null ? NO_TYPES : superInterfaces);
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
    public ResolvedType getParentClass() {
        // interfaces do not have parent class, just interfaces
        return null;
    }

    @Override
    public ResolvedType getSelfReferencedType() { return null; }
    
    @Override
    public List<ResolvedType> getImplementedInterfaces() {
        return (_superInterfaces.length == 0) ?
                Collections.<ResolvedType>emptyList() : Arrays.asList(_superInterfaces);
    }
    
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
    public boolean isInterface() { return true; }

    @Override
    public boolean isAbstract() { return true; }

    @Override
    public boolean isArray() { return false; }

    @Override
    public boolean isPrimitive() { return false; }

    /*
    /**********************************************************************
    /* Accessors for raw (minimally procesed) members
    /**********************************************************************
     */

    @Override
    public synchronized List<RawField> getStaticFields()
    {
        // Interfaces can have static fields, but only as static constants...
        if (_constantFields == null) {
            _constantFields = _getFields(true);
        }
        if (_constantFields.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(_constantFields);
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
        // interfaces 'extend' other interfaces...
        int count = _superInterfaces.length;
        if (count > 0) {
            sb.append(" extends ");
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



