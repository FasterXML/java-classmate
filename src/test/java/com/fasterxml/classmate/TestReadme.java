package com.fasterxml.classmate;

import com.fasterxml.classmate.members.RawMethod;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.junit.Test;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

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

}
