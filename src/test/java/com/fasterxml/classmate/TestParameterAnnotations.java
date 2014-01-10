package com.fasterxml.classmate;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.junit.Test;

import java.lang.annotation.*;

import static org.junit.Assert.*;

/**
 * Tests that parameters annotations (on constructors and member methods) can be
 * inherited and overridden.
 */
public class TestParameterAnnotations {

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

    // todo: test more complex hierarchies
    // todo: test attempts to inherit un-inheritable annotations
    // todo: test mix-ins
    // todo: test constructors
    // note: most of the above is unimplemented currently

    @Test
    public void testInheritsMarkedAnnotations() throws NoSuchMethodException {
        AnnotationConfiguration annotations = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED);
        TypeResolver types = new TypeResolver();
        MemberResolver members = new MemberResolver(types);
        ResolvedTypeWithMembers type = members.resolve(types.resolve(ExtendedInterface.class), annotations, null);
        ResolvedMethod[] methods = type.getMemberMethods();

        // sanity test our method
        assertEquals(1, methods.length);

        ResolvedMethod m = methods[0];
        assertEquals("something", m.getName());
        assertEquals(1, m.getArgumentCount());
        assertEquals(ExtendedInterface.class.getDeclaredMethod("something", String.class), m.getRawMember());

        // check that the correct annotations were detected
        assertNull(m.getArgument(0, Marker.class));
        assertNotNull(m.getArgument(0, MarkerInherited.class));
        assertNotNull(m.getArgument(0, MarkerOverridden.class));
        assertEquals(456, m.getArgument(0, MarkerOverridden.class).value());
    }
}
