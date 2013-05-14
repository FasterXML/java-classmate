package com.fasterxml.classmate;

import com.fasterxml.classmate.members.*;
import org.junit.Test;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * User: blangel
 * Date: 4/26/12
 * Time: 1:49 PM
 *
 * Test the examples listed in ReadMe to ensure correctness.
 */
public class TestReadme {

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Marker { }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public static @interface MarkerA { }

    public static class SomeClass {
        @Marker @MarkerA
        public void someMethod() { }
    }
    public static class SomeSubclass extends SomeClass {
        @Override
        public void someMethod() { }
    }

    public static class SomeOtherClass {
        public void someMethod() { }
    }

    @Test @SuppressWarnings("serial")
    public void resolvingClasses() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType listType = typeResolver.resolve(List.class);
        assertEquals("java.util.List extends java.util.Collection<java.lang.Object>", listType.getFullDescription());

        typeResolver = new TypeResolver();
        listType = typeResolver.resolve(List.class, String.class);
        assertEquals("java.util.List<java.lang.String> extends java.util.Collection<java.lang.String>", listType.getFullDescription());

        typeResolver = new TypeResolver();
        ResolvedType stringType = typeResolver.resolve(String.class);
        listType = typeResolver.resolve(List.class, stringType);
        assertEquals("java.util.List<java.lang.String> extends java.util.Collection<java.lang.String>", listType.getFullDescription());

        typeResolver = new TypeResolver();
        listType = typeResolver.resolve(new GenericType<List<String>>() {});
        assertEquals("java.util.List<java.lang.String> extends java.util.Collection<java.lang.String>", listType.getFullDescription());
    }

    @Test
    public void resolvingAllMembers()
    {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType arrayListType = typeResolver.resolve(ArrayList.class, String.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        ResolvedTypeWithMembers arrayListTypeWithMembers = memberResolver.resolve(arrayListType, null, null);
        ResolvedMethod[] staticArrayListMethods = arrayListTypeWithMembers.getStaticMethods();
        /* 13-May-2013, tatu: Java 7 causing trouble here, adding 2 new static methods...
         *   Needs to be fixed.
         */
        if (0 != staticArrayListMethods.length) {
            fail("Should not find static methods in ArrayList, but found "+staticArrayListMethods.length
                    +": "+Arrays.asList(staticArrayListMethods));
        }
        ResolvedMethod[] arrayListMethods = arrayListTypeWithMembers.getMemberMethods();
        assertEquals(34, arrayListMethods.length);

        ResolvedField[] arrayListFields = arrayListTypeWithMembers.getMemberFields();
        assertEquals(3, arrayListFields.length);

        ResolvedConstructor[] arrayListConstructors = arrayListTypeWithMembers.getConstructors();
        assertEquals(3, arrayListConstructors.length);
    }

    @Test
    public void resolvingParticularMembers() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType arrayListType = typeResolver.resolve(ArrayList.class, String.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "size".equals(element.getName());
            }
        });
        ResolvedTypeWithMembers arrayListTypeWithMembers = memberResolver.resolve(arrayListType, null, null);
        ResolvedMethod sizeMethod = arrayListTypeWithMembers.getMemberMethods()[0];
        assertNotNull(sizeMethod);
        assertEquals("size", sizeMethod.getName());

        memberResolver = new MemberResolver(typeResolver);
        memberResolver.setFieldFilter(new Filter<RawField>() {
            @Override public boolean include(RawField element) {
                return "size".equals(element.getName());
            }
        });
        arrayListTypeWithMembers = memberResolver.resolve(arrayListType, null, null);
        ResolvedField sizeField = arrayListTypeWithMembers.getMemberFields()[0];
        assertNotNull(sizeField);
        assertEquals("size", sizeField.getName());
    }

    @Test
    public void testSomeClassSomeMethod() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType someType = typeResolver.resolve(SomeClass.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "someMethod".equals(element.getName());
            }
        });
        AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
        ResolvedTypeWithMembers someTypeWithMembers = memberResolver.resolve(someType, annConfig, null);
        ResolvedMethod someMethod = someTypeWithMembers.getMemberMethods()[0];
        Marker marker = someMethod.get(Marker.class);  // marker != null
        assertNotNull(marker);
        MarkerA markerA = someMethod.get(MarkerA.class); // markerA != null
        assertNotNull(markerA);
    }

    @Test
    public void testSomeSubclassSomeMethod() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType someSubclassType = typeResolver.resolve(SomeSubclass.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "someMethod".equals(element.getName());
            }
        });

        AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
        ResolvedTypeWithMembers someSubclassTypeWithMembers = memberResolver.resolve(someSubclassType, annConfig, null);
        ResolvedMethod someMethod = someSubclassTypeWithMembers.getMemberMethods()[0];
        Marker marker = someMethod.get(Marker.class);  // marker == null
        assertNull(marker);
        MarkerA markerA = someMethod.get(MarkerA.class); // markerA == null
        assertNull(markerA);
        Override override = someMethod.get(Override.class); // override == null (RetentionPolicy = SOURCE)
        assertNull(override);
    }

    @Test
    public void testSomeSubclassSomeMethodWithInherited() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType someSubclassType = typeResolver.resolve(SomeSubclass.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "someMethod".equals(element.getName());
            }
        });
        AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED);
        ResolvedTypeWithMembers someSubclassTypeWithMembers = memberResolver.resolve(someSubclassType, annConfig, null);
        ResolvedMethod someMethod = someSubclassTypeWithMembers.getMemberMethods()[0];
        Marker marker = someMethod.get(Marker.class);  // marker == null
        assertNull(marker);
        MarkerA markerA = someMethod.get(MarkerA.class); // markerA != null
        assertNotNull(markerA);
        Override override = someMethod.get(Override.class); // override == null (RetentionPolicy = SOURCE)
        assertNull(override);
    }

    @Test
    public void testSomeSubclassSomeMethodWithAllInherited() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType someSubclassType = typeResolver.resolve(SomeSubclass.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "someMethod".equals(element.getName());
            }
        });
        AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT);
        ResolvedTypeWithMembers someSubclassTypeWithMembers = memberResolver.resolve(someSubclassType, annConfig, null);
        ResolvedMethod someMethod = someSubclassTypeWithMembers.getMemberMethods()[0];
        Marker marker = someMethod.get(Marker.class);  // marker != null
        assertNotNull(marker);
        MarkerA markerA = someMethod.get(MarkerA.class); // markerA != null
        assertNotNull(markerA);
        Override override = someMethod.get(Override.class); // override == null (RetentionPolicy = SOURCE)
        assertNull(override);
    }

    @Test
    public void testSomeOtherClassSomeMethodWithoutMixins() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType someOtherType = typeResolver.resolve(SomeOtherClass.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "someMethod".equals(element.getName());
            }
        });
        AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT);
        ResolvedTypeWithMembers someOtherTypeWithMembers = memberResolver.resolve(someOtherType, annConfig, null);
        ResolvedMethod someMethod = someOtherTypeWithMembers.getMemberMethods()[0];
        Marker marker = someMethod.get(Marker.class);  // marker == null
        assertNull(marker);
        MarkerA markerA = someMethod.get(MarkerA.class); // markerA == null
        assertNull(markerA);
    }

    @Test
    public void testSomeOtherClassSomeMethodWithMixins() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType someOtherType = typeResolver.resolve(SomeOtherClass.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "someMethod".equals(element.getName());
            }
        });
        AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT);
        AnnotationOverrides annOverrides = AnnotationOverrides.builder().add(SomeOtherClass.class, SomeClass.class).build();
        ResolvedTypeWithMembers someOtherTypeWithMembers = memberResolver.resolve(someOtherType, annConfig, annOverrides);
        ResolvedMethod someMethod = someOtherTypeWithMembers.getMemberMethods()[0];
        Marker marker = someMethod.get(Marker.class);  // marker != null
        assertNotNull(marker);
        MarkerA markerA = someMethod.get(MarkerA.class); // markerA != null
        assertNotNull(markerA);
    }

}
