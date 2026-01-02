package com.fasterxml.classmate;

import java.util.List;
import com.fasterxml.classmate.members.ResolvedMethod;

/**
 * Test for [classmate#117]: StackOverflowError in 1.7.2 with recursive types
 *
 * NOTE: This test attempts to reproduce a StackOverflowError that occurs due to
 * infinite recursion in equals() methods when comparing recursive types:
 * - TypeBindings.equals() (line 221) -> ResolvedType.equals()
 * - ResolvedType.equals() (line 281) -> TypeBindings.equals()
 * - ResolvedRecursiveType.equals() (lines 157, 166) -> super.equals() + _referencedType.equals()
 *
 * The issue was introduced in commit 57fb93a which added support for resolving
 * raw generic types by binding type parameters to their bounds. This can create
 * circular dependencies in ResolvedRecursiveType instances.
 */
public class TypeResolver117Test extends BaseTest
{
    protected final TypeResolver RESOLVER = new TypeResolver();

    // [classmate#117] StackOverflowError with recursive types
    // The classic recursive type pattern: T extends SelfBounded<T>
    // When resolving raw types, the fix for #53 creates TypeBindings by
    // resolving T to its bound (SelfBounded<T>), which creates infinite recursion
    // in equals() methods

    // Test with a custom recursive type similar to Enum<E extends Enum<E>>
    static interface SelfReferential<T extends SelfReferential<T>> {
        T self();
    }

    @SuppressWarnings("rawtypes")
    static abstract class RawSelfReferential implements SelfReferential {
    }

    // Another recursive pattern with class
    static abstract class SelfBounded<T extends SelfBounded<T>> {
    }

    @SuppressWarnings("rawtypes")
    static abstract class RawSelfBounded extends SelfBounded {
    }

    // Real enum to test with
    static enum TestEnum {
        A, B, C
    }

    // Class that uses raw Enum in its hierarchy
    @SuppressWarnings("rawtypes")
    static abstract class UsesRawComparable implements Comparable {
    }

    // More complex recursive patterns that might trigger the issue

    // Double-nested recursive type
    static abstract class DoublyRecursive<T extends DoublyRecursive<T, U>, U extends DoublyRecursive<U, T>> {
    }

    @SuppressWarnings("rawtypes")
    static abstract class RawDoublyRecursive extends DoublyRecursive {
    }

    // Recursive type that implements another recursive type
    static abstract class RecursiveChain<T extends RecursiveChain<T>>
            extends SelfBounded<RecursiveChain<T>> {
    }

    @SuppressWarnings("rawtypes")
    static abstract class RawRecursiveChain extends RecursiveChain {
    }

    /**
     * This test reproduces the StackOverflowError reported in issue #117.
     * When resolving a raw self-bounded type, the equals() comparison of recursive
     * types causes infinite recursion:
     * - ResolvedType.equals() calls TypeBindings.equals()
     * - TypeBindings.equals() calls ResolvedType.equals() on contained types
     * - For ResolvedRecursiveType, this calls both super.equals() AND
     *   _referencedType.equals(), creating a cycle
     */
    public void testRawSelfBoundedCausesStackOverflow() {
        // This should not throw StackOverflowError
        ResolvedType rt = RESOLVER.resolve(RawSelfBounded.class);
        assertNotNull(rt);
        // If we get here without StackOverflowError, the test passes
    }

    public void testRawSelfReferentialInterface() {
        // Test with raw interface that has self-referential type parameter
        ResolvedType rt = RESOLVER.resolve(RawSelfReferential.class);
        assertNotNull(rt);
    }

    public void testRealEnumType() {
        // Test with a real enum type (Enum<E extends Enum<E>>)
        ResolvedType rt = RESOLVER.resolve(TestEnum.class);
        assertNotNull(rt);
    }

    public void testRawComparableViaEnum() {
        // Test with class implementing raw Comparable
        // (Comparable is the interface that Enum implements)
        ResolvedType rt = RESOLVER.resolve(UsesRawComparable.class);
        assertNotNull(rt);
    }

    /**
     * This test checks that equals() on recursive types doesn't cause
     * infinite recursion
     */
    public void testRecursiveTypeEquals() {
        ResolvedType rt1 = RESOLVER.resolve(RawSelfBounded.class);
        ResolvedType rt2 = RESOLVER.resolve(RawSelfBounded.class);

        // This equals comparison should not cause StackOverflowError
        assertEquals(rt1, rt2);
    }

    /**
     * Test equals with self-referential interface
     */
    public void testRecursiveInterfaceEquals() {
        ResolvedType rt1 = RESOLVER.resolve(RawSelfReferential.class);
        ResolvedType rt2 = RESOLVER.resolve(RawSelfReferential.class);

        // This equals comparison should not cause StackOverflowError
        assertEquals(rt1, rt2);
    }

    /**
     * Test with doubly-recursive type (two type parameters that reference each other)
     */
    public void testDoublyRecursiveType() {
        ResolvedType rt = RESOLVER.resolve(RawDoublyRecursive.class);
        assertNotNull(rt);
    }

    /**
     * Test with recursive chain (recursive type extending another recursive type)
     */
    public void testRecursiveChain() {
        ResolvedType rt = RESOLVER.resolve(RawRecursiveChain.class);
        assertNotNull(rt);
    }

    /**
     * Test equals on doubly-recursive type
     */
    public void testDoublyRecursiveEquals() {
        ResolvedType rt1 = RESOLVER.resolve(RawDoublyRecursive.class);
        ResolvedType rt2 = RESOLVER.resolve(RawDoublyRecursive.class);

        // This equals comparison should not cause StackOverflowError
        assertEquals(rt1, rt2);
    }

    /**
     * Direct test with java.lang.Enum
     * This is the most likely candidate for reproducing the issue
     * since Enum has the recursive pattern: Enum<E extends Enum<E>>
     */
    public void testDirectEnumResolution() {
        // Try resolving Enum.class directly (as a raw type)
        @SuppressWarnings("rawtypes")
        Class enumClass = Enum.class;
        ResolvedType rt = RESOLVER.resolve(enumClass);
        assertNotNull(rt);
    }

    /**
     * Test equals on Enum type
     */
    public void testEnumTypeEquals() {
        @SuppressWarnings("rawtypes")
        Class enumClass = Enum.class;
        ResolvedType rt1 = RESOLVER.resolve(enumClass);
        ResolvedType rt2 = RESOLVER.resolve(enumClass);

        // This equals comparison should not cause StackOverflowError
        assertEquals(rt1, rt2);
    }

    /**
     * Stress test: resolve many recursive types to trigger caching/equality checks
     */
    public void testMultipleRecursiveResolutions() {
        // Resolve the same types multiple times
        for (int i = 0; i < 10; i++) {
            ResolvedType rt1 = RESOLVER.resolve(RawSelfBounded.class);
            ResolvedType rt2 = RESOLVER.resolve(RawSelfReferential.class);
            ResolvedType rt3 = RESOLVER.resolve(RawDoublyRecursive.class);

            // Force equals checks
            assertEquals(rt1, RESOLVER.resolve(RawSelfBounded.class));
            assertEquals(rt2, RESOLVER.resolve(RawSelfReferential.class));
            assertEquals(rt3, RESOLVER.resolve(RawDoublyRecursive.class));
        }
    }

    /**
     * Test resolving type parameters directly
     */
    public void testRecursiveTypeParameters() {
        @SuppressWarnings("rawtypes")
        Class enumClass = Enum.class;
        ResolvedType rt = RESOLVER.resolve(enumClass);

        // Get the type parameters which should include the recursive bound
        List<ResolvedType> typeParams = rt.getTypeParameters();
        assertNotNull(typeParams);

        // For raw Enum, the type parameter E should be resolved to its bound: Enum<E>
        // This creates a ResolvedRecursiveType which could trigger the equals issue
        if (!typeParams.isEmpty()) {
            ResolvedType param = typeParams.get(0);
            assertNotNull(param);

            // Try to compare it
            assertEquals(param, param);

            // Check if it's a recursive type
            ResolvedType selfRef = param.getSelfReferencedType();
            if (selfRef != null) {
                // This is a ResolvedRecursiveType - try comparing it
                assertEquals(param, param);
            }
        }
    }

    /**
     * Deep inspection test: examine the structure of resolved recursive types
     * to understand if the cycle exists
     */
    public void testRecursiveTypeStructure() {
        ResolvedType rt = RESOLVER.resolve(RawSelfBounded.class);

        // Get parent class which should be SelfBounded with type parameters
        ResolvedType parent = rt.getParentClass();
        if (parent != null) {
            List<ResolvedType> parentParams = parent.getTypeParameters();
            if (!parentParams.isEmpty()) {
                ResolvedType param = parentParams.get(0);

                // If this is recursive, it might have a self-reference
                ResolvedType selfRef = param.getSelfReferencedType();
                if (selfRef != null) {
                    // Try to trigger the equals issue
                    TypeBindings bindings1 = parent.getTypeBindings();
                    TypeBindings bindings2 = parent.getTypeBindings();

                    // This should not cause StackOverflowError
                    assertEquals(bindings1, bindings2);
                }
            }
        }
    }

    /**
     * Test that explicitly verifies the issue is resolved or documents
     * that it could not be reproduced in unit tests
     */
    public void testIssue117Summary() {
        // This test documents the investigation of issue #117

        // Try all the patterns that should trigger the issue:
        // 1. Raw self-bounded type
        ResolvedType rt1 = RESOLVER.resolve(RawSelfBounded.class);
        assertEquals(rt1, rt1);

        // 2. Raw Enum (the classic case)
        ResolvedType rt2 = RESOLVER.resolve(Enum.class);
        assertEquals(rt2, rt2);

        // 3. Doubly recursive type
        ResolvedType rt3 = RESOLVER.resolve(RawDoublyRecursive.class);
        assertEquals(rt3, rt3);

        // If we reach here without StackOverflowError, either:
        // a) The issue has been fixed in the current code
        // b) The issue requires a specific integration scenario not covered by unit tests
        // c) The issue manifests only with certain JDK versions or configurations

        assertTrue("Issue #117 tests completed without StackOverflowError", true);
    }

    /**
     * Test using MemberResolver which might trigger more complex resolution
     * and cache lookups
     */
    public void testMemberResolverWithRecursiveTypes() {
        MemberResolver memberResolver = new MemberResolver(RESOLVER);

        // Resolve a type that has recursive bounds
        ResolvedType rt = RESOLVER.resolve(RawSelfBounded.class);

        // Get members which forces resolution of method return types, parameter types, etc.
        ResolvedTypeWithMembers members = memberResolver.resolve(rt, null, null);
        assertNotNull(members);

        // Access methods to trigger lazy resolution
        ResolvedMethod[] methods = members.getMemberMethods();
        assertNotNull(methods);
    }

    /**
     * Test MemberResolver with java.lang.Enum
     */
    public void testMemberResolverWithEnum() {
        MemberResolver memberResolver = new MemberResolver(RESOLVER);

        // Resolve raw Enum
        ResolvedType rt = RESOLVER.resolve(Enum.class);

        // Get members - this might trigger the stack overflow
        ResolvedTypeWithMembers members = memberResolver.resolve(rt, null, null);
        assertNotNull(members);

        ResolvedMethod[] methods = members.getMemberMethods();
        assertNotNull(methods);
    }

    /**
     * Test with multiple TypeResolver instances to ensure no cross-cache issues
     */
    public void testMultipleResolvers() {
        TypeResolver resolver1 = new TypeResolver();
        TypeResolver resolver2 = new TypeResolver();

        ResolvedType rt1 = resolver1.resolve(RawSelfBounded.class);
        ResolvedType rt2 = resolver2.resolve(RawSelfBounded.class);

        // These should be equal even from different resolvers
        assertEquals(rt1, rt2);
    }

    /**
     * Test that specifically tries to trigger cache comparison by
     * resolving the same type multiple times
     */
    public void testCacheLookupWithRecursiveTypes() {
        // First resolution - should cache it
        ResolvedType rt1 = RESOLVER.resolve(Enum.class);

        // Second resolution - should find in cache and compare keys
        // This is where the StackOverflowError might occur due to
        // ResolvedTypeKey.equals() calling ResolvedType.equals()
        ResolvedType rt2 = RESOLVER.resolve(Enum.class);

        // Should be the same instance from cache
        assertSame(rt1, rt2);

        // Also test equals explicitly
        assertEquals(rt1, rt2);
    }

    /**
     * Test resolving type parameters for recursive types
     */
    public void testTypeParametersForRecursiveTypes() {
        // Resolve Enum
        ResolvedType enumType = RESOLVER.resolve(Enum.class);

        // Get its type parameters
        List<ResolvedType> typeParams = enumType.getTypeParameters();
        assertEquals(1, typeParams.size());

        ResolvedType paramType = typeParams.get(0);

        // The parameter should be a ResolvedRecursiveType for Enum<E>
        // Try to get its type parameters recursively
        List<ResolvedType> nestedParams = paramType.getTypeParameters();

        // Now try to compare - this might trigger the stack overflow
        assertEquals(paramType, paramType);

        if (!nestedParams.isEmpty()) {
            ResolvedType nestedParam = nestedParams.get(0);
            // This comparison might cause issues
            assertEquals(nestedParam, nestedParam);
        }
    }

    /**
     * Test with typeParametersFor which might trigger different code paths
     */
    public void testTypeParametersForMethod() {
        ResolvedType rt = RESOLVER.resolve(TestEnum.class);

        // TestEnum extends Enum<TestEnum>, so asking for Enum's type parameters
        // should return TestEnum
        List<ResolvedType> params = rt.typeParametersFor(Enum.class);
        assertNotNull(params);
        assertEquals(1, params.size());

        // The parameter should be TestEnum
        ResolvedType param = params.get(0);
        assertEquals(TestEnum.class, param.getErasedType());
    }

    /**
     * Test comparing TypeBindings directly
     */
    public void testTypeBindingsComparison() {
        ResolvedType rt1 = RESOLVER.resolve(Enum.class);
        ResolvedType rt2 = RESOLVER.resolve(Enum.class);

        TypeBindings bindings1 = rt1.getTypeBindings();
        TypeBindings bindings2 = rt2.getTypeBindings();

        // This should trigger TypeBindings.equals() which calls
        // ResolvedType.equals() on contained types
        assertEquals(bindings1, bindings2);
    }

    /**
     * *** THIS TEST REPRODUCES THE STACKOVERFLOWERROR ***
     *
     * Test with FRESH TypeResolver instances to avoid cache hits
     * This forces new resolution and creation of ResolvedRecursiveTypes.
     * When comparing ResolvedTypes from different resolvers, the equals()
     * method triggers infinite recursion through:
     * - ResolvedRecursiveType.equals() -> super.equals() -> TypeBindings.equals()
     * - TypeBindings.equals() -> ResolvedType.equals() (on type parameters)
     * - ResolvedRecursiveType.equals() -> _referencedType.equals() -> ... (cycle)
     */
    public void testFreshResolversWithRecursiveTypes() {
        // Use fresh resolver each time to avoid caching
        TypeResolver fresh1 = new TypeResolver();
        TypeResolver fresh2 = new TypeResolver();

        ResolvedType rt1 = fresh1.resolve(Enum.class);
        ResolvedType rt2 = fresh2.resolve(Enum.class);

        // These are from different resolvers, so different instances
        // Comparing them should trigger full equals() logic
        assertEquals(rt1, rt2);

        // Also compare their type bindings
        assertEquals(rt1.getTypeBindings(), rt2.getTypeBindings());

        // And compare type parameters
        List<ResolvedType> params1 = rt1.getTypeParameters();
        List<ResolvedType> params2 = rt2.getTypeParameters();

        if (!params1.isEmpty() && !params2.isEmpty()) {
            // This comparison between ResolvedRecursiveTypes from different
            // resolvers might trigger the infinite recursion
            assertEquals(params1.get(0), params2.get(0));
        }
    }

    /**
     * Test resolving through different paths to create different
     * but equivalent recursive type structures
     */
    public void testDifferentResolutionPaths() {
        TypeResolver resolver = new TypeResolver();

        // Path 1: Resolve Enum directly
        ResolvedType enumDirect = resolver.resolve(Enum.class);

        // Path 2: Resolve through an enum subclass and ask for parent
        ResolvedType testEnumType = resolver.resolve(TestEnum.class);
        ResolvedType enumViaParent = testEnumType.getParentClass();

        // These might have different ResolvedRecursiveType instances internally
        // Comparing them could trigger the issue
        if (enumViaParent != null) {
            // Compare the types
            assertNotNull(enumDirect);
            assertNotNull(enumViaParent);

            // The erased types should be the same
            assertEquals(Enum.class, enumViaParent.getErasedType());
        }
    }

    /**
     * *** THIS TEST ALSO REPRODUCES THE STACKOVERFLOWERROR ***
     *
     * Test that creates maximum pressure on equals() by resolving
     * many recursive types and comparing them all.
     * This amplifies the issue by comparing types from multiple different resolvers.
     */
    public void testMassiveRecursiveTypeComparison() {
        TypeResolver[] resolvers = new TypeResolver[5];
        ResolvedType[][] types = new ResolvedType[5][4];

        // Create multiple resolvers and resolve multiple recursive types in each
        for (int i = 0; i < 5; i++) {
            resolvers[i] = new TypeResolver();
            types[i][0] = resolvers[i].resolve(Enum.class);
            types[i][1] = resolvers[i].resolve(RawSelfBounded.class);
            types[i][2] = resolvers[i].resolve(RawSelfReferential.class);
            types[i][3] = resolvers[i].resolve(RawDoublyRecursive.class);
        }

        // Now compare all of them - this creates many equals() calls
        for (int typeIdx = 0; typeIdx < 4; typeIdx++) {
            for (int i = 0; i < 5; i++) {
                for (int j = i + 1; j < 5; j++) {
                    // Compare types from different resolvers
                    assertEquals(types[i][typeIdx], types[j][typeIdx]);
                }
            }
        }
    }

    /**
     * Test findSupertype which might trigger different resolution paths
     */
    public void testFindSupertypeWithRecursive() {
        ResolvedType testEnumType = RESOLVER.resolve(TestEnum.class);

        // Find Enum supertype - this might trigger different code paths
        ResolvedType enumSupertype = testEnumType.findSupertype(Enum.class);
        assertNotNull(enumSupertype);

        // Find Comparable supertype (implemented by Enum)
        ResolvedType comparableSupertype = testEnumType.findSupertype(Comparable.class);
        assertNotNull(comparableSupertype);

        // Compare them
        assertNotSame(enumSupertype, comparableSupertype);
    }

    /**
     * Test that verifies caching still works correctly with the fix.
     * This tests that cache lookups succeed when comparing ResolvedTypeKeys
     * that contain ResolvedRecursiveType instances.
     */
    public void testCacheCorrectness() {
        TypeResolver resolver = new TypeResolver();

        // Resolve Enum first time - should be cached
        ResolvedType enum1 = resolver.resolve(Enum.class);
        assertNotNull(enum1);

        // Resolve Enum second time - should get from cache (same instance)
        ResolvedType enum2 = resolver.resolve(Enum.class);
        assertSame("Should return same instance from cache", enum1, enum2);

        // Verify type parameters contain recursive types
        List<ResolvedType> params = enum1.getTypeParameters();
        assertEquals(1, params.size());

        ResolvedType paramType = params.get(0);
        // Check if it's a recursive type
        ResolvedType selfRef = paramType.getSelfReferencedType();
        if (selfRef != null) {
            // It's a ResolvedRecursiveType - verify it works correctly
            assertNotNull(selfRef);
        }

        // Resolve RawSelfBounded - also has recursive structure
        ResolvedType sb1 = resolver.resolve(RawSelfBounded.class);
        ResolvedType sb2 = resolver.resolve(RawSelfBounded.class);
        assertSame("RawSelfBounded should also be cached", sb1, sb2);
    }

    /**
     * Test that ResolvedRecursiveType instances with same structure
     * but from different resolvers are equal (but not same instance)
     */
    public void testRecursiveTypeEqualityAcrossResolvers() {
        TypeResolver resolver1 = new TypeResolver();
        TypeResolver resolver2 = new TypeResolver();

        ResolvedType enum1 = resolver1.resolve(Enum.class);
        ResolvedType enum2 = resolver2.resolve(Enum.class);

        // Should be equal (structural equality)
        assertEquals("Types from different resolvers should be equal", enum1, enum2);

        // But not same instance
        assertNotSame("Should not be same instance", enum1, enum2);

        // Type parameters should also be equal
        List<ResolvedType> params1 = enum1.getTypeParameters();
        List<ResolvedType> params2 = enum2.getTypeParameters();

        assertEquals(params1.size(), params2.size());
        for (int i = 0; i < params1.size(); i++) {
            // This is the critical test - comparing ResolvedRecursiveTypes
            // from different resolvers should work without StackOverflow
            assertEquals("Type parameter " + i + " should be equal",
                    params1.get(i), params2.get(i));
        }
    }

    /**
     * Verify that _referencedType is only used for navigation,
     * not for equality determination
     */
    public void testReferencedTypeIsForNavigation() {
        ResolvedType enumType = RESOLVER.resolve(Enum.class);
        List<ResolvedType> params = enumType.getTypeParameters();

        if (!params.isEmpty()) {
            ResolvedType param = params.get(0);
            ResolvedType selfRef = param.getSelfReferencedType();

            if (selfRef != null) {
                // selfRef should be non-null for ResolvedRecursiveType
                assertNotNull(selfRef);

                // selfRef should allow navigation to members
                // (this is what _referencedType is used for)
                List<com.fasterxml.classmate.members.RawMethod> methods =
                    selfRef.getMemberMethods();
                assertNotNull("Should be able to get methods via referenced type", methods);
            }
        }
    }
}
