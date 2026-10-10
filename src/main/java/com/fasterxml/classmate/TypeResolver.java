package com.fasterxml.classmate;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.UnaryOperator;

import com.fasterxml.classmate.types.*;
import com.fasterxml.classmate.util.ClassKey;
import com.fasterxml.classmate.util.ClassStack;
import com.fasterxml.classmate.util.ResolvedTypeCache;
import com.fasterxml.classmate.util.ResolvedTypeKey;

/**
 * Object that is used for resolving generic type information of a class
 * so that it is accessible using simple API. Resolved types are also starting
 * point for accessing resolved (generics-aware) return and argument types
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
     * Simple cache of types resolved by this resolver.
     * Caching works because type instances themselves are mostly immutable;
     * and properly synchronized in cases where transient data (raw members) are
     * accessed.
     */
    protected final ResolvedTypeCache _resolvedTypes;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    /**
     * Constructs type cache; equivalent to:
     *<pre> 
     *   TypeResolver(ResolvedTypeCache.lruCache(200));
     *</pre>
     */
    public TypeResolver() {
        this(ResolvedTypeCache.lruCache(200));
    }

    /**
     * Constructor that specifies type cache to use.
     *
     * @param typeCache Cache to use for avoiding repeated resolution of already resolved
     *    types
     *
     * @since 1.4
     */
    public TypeResolver(ResolvedTypeCache typeCache) {
        _resolvedTypes = typeCache;
    }

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
     *  ResolvedType type = TypeResolver.resolve(new GenericType&lt;List&lt;Integer&gt;&gt;() { });
     *</pre>
     * Note that you can mix different types of type parameters, whether already
     * resolved ({@link ResolvedType}), type-erased ({@link java.lang.Class}) or
     * generic type reference ({@link GenericType}).
     *
     * @throws IllegalArgumentException If any of type parameters is a primitive type
     *   (since 1.8)
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
                return _completeType(_fromGenericType(null, (GenericType<?>) type, bindings));
            }
            ResolvedType rt = _fromAny(null, type, bindings);
            rawBase = rt.getErasedType();
        } else if (type instanceof ResolvedType) {
            ResolvedType rt = (ResolvedType) type;
            if (noParams) {
                // [classmate#128]: same as `resolve(TypeBindings, Type)`
                return _completeType(rt);
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
            // [classmate#128]: type parameters may be incomplete types (from earlier resolution)
            resolvedParams[i] = _completeType(_fromAny(null, typeParameters[i], bindings));
        }
        // and the type itself may be incomplete due to self-references in type parameters
        return _completeType(_fromClass(null, rawBase, TypeBindings.create(rawBase, resolvedParams)));
    }

    /**
     * Factory method for constructing array type of given element type.
     */
    public ResolvedArrayType arrayType(Type elementType)
    {
        ResolvedType resolvedElementType = resolve(TypeBindings.emptyBindings(), elementType);
        return _arrayOf(null, _arrayClassFor(resolvedElementType), resolvedElementType);
    }

    /**
     * Factory method for resolving specified Java {@link java.lang.reflect.Type}, given
     * {@link TypeBindings} needed to resolve any type variables.
     *<p>
     * Use of this method is discouraged (use if and only if you really know what you
     * are doing!); but if used, type bindings passed should come from {@link ResolvedType}
     * instance of declaring class (or interface).
     *<p>
     * NOTE: bindings are only used for resolving type variables (like {@code T}
     * or {@code List<T>}); they are NOT applied to {@link java.lang.Class} (raw type)
     * passed as {@code jdkType} itself: so passing bindings of {@code List<String>} with
     * {@code List.class} results in {@code List<Object>}. To construct parameterized
     * types, use {@link #resolve(Type, Type...)} instead.
     * (behavior changed in 1.8, see [classmate#125])
     *<p>
     * NOTE: order of arguments was reversed for 0.8, to avoid problems with
     * overload varargs method.
     */
    public ResolvedType resolve(TypeBindings typeBindings, Type jdkType)
    {
        // [classmate#128]: bindings may contain incomplete types (like when resolving
        // members of a type), which need to be completed where possible
        return _completeType(_fromAny(null, jdkType, typeBindings));
    }

    /**
     * Factory method for constructing sub-classing specified type; class specified
     * as sub-class must be compatible according to basic Java inheritance rules
     * (subtype must properly extend or implement specified supertype).
     *<p>
     * A typical use case here is to refine a generic type; for example, given
     * that we have generic type like <code>List&lt;Integer&gt;</code>, but we want
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
        // first: [classmate#127] replace self-references only valid within their original
        // resolution context (like supertype itself being one, or one nested within supertype
        // obtained from `getParentClass()`) with stand-alone types
        supertype = _resolveSelfReferences(supertype, null);
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
        TypeBindings tbForPlaceholders;

        if (paramCount == 0) { // no generics
            placeholders = null;
            // 26-Oct-2015, tatu: Used to do "full" call:
//            resolvedSubtype = resolve(subtype);
            // but should be able to streamline
            tbForPlaceholders = TypeBindings.emptyBindings();
        } else {
            placeholders = new TypePlaceHolder[paramCount];
            ResolvedType[] resolvedParams = new ResolvedType[paramCount];
            for (int i = 0; i < paramCount; ++i) {
                resolvedParams[i] = placeholders[i] = new TypePlaceHolder(i);
            }
            tbForPlaceholders = TypeBindings.create(subtype, resolvedParams);
        }
        resolvedSubtype = _fromClass(null, subtype, tbForPlaceholders);
        ResolvedType resolvedSupertype = resolvedSubtype.findSupertype(superclass);
        if (resolvedSupertype == null) { // sanity check, should never occur
            throw new IllegalArgumentException("Internal error: unable to locate supertype ("+subtype.getName()+") for type "+supertype.getBriefDescription());
        }
        // Ok, then, let's find and verify type assignments; resolve type holders if any
        // (and yes, even for no-type-parameters case)
        _resolveTypePlaceholders(supertype, resolvedSupertype, subtype, placeholders);
        // And then re-construct, if necessary
        if (paramCount == 0) { // if no type parameters, fine as is
            return resolvedSubtype;
        }
        // but with type parameters, need to reconstruct
        final ResolvedType[] typeParams = new ResolvedType[paramCount];
        for (int i = 0; i < paramCount; ++i) {
            ResolvedType t = placeholders[i].actualType();
            // Is it ok for it to be left unassigned? For now let's not allow that
            // 18-Oct-2017, tatu: Highly likely that we'll need to allow this, substitute with "unknown" --
            //    had to do that in Jackson. Occurs when subtype is generic, with "bogus" type declared
            //    but not bound in supertype(s). But leaving checking in for now.
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
     * Convenience method that can be used to checked whether given resolved type
     * (with erased type of <code>java.lang.Object</code>) is a placeholder
     * for "self-reference"; these are nasty recursive ("self") types
     * needed with some interfaces
     *
     * @param type Type to check
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
            // [classmate#125]: a Class here is a raw (or non-generic) type reference;
            // bindings of the enclosing context are not its own and must not be used
            // (otherwise raw `Map` within `Holder<String>` would become `Map<String>`)
            return _fromClass(context, (Class<?>) mainType, TypeBindings.emptyBindings());
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
        // [classmate#125]: Arrays have no type parameters of their own, so bindings
        // must not be retained (nor used for element type)
        if (rawType.isArray()) {
            return _arrayOf(context, rawType, _fromClass(context, rawType.getComponentType(),
                    TypeBindings.emptyBindings()));
        }
        // Second: recursive reference?
        if (context != null) {
            ClassStack prev = context.find(rawType);
            if (prev != null) {
                // Self-reference: needs special handling, then...
                // [classmate#128]: raw self-reference gets type parameters resolved to
                // their bounds, same as other raw types (unless already resolving bounds
                // of the type, to avoid infinite recursion)
                if (typeBindings.isEmpty() && (rawType.getTypeParameters().length > 0)
                        && !prev.isResolvingBounds()) {
                    typeBindings = TypeBindings.create(rawType,
                            _resolveBounds(context, prev, rawType));
                }
                ResolvedRecursiveType selfRef = new ResolvedRecursiveType(rawType, typeBindings);
                // [classmate#128]: also need to keep track of types containing self-references
                // (to types still being resolved) to avoid caching them
                ((ResolvedType) selfRef)._markIncomplete();
                context.selfReferenceCreated(selfRef, prev);
                return selfRef;
            }
        }

        // If not, already recently resolved?
        // 25-Oct-2015, tatu: one twist; if any TypePlaceHolders included, key will NOT be created,
        //   which means that caching should not be used (since type is mutable)
        // [classmate#125]: same for nested placeholders, self-references (only valid
        //   within resolution context)
        ResolvedTypeKey key = typeBindings.hasContextualTypes() ? null
                : _resolvedTypes.key(rawType, typeBindings.typeParameterArray());
        type = _findType(context, key);
        if (type != null) {
            return type;
        }
        // If not, need to construct
        context = (context == null) ? new ClassStack(rawType) : context.child(rawType);
        type = _constructType(context, rawType, typeBindings);
        // [classmate#128]: self-references with different bindings (like raw `Mid` within
        // `Mid<String>`) represent differently parameterized type, resolved lazily. Except
        // if within type parameters of the type itself (like `E` in raw `Enum<E extends Enum<E>>`)
        if (context.hasSelfReferences()) {
            final ResolvedType resolved = type;
            context.resolveSelfReferences(type, ref -> _representsReferenced(ref, resolved)
                    ? null : () -> _standaloneSelfReference(ref));
        }
        // [classmate#128]: nor can types with self-references to types still being
        // resolved (like `B` in `B extends Base<A>`, when resolving `A extends Base<B>`)
        // be cached, whether via type parameters, supertypes or array element types
        // (note: self-references created within this frame, like ones in bounds of
        // raw type, are accounted for by frame; but type parameters are resolved outside)
        boolean incomplete = context.hasOuterReferences();
        for (ResolvedType param : typeBindings.typeParameterArray()) {
            incomplete |= param._isIncomplete();
        }
        if (incomplete) {
            type._markIncomplete();
            context.addIncomplete(key, type);
        } else if (key != null) {
            _resolvedTypes.put(key, type);
        }
        return type;
    }

    /**
     * Helper method for replacing incomplete type (see {@link ResolvedType}) obtained
     * from an earlier resolution with stand-alone type, to avoid it being used outside
     * of its resolution context (see {@link #_resolveSelfReferences}). Called by public
     * entry points only.
     *<p>
     * NOTE: self-references themselves are retained as-is, as are incomplete types with
     * self-references (in type parameters) to enclosing types, since those are only valid
     * within the enclosing types: so returned type may still be incomplete.
     *
     * @since 1.8
     */
    private ResolvedType _completeType(ResolvedType type)
    {
        if (type._isIncomplete() && !isSelfReference(type)) {
            return _resolveSelfReferences(type, null);
        }
        return type;
    }

    /**
     * Helper method for finding cached type with given key, if any; or, failing that,
     * incomplete type constructed earlier during current resolution that may be reused.
     *
     * @since 1.8
     */
    private ResolvedType _findType(ClassStack context, ResolvedTypeKey key)
    {
        if (key == null) {
            return null;
        }
        ResolvedType type = _resolvedTypes.find(key);
        if ((type == null) && (context != null)) {
            type = context.findIncomplete(key);
        }
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
        // [classmate#125]: GenericType sub-class does not use bindings of enclosing context
        ResolvedType type = _fromClass(context, generic.getClass(), TypeBindings.emptyBindings());
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
        // (note: array types handled by `_fromClass()`)
        final TypeVariable<?>[] rawTypeParameters = rawType.getTypeParameters();
        // [classmate#53]: Handle raw generic types - resolve type parameters to their bounds
        // (note: [classmate#33] work-around for non-empty bindings of non-generic types
        // no longer needed as of [classmate#125]: such bindings are never passed)
        if (typeBindings.isEmpty() && (rawTypeParameters.length > 0)) {
            typeBindings = TypeBindings.create(rawType, _resolveBounds(context, context, rawType));
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

    /**
     * Helper method for resolving type parameters of a raw generic type to their bounds.
     *
     * @param context Resolution context to use
     * @param frame Stack frame of the raw type (to mark it as having its bounds resolved)
     */
    private ResolvedType[] _resolveBounds(ClassStack context, ClassStack frame, Class<?> rawType)
    {
        final TypeVariable<?>[] rawTypeParameters = rawType.getTypeParameters();
        final boolean wasResolving = frame.isResolvingBounds();
        frame.setResolvingBounds(true);
        try {
            ResolvedType[] types = new ResolvedType[rawTypeParameters.length];
            for (int i = 0; i < rawTypeParameters.length; ++i) {
                // Resolve each type parameter to its bound (similar to _fromVariable)
                TypeVariable<?> var = rawTypeParameters[i];
                // Avoid self-reference cycles by marking as unbound during resolution
                TypeBindings tempBindings = TypeBindings.emptyBindings()
                        .withUnboundVariable(var.getName());
                types[i] = _fromAny(context, var.getBounds()[0], tempBindings);
            }
            return types;
        } finally {
            frame.setResolvingBounds(wasResolving);
        }
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
        // [classmate#125]: bindings only needed for element type, not retained by array
        ResolvedType elementType = _fromAny(context, arrayType.getGenericComponentType(), typeBindings);
        return _arrayOf(context, _arrayClassFor(elementType), elementType);
    }

    private static Class<?> _arrayClassFor(ResolvedType elementType) {
        // Figuring out raw class for generic array is actually bit tricky...
        return Array.newInstance(elementType.getErasedType(), 0).getClass();
    }

    /**
     * Helper method for constructing (or finding cached) array type with given
     * element type. Arrays are cached using element type as the "type parameter"
     * of the key, so that differently parameterized element types do not collide.
     * Arrays with element types only valid within resolution context (self-references,
     * placeholders, types containing self-references to types still being resolved)
     * are not cached.
     *
     * @param context Resolution context, if any; {@code null} if none
     */
    private ResolvedArrayType _arrayOf(ClassStack context, Class<?> arrayClass, ResolvedType elementType)
    {
        ResolvedTypeKey key = TypeBindings.isContextual(elementType) ? null
                : _resolvedTypes.key(arrayClass, new ResolvedType[] { elementType });
        ResolvedType type = _findType(context, key);
        if (type == null) {
            type = new ResolvedArrayType(arrayClass, TypeBindings.emptyBindings(), elementType);
            // [classmate#128]: element type may contain self-references to types still being resolved
            if (elementType._isIncomplete()) {
                type._markIncomplete();
                if (context != null) {
                    context.addIncomplete(key, type);
                }
            } else if (key != null) {
                _resolvedTypes.put(key, type);
            }
        }
        return (ResolvedArrayType) type;
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
     * @param subtype Type-erased subtype being resolved (for error messages)
     * @param placeholders Placeholders for type parameters of subtype, if any (for error messages)
     */
    private void _resolveTypePlaceholders(ResolvedType sourceType, ResolvedType actualType,
            Class<?> subtype, TypePlaceHolder[] placeholders)
        throws IllegalArgumentException
    {
        List<ResolvedType> expectedTypes = sourceType.getTypeParameters();
        List<ResolvedType> actualTypes = actualType.getTypeParameters();
        for (int i = 0, len = expectedTypes.size(); i < len; ++i) {
            ResolvedType exp = expectedTypes.get(i);
            ResolvedType act = actualTypes.get(i);
            String msg;
            try {
                if (_verifyAndResolve(exp, act)) {
                    continue;
                }
                msg = "expected "+exp.getBriefDescription()+", got "+act.getBriefDescription();
            } catch (BindingConflict e) {
                msg = String.format("conflicting bindings for type variable `%s` of %s: %s vs %s",
                        subtype.getTypeParameters()[Arrays.asList(placeholders).indexOf(e.placeholder)].getName(),
                        subtype.getName(),
                        e.placeholder.actualType().getBriefDescription(), e.type.getBriefDescription());
            }
            throw new IllegalArgumentException("Type parameter #"+(i+1)+"/"+len+" differs; "+msg);
        }
    }

    /**
     * @param exp Expected type, from supertype being refined (with all self-references
     *    not valid outside their resolution context already replaced)
     * @param act Actual type, from re-resolved subtype; may contain placeholders
     *
     * @throws BindingConflict If a placeholder is already bound to an incompatible type
     */
    private boolean _verifyAndResolve(ResolvedType exp, ResolvedType act)
    {
        // See if we have an actual type placeholder to resolve; if yes, replace
        if (act instanceof TypePlaceHolder) {
            // [classmate#127]: primitive types are not valid type parameters
            if (exp.isPrimitive()) {
                return false;
            }
            // [classmate#127]: self-references to types enclosing `exp` (if any) are
            // not valid outside of it
            exp = _resolveSelfReferences(exp, null);
            TypePlaceHolder placeholder = (TypePlaceHolder) act;
            ResolvedType prev = placeholder.actualType();
            if (prev == null) {
                placeholder.actualType(exp);
                return true;
            }
            // [classmate#127]: if already bound, must be bound to compatible type
            ResolvedType merged = _mergeBindings(prev, exp);
            if (merged == null) {
                throw new BindingConflict(placeholder, exp);
            }
            placeholder.actualType(merged);
            return true;
        }
        // [classmate#127]: self-reference needs to be resolved to be comparable with
        // other types (but not with another self-reference)
        if (isSelfReference(exp) && !isSelfReference(act)) {
            exp = _selfReferenceTarget(exp);
        } else if (isSelfReference(act) && !isSelfReference(exp)) {
            // but only raw one: others have bindings (possibly with placeholders) to use
            if (act.getTypeBindings().isEmpty()
                    && (act.getErasedType().getTypeParameters().length > 0)) {
                act = _standaloneSelfReference(act);
            }
        }
        // [classmate#127]: Array types have no type parameters, so need to verify
        // (and resolve) element types instead. Must be done before erased type check
        // since array of placeholder has erased type of `Object[]`
        if (exp.isArray() != act.isArray()) {
            return false;
        }
        if (exp.isArray()) {
            return _verifyAndResolve(exp.getArrayElementType(), act.getArrayElementType());
        }
        // if not, try to verify compatibility. But note that we can not
        // use simple equality as we need to resolve recursively
        if (exp.getErasedType() != act.getErasedType()) {
            return false;
        }
        // But we can check type parameters "blindly"
        List<ResolvedType> expectedTypes = exp.getTypeParameters();
        List<ResolvedType> actualTypes = act.getTypeParameters();
        final int len = expectedTypes.size();
        if (len != actualTypes.size()) {
            return false;
        }
        for (int i = 0; i < len; ++i) {
            if (!_verifyAndResolve(expectedTypes.get(i), actualTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method for merging two bindings of the same type variable: they are
     * compatible if they are equal, except that {@code java.lang.Object} (which may come
     * from wildcard or raw type) is compatible with any non-primitive type, at any level
     * of nesting. If so, the more specific type is returned.
     *
     * @return Merged type, if types are compatible; {@code null} if not
     *
     * @since 1.8
     */
    private ResolvedType _mergeBindings(ResolvedType a, ResolvedType b)
    {
        // [classmate#128]: self-reference equal to fully resolved type, latter preferred
        if (ResolvedType._equalTypes(a, b)) {
            return isSelfReference(a) ? b : a;
        }
        if (_isJavaLangObject(a) && !b.isPrimitive()) {
            return b;
        }
        if (_isJavaLangObject(b) && !a.isPrimitive()) {
            return a;
        }
        // [classmate#128]: self-reference (like one in raw `Enum`) can be merged as the
        // type it represents; as long as that contains no self-references (to ensure
        // termination)
        if (isSelfReference(a) != isSelfReference(b)) {
            ResolvedType standalone = _standaloneSelfReference(isSelfReference(a) ? a : b);
            if (TypeBindings.isContextual(standalone)) {
                return null;
            }
            return isSelfReference(a) ? _mergeBindings(standalone, b) : _mergeBindings(a, standalone);
        }
        if (isSelfReference(a) || (a.isArray() != b.isArray())) {
            return null;
        }
        if (a.isArray()) {
            ResolvedType elemA = a.getArrayElementType();
            ResolvedType elemB = b.getArrayElementType();
            ResolvedType elem = _mergeBindings(elemA, elemB);
            if (elem == null) {
                return null;
            }
            if (elem == elemA) {
                return a;
            }
            if (elem == elemB) {
                return b;
            }
            return _arrayOf(null, _arrayClassFor(elem), elem);
        }
        if (a.getErasedType() != b.getErasedType()) {
            return null;
        }
        final List<ResolvedType> paramsA = a.getTypeParameters();
        final List<ResolvedType> paramsB = b.getTypeParameters();
        final int len = paramsA.size();
        if (len != paramsB.size()) {
            return null;
        }
        ResolvedType[] merged = new ResolvedType[len];
        boolean sameAsA = true, sameAsB = true;
        for (int i = 0; i < len; ++i) {
            merged[i] = _mergeBindings(paramsA.get(i), paramsB.get(i));
            if (merged[i] == null) {
                return null;
            }
            sameAsA &= (merged[i] == paramsA.get(i));
            sameAsB &= (merged[i] == paramsB.get(i));
        }
        if (sameAsA) {
            return a;
        }
        if (sameAsB) {
            return b;
        }
        return _fromClass(null, a.getErasedType(), TypeBindings.create(a.getErasedType(), merged));
    }

    private static boolean _isJavaLangObject(ResolvedType type) {
        return type.getErasedType() == Object.class;
    }

    /**
     * Helper method for finding stand-alone type to use in place of given self-reference:
     * the type referenced, if valid outside its resolution context and with matching
     * bindings; otherwise resolved using own bindings (raw one to bounds).
     *
     * @since 1.8
     */
    private ResolvedType _selfReferenceTarget(ResolvedType selfRef)
    {
        final ResolvedRecursiveType rrt = (ResolvedRecursiveType) selfRef;
        final ResolvedType ref = rrt.getSelfReferencedType();
        if ((ref != null) && !ref._isIncomplete()) {
            // [classmate#128]: actual type (with own bindings), if differs
            ResolvedType actual = rrt.getActualType();
            if (actual != ref) {
                return actual;
            }
            // otherwise referenced type, if self-reference represents it (only not known
            // if self-reference was not constructed by `TypeResolver`)
            if (_representsReferenced(rrt, ref)) {
                return ref;
            }
        }
        return _standaloneSelfReference(selfRef);
    }

    /**
     * Helper method for checking whether given self-reference represents the type it
     * references: that is, either has same type bindings, or is (directly) a type parameter
     * of the referenced type (like {@code E} in raw {@code Enum<E extends Enum<E>>}).
     * Note that self-references nested deeper (like {@code W<String,?>} within
     * {@code W<A, T extends List<W<String,?>>>}) represent differently parameterized types.
     *
     * @since 1.8
     */
    private static boolean _representsReferenced(ResolvedType selfRef, ResolvedType ref)
    {
        final TypeBindings refBindings = ref.getTypeBindings();
        if (refBindings.equals(selfRef.getTypeBindings())) {
            return true;
        }
        for (int i = 0, len = refBindings.size(); i < len; ++i) {
            ResolvedType t = refBindings.getBoundType(i);
            while (t.isArray()) {
                t = t.getArrayElementType();
            }
            if (t == selfRef) {
                return true;
            }
        }
        return false;
    }
    /**
     * Helper method for resolving stand-alone type that given self-reference represents,
     * using its own type bindings (with self-references in them resolved similarly).
     *
     * @since 1.8
     */
    private ResolvedType _standaloneSelfReference(ResolvedType selfRef)
    {
        final Class<?> erased = selfRef.getErasedType();
        final TypeBindings bindings = selfRef.getTypeBindings();
        if (bindings.isEmpty()) { // raw (or non-generic) type
            return _fromClass(null, erased, bindings);
        }
        ResolvedType[] params = _mapTypes(bindings, t -> _resolveSelfReferences(t, null));
        if (params == null) {
            params = bindings.typeParameterArray();
        }
        return _fromClass(null, erased, TypeBindings.create(erased, params));
    }

    /**
     * Helper method for applying given function to types of given bindings.
     *
     * @return Array of resulting types, if any changed (by identity); {@code null} if none
     *
     * @since 1.8
     */
    private static ResolvedType[] _mapTypes(TypeBindings bindings,
            UnaryOperator<ResolvedType> mapper)
    {
        ResolvedType[] types = null;
        for (int i = 0, len = bindings.size(); i < len; ++i) {
            ResolvedType t = bindings.getBoundType(i);
            ResolvedType newT = mapper.apply(t);
            if (newT != t) {
                if (types == null) {
                    types = bindings.typeParameterArray().clone();
                }
                types[i] = newT;
            }
        }
        return types;
    }

    /**
     * Helper method for replacing self-references ({@link ResolvedRecursiveType}s)
     * that are not valid outside their original resolution context with stand-alone
     * resolved types. This includes the type itself, as well as ones within type
     * parameters and array element types, at any level of nesting: except for
     * self-references to an enclosing type (like {@code E} in {@code Enum<E extends Enum<E>>})
     * which are valid as-is. Types containing such self-references via their supertypes
     * (see [classmate#128]) are re-resolved.
     *
     * @param enclosing Enclosing types (containing given type as type parameter), used as
     *    a stack (must be restored before returning); {@code null} if none
     *
     * @return Type with self-references replaced; given type itself if it contains none
     *
     * @since 1.8
     */
    private ResolvedType _resolveSelfReferences(ResolvedType type, List<ResolvedType> enclosing)
    {
        if (!TypeBindings.isContextual(type) && !type._isIncomplete()) {
            return type;
        }
        if (type.isArray()) {
            ResolvedType elem = type.getArrayElementType();
            ResolvedType newElem = _resolveSelfReferences(elem, enclosing);
            return (newElem == elem) ? type : _arrayOf(null, type.getErasedType(), newElem);
        }
        if (isSelfReference(type)) {
            // Self-reference to an enclosing type (by identity) is valid as-is
            // [classmate#128]: as long as it represents that type
            if (enclosing != null) {
                final ResolvedType ref = type.getSelfReferencedType();
                for (ResolvedType t : enclosing) {
                    if ((t == ref) && (((ResolvedRecursiveType) type).getActualType() == ref)) {
                        return type;
                    }
                }
            }
            return _selfReferenceTarget(type);
        }
        final List<ResolvedType> encl = (enclosing == null) ? new ArrayList<>() : enclosing;
        encl.add(type);
        final TypeBindings bindings = type.getTypeBindings();
        ResolvedType[] newTypes = _mapTypes(bindings, t -> _resolveSelfReferences(t, encl));
        encl.remove(encl.size() - 1);
        if (newTypes == null) {
            // [classmate#128]: type with self-references via supertypes needs to be re-resolved;
            // but not if it retains self-references to enclosing types (since those are
            // valid as-is, and would not be valid after re-resolution)
            if (!type._isIncomplete()) {
                return type;
            }
            if (bindings.hasContextualTypes()) {
                // ... although incomplete raw type with self-references to itself (like
                // `B<T extends B<T>>`) may be replaced with equal stand-alone raw type
                ResolvedType rawType = _fromClass(null, type.getErasedType(),
                        TypeBindings.emptyBindings());
                return rawType.equals(type) ? rawType : type;
            }
            newTypes = bindings.typeParameterArray();
        }
        final Class<?> raw = type.getErasedType();
        return _fromClass(null, raw, TypeBindings.create(raw, newTypes));
    }

    /**
     * Exception used to indicate that a type placeholder (type variable of subtype)
     * would need to be bound to two incompatible types.
     */
    @SuppressWarnings("serial")
    private static final class BindingConflict extends RuntimeException
    {
        final TypePlaceHolder placeholder;
        final ResolvedType type;

        BindingConflict(TypePlaceHolder placeholder, ResolvedType type) {
            super(null, null, false, false);
            this.placeholder = placeholder;
            this.type = type;
        }
    }
}
