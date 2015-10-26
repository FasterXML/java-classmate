package com.fasterxml.classmate;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("deprecation")
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

        assertEquals(2, annotations.size());
        assertEquals(2, annotations.asList().size());
        assertEquals(2, annotations.asArray().length);
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

        // order is unspecified as the internal representation is a HashMap; just assert the constituent parts are present
        String asString = annotations.toString();
        assertTrue(asString.contains("{interface org.junit.Test=@org.junit.Test("));
        assertTrue(asString.contains("timeout=0"));
        assertTrue(asString.contains("expected=class org.junit.Test$None"));

        Annotation markerAnnotation = thisMethod.getAnnotation(Marker.class);
        annotations.addAsDefault(markerAnnotation);

        asString = annotations.toString();
        assertTrue(asString.contains("interface com.fasterxml.classmate.AnnotationsTest$Marker=@com.fasterxml.classmate.AnnotationsTest$Marker()"));
        assertTrue(asString.contains("interface org.junit.Test=@org.junit.Test"));
        assertTrue(asString.contains("timeout=0"));
        assertTrue(asString.contains("expected=class org.junit.Test$None"));
    }
}
