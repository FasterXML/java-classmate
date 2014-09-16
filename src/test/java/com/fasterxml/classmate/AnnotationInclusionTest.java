package com.fasterxml.classmate;

import org.junit.Test;

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
}
