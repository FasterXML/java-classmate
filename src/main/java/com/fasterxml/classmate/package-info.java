/**
 * Package that contains main public interface of ClassMate
 * package.
 *<p>
 * Most commonly resolution starts with {@link com.fasterxml.classmate.TypeResolver},
 * using its <code>resolve()</code> method, which returns a
 * {@link com.fasterxml.classmate.ResolvedType} instance.
 * These type objects contain all necessary information about type itself;
 * but if type information on members (fields, methods, constructors, static
 * members) is needed, {@link com.fasterxml.classmate.MemberResolver} can
 * resolve types for members: it takes {@link com.fasterxml.classmate.ResolvedType}s.
 */
package com.fasterxml.classmate;
