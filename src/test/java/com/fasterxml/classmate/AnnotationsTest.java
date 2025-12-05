package com.fasterxml.classmate;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class AnnotationsTest
{
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Marker { }

    @Test @Marker
    public void addAsDefault() throws Exception {
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
    public void size() throws Exception {
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
    public void annotationsToSize() throws Exception {
        Annotations annotations = new Annotations();
        Method thisMethod = AnnotationsTest.class.getDeclaredMethod("addAsDefault");

        assertEquals("[null]", annotations.toString());

        Annotation testAnnotation = thisMethod.getAnnotation(Test.class);
        annotations.addAsDefault(testAnnotation);

        // order is unspecified as the internal representation is a HashMap; just assert the constituent parts are present
        String asString = _normalize(annotations.toString());
        assertTrue(asString.contains("{interface org.junit.Test=@org.junit.Test("));
        assertTrue(asString.contains("timeout=0"));

        // 15-Nov-2016, tatu: Java 9 changes description slightly, need to modify
        // 05-Dec-2025, tatu: Java 21 adds further variation
        if (!(asString.contains("expected=class org.junit.Test.None") // until Java 8
                || asString.contains("expected=org.junit.Test.None"))) {
            fail("No 'expected' in: "+asString);
        }

        Annotation markerAnnotation = thisMethod.getAnnotation(Marker.class);
        annotations.addAsDefault(markerAnnotation);

        asString = _normalize(annotations.toString());

        String exp = "interface com.fasterxml.classmate.AnnotationsTest.Marker=@com.fasterxml.classmate.AnnotationsTest.Marker()";
        if (!asString.contains(exp)) {
            fail("Expected: ["+exp+"]\nin ["+asString+"]");
        }
        assertTrue(asString.contains("interface org.junit.Test=@org.junit.Test"));
        assertTrue(asString.contains("timeout=0"));
        // 15-Nov-2016, tatu: Java 9 changes description slightly, need to modify
        // 05-Dec-2025, tatu: Java 21 adds further variation
        if (!(asString.contains("expected=class org.junit.Test$None") // until Java 8
                || asString.contains("expected=org.junit.Test$None") // Java 9 - 17
                || asString.contains("expected=org.junit.Test.None"))) {
            fail("No 'expected' in: "+asString);
        }
    }

    private static String _normalize(String str) {
        // 05-Dec-2025, tatu: Java 21 changes from "org.junit.Test$None" to "org.junit.Test.None"
        String str2;
        while ((str2 = str.replace('$', '.')) != str) {
            str = str2;
        }
        return str;
    }
}
