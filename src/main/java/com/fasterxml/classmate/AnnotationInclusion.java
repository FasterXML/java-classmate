package com.fasterxml.classmate;

/**
 * Enumeration that defines different settings for handling behavior
 * of individual annotations
 */
public enum AnnotationInclusion
{
    /**
     * Value that indicates that annotation is to be ignored, not included
     * in resolved bean information.
     */
    DONT_INCLUDE,

    /**
     * Value that indicates that annotation is to be included in results, but
     * only if directly associated with included member (or attached mix-in);
     * will not inherit from supertypes.
     */
    INCLUDE_BUT_DONT_INHERIT,

    /**
     * Value that indicates that annotation is to be included in results; and
     * values from overridden members are also inherited if not overridden
     * by members of subtypes.
     */
    INCLUDE_AND_INHERIT
    ;

}
