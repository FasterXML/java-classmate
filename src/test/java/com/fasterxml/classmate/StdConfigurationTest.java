package com.fasterxml.classmate;

import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 2:49 PM
 */
public class StdConfigurationTest {

    @Test
    public void getInclusionForClass() {
        AnnotationConfiguration.StdConfiguration config = new AnnotationConfiguration.StdConfiguration(null);
        assertNull(config.getInclusionForClass(Test.class));

        config = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.DONT_INCLUDE);
        AnnotationInclusion result = config.getInclusionForClass(Test.class);
        assertSame(AnnotationInclusion.DONT_INCLUDE, result);
        config.setInclusion(Test.class, AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
        result = config.getInclusionForClass(Test.class);
        assertSame(AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT, result);
    }

    @Test
    public void getInclusionForConstructor() {
        AnnotationConfiguration.StdConfiguration config = new AnnotationConfiguration.StdConfiguration(null);
        assertNull(config.getInclusionForConstructor(Test.class));

        config = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.DONT_INCLUDE);
        AnnotationInclusion result = config.getInclusionForConstructor(Test.class);
        assertSame(config.getInclusionForClass(Test.class), result);
        config.setInclusion(Test.class, AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
        result = config.getInclusionForConstructor(Test.class);
        assertSame(config.getInclusionForClass(Test.class), result);
    }

    @Test
    public void getInclusionForField() {
        AnnotationConfiguration.StdConfiguration config = new AnnotationConfiguration.StdConfiguration(null);
        assertNull(config.getInclusionForField(Test.class));

        config = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.DONT_INCLUDE);
        AnnotationInclusion result = config.getInclusionForField(Test.class);
        assertSame(config.getInclusionForClass(Test.class), result);
        config.setInclusion(Test.class, AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
        result = config.getInclusionForField(Test.class);
        assertSame(config.getInclusionForClass(Test.class), result);
    }

    @Test
    public void getInclusionForMethod() {
        AnnotationConfiguration.StdConfiguration config = new AnnotationConfiguration.StdConfiguration(null);
        assertNull(config.getInclusionForMethod(Test.class));

        config = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.DONT_INCLUDE);
        AnnotationInclusion result = config.getInclusionForMethod(Test.class);
        assertSame(config.getInclusionForClass(Test.class), result);
        config.setInclusion(Test.class, AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
        result = config.getInclusionForMethod(Test.class);
        assertSame(config.getInclusionForClass(Test.class), result);
    }
}
