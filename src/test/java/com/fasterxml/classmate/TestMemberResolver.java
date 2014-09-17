package com.fasterxml.classmate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import com.fasterxml.classmate.members.*;
import com.fasterxml.classmate.types.ResolvedObjectType;
import com.fasterxml.classmate.util.ClassKey;

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

    // simple class extending from Object for config tests (includeObject, filtering, etc)
    @SuppressWarnings("unused")
    static class SimpleClass
    {
        private String test;

        SimpleClass(String test) {
            this.test = test;
        }
        SimpleClass() { }

        private String getTest() { return test; }
    }
    
    /*
    /**********************************************************************
    /* setup
    /**********************************************************************
     */

    protected TypeResolver typeResolver;

    @Override
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

    public void testIncludeObject()
    {
        ResolvedType simpleResolvedType = typeResolver.resolve(SimpleClass.class);
        MemberResolver mr = new MemberResolver(typeResolver);
        mr.setIncludeLangObject(false);

        ResolvedTypeWithMembers simpleResolvedTypeWithMembers = mr.resolve(simpleResolvedType, null, null);
        assertEquals(1, simpleResolvedTypeWithMembers.getMemberMethods().length);
        assertEquals(1, simpleResolvedTypeWithMembers.getMemberFields().length);

        mr = new MemberResolver(typeResolver);
        mr.setIncludeLangObject(true);

        simpleResolvedTypeWithMembers = mr.resolve(simpleResolvedType, null, null);
        assertEquals(12, simpleResolvedTypeWithMembers.getMemberMethods().length);
        assertEquals(1, simpleResolvedTypeWithMembers.getMemberFields().length);
    }

    public void testFilters()
    {
        ResolvedType simpleResolvedType = typeResolver.resolve(SimpleClass.class);
        MemberResolver mr = new MemberResolver(typeResolver);
        mr.setIncludeLangObject(true);

        ResolvedTypeWithMembers simpleResolvedTypeWithMembers = mr.resolve(simpleResolvedType, null, null);
        assertEquals(12, simpleResolvedTypeWithMembers.getMemberMethods().length);
        assertEquals(1, simpleResolvedTypeWithMembers.getMemberFields().length);
        assertEquals(2, simpleResolvedTypeWithMembers.getConstructors().length);

        // now filter methods
        mr = new MemberResolver(typeResolver);
        mr.setIncludeLangObject(true);
        mr.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "notify".equals(element.getName());
            }
        });

        simpleResolvedTypeWithMembers = mr.resolve(simpleResolvedType, null, null);
        assertEquals(1, simpleResolvedTypeWithMembers.getMemberMethods().length);
        assertEquals(1, simpleResolvedTypeWithMembers.getMemberFields().length);
        assertEquals(2, simpleResolvedTypeWithMembers.getConstructors().length);

        // now filter fields
        mr = new MemberResolver(typeResolver);
        mr.setIncludeLangObject(true);
        mr.setFieldFilter(new Filter<RawField>() {
            @Override public boolean include(RawField element) {
                return "DNE".equals(element.getName());
            }
        });

        simpleResolvedTypeWithMembers = mr.resolve(simpleResolvedType, null, null);
        assertEquals(12, simpleResolvedTypeWithMembers.getMemberMethods().length);
        assertEquals(0, simpleResolvedTypeWithMembers.getMemberFields().length);
        assertEquals(2, simpleResolvedTypeWithMembers.getConstructors().length);

        // now filter constructors
        mr = new MemberResolver(typeResolver);
        mr.setIncludeLangObject(true);
        mr.setConstructorFilter(new Filter<RawConstructor>() {
            @Override public boolean include(RawConstructor element) {
                return element.getRawMember().getParameterTypes().length > 0;
            }
        });

        simpleResolvedTypeWithMembers = mr.resolve(simpleResolvedType, null, null);
        assertEquals(12, simpleResolvedTypeWithMembers.getMemberMethods().length);
        assertEquals(1, simpleResolvedTypeWithMembers.getMemberFields().length);
        assertEquals(1, simpleResolvedTypeWithMembers.getConstructors().length);
    }

    public void testAddOverridesFromInterfaces() throws IllegalAccessException, InvocationTargetException
    {
        ResolvedType resolvedType = typeResolver.resolve(MemberResolver.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "_addOverrides".equals(element.getName()) && element.getRawMember().getParameterTypes()[2].equals(Class.class);
            }
        });
        ResolvedTypeWithMembers resolvedTypeWithMembers = memberResolver.resolve(resolvedType, null, null);
        ResolvedMethod addOverridesResolvedMethod = resolvedTypeWithMembers.getMemberMethods()[0];
        Method addOverridesMethod = addOverridesResolvedMethod.getRawMember();
        addOverridesMethod.setAccessible(true);

        List<HierarchicType> typesWithOverrides = new ArrayList<HierarchicType>();
        Set<ClassKey> seenTypes = new HashSet<ClassKey>();
        addOverridesMethod.invoke(memberResolver, typesWithOverrides, seenTypes, String.class);
        assertEquals(4, seenTypes.size());
        assertEquals(4, typesWithOverrides.size());

        memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "_addOverrides".equals(element.getName()) && element.getRawMember().getParameterTypes()[2].equals(ResolvedType.class);
            }
        });
        resolvedTypeWithMembers = memberResolver.resolve(resolvedType, null, null);
        addOverridesResolvedMethod = resolvedTypeWithMembers.getMemberMethods()[0];
        addOverridesMethod = addOverridesResolvedMethod.getRawMember();
        addOverridesMethod.setAccessible(true);

        typesWithOverrides = new ArrayList<HierarchicType>();
        seenTypes = new HashSet<ClassKey>();
        // first test null case.
        addOverridesMethod.invoke(memberResolver, typesWithOverrides, seenTypes, null);
        assertEquals(0, seenTypes.size());
        assertEquals(0, typesWithOverrides.size());
        // now test case with interfaces
        ResolvedType comparator = typeResolver.resolve(Comparator.class);
        ResolvedObjectType stringType = new ResolvedObjectType(String.class, TypeBindings.emptyBindings(),
                (ResolvedType) null, new ResolvedType[] { comparator });
        addOverridesMethod.invoke(memberResolver, typesWithOverrides, seenTypes, stringType);
        assertEquals(2, seenTypes.size());
        assertEquals(2, typesWithOverrides.size());
    }

    public void testGatherTypesWithInterfaces() throws IllegalAccessException, InvocationTargetException
    {
        ResolvedType resolvedType = typeResolver.resolve(MemberResolver.class);
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new Filter<RawMethod>() {
            @Override public boolean include(RawMethod element) {
                return "_gatherTypes".equals(element.getName());
            }
        });
        ResolvedTypeWithMembers resolvedTypeWithMembers = memberResolver.resolve(resolvedType, null, null);
        ResolvedMethod gatherTypesResolvedMethod = resolvedTypeWithMembers.getMemberMethods()[0];
        Method gatherTypesMethod = gatherTypesResolvedMethod.getRawMember();
        gatherTypesMethod.setAccessible(true);

        // test that the value within the seenTypes cache short-circuits execution
        ResolvedType currentType;
        Set<ClassKey> seenTypes = new HashSet<ClassKey>();
        List<ResolvedType> types = new ArrayList<ResolvedType>();
        currentType = new ResolvedObjectType(String.class, TypeBindings.emptyBindings(), (ResolvedType) null, ResolvedType.NO_TYPES);
        seenTypes.add(new ClassKey(String.class));

        gatherTypesMethod.invoke(memberResolver, currentType, seenTypes, types);
        assertEquals(1, seenTypes.size());
        assertEquals(0, types.size());

        // now test that a type with interfaces has its interfaces gathered
        ResolvedType comparator = typeResolver.resolve(Comparator.class);
        currentType = new ResolvedObjectType(String.class, TypeBindings.emptyBindings(), (ResolvedType) null, new ResolvedType[] { comparator });
        seenTypes.clear();
        gatherTypesMethod.invoke(memberResolver, currentType, seenTypes, types);
        assertEquals(2, seenTypes.size());
        assertEquals(2, types.size());
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
