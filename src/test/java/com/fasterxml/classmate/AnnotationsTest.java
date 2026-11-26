package com.fasterxml.classmate;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationsTest
{
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Marker { }

    // Annotation with a property, to verify `toString()` output
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Configured {
        int timeout() default 0;
    }

    @Test @Marker @Configured
    public void addAsDefault() throws Exception {
        Annotations annotations = new Annotations();
        Method thisMethod = AnnotationsTest.class.getDeclaredMethod("addAsDefault");

        assertNull(annotations.get(Configured.class));
        assertNull(annotations.get(Marker.class));

        Annotation testAnnotation = thisMethod.getAnnotation(Configured.class);
        annotations.addAsDefault(testAnnotation);

        Annotation markerAnnotation = thisMethod.getAnnotation(Marker.class);
        annotations.addAsDefault(markerAnnotation);

        assertNotNull(annotations.get(Configured.class));
        assertNotNull(annotations.get(Marker.class));

        assertEquals(2, annotations.size());
        assertEquals(2, annotations.asList().size());
        assertEquals(2, annotations.asArray().length);
    }

    @Test
    public void size() throws Exception {
        Annotations annotations = new Annotations();
        Method thisMethod = AnnotationsTest.class.getDeclaredMethod("addAsDefault");

        assertEquals(0, annotations.size());

        Annotation testAnnotation = thisMethod.getAnnotation(Configured.class);
        annotations.addAsDefault(testAnnotation);

        assertEquals(1, annotations.size());

        Annotation markerAnnotation = thisMethod.getAnnotation(Marker.class);
        annotations.addAsDefault(markerAnnotation);

        assertEquals(2, annotations.size());
    }

    @Test
    public void annotationsToSize() throws Exception {
        Annotations annotations = new Annotations();
        Method thisMethod = AnnotationsTest.class.getDeclaredMethod("addAsDefault");

        assertEquals("[null]", annotations.toString());

        Annotation testAnnotation = thisMethod.getAnnotation(Configured.class);
        annotations.addAsDefault(testAnnotation);

        // order is unspecified as the internal representation is a HashMap; just assert the constituent parts are present
        String asString = _normalize(annotations.toString());
        assertTrue(asString.contains("{interface com.fasterxml.classmate.AnnotationsTest.Configured=@com.fasterxml.classmate.AnnotationsTest.Configured("),
                asString);
        assertTrue(asString.contains("timeout=0"), asString);

        Annotation markerAnnotation = thisMethod.getAnnotation(Marker.class);
        annotations.addAsDefault(markerAnnotation);

        asString = _normalize(annotations.toString());

        String exp = "interface com.fasterxml.classmate.AnnotationsTest.Marker=@com.fasterxml.classmate.AnnotationsTest.Marker()";
        if (!asString.contains(exp)) {
            fail("Expected: ["+exp+"]\nin ["+asString+"]");
        }
        assertTrue(asString.contains("interface com.fasterxml.classmate.AnnotationsTest.Configured=@com.fasterxml.classmate.AnnotationsTest.Configured("),
                asString);
        assertTrue(asString.contains("timeout=0"), asString);
    }

    private static String _normalize(String str) {
        // Nested type names use '$' on some JDKs and '.' on others
        String str2;
        while ((str2 = str.replace('$', '.')) != str) {
            str = str2;
        }
        return str;
    }
}
