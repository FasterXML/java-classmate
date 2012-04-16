package com.fasterxml.classmate;

import com.fasterxml.classmate.members.*;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/14/12
 * Time: 12:05 PM
 */
public class ResolvedTypeWithMembersTest {

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Marker { }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface MarkerB { }

    private static class MixinCandidate {
        private static void staticOverride() { }
        private String shadowed;
        private MixinCandidate() { }
        protected String getShadowed() { return shadowed; }
    }

    private static class MixinA {
        @Marker
        private static void staticOverride() { }

        @Marker
        private String shadowed;

        @Marker
        private MixinA() { }

        @Marker
        protected String getShadowed() { return shadowed; }
    }

    private static class MixinB {
        @MarkerB
        protected String getShadowed() { return null; }
    }

    private static class MixinC {
        @MarkerB
        protected String getShadowed() { return null; }
    }

    private static class MixinD {

        @Marker
        private String field;

        @Marker
        private MixinD() { }

        @MarkerB
        protected String getShadowed() { return null; }
    }

    @Test
    public void size() {
        ResolvedTypeWithMembers members = new ResolvedTypeWithMembers(null, null, null, new HierarchicType[0], null, null, null);
        assertEquals(0, members.size());

        members = new ResolvedTypeWithMembers(null, null, null, new HierarchicType[] { new HierarchicType(null, true, 0) }, null, null, null);
        assertEquals(1, members.size());
    }

    @Test
    public void mainTypeAndOverrides() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType string = typeResolver.resolve(String.class);
        ResolvedType comparator = typeResolver.resolve(Comparator.class);
        ResolvedType comparable = typeResolver.resolve(Comparable.class);
        // test such that priority of main-type means all types are returned
        HierarchicType stringHierarchicType = new HierarchicType(string, true, 3);
        HierarchicType comparatorHierarchicType = new HierarchicType(comparator, true, 0);
        HierarchicType comparableHierarchicType = new HierarchicType(comparable, true, 1);
        HierarchicType[] types = new HierarchicType[] { comparatorHierarchicType, comparableHierarchicType };
        ResolvedTypeWithMembers members = new ResolvedTypeWithMembers(null, null, stringHierarchicType, types, null, null, null);

        List<HierarchicType> mainTypeAndOverrides = members.mainTypeAndOverrides();
        assertEquals(2, mainTypeAndOverrides.size());
        assertTrue(mainTypeAndOverrides.contains(comparatorHierarchicType));
        assertTrue(mainTypeAndOverrides.contains(comparableHierarchicType));

        // test subset
        stringHierarchicType = new HierarchicType(string, true, 0);
        members = new ResolvedTypeWithMembers(null, null, stringHierarchicType, types, null, null, null);
        mainTypeAndOverrides = members.mainTypeAndOverrides();
        assertEquals(1, mainTypeAndOverrides.size());
        assertTrue(mainTypeAndOverrides.contains(comparatorHierarchicType));
    }

    @Test
    public void resolveConstructors() {
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType string = typeResolver.resolve(String.class);
        HierarchicType stringHierarchicType = new HierarchicType(string, true, 0);
        // test all constructors filtered out
        ResolvedTypeWithMembers members = new ResolvedTypeWithMembers(typeResolver, null, stringHierarchicType, new HierarchicType[0], new Filter<RawConstructor>() {
            @Override public boolean include(RawConstructor element) {
                return false;
            }
        }, null, null);
        assertEquals(0, members.resolveConstructors().length);

        // test, adding annotation from constructor on class but without an annotation-handler which
        // allows for mix-ins (i.e., AnnotationInclusion.DONT_INCLUDE)
        ResolvedType mixinCandidateResolved = typeResolver.resolve(MixinCandidate.class);
        ResolvedType mixinAResolved = typeResolver.resolve(MixinA.class);
        HierarchicType mixinCandidateHierarchicType = new HierarchicType(mixinCandidateResolved, false, 1);
        HierarchicType mixinAHierarchicType = new HierarchicType(mixinAResolved, true, 0);

        members = new ResolvedTypeWithMembers(typeResolver, null, mixinCandidateHierarchicType,
                new HierarchicType[] { mixinAHierarchicType, mixinCandidateHierarchicType }, null, null, null);
        ResolvedConstructor[] resolvedConstructors = members.resolveConstructors();
        assertEquals(1, resolvedConstructors.length);
        ResolvedConstructor resolvedConstructor = resolvedConstructors[0];
        assertNull(resolvedConstructor.get(Marker.class));

        // TODO - there's no way of making a mix-in constructor unless the key used for constructor is changed
        // TODO - to disregard the name as constructor's are not overridden

//        members = new ResolvedTypeWithMembers(typeResolver,
//                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT), mixinCandidateHierarchicType,
//                new HierarchicType[] { mixinAHierarchicType, mixinCandidateHierarchicType }, null, null, null);
//        resolvedConstructors = members.resolveConstructors();
//        assertEquals(1, resolvedConstructors.length);
//        resolvedConstructor = resolvedConstructors[0];
//        annotations = (Annotations) annotationsField.get(resolvedConstructor);
//        assertEquals(1, annotations.size());
//        assertNotNull(annotations.get(Marker.class));
    }

    @Test
    public void resolveMemberFields() {
        // first, test by filtering all fields; including mix-ins
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType mixinCandidateResolved = typeResolver.resolve(MixinCandidate.class);
        ResolvedType mixinAResolved = typeResolver.resolve(MixinA.class);
        HierarchicType mixinCandidateHierarchicType = new HierarchicType(mixinCandidateResolved, false, 1);
        HierarchicType mixinAHierarchicType = new HierarchicType(mixinAResolved, true, 0);
        ResolvedTypeWithMembers members = new ResolvedTypeWithMembers(typeResolver, null, mixinCandidateHierarchicType,
                new HierarchicType[] { mixinAHierarchicType }, null, new Filter<RawField>() {
            @Override public boolean include(RawField element) {
                return false;
            }
        }, null);
        ResolvedField[] resolvedFields = members.resolveMemberFields();
        assertEquals(0, resolvedFields.length);

        // test, adding annotation from shadowed field on super-class but without an annotation-handler which
        // allows for mix-ins (i.e., AnnotationInclusion.DONT_INCLUDE)
        members = new ResolvedTypeWithMembers(typeResolver, null, mixinCandidateHierarchicType,
                new HierarchicType[] { mixinAHierarchicType, mixinCandidateHierarchicType }, null, null, null);
        resolvedFields = members.resolveMemberFields();
        assertEquals(1, resolvedFields.length);
        ResolvedField resolvedField = resolvedFields[0];
        assertNull(resolvedField.get(Marker.class));

        // test, changing annotation-handler to allow for mix-in
        members = new ResolvedTypeWithMembers(typeResolver,
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT), mixinCandidateHierarchicType,
                new HierarchicType[] { mixinAHierarchicType, mixinCandidateHierarchicType }, null, null, null);
        resolvedFields = members.resolveMemberFields();
        assertEquals(1, resolvedFields.length);
        resolvedField = resolvedFields[0];
        assertNotNull(resolvedField.get(Marker.class));
    }

    @Test
    public void resolveStaticMethods() {
        // first, test by filtering all fields; including mix-ins
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType mixinCandidateResolved = typeResolver.resolve(MixinCandidate.class);
        ResolvedType mixinAResolved = typeResolver.resolve(MixinA.class);
        HierarchicType mixinCandidateHierarchicType = new HierarchicType(mixinCandidateResolved, false, 1);
        HierarchicType mixinAHierarchicType = new HierarchicType(mixinAResolved, true, 0);
        ResolvedTypeWithMembers members = new ResolvedTypeWithMembers(typeResolver, null, mixinCandidateHierarchicType,
                new HierarchicType[] { mixinAHierarchicType }, null, null, new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return false;
            }
        });
        ResolvedMethod[] resolvedMethods = members.resolveStaticMethods();
        assertEquals(0, resolvedMethods.length);

        // test, adding annotation from method on class but without an annotation-handler which
        // allows for mix-ins (i.e., AnnotationInclusion.DONT_INCLUDE)
        members = new ResolvedTypeWithMembers(typeResolver, null, mixinCandidateHierarchicType,
                new HierarchicType[] { mixinAHierarchicType, mixinCandidateHierarchicType }, null, null, null);
        resolvedMethods = members.resolveStaticMethods();
        assertEquals(1, resolvedMethods.length);
        ResolvedMethod resolvedMethod = resolvedMethods[0];
        assertNull(resolvedMethod.get(Marker.class));

        // test, changing annotation-handler to allow for mix-in
        members = new ResolvedTypeWithMembers(typeResolver,
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT), mixinCandidateHierarchicType,
                new HierarchicType[] { mixinAHierarchicType, mixinCandidateHierarchicType }, null, null, null);
        resolvedMethods = members.resolveStaticMethods();
        assertEquals(1, resolvedMethods.length);
        resolvedMethod = resolvedMethods[0];
        assertNotNull(resolvedMethod.get(Marker.class));
    }

    @Test
    public void resolveMemberMethods() {
        // first, test by filtering all fields; including mix-ins
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType mixinCandidateResolved = typeResolver.resolve(MixinCandidate.class);
        ResolvedType mixinAResolved = typeResolver.resolve(MixinA.class);
        HierarchicType mixinCandidateHierarchicType = new HierarchicType(mixinCandidateResolved, false, 1);
        HierarchicType mixinAHierarchicType = new HierarchicType(mixinAResolved, true, 0);
        ResolvedTypeWithMembers members = new ResolvedTypeWithMembers(typeResolver, null, mixinCandidateHierarchicType,
                new HierarchicType[] { mixinAHierarchicType }, null, null, new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return false;
            }
        });
        ResolvedMethod[] resolvedMethods = members.resolveMemberMethods();
        assertEquals(0, resolvedMethods.length);

        // test, adding annotation from class but without an annotation-handler which
        // allows for mix-ins (i.e., AnnotationInclusion.DONT_INCLUDE)
        members = new ResolvedTypeWithMembers(typeResolver, null, mixinCandidateHierarchicType,
                new HierarchicType[] { mixinCandidateHierarchicType, mixinAHierarchicType }, null, null, null);
        resolvedMethods = members.resolveMemberMethods();
        assertEquals(1, resolvedMethods.length);
        ResolvedMethod resolvedMethod = resolvedMethods[0];
        assertNull(resolvedMethod.get(Marker.class));
        assertNull(resolvedMethod.get(MarkerB.class));

        // test, changing annotation-handler to allow for mix-in
        ResolvedType mixinBResolved = typeResolver.resolve(MixinB.class);
        HierarchicType mixinBHierarchicType = new HierarchicType(mixinBResolved, true, 0);
        members = new ResolvedTypeWithMembers(typeResolver,
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT), mixinCandidateHierarchicType,
                new HierarchicType[] { mixinBHierarchicType, mixinCandidateHierarchicType, mixinAHierarchicType }, null, null, null);
        resolvedMethods = members.resolveMemberMethods();
        assertEquals(1, resolvedMethods.length);
        resolvedMethod = resolvedMethods[0];
        assertNotNull(resolvedMethod.get(Marker.class));
        assertNotNull(resolvedMethod.get(MarkerB.class));

        // test, adding non-mixin-only into the hierarchy-type list
        ResolvedType mixinCResolved = typeResolver.resolve(MixinC.class);
        HierarchicType mixinCHierarchicType = new HierarchicType(mixinCResolved, false, 0);
        ResolvedType mixinDResolved = typeResolver.resolve(MixinD.class);
        HierarchicType mixinDHierarchicType = new HierarchicType(mixinDResolved, true, 0);
        members = new ResolvedTypeWithMembers(typeResolver,
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT), mixinCandidateHierarchicType,
                new HierarchicType[] { mixinBHierarchicType, mixinDHierarchicType, mixinCandidateHierarchicType, mixinAHierarchicType, mixinCHierarchicType }, null, null, null);
        resolvedMethods = members.resolveMemberMethods();
        assertEquals(1, resolvedMethods.length);
        resolvedMethod = resolvedMethods[0];
        assertNotNull(resolvedMethod.get(Marker.class));
        assertNotNull(resolvedMethod.get(MarkerB.class));

    }

    @Test
    public void resolveConstructor() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // test where annotation is 'masked' via the annotation-config handler
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType mixinDResolved = typeResolver.resolve(MixinD.class);
        HierarchicType mixinDHierarchicType = new HierarchicType(mixinDResolved, false, 0);
        RawConstructor rawConstructor = new RawConstructor(mixinDResolved, MixinD.class.getDeclaredConstructor());
        ResolvedTypeWithMembers members = new ResolvedTypeWithMembers(typeResolver, null, mixinDHierarchicType,
                new HierarchicType[] { mixinDHierarchicType }, null, null, null);
        ResolvedType resolvedTypeWithMembersResolvedType = typeResolver.resolve(ResolvedTypeWithMembers.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "resolveConstructor".equals(element.getName());
            }
        });
        ResolvedTypeWithMembers resolved = memberResolver.resolve(resolvedTypeWithMembersResolvedType, null, null);
        ResolvedMethod resolveConstructorResolvedMethod = resolved.getMemberMethods()[0];
        Method resolveConstructorMethod = resolveConstructorResolvedMethod.getRawMember();
        resolveConstructorMethod.setAccessible(true);

        ResolvedConstructor resolvedConstructor = (ResolvedConstructor) resolveConstructorMethod.invoke(members, rawConstructor);
        assertNull(resolvedConstructor.get(Marker.class));
        // do again, now that annotation-handler's cache is primed.
        resolvedConstructor = (ResolvedConstructor) resolveConstructorMethod.invoke(members, rawConstructor);
        assertNull(resolvedConstructor.get(Marker.class));

        // test with annotation-handler which allows for annotations
        members = new ResolvedTypeWithMembers(typeResolver,
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT), mixinDHierarchicType,
                new HierarchicType[] { mixinDHierarchicType }, null, null, null);
        resolvedConstructor = (ResolvedConstructor) resolveConstructorMethod.invoke(members, rawConstructor);
        assertNotNull(resolvedConstructor.get(Marker.class));
        // do again, now that annotation-handler's cache is primed.
        resolvedConstructor = (ResolvedConstructor) resolveConstructorMethod.invoke(members, rawConstructor);
        assertNotNull(resolvedConstructor.get(Marker.class));
    }

    @Test
    public void resolveField() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException,
            InvocationTargetException {
        // test where annotation is 'masked' via the annotation-config handler
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType mixinDResolved = typeResolver.resolve(MixinD.class);
        HierarchicType mixinDHierarchicType = new HierarchicType(mixinDResolved, false, 0);
        RawField rawField = new RawField(mixinDResolved, MixinD.class.getDeclaredField("field"));
        ResolvedTypeWithMembers members = new ResolvedTypeWithMembers(typeResolver, null, mixinDHierarchicType,
                new HierarchicType[] { mixinDHierarchicType }, null, null, null);
        ResolvedType resolvedTypeWithMembersResolvedType = typeResolver.resolve(ResolvedTypeWithMembers.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "resolveField".equals(element.getName());
            }
        });
        ResolvedTypeWithMembers resolved = memberResolver.resolve(resolvedTypeWithMembersResolvedType, null, null);
        ResolvedMethod resolveFieldResolvedMethod = resolved.getMemberMethods()[0];
        Method resolveFieldMethod = resolveFieldResolvedMethod.getRawMember();
        resolveFieldMethod.setAccessible(true);

        ResolvedField resolvedField = (ResolvedField) resolveFieldMethod.invoke(members, rawField);
        assertNull(resolvedField.get(Marker.class));
        // do it again now that the annotation-handler's cache has been primed
        resolvedField = (ResolvedField) resolveFieldMethod.invoke(members, rawField);
        assertNull(resolvedField.get(Marker.class));

        // test with annotation-handler which allows for annotations
        members = new ResolvedTypeWithMembers(typeResolver,
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT), mixinDHierarchicType,
                new HierarchicType[] { mixinDHierarchicType }, null, null, null);
        resolvedField = (ResolvedField) resolveFieldMethod.invoke(members, rawField);
        assertNotNull(resolvedField.get(Marker.class));
        // do it again now that the annotation-handler's cache has been primed
        resolvedField = (ResolvedField) resolveFieldMethod.invoke(members, rawField);
        assertNotNull(resolvedField.get(Marker.class));
    }

    @Test
    public void resolveMethod() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // test where annotation is 'masked' via the annotation-config handler
        TypeResolver typeResolver = new TypeResolver();
        ResolvedType mixinDResolved = typeResolver.resolve(MixinD.class);
        HierarchicType mixinDHierarchicType = new HierarchicType(mixinDResolved, false, 0);
        RawMethod rawMethod = new RawMethod(mixinDResolved, MixinD.class.getDeclaredMethod("getShadowed"));
        ResolvedTypeWithMembers members = new ResolvedTypeWithMembers(typeResolver, null, mixinDHierarchicType,
                new HierarchicType[] { mixinDHierarchicType }, null, null, null);
        ResolvedType resolvedTypeWithMembersResolvedType = typeResolver.resolve(ResolvedTypeWithMembers.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "resolveMethod".equals(element.getName());
            }
        });
        ResolvedTypeWithMembers resolved = memberResolver.resolve(resolvedTypeWithMembersResolvedType, null, null);
        ResolvedMethod resolveMethodResolvedMethod = resolved.getMemberMethods()[0];
        Method resolveMethodMethod = resolveMethodResolvedMethod.getRawMember();
        resolveMethodMethod.setAccessible(true);

        ResolvedMethod resolvedMethod = (ResolvedMethod) resolveMethodMethod.invoke(members, rawMethod);
        assertNull(resolvedMethod.get(MarkerB.class));
        // do it again now that the annotation-handler's cache has been primed
        resolvedMethod = (ResolvedMethod) resolveMethodMethod.invoke(members, rawMethod);
        assertNull(resolvedMethod.get(MarkerB.class));

        // test with annotation-handler which allows for annotations
        members = new ResolvedTypeWithMembers(typeResolver,
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT), mixinDHierarchicType,
                new HierarchicType[] { mixinDHierarchicType }, null, null, null);
        resolvedMethod = (ResolvedMethod) resolveMethodMethod.invoke(members, rawMethod);
        assertNotNull(resolvedMethod.get(MarkerB.class));
        // do it again now that the annotation-handler's cache has been primed
        resolvedMethod = (ResolvedMethod) resolveMethodMethod.invoke(members, rawMethod);
        assertNotNull(resolvedMethod.get(MarkerB.class));
    }
}
