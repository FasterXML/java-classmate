package com.fasterxml.classmate;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.*;
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
        void something(@MarkerOverridden(456) String value);
    }

    static interface TargetInterface {
        void something(@Marker String value);
    }

    static interface MixIn {
        void something(@MarkerOverridden(123) String value);
    }

    static interface ExtendedMixIn extends MixIn {
        void something(@MarkerInherited @MarkerOverridden(456) String value);
    }

    // todo: test more complex hierarchies
    // todo: test constructors
    // note: most of the above is unimplemented currently

    @Test
    public void testIncludesUninheritableAnnotationsDirectly() {
        ResolvedTypeWithMembers type = members.resolve(types.resolve(BaseInterface.class), annotations, null);
        ResolvedMethod[] methods = type.getMemberMethods();

        // sanity test our method
        checkMethods(methods, BaseInterface.class);

        ResolvedMethod m = methods[0];
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
}
