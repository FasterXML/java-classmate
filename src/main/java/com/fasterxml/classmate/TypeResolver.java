package com.fasterxml.classmate;

import java.lang.reflect.*;
import java.util.*;

import com.fasterxml.classmate.types.ResolvedAbstractClass;
import com.fasterxml.classmate.types.ResolvedArrayType;
import com.fasterxml.classmate.types.ResolvedClass;
import com.fasterxml.classmate.types.ResolvedConcreteClass;
import com.fasterxml.classmate.types.ResolvedInterface;
import com.fasterxml.classmate.types.ResolvedPrimitiveType;
import com.fasterxml.classmate.util.ClassKey;
import com.fasterxml.classmate.util.ResolvedTypeCache;

/**
 * Class used for constructing {@link ResolvedType} instances from type-erased classes
 * (that may extend generic classes) and {@link GenericType}s.
 *<p>
 * Note that resolver instances are stateful in that resolvers cache resolved
 * types for efficiency. Since this is internal state and not directly visible
 * to callers, access to state is fully synchronized so that access from
 * multiple threads is safe.
 */
public class TypeResolver
{
    /*
    /**********************************************************************
    /* Pre-created instances
    /**********************************************************************
     */

    /**
     * We will also need to return "unknown" type for cases where type variable binding
     * is not found ('raw' instances of generic types); easiest way is to
     * pre-create type for <code>java.lang.Object</code>
     */
    private final static ResolvedConcreteClass sJavaLangObject = 
        new ResolvedConcreteClass(Object.class, null, null, null);

    /**
     * Also, let's use another marker for self-references; points to <code>java.lang.Object</code>
     * but is different object.
     */
    private final static ResolvedConcreteClass sSelfReference = 
        new ResolvedConcreteClass(Object.class, null, null, null);
    
    /**
     * Since number of primitive types is small, and they are frequently needed,
     * let's actually pre-create them for efficient reuse. Same goes for limited number
     * of other "standard" types...
     */
    protected final static HashMap<ClassKey, ResolvedType> _primitiveTypes;
    static {
        _primitiveTypes = new HashMap<ClassKey, ResolvedType>(16);
        for (ResolvedPrimitiveType type : ResolvedPrimitiveType.all()) {
            _primitiveTypes.put(new ClassKey(type.getErasedType()), type);
        }
        // should we include "void"? might as well...
        _primitiveTypes.put(new ClassKey(Void.TYPE), ResolvedPrimitiveType.voidType());
        // and at least java.lang.Object should be added too.
        _primitiveTypes.put(new ClassKey(Object.class), sJavaLangObject);
        // but most other types can be added dynamically
    }

    /*
    /**********************************************************************
    /* Caching
    /**********************************************************************
     */
    
    /**
     * Simple cache of types resolved by this resolved; capped to last 200 resolved types.
     */
    protected final ResolvedTypeCache _resolvedTypes = new ResolvedTypeCache(200);

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    public TypeResolver() { }

    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */
    
    /**
     * Factory method for resolving a type-erased class; in this case any
     * generic type information has to come from super-types (via inheritance).
     */
    public ResolvedType resolve(Class<?> rawType)
    {
        // with erased class, no bindings:
        return _fromClass(null, rawType, TypeBindings.emptyBindings());
    }

    /**
     * Factory method for resolving given type (specified by type-erased class),
     * using specified types as type parameters.
     * Sample usage would be:
     *<pre>
     *  ResolvedType type = TypeResolver.resolve(List.class, Integer.class);
     *</pre>
     * which would be equivalent to
     *<pre>
     *  ResolvedType type = TypeResolver.resolve(new GenericType&lt;List&lt;Integer>>() { });
     *</pre>
     */
    public ResolvedType resolve(Class<?> type, Class<?>... typeParameters)
    {
        if (typeParameters == null || typeParameters.length == 0) {
            return resolve(type);
        }
        // with erased class, no bindings "from above"
        TypeBindings bindings = TypeBindings.emptyBindings();
        // First: resolve type parameters
        int len = typeParameters.length;
        ResolvedType[] resolvedParams = new ResolvedType[len];
        for (int i = 0; i < len; ++i) {
            resolvedParams[i] = _fromClass(null, typeParameters[i], bindings);
        }
        return resolve(type, resolvedParams);
    }

    /**
     * Factory method for resolving given type (specified by type-erased class),
     * using specified types as type parameters.
     * Sample usage would be:
     *<pre>
     *  ResolvedType valueType = TypeResolver.resolve(new GenericType&lt;Set&lt;String>>() { });
     *  ResolvedType type = TypeResolver.resolve(List.class, valueType);
     *</pre>
     * which would be equivalent to
     *<pre>
     *  ResolvedType type = TypeResolver.resolve(new GenericType&lt;List&lt;Set&lt;String>>() { });
     *</pre>
     */
    public ResolvedType resolve(Class<?> type, ResolvedType[] typeParameters)
    {
        if (typeParameters == null || typeParameters.length == 0) {
            return resolve(type);
        }
        return _fromClass(null, type, TypeBindings.create(type, typeParameters));
    }
    
    /**
     * Factory method for resolving given generic type.
     */
    public ResolvedType resolve(GenericType<?> generic)
    {
        /* To allow multiple levels of inheritance (just in case someone
         * wants to go to town with inheritnace of GenericType),
         * we better resolve the whole thing; then dig out
         * type parameterization...
         */
        ResolvedType type = _fromClass(null, generic.getClass(), TypeBindings.emptyBindings());
        ResolvedType genType = type.findSupertype(GenericType.class);
        if (genType == null) { // sanity check; shouldn't occur
            throw new IllegalArgumentException("Unparameterized GenericType instance ("+generic.getClass().getName()+")");
        }
        TypeBindings b = genType.getBindings();
        ResolvedType[] params = b.typeParameterArray();
        if (params.length == 0) {
            throw new IllegalArgumentException("Unparameterized GenericType instance ("+generic.getClass().getName()+")");
        }
        return params[0];
    }

    /**
     * Factory method for constructing array type of given element type
     */
    public ResolvedArrayType arrayType(ResolvedType elementType)
    {
        // Arrays are cumbersome for some reason:
        Object emptyArray = Array.newInstance(elementType.getErasedType(), 0);
        // Should we try to use cache? It's bit tricky, so let's not bother yet
        return new ResolvedArrayType(emptyArray.getClass(), TypeBindings.emptyBindings(),
                sJavaLangObject, elementType);
    }
    
    /*
    /**********************************************************************
    /* Misc other methods
    /**********************************************************************
     */

    /**
     * Helper method that can be used to checked whether given resolved type
     * (with erased type of <code>java.lang.Object</code>) is a placeholder
     * for "self-reference"; these are nasty recursive ("self") types
     * needed with some interfaces
     */
    public static boolean isSelfReference(ResolvedType type)
    {
        return (type == sSelfReference);
    }
    
    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    private ResolvedType _fromAny(ClassStack context, Type mainType, TypeBindings typeBindings)
    {
        if (mainType instanceof Class<?>) {
            return _fromClass(context, (Class<?>) mainType, typeBindings);
        }
        if (mainType instanceof ParameterizedType) {
            return _fromParamType(context, (ParameterizedType) mainType, typeBindings);
        }
        if (mainType instanceof GenericArrayType) {
            return _fromArrayType(context, (GenericArrayType) mainType, typeBindings);
        }
        if (mainType instanceof TypeVariable<?>) {
            return _fromVariable(context, (TypeVariable<?>) mainType, typeBindings);
        }
        if (mainType instanceof WildcardType) {
            return _fromWildcard(context, (WildcardType) mainType, typeBindings);
        }
        // should never get here...
        throw new IllegalArgumentException("Unrecognized type class: "+mainType.getClass().getName());
    }

    private ResolvedType _fromClass(ClassStack context, Class<?> rawType, TypeBindings typeBindings)
    {
        // First: a primitive type perhaps?
        ResolvedType type = _primitiveTypes.get(new ClassKey(rawType));
        if (type != null) {
            return type;
        }
        // Second: recursive reference?
        if (context == null) {
            context = new ClassStack(rawType);
        } else {
            context = context.add(rawType);
            if (context == null) { // yuck; self-reference; gotta bail
                return sSelfReference;
            }
        }
        
        // If not, already recently resolved?
        ResolvedType[] typeParameters = typeBindings.typeParameterArray();
        ResolvedTypeCache.Key key = _resolvedTypes.key(rawType, typeParameters);
                
        type = _resolvedTypes.find(key);
        if (type != null) {
            return type;
        }
        type = _constructType(context, rawType, typeBindings);
        _resolvedTypes.put(key, type);
        return type;
    }

    private ResolvedType _constructType(ClassStack context, Class<?> rawType, TypeBindings typeBindings)
    {
        // Ok: no easy shortcut, let's figure out type of type...
        if (rawType.isArray()) {
            ResolvedType elementType = _fromAny(context, rawType.getComponentType(), typeBindings);
            return new ResolvedArrayType(rawType, typeBindings, sJavaLangObject, elementType);
        }
        // For other types super interfaces are needed...
        if (rawType.isInterface()) {
            return new ResolvedInterface(rawType, typeBindings,
                    _resolveSuperInterfaces(context, rawType, typeBindings));
            
        }
        if (Modifier.isAbstract(rawType.getModifiers())) {
            return new ResolvedAbstractClass(rawType, typeBindings,
                    _resolveSuperClass(context, rawType, typeBindings),
                    _resolveSuperInterfaces(context, rawType, typeBindings));
        }
        return new ResolvedConcreteClass(rawType, typeBindings,
                _resolveSuperClass(context, rawType, typeBindings),
                _resolveSuperInterfaces(context, rawType, typeBindings));
    }

    private ResolvedType[] _resolveSuperInterfaces(ClassStack context, Class<?> rawType, TypeBindings typeBindings)
    {
        Type[] types = rawType.getGenericInterfaces();
        if (types == null || types.length == 0) {
            return ResolvedType.NO_TYPES;
        }
        int len = types.length;
        ResolvedType[] resolved = new ResolvedType[len];
        for (int i = 0; i < len; ++i) {
            resolved[i] = _fromAny(context, types[i], typeBindings);
        }
        return resolved;
    }

    private ResolvedClass _resolveSuperClass(ClassStack context, Class<?> rawType, TypeBindings typeBindings)
    {
        Type parent = rawType.getGenericSuperclass();
        if (parent == null) {
            return null;
        }
        ResolvedType rt = _fromAny(context, parent, typeBindings);
        // can this ever be something other than class? (primitive, array)
        return (ResolvedClass) rt;
    }
    
    private ResolvedType _fromParamType(ClassStack context, ParameterizedType ptype, TypeBindings parentBindings)
    {
        /* First: what is the actual base type? One odd thing is that 'getRawType'
         * returns Type, not Class<?> as one might expect. But let's assume it is
         * always of type Class: if not, need to add more code to resolve it...
         */
        Class<?> rawType = (Class<?>) ptype.getRawType();
        Type[] params = ptype.getActualTypeArguments();
        int len = params.length;
        ResolvedType[] types = new ResolvedType[len];

        for (int i = 0; i < len; ++i) {
            types[i] = _fromAny(context, params[i], parentBindings);
        }
        // Ok: this gives us current bindings for this type:
        TypeBindings newBindings = TypeBindings.create(rawType, types);
        return _fromClass(context, rawType, newBindings);
    }

    private ResolvedType _fromArrayType(ClassStack context, GenericArrayType arrayType, TypeBindings typeBindings)
    {
        ResolvedType elementType = _fromAny(context, arrayType.getGenericComponentType(), typeBindings);
        // Figuring out raw class for generic array is actually bit tricky...
        Object emptyArray = Array.newInstance(elementType.getErasedType(), 0);
        return new ResolvedArrayType(emptyArray.getClass(), typeBindings,
                sJavaLangObject, elementType);
    }

    private ResolvedType _fromWildcard(ClassStack context, WildcardType wildType, TypeBindings typeBindings)
    {
        /* Similar to challenges with TypeVariable, we may have multiple upper bounds.
         * But it is also possible that if upper bound defaults to Object, we might want to
         * consider lower bounds instead?
         * For now, we won't try anything more advanced; above is just for future reference.
         */
        return _fromAny(context, wildType.getUpperBounds()[0], typeBindings);
    }
    
    private ResolvedType _fromVariable(ClassStack context, TypeVariable<?> variable, TypeBindings typeBindings)
    {
        // ideally should find it via bindings:
        ResolvedType type = typeBindings.findBoundType(variable.getName());
        if (type != null) {
            return type;
        }
        /* but if not, use bounds... note that approach here is simplistics; not taking
         * into account possible multiple bounds, nor consider upper bounds.
         */
        // !!! 05-Nov-2010, tatu: How about recursive types? (T extends Comparable<T>)
        Type[] bounds = variable.getBounds();
        //context._addPlaceholder(name);        
        return _fromAny(context, bounds[0], typeBindings);
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */
    
    /**
     * Simple helper class used to keep track of 'call stack' for classes being referenced
     */
    private final static class ClassStack
    {
        private final ClassStack _parent;
        private final Class<?> _current;

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
        public ClassStack add(Class<?> cls)
        {
            if (contains(cls)) {
                return null;
            }
            return new ClassStack(this, cls);
        }

        public boolean contains(Class<?> cls)
        {
            if (_current == cls) return true;
            if (_parent != null && _parent.contains(cls)) {
                return true;
            }
            return false;
        }
    }
}
