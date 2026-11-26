package com.fasterxml.classmate;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AnnotationInclusionTest
    extends BaseTest
{
    @Test
    public void testValues() {  // stupid test for code coverage (being pedantic here)
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
