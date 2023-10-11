package com.fasterxml.classmate;

/**
 * This class is used to pass full generics type information, and
 * avoid problems with type erasure (that basically removes most
 * usable type references from runtime Class objects).
 * It is based on ideas from
 * <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html"
 * >http://gafter.blogspot.com/2006/12/super-type-tokens.html</a>,
 *<p>
 * Usage is by sub-classing: here is one way to instantiate reference
 * to generic type <code>List&lt;Integer&gt;</code>:
 *<pre>
 *  GenericType type = new GenericType&lt;List&lt;Integer&gt;&gt;() { };
 *</pre>
 * which can be passed to methods that accept <code>GenericReference</code>.
 *<p>
 * NOTE: before version 1.6 implemented {@link java.io.Serializable}.
 * Removed due to
 * <a href="https://github.com/FasterXML/java-classmate/issues/73">issue #73</a>.
 */
public abstract class GenericType<T>
    implements java.lang.reflect.Type
{
    protected GenericType() { }
}
