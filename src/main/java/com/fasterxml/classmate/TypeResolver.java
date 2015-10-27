package com.fasterxml.classmate;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

import com.fasterxml.classmate.types.*;
import com.fasterxml.classmate.util.ClassKey;
import com.fasterxml.classmate.util.ClassStack;
import com.fasterxml.classmate.util.ResolvedTypeCache;

/**
 * Object that is used for resolving generic type information of a class
 * so that it is accessible using simple API. Resolved types are also starting
 * point for accessing resolved (generics aware) return and argument types
 * of class members (methods, fields, constructors).
 *<p>
 * Note that resolver instances are stateful in that resolvers cache resolved
 * types for efficiency. Since this is internal state and not directly visible
 * to callers, access to state is fully synchronized so that access from
 * multiple threads is safe.
 */
@SuppressWarnings("serial")
public class TypeResolver implements Serializable
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
    private final static ResolvedObjectType sJavaLangObject =
        ResolvedObjectType.create(Object.class, null, null, null);

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
     * Caching works because type instances themselves are mostly immutable;
     * and properly synchronized in cases where transient data (raw members) are
     * accessed.
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
    /* Factory methods, with explicit parameterization
    /**********************************************************************
     */
    
    /**
     * Factory method for resolving given base type
     * using specified types as type parameters.
     * Sample usage would be:
     *<pre>
     *  ResolvedType type = TypeResolver.resolve(List.class, Integer.class);
     *</pre>
     * which would be equivalent to
     *<pre>
     *  ResolvedType type = TypeResolver.resolve(new GenericType&lt;List&lt;Integer>>() { });
     *</pre>
     * Note that you can mix different types of type parameters, whether already
     * resolved ({@link ResolvedType}), type-erased ({@link java.lang.Class}) or
     * generic type reference ({@link GenericType}).
     */
    public ResolvedType resolve(Type type, Type... typeParameters)
    {
        boolean noParams = (typeParameters == null || typeParameters.length == 0);
        TypeBindings bindings;
        Class<?> rawBase;

        if (type instanceof Class<?>) {
            bindings = TypeBindings.emptyBindings();
            if (noParams) {
                return _fromClass(null, (Class<?>) type, bindings);
            }
            rawBase = (Class<?>) type;
        } else if (type instanceof GenericType<?>) {
            bindings = TypeBindings.emptyBindings();
            if (noParams) {
                return _fromGenericType(null, (GenericType<?>) type, bindings);
            }
            ResolvedType rt = _fromAny(null, type, bindings);
            rawBase = rt.getErasedType();
        } else if (type instanceof ResolvedType) {
            ResolvedType rt = (ResolvedType) type;
            if (noParams) {
                return rt;
            }
            bindings = rt.getTypeBindings();
            rawBase = rt.getErasedType();
        } else {
            bindings = TypeBindings.emptyBindings();
            if (noParams) {
                return resolve(bindings, type);
            }
            // Quite convoluted... but necessary to find Class<?> underlying it all
            ResolvedType rt = _fromAny(null, type, bindings);
            rawBase = rt.getErasedType();
        }

        // Next: resolve type parameters
        int len = typeParameters.length;
        ResolvedType[] resolvedParams = new ResolvedType[len];
        for (int i = 0; i < len; ++i) {
            resolvedParams[i] = _fromAny(null, typeParameters[i], bindings);
        }
        return _fromClass(null, rawBase, TypeBindings.create(rawBase, resolvedParams));
    }

    /**
     * Factory method for constructing array type of given element type.
     */
    public ResolvedArrayType arrayType(Type elementType)
    {
        ResolvedType resolvedElementType = resolve(TypeBindings.emptyBindings(), elementType);
        // Arrays are cumbersome for some reason:
        Object emptyArray = Array.newInstance(resolvedElementType.getErasedType(), 0);
        // Should we try to use cache? It's bit tricky, so let's not bother yet
        return new ResolvedArrayType(emptyArray.getClass(), TypeBindings.emptyBindings(),
                resolvedElementType);
    }

    /**
     * Factory method for resolving specified Java {@link java.lang.reflect.Type}, given
     * {@link TypeBindings} needed to resolve any type variables.
     *<p>
     * Use of this method is discouraged (use if and only if you really know what you
     * are doing!); but if used, type bindings passed should come from {@link ResolvedType}
     * instance of declaring class (or interface).
     *<p>
     * NOTE: order of arguments was reversed for 0.8, to avoid problems with
     * overload varargs method.
     */
    public ResolvedType resolve(TypeBindings typeBindings, Type jdkType)
    {
        return _fromAny(null, jdkType, typeBindings);
    }

    /**
     * Factory method for constructing sub-classing specified type; class specified
     * as sub-class must be compatible according to basic Java inheritance rules
     * (subtype must properly extend or implement specified supertype).
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
     * @param subtype Type-erased sub-class or sub-interface
     * 
     * @return Resolved subtype
     * 
     * @throws IllegalArgumentException If this type can be extended in general, but not into specified sub-class
     * @throws UnsupportedOperationException If this type can not be sub-classed
     */
    public ResolvedType resolveSubtype(ResolvedType supertype, final Class<?> subtype)
        throws IllegalArgumentException, UnsupportedOperationException
    {
        // first: if it's a recursive reference, find out referred-to type
        ResolvedType refType = supertype.getSelfReferencedType();
        if (refType != null) {
            supertype = refType;
        }
        // Then, trivial check for case where subtype is supertype...
        final Class<?> superclass = supertype.getErasedType();
        if (superclass == subtype) { // unlikely but cheap check so let's just do it
            return supertype;
        }
        // First: can not sub-class primitives, or array types
        if (!supertype.canCreateSubtypes()) {
            throw new UnsupportedOperationException("Can not subtype primitive or array types (type "+supertype.getFullDescription()+")");
        }
        // And in general must be able to subtype as per JVM rules:
        if (!superclass.isAssignableFrom(subtype)) {
            throw new IllegalArgumentException("Can not sub-class "+supertype.getBriefDescription()
                    +" into "+subtype.getName());
        }
        // Ok, then, let us instantiate type with placeholders
        ResolvedType resolvedSubtype;
        int paramCount = subtype.getTypeParameters().length;
        TypePlaceHolder[] placeholders;

        if (paramCount == 0) { // no generics
            placeholders = null;
            // 26-Oct-2015, tatu: Used to do "full" call:
//            resolvedSubtype = resolve(subtype);
            // but should be able to streamline it as:
            resolvedSubtype = _fromClass(null, subtype, TypeBindings.emptyBindings());

        } else {
            placeholders = new TypePlaceHolder[paramCount];
            for (int i = 0; i < paramCount; ++i) {
                placeholders[i] = new TypePlaceHolder(i);
            }
            // 26-Oct-2015, tatu: Used to do "full" call:
//            resolvedSubtype = resolve(subtype, placeholders);            
            // but let's actually inline it:
            ResolvedType[] resolvedParams = new ResolvedType[paramCount];
            TypeBindings bindings = TypeBindings.emptyBindings();
            for (int i = 0; i < paramCount; ++i) {
                resolvedParams[i] = _fromAny(null, placeholders[i], bindings);
            }
            resolvedSubtype = _fromClass(null, subtype,
                    TypeBindings.create(subtype, resolvedParams));
        }
        ResolvedType resolvedSupertype = resolvedSubtype.findSupertype(superclass);
        if (resolvedSupertype == null) { // sanity check, should never occur
            throw new IllegalArgumentException("Internal error: unable to locate supertype ("+subtype.getName()+") for type "+supertype.getBriefDescription());
        }
        // Ok, then, let's find and verify type assignments
        _resolveTypePlaceholders(supertype, resolvedSupertype);
        // And then re-construct, if necessary
        if (paramCount == 0) { // if no type parameters, fine as is
            return resolvedSubtype;
        }
        // but with type parameters, need to reconstruct
        final ResolvedType[] typeParams = new ResolvedType[paramCount];
        for (int i = 0; i < paramCount; ++i) {
            ResolvedType t = placeholders[i].actualType();
            // Is it ok for it to be left unassigned? For now let's not allow that
            if (t == null) {
                throw new IllegalArgumentException("Failed to find type parameter #"+(i+1)+"/"
                        +paramCount+" for "+subtype.getName());
            }
            typeParams[i] = t;
        }
        return resolve(subtype, typeParams);
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
        if (mainType instanceof ResolvedType) { // Esp. TypePlaceHolder
            return (ResolvedType) mainType;
        }
        if (mainType instanceof GenericType<?>) {
            return _fromGenericType(context, (GenericType<?>) mainType, typeBindings);
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
        // 25-Oct-2015, tatu: one twist; if any TypePlaceHolders included, key will NOT be created,
        //   which means that caching should not be used (since type is mutable)
        if (key == null) {
            type = _constructType(context, rawType, typeBindings);
        } else {
            type = _resolvedTypes.find(key);
            if (type == null) {
                type = _constructType(context, rawType, typeBindings);
                _resolvedTypes.put(key, type);
            }
        }
        context.resolveSelfReferences(type);
        return type;
    }

    /**
     * Factory method for resolving given generic type, defined by using sub-class
     * instance of {@link GenericType}
     */
    private ResolvedType _fromGenericType(ClassStack context, GenericType<?> generic, TypeBindings typeBindings)
    {
        /* To allow multiple levels of inheritance (just in case someone
         * wants to go to town with inheritance of GenericType),
         * we better resolve the whole thing; then dig out
         * type parameterization...
         */
        ResolvedType type = _fromClass(context, generic.getClass(), typeBindings);
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

    private ResolvedType _constructType(ClassStack context, Class<?> rawType, TypeBindings typeBindings)
    {
        // Ok: no easy shortcut, let's figure out type of type...
        if (rawType.isArray()) {
            ResolvedType elementType = _fromAny(context, rawType.getComponentType(), typeBindings);
            return new ResolvedArrayType(rawType, typeBindings, elementType);
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

    /**
     * NOTE: return type changed in 1.0.1 from {@link ResolvedObjectType} to
     *    {@link ResolvedType}, since it was found that other types may
     *    be returned...
     * 
     * @return Usually a {@link ResolvedObjectType}, but possibly also
     *    {@link ResolvedRecursiveType}
     */
    private ResolvedType _resolveSuperClass(ClassStack context, Class<?> rawType, TypeBindings typeBindings)
    {
        Type parent = rawType.getGenericSuperclass();
        if (parent == null) {
            return null;
        }
        return _fromAny(context, parent, typeBindings);
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
        return new ResolvedArrayType(emptyArray.getClass(), typeBindings, elementType);
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
        String name = variable.getName();
        ResolvedType type = typeBindings.findBoundType(name);

        if (type != null) {
            return type;
        }
        
        /* but if not, use bounds... note that approach here is simplistic; not taking
         * into account possible multiple bounds, nor consider upper bounds.
         */
        /* 02-Mar-2011, tatu: As per issue#4, need to avoid self-reference cycles here;
         *   can be handled by (temporarily) adding binding:
         */
        if (typeBindings.hasUnbound(name)) {
            return sJavaLangObject;
        }
        typeBindings = typeBindings.withUnboundVariable(name);

        Type[] bounds = variable.getBounds();
        return _fromAny(context, bounds[0], typeBindings);
    }

    /*
    /**********************************************************************
    /* Internal methods, replacing and verifying type placeholders
    /**********************************************************************
     */

    /**
     * Method called to verify that types match; and if there are any placeholders,
     * replace them in <code>actualType</code>.
     *
     * @param sourceType Original base type used for specification/refinement
     * @param actualType Base type instance after re-resolving, possibly containing type placeholders
     */
    private void _resolveTypePlaceholders(ResolvedType sourceType, ResolvedType actualType)
        throws IllegalArgumentException
    {
        List<ResolvedType> expectedTypes = sourceType.getTypeParameters();
        List<ResolvedType> actualTypes = actualType.getTypeParameters();
        for (int i = 0, len = expectedTypes.size(); i < len; ++i) {
            ResolvedType exp = expectedTypes.get(i);
            ResolvedType act = actualTypes.get(i);
            if (!_verifyAndResolve(exp, act)) {
                throw new IllegalArgumentException("Type parameter #"+(i+1)+"/"+len+" differs; expected "
                        +exp.getBriefDescription()+", got "+act.getBriefDescription());
            }
        }
    }

    private boolean _verifyAndResolve(ResolvedType exp, ResolvedType act)
    {
        // See if we have an actual type placeholder to resolve; if yes, replace
        if (act instanceof TypePlaceHolder) {
            ((TypePlaceHolder) act).actualType(exp);
            return true;
        }
        // if not, try to verify compatibility. But note that we can not
        // use simple equality as we need to resolve recursively
        if (exp.getErasedType() != act.getErasedType()) {
            return false;
        }
        // But we can check type parameters "blindly"
        List<ResolvedType> expectedTypes = exp.getTypeParameters();
        List<ResolvedType> actualTypes = act.getTypeParameters();
        for (int i = 0, len = expectedTypes.size(); i < len; ++i) {
            ResolvedType exp2 = expectedTypes.get(i);
            ResolvedType act2 = actualTypes.get(i);
            if (!_verifyAndResolve(exp2, act2)) {
                return false;
            }
        }
        return true;
    }
}
