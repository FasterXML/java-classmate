package com.fasterxml.classmate;

import java.util.List;

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
public class TestTypeResolver117 extends BaseTest
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
}
