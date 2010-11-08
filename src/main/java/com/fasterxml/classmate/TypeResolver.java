package com.fasterxml.classmate;

import java.lang.reflect.*;
import java.util.*;

import com.fasterxml.classmate.types.*;
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
    private final static ResolvedType[] NO_TYPES = new ResolvedType[0];
    
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
    private final static ResolvedObjectType sJavaLangObject =  new ResolvedObjectType(Object.class, null, null,
            
            NO_TYPES);

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
     * Factory method for resolving given generic type, defined by using sub-class
     * instance of {@link GenericType}
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
        TypeBindings b = genType.getTypeBindings();
        ResolvedType[] params = b.typeParameterArray();
        if (params.length == 0) {
            throw new IllegalArgumentException("Unparameterized GenericType instance ("+generic.getClass().getName()+")");
        }
        return params[0];
    }

    /**
     * Factory method for constructing array type of given element type.
     */
    public ResolvedArrayType arrayType(ResolvedType elementType)
    {
        // Arrays are cumbersome for some reason:
        Object emptyArray = Array.newInstance(elementType.getErasedType(), 0);
        // Should we try to use cache? It's bit tricky, so let's not bother yet
        return new ResolvedArrayType(emptyArray.getClass(), TypeBindings.emptyBindings(),
                sJavaLangObject, elementType);
    }

    /**
     * Factory method for resolving specified Java {@link java.lang.reflect.Type}, given
     * {@link TypeBindings} needed to resolve any type variables.
     *<p>
     * Use of this method is discouraged (use if and only if you really know what you
     * are doing!); but if used, type bindings passed should come from {@link ResolvedType}
     * instance of declaring class (or interface).
     */
    public ResolvedType resolve(Type jdkType, TypeBindings typeBindings)
    {
        return _fromAny(null, jdkType, typeBindings);
    }

    /**
     * Factory method for constructing sub-classing specified type; class specified
     * as sub-class must be compatible according to basic Java inheritance rules
     * (subtype must propery extend or implement specified supertype).
     *<p>
     * A typical use case here is to refine a generic type; for example, given
     * that we have generic type like <code>List&ltInteger></code>, but we want
     * a more specific implementation type like
     * class <code>ArrayList</code> but with same parameterization (here just <code>Integer</code>),
     * we could achieve it by:
     *<pre>
     *  ResolvedType mapType = typeResolver.resolve(List.class, Integer.class);
     *  ResolveType concreteMapType = typeResolver.resolveSubType(mapType, ArrayList.class);
     *</pre>
     * (in this case, it would have been simpler to resolve directly; but in some
     * cases we are handled supertype and want to refine it, in which case steps
     * would be the same but separated by other code)
     *<p>
     * Note that this method will fail if extension can not succeed; either because
     * this type is not extendable (sub-classable) -- which is true for primitive
     * and array types -- or because given class is not a subtype of this type.
     * To check whether subtyping could succeed, you can call
     * {@link ResolvedType#canCreateSubtypes()} to see if supertype can ever
     * be extended.
     *
     * @param supertype Type to subtype (extend)
     * @param subclass Type-erased sub-class or sub-interface 
     * 
     * @return Resolved subtype
     * 
     * @throws IllegalArgumentException If this type can be extended in general, but not into specified sub-class
     * @throws UnsupportedOperationException If this type can not be sub-classed
     */
    public ResolvedType resolveSubtype(ResolvedType supertype, Class<?> subtype)
        throws IllegalArgumentException, UnsupportedOperationException
    {
        // first: if it's a recursive reference, find out referred-to type
        ResolvedType refType = supertype.getSelfReferencedType();
        if (refType != null) {
            supertype = refType;
        }
        if (!supertype.canCreateSubtypes()) {
            throw new UnsupportedOperationException("Can not subtype primitive or array types (type "+supertype.getFullDescription()+")");
        }
        // In general, must be able to subtype as per JVM rules:
        Class<?> superclass = supertype.getErasedType();
        if (!superclass.isAssignableFrom(subtype)) {
            throw new IllegalArgumentException("Can not sub-class "+supertype.getBriefDescription()
                    +" into "+subtype.getName());
        }
        // First things first: need to create raw subtype, then replace parts with supertype
        ResolvedType resolvedSubtype = resolve(subtype);
        
        // interfaces can be extended as sub-classes or as sub-interfaces
        if (supertype.isInterface()) {
            // interface extension is easy; just add/replace super-interface:
            if (subtype.isInterface()) {
                return new ResolvedInterfaceType(subtype, resolvedSubtype.getTypeBindings(),
                        _replaceInterface(resolvedSubtype.getImplementedInterfaces(), supertype));
            }
            // class implementing an interface is similarly quite easy with add/replace
            return new ResolvedObjectType(subtype, resolvedSubtype.getTypeBindings(),
                    (ResolvedObjectType) resolvedSubtype.getParentClass(),
                    _replaceInterface(resolvedSubtype.getImplementedInterfaces(), supertype));
        }
        /* Class extending class is trickiest, since we have to find where super-class
         * needs to be replaced, and need to do that recursively...
         */
        return _resolveSubClass(supertype, resolvedSubtype);
    }
    
    private ResolvedType _resolveSubClass(ResolvedType supertype, ResolvedType subtype)
    {
        // Not direct super/subtype? Resolve recursively...
        if (supertype.getErasedType() != subtype.getParentClass().getErasedType()) {
            supertype = _resolveSubClass(supertype, subtype.getParentClass());
        }
        return new ResolvedObjectType(subtype.getErasedType(), subtype.getTypeBindings(),
                (ResolvedObjectType) supertype, subtype.getImplementedInterfaces());
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
        return (type instanceof ResolvedRecursiveType);
    }
    
    /*
    /**********************************************************************
    /* Internal methods, second-level factory methods
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
            ClassStack prev = context.find(rawType);
            if (prev != null) {
                // Self-reference: needs special handling, then...
                ResolvedRecursiveType selfRef = new ResolvedRecursiveType(rawType, typeBindings);
                prev.addSelfReference(selfRef);
                return selfRef;
            }
            // no, can just add
            context = context.child(rawType);
        }
        
        // If not, already recently resolved?
        ResolvedType[] typeParameters = typeBindings.typeParameterArray();
        ResolvedTypeCache.Key key = _resolvedTypes.key(rawType, typeParameters);
                
        type = _resolvedTypes.find(key);
        if (type == null) {
            type = _constructType(context, rawType, typeBindings);
            _resolvedTypes.put(key, type);
        }
        context.resolveSelfReferences(type);
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
            return new ResolvedInterfaceType(rawType, typeBindings,
                    _resolveSuperInterfaces(context, rawType, typeBindings));
            
        }
        return new ResolvedObjectType(rawType, typeBindings,
                _resolveSuperClass(context, rawType, typeBindings),
                _resolveSuperInterfaces(context, rawType, typeBindings));
    }

    private ResolvedType[] _resolveSuperInterfaces(ClassStack context, Class<?> rawType, TypeBindings typeBindings)
    {
        Type[] types = rawType.getGenericInterfaces();
        if (types == null || types.length == 0) {
            return NO_TYPES;
        }
        int len = types.length;
        ResolvedType[] resolved = new ResolvedType[len];
        for (int i = 0; i < len; ++i) {
            resolved[i] = _fromAny(context, types[i], typeBindings);
        }
        return resolved;
    }

    private ResolvedObjectType _resolveSuperClass(ClassStack context, Class<?> rawType, TypeBindings typeBindings)
    {
        Type parent = rawType.getGenericSuperclass();
        if (parent == null) {
            return null;
        }
        ResolvedType rt = _fromAny(context, parent, typeBindings);
        // can this ever be something other than class? (primitive, array)
        return (ResolvedObjectType) rt;
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
        Type[] bounds = variable.getBounds();
        //context._addPlaceholder(name);        
        return _fromAny(context, bounds[0], typeBindings);
    }

    /*
    /**********************************************************************
    /* Internal methods, other
    /**********************************************************************
     */
    
    private ResolvedType[] _replaceInterface(List<ResolvedType> interfaces, ResolvedType newInterface)
    {
        ArrayList<ResolvedType> result = new ArrayList<ResolvedType>();
        Class<?> interfaceToAdd = newInterface.getErasedType();
        boolean wasAdded = false;
        for (ResolvedType iface : interfaces) {
            if (iface.getErasedType() == interfaceToAdd) {
                result.add(newInterface);
                wasAdded = true;
            } else {
                result.add(iface);
            }
        }
        if (!wasAdded) {
            result.add(newInterface);
        }
        return result.toArray(new ResolvedType[result.size()]);
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
}
