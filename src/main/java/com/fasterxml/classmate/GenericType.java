package com.fasterxml.classmate;

import java.io.Serializable;

/**
 * This class is used to pass full generics type information, and
 * avoid problems with type erasure (that basically removes most
 * usable type references from runtime Class objects).
 * It is based on ideas from
 * <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html"
 * >http://gafter.blogspot.com/2006/12/super-type-tokens.html</a>,
 *<p>
 * Usage is by sub-classing: here is one way to instantiate reference
 * to generic type <code>List&lt;Integer></code>:
 *<pre>
 *  GenericType type = new GenericType&lt;List&lt;Integer>>() { };
 *</pre>
 * which can be passed to methods that accept <code>GenericReference</code>.
 */
@SuppressWarnings("serial")
public abstract class GenericType<T>
    implements Serializable, java.lang.reflect.Type
{
    protected GenericType() { }
}
