package com.fasterxml.classmate;

import com.fasterxml.classmate.members.ResolvedConstructor;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Tests that parameters annotations (on constructors and member methods) can be
 * inherited and overridden.
 */
public class TestParameterAnnotations {

    TypeResolver types;
    MemberResolver members;
    AnnotationConfiguration annotations;

    @Before
    public void setup() {
        types = new TypeResolver();
        members = new MemberResolver(types);
        annotations = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    private static @interface Marker { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @Inherited
    private static @interface MarkerInherited { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @Inherited
    private static @interface MarkerOverridden { int value(); }

    static interface BaseInterface {
        void something(@Marker @MarkerInherited @MarkerOverridden(123) String value);
    }

    static interface ExtendedInterface extends BaseInterface {
        @Override
        void something(@MarkerOverridden(456) String value);
    }

    static interface TargetInterface {
        void something(@Marker String value);
    }

    static interface MixIn {
        void something(@MarkerOverridden(123) String value);
    }

    static interface ExtendedMixIn extends MixIn {
        @Override
        void something(@MarkerInherited @MarkerOverridden(456) String value);
    }

    static abstract class BaseImpl {
        public String v;
        public BaseImpl(@Marker @MarkerOverridden(789) String v) {
            this.v = v;
        }
    }

    static class MixInClass {
        public MixInClass(@MarkerInherited String v) { }
    }

    static class Impl extends BaseImpl implements ExtendedInterface {

        public Impl(@MarkerOverridden(999) String v) {
            super(v);
        }

        @Override
        public void something(final String value) { }
    }

    @Test
    public void testIncludesUninheritableAnnotationsDirectly() {
        ResolvedTypeWithMembers type = members.resolve(types.resolve(BaseInterface.class), annotations, null);
        ResolvedMethod[] methods = type.getMemberMethods();

        // sanity test our method
        checkMethods(methods, BaseInterface.class);

        ResolvedMethod m = methods[0];
        assertEquals("something", m.getName());
        assertNotNull(m.getParam(0, Marker.class));
    }

    @Test
    public void testInheritsOnlyMarkedAnnotations() {
        ResolvedTypeWithMembers type = members.resolve(types.resolve(ExtendedInterface.class), annotations, null);
        ResolvedMethod[] methods = type.getMemberMethods();

        // sanity test our method
        checkMethods(methods, ExtendedInterface.class);

        // check that the correct annotations were detected
        ResolvedMethod m = methods[0];
        assertNull(m.getParam(0, Marker.class));
        assertNotNull(m.getParam(0, MarkerInherited.class));
        assertNotNull(m.getParam(0, MarkerOverridden.class));
        assertEquals(456, m.getParam(0, MarkerOverridden.class).value());
    }

    @Test
    public void testMixInAnnotations() {
        ResolvedTypeWithMembers type = members.resolve(types.resolve(TargetInterface.class), annotations, AnnotationOverrides.builder().add(TargetInterface.class, ExtendedMixIn.class).build());
        ResolvedMethod[] methods = type.getMemberMethods();

        // sanity test our method
        checkMethods(methods, TargetInterface.class);

        // check that the mixed-in annotations are present
        ResolvedMethod m = methods[0];
        assertNotNull(m.getParam(0, Marker.class));
        assertNotNull(m.getParam(0, MarkerInherited.class));
        assertNotNull(m.getParam(0, MarkerOverridden.class));
        assertEquals(456, m.getParam(0, MarkerOverridden.class).value());
    }

    @Test
    public void testConstructorParameterAnnotations() {
        ResolvedTypeWithMembers type = members.resolve(types.resolve(Impl.class), annotations, AnnotationOverrides.builder().add(Impl.class, MixInClass.class).build());
        ResolvedConstructor[] constructors = type.getConstructors();

        // sanity test our constructor
        checkConstructors(constructors, Impl.class);

        // check that the constructor parameter annotations are properly inherited/overridden
        ResolvedConstructor c = constructors[0];
        assertNull(c.getParam(0, Marker.class));
        assertNotNull(c.getParam(0, MarkerInherited.class));
        assertNotNull(c.getParam(0, MarkerOverridden.class));
        assertEquals(999, c.getParam(0, MarkerOverridden.class).value());
    }

    private void checkMethods(ResolvedMethod[] methods, Class<?> type) {
        assertEquals(type.getMethods().length, methods.length);
        for (ResolvedMethod method : methods) {
            try {
                Method raw = method.getRawMember();
                assertEquals(type.getMethod(method.getName(), raw.getParameterTypes()), raw);
            } catch (NoSuchMethodException e) {
                fail("No such method: " + method);
            }
        }
    }

    private void checkConstructors(ResolvedConstructor[] constructors, Class<?> type) {
        assertEquals(type.getConstructors().length, constructors.length);
        for (ResolvedConstructor constructor : constructors) {
            try {
                Constructor<?> raw = constructor.getRawMember();
                assertEquals(type.getConstructor(raw.getParameterTypes()), raw);
            } catch (NoSuchMethodException e) {
                fail("No such constructor: " + constructor);
            }
        }
    }
}
