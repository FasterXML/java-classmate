package com.fasterxml.classmate;

import org.junit.Test;

import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 1:31 PM
 */
public class AnnotationInclusionTest {

    @Test
    public void values() {  // stupid test for code coverage (being pedantic here)
        includesEnum(AnnotationInclusion.DONT_INCLUDE);
        includesEnum(AnnotationInclusion.INCLUDE_AND_INHERIT);
        includesEnum(AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
    }

    private void includesEnum(AnnotationInclusion toFind) {
        AnnotationInclusion[] annotationInclusions = AnnotationInclusion.values();
        boolean found = false;
        for (AnnotationInclusion annotationInclusion : annotationInclusions) {
            if (annotationInclusion == toFind) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void valueOf() {
        AnnotationInclusion dontInclude = AnnotationInclusion.valueOf("DONT_INCLUDE");
        assertSame(AnnotationInclusion.DONT_INCLUDE, dontInclude);
        AnnotationInclusion includeAndInherit = AnnotationInclusion.valueOf("INCLUDE_AND_INHERIT");
        assertSame(AnnotationInclusion.INCLUDE_AND_INHERIT, includeAndInherit);
        AnnotationInclusion includeButDontInherit = AnnotationInclusion.valueOf("INCLUDE_BUT_DONT_INHERIT");
        assertSame(AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT, includeButDontInherit);
    }

}
