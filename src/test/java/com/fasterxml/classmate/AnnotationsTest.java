package com.fasterxml.classmate;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 1:21 PM
 */
public class AnnotationsTest {

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Marker { }

    @Test @Marker
    public void addAsDefault() throws NoSuchMethodException {
        Annotations annotations = new Annotations();
        Method thisMethod = AnnotationsTest.class.getDeclaredMethod("addAsDefault");

        assertNull(annotations.get(Test.class));
        assertNull(annotations.get(Marker.class));

        Annotation testAnnotation = thisMethod.getAnnotation(Test.class);
        annotations.addAsDefault(testAnnotation);

        Annotation markerAnnotation = thisMethod.getAnnotation(Marker.class);
        annotations.addAsDefault(markerAnnotation);

        assertNotNull(annotations.get(Test.class));
        assertNotNull(annotations.get(Marker.class));
    }

    @Test
    public void size() throws NoSuchMethodException {
        Annotations annotations = new Annotations();
        Method thisMethod = AnnotationsTest.class.getDeclaredMethod("addAsDefault");

        assertEquals(0, annotations.size());

        Annotation testAnnotation = thisMethod.getAnnotation(Test.class);
        annotations.addAsDefault(testAnnotation);

        assertEquals(1, annotations.size());

        Annotation markerAnnotation = thisMethod.getAnnotation(Marker.class);
        annotations.addAsDefault(markerAnnotation);

        assertEquals(2, annotations.size());
    }

    @Test
    public void annotationsToSize() throws NoSuchMethodException {
        Annotations annotations = new Annotations();
        Method thisMethod = AnnotationsTest.class.getDeclaredMethod("addAsDefault");

        assertEquals("[null]", annotations.toString());

        Annotation testAnnotation = thisMethod.getAnnotation(Test.class);
        annotations.addAsDefault(testAnnotation);

        assertEquals("{interface org.junit.Test=@org.junit.Test(expected=class org.junit.Test$None, timeout=0)}", annotations.toString());

        Annotation markerAnnotation = thisMethod.getAnnotation(Marker.class);
        annotations.addAsDefault(markerAnnotation);

        // order is unspecified as the internal representation is a HashMap; just assert the constituent parts are present
        String asString = annotations.toString();
        assertTrue(asString.contains("interface com.fasterxml.classmate.AnnotationsTest$Marker=@com.fasterxml.classmate.AnnotationsTest$Marker()"));
        assertTrue(asString.contains("interface org.junit.Test=@org.junit.Test(expected=class org.junit.Test$None, timeout=0)"));
    }

}
