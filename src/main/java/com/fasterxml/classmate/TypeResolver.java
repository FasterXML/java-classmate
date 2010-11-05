package com.fasterxml.classmate;

import java.lang.reflect.*;

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
    /**
     * Simple cache of types resolved by this resolved; capped to last 200 resolved types.
     */
    protected final ResolvedTypeCache _resolvedTypes = new ResolvedTypeCache(200);

    public TypeResolver() { }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////////////////
     */
    
    /**
     * Factory method for resolving a type-erased class; in this case any
     * generic type information has to come from super-types (via inheritance).
     */
    public static ResolvedType resolve(Class<?> type) {
        // !!! TBI
        return null;
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
    public static ResolvedType resolve(Class<?> type, Class<?>... typeParameters)
    {
        if (typeParameters == null || typeParameters.length == 0) {
            return resolve(type);
        }
        // First: resolve type parameters
        int len = typeParameters.length;
        ResolvedType[] resolvedParams = new ResolvedType[len];
        for (int i = 0; i < len; ++i) {
            resolvedParams[i] = resolve(typeParameters[i]);
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
    public static ResolvedType resolve(Class<?> type, ResolvedType[] typeParameters)
    {
        if (typeParameters == null || typeParameters.length == 0) {
            return resolve(type);
        }
        // !!! TBI
        return null;
    }
    
    /**
     * Factory method for resolving given generic type.
     */
    public static ResolvedType resolve(GenericType<?> type)
    {
        // !!! TBI
        return null;
    }
    
    /*
    ///////////////////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////////////////
     */

    static class StringIntMap extends StringKeyMap<Integer> { }
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
