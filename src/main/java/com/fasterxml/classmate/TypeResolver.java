package com.fasterxml.classmate;

import java.lang.reflect.*;
import java.util.HashMap;

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
     * "Void" type is sometimes used as placeholder, so let's pre-create it
     */
    private final static ResolvedPrimitiveType sVoid = new ResolvedPrimitiveType(Void.TYPE, 'V');

    /**
     * We will also need to return "unknown" type for cases where type variable binding
     * is not found ('raw' instances of generic types); easiest way is to
     * pre-create type for <code>java.lang.Object</code>
     */
    private final static ResolvedConcreteClass sJavaLangObject =
        new ResolvedConcreteClass(Object.class, null, null, null);
    
    /**
     * Since number of primitive types is small, and they are frequently needed,
     * let's actually pre-create them for efficient reuse
     */
    protected final static HashMap<ClassKey, ResolvedPrimitiveType> _primitiveTypes;
    static {
        _primitiveTypes = new HashMap<ClassKey, ResolvedPrimitiveType>(16);
        _primitiveTypes.put(new ClassKey(Boolean.TYPE), new ResolvedPrimitiveType(Boolean.TYPE, 'Z'));
        _primitiveTypes.put(new ClassKey(Byte.TYPE), new ResolvedPrimitiveType(Byte.TYPE, 'B'));
        _primitiveTypes.put(new ClassKey(Short.TYPE), new ResolvedPrimitiveType(Short.TYPE, 'S'));
        _primitiveTypes.put(new ClassKey(Character.TYPE), new ResolvedPrimitiveType(Character.TYPE, 'C'));
        _primitiveTypes.put(new ClassKey(Integer.TYPE), new ResolvedPrimitiveType(Integer.TYPE, 'I'));
        _primitiveTypes.put(new ClassKey(Long.TYPE), new ResolvedPrimitiveType(Long.TYPE, 'J'));
        _primitiveTypes.put(new ClassKey(Float.TYPE), new ResolvedPrimitiveType(Float.TYPE, 'F'));
        _primitiveTypes.put(new ClassKey(Double.TYPE), new ResolvedPrimitiveType(Double.TYPE, 'D'));
        // should we include "void"? might as well...
        _primitiveTypes.put(new ClassKey(Void.TYPE), sVoid);
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
        // First: a primitive type perhaps?
        ResolvedType type = _primitiveTypes.get(new ClassKey(rawType));
        if (type != null) {
            return type;
        }
        // with erased class, no bindings:
        return _fromClass(rawType, TypeBindings.emptyBindings());
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
            resolvedParams[i] = _fromClass(typeParameters[i], bindings);
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
        return _fromClass(type, TypeBindings.create(type, typeParameters));
    }
    
    /**
     * Factory method for resolving given generic type.
     */
    public ResolvedType resolve(GenericType<?> type)
    {
        return _fromAny(type.getType(), TypeBindings.emptyBindings());
    }
    
    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    private ResolvedType _fromAny(Type mainType, TypeBindings typeBindings)
    {
        if (mainType instanceof Class<?>) {
            return _fromClass((Class<?>) mainType, typeBindings);
        }
        if (mainType instanceof ParameterizedType) {
            return _fromParamType((ParameterizedType) mainType, typeBindings);
        }
        if (mainType instanceof GenericArrayType) {
            return _fromArrayType((GenericArrayType) mainType, typeBindings);
        }
        if (mainType instanceof TypeVariable<?>) {
            return _fromVariable((TypeVariable<?>) mainType, typeBindings);
        }
        if (mainType instanceof WildcardType) {
            return _fromWildcard((WildcardType) mainType, typeBindings);
        }
        // should never get here...
        throw new IllegalArgumentException("Unrecognized type class: "+mainType.getClass().getName());
    }

    private ResolvedType _fromClass(Class<?> cls, TypeBindings typeBindings)
    {
        return null;
    }
    
    private ResolvedType _fromParamType(ParameterizedType ptype, TypeBindings typeBindings)
    {
        return null;
    }

    private ResolvedType _fromArrayType(GenericArrayType arrayType, TypeBindings typeBindings)
    {
        return null;
    }

    private ResolvedType _fromWildcard(WildcardType wcType, TypeBindings typeBindings)
    {
        return null;
    }
    
    private ResolvedType _fromVariable(TypeVariable<?> variable, TypeBindings typeBindings)
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
        return _fromAny(bounds[0], typeBindings);
    }

    /*
    /**********************************************************************
    /* Manual test method(s)
    /**********************************************************************
     */
    
    @SuppressWarnings("serial")
    static class StringIntMap extends StringKeyMap<Integer> { }
    @SuppressWarnings("serial")
    static class StringKeyMap<V> extends java.util.HashMap<String,V> { }
    
    public static void main(String[] args)
    {
        print(StringIntMap.class, false);
    }

    public static void print(java.lang.reflect.Type t, boolean supertype)
    {
        if (t == null) return; // to stop superclasses
        if (supertype) {
            System.out.print(" -> supertype = ");
        }
        if (t instanceof Class<?>) {
            Class<?> cls = (Class<?>) t;
            System.out.println("Class "+cls.getName());
            print(cls.getGenericSuperclass(), true);
            return;
        }
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            Class<?> cls = (Class<?>) pt.getRawType();
            System.out.println("ParametricType "+cls.getName()+", actuals = {");
            Type[] types = pt.getActualTypeArguments();
            TypeVariable<?>[] vars = cls.getTypeParameters();
            int count = 0;
            for (Type t2 : types) {
                System.out.print(" type param #"+count+", '"+vars[count].getName()+"' = ");
                print(t2, false);
                ++count;
            }
            System.out.println("}");
            print(cls.getGenericSuperclass(), true);
            return;
        }
        if (t instanceof TypeVariable<?>) {
            System.out.println("Type variable '"+((TypeVariable<?>) t).getName()+"'");
            return;
        }
        throw new Error("Weird type: "+t.getClass());
    }
}
