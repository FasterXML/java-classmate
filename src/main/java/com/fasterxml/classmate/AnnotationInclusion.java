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
     * Applicable to all member types.
     */
    DONT_INCLUDE,

    /**
     * Value that indicates that annotation is to be included in results, but
     * only if directly associated with included member (or attached mix-in);
     * will not inherit from supertypes.
     * Applicable only to member methods; if used with other members will
     * mean basic inclusion.
     */
    INCLUDE_BUT_DONT_INHERIT,

    /**
     * Value that indicates that annotation is to be included in results, and
     * values from overridden members are inherited only if the annotation is
     * marked with the {@link java.lang.annotation.Inherited} annotation.
     * Applicable only to member methods; if used with other members will
     * mean basic inclusion.
     */
    INCLUDE_AND_INHERIT_IF_INHERITED,

    /**
     * Value that indicates that annotation is to be included in results; and
     * values from overridden members are also inherited if not overridden
     * by members of subtypes.
     * Note that inheritance only matters with member methods; for other types
     * it just means "include".
     */
    INCLUDE_AND_INHERIT
    ;

}
