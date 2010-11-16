package com.fasterxml.classmate;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.classmate.members.*;

public class TestMemberResolver extends BaseTest
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    static class BaseClass
    {
        protected int intField;
        
        protected static int staticIntField;

        public BaseClass(String arg) { }

        public BaseClass(String arg, boolean b) { }
        
        public static BaseClass factory1(String arg) { return null; }

        public void member1() { }

        protected void member2() { }
    }

    static class SubClass extends BaseClass
    {
        public int intField2;

        protected String stringField;

        protected static int staticIntField2;
        
        public SubClass() { super(""); }

        @Override public void member2() { }
    }

    // Silly mix-in that does not really add anything... (no annotations)
    static class DummyMixIn
    {
        public int x;
        public static int y;

        public DummyMixIn() { }
        
        public void method(String arg) { }
        public static void staticMethod(String arg) { }
    } 

    static class DummyMixIn2 extends DummyMixIn { }
    
    /*
    /**********************************************************************
    /* setup
    /**********************************************************************
     */

    protected TypeResolver typeResolver;
    
    protected void setUp()
    {
        // Let's use a single instance for all tests, to increase chance of seeing failures
        typeResolver = new TypeResolver();
    }

    /*
    /**********************************************************************
    /* Unit tests, type hierarchy handling
    /**********************************************************************
     */

    /**
     * Test for most basic thing; type hierarchy resolution 
     */
    public void testSimpleHierarchy()
    {
        MemberResolver mr = new MemberResolver(typeResolver);
        ResolvedType mainType = typeResolver.resolve(SubClass.class);
        // for now, use default annotation settings (== ignore), overrides (none), filtering (none)
        ResolvedTypeWithMembers bean = mr.resolve(mainType, null, null);
        assertNotNull(bean);

        // by default will NOT include Object.class, so should have just 2 types
        List<HierarchicType> types = bean.allTypesAndOverrides();
        assertEquals(2, types.size());
        assertSame(SubClass.class, types.get(0).getErasedType());
        assertSame(BaseClass.class, types.get(1).getErasedType());
    }

    public void testSimpleHierarchyWithMixins()
    {
        MemberResolver mr = new MemberResolver(typeResolver);
        ResolvedType mainType = typeResolver.resolve(SubClass.class);
        // for now, use default annotation settings (== ignore), but one explict override (mixin)
        AnnotationOverrides overrides = AnnotationOverrides.builder()
            .add(SubClass.class, DummyMixIn.class)
            .build();
        ResolvedTypeWithMembers bean = mr.resolve(mainType, null, overrides);
        assertNotNull(bean);
        // with one mix-in/override, 3 classes
        List<HierarchicType> types = bean.allTypesAndOverrides();
        assertEquals(3, types.size());
        assertSame(DummyMixIn.class, types.get(0).getErasedType());
        assertSame(SubClass.class, types.get(1).getErasedType());
        assertSame(BaseClass.class, types.get(2).getErasedType());

        // or, 4, if we do one more level of mix-ins
        overrides = AnnotationOverrides.builder()
            .add(BaseClass.class, DummyMixIn2.class)
            .build();
        bean = mr.resolve(mainType, null, overrides);
        types = bean.allTypesAndOverrides();
        assertEquals(4, types.size());
        assertSame(SubClass.class, types.get(0).getErasedType());
        assertSame(DummyMixIn2.class, types.get(1).getErasedType());
        assertSame(DummyMixIn.class, types.get(2).getErasedType());
        assertSame(BaseClass.class, types.get(3).getErasedType());
    }

    /*
    /**********************************************************************
    /* Unit tests, basic aggregation of fields, methods and constructors
    /**********************************************************************
     */

    /**
     * Test for checking basic member detection for super type.
     */
    public void testMembersForSupertype()
    {
        MemberResolver mr = new MemberResolver(typeResolver);
        ResolvedType mainType = typeResolver.resolve(BaseClass.class);
        ResolvedTypeWithMembers bean = mr.resolve(mainType, null, null);
        ResolvedMethod[] statics = bean.getStaticMethods();
        assertEquals(1, statics.length);
        
        ResolvedMethod[] members = bean.getMemberMethods();
        assertEquals(2, members.length);

        ResolvedField[] fields = bean.getMemberFields();
        assertEquals(1, fields.length);

        ResolvedConstructor[] ctors = bean.getConstructors();
        assertEquals(2, ctors.length);
    }
    
    /**
     * Test for checking basic aggregation with two types (subtype that
     * extends supertype)
     */
    public void testAggregationForSubtype()
    {
        MemberResolver mr = new MemberResolver(typeResolver);
        ResolvedType mainType = typeResolver.resolve(SubClass.class);
        ResolvedTypeWithMembers bean = mr.resolve(mainType, null, null);
        
        verifySubtypeAggregate(bean);
    }

    public void testAggregationForSubtypeAndDummyMixin()
    {
        MemberResolver mr = new MemberResolver(typeResolver);
        // Add "dummy" override/mix-in, which has nothing relevant to add; ensure nothing is added:
        AnnotationOverrides overrides = AnnotationOverrides.builder()
            .add(SubClass.class, DummyMixIn.class)
            .build();
        ResolvedType mainType = typeResolver.resolve(SubClass.class);
        ResolvedTypeWithMembers bean = mr.resolve(mainType, null, overrides);

        verifySubtypeAggregate(bean);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
    
    private void verifySubtypeAggregate(ResolvedTypeWithMembers bean)
    {
        ResolvedMethod[] statics = bean.getStaticMethods();
        assertEquals(0, statics.length);
        
        ResolvedMethod[] members = bean.getMemberMethods();
        assertEquals(2, members.length);

        ResolvedField[] fields = bean.getMemberFields();
        assertEquals(3, fields.length);

        ResolvedConstructor[] ctors = bean.getConstructors();
        assertEquals(1, ctors.length);
    }
    
    
}
