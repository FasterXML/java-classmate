package com.fasterxml.classmate;

/**
 * Interface that defines API for basic filtering objects, used to prune set
 * of things to include in result sets like flattened member lists.
 */
public interface Filter<T>
{
    public boolean include(T element);
}
