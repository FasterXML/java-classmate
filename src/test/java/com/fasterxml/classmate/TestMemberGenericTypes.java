package com.fasterxml.classmate;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.classmate.members.ResolvedConstructor;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;

/**
 * Unit tests focused on ensuring that generic type information is
 * properly resolved for members.
 */
public class TestMemberGenericTypes extends BaseTest
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    static class Wrapper<T>
    {
        protected T value;
        
        protected T memberMethod(T argument) { return null; }

        protected static <ST> ST static1(ST argument) { return null; }

        public Wrapper() { }
        public Wrapper(T v) { value = v; }
    }
    
    static class ListWrapper<T> extends Wrapper<List<T>> { }

    static class StringListWrapper extends ListWrapper<String> { }

    // To test issue #3, local declarations
    static class WithLocals {
        public <T extends Serializable> T memberValue() { return null; }

        public static <T extends Serializable> void staticValue(T value) { }
    }
    
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
    /* Unit tests
    /**********************************************************************
     */

    /**
     * Test for verifying that basic generic type information is properly resolved
     * through type hierarchy.
     */
    public void testGenericWrappersForResolvedLeafType()
    {
        MemberResolver mr = new MemberResolver(typeResolver);
        ResolvedType mainType = typeResolver.resolve(StringListWrapper.class);
        // for now, use default annotation settings (== ignore), overrides (none), filtering (none)
        ResolvedTypeWithMembers bean = mr.resolve(mainType, null, null);
        
        // First let's verify that number of members is as expected
        ResolvedMethod[] statics = bean.getStaticMethods();
        assertEquals(0, statics.length);
        ResolvedMethod[] members = bean.getMemberMethods();
        assertEquals(1, members.length);
        ResolvedField[] fields = bean.getMemberFields();
        assertEquals(1, fields.length);
        ResolvedConstructor[] ctors = bean.getConstructors();
        // actually, compiler will auto-generate default ctor, so:
        assertEquals(1, ctors.length);

        // Type checking:
        
        // First, method:
        ResolvedType expectedType = typeResolver.resolve(List.class, String.class);
        ResolvedMethod m = members[0];
        assertEquals("memberMethod", m.getName());
        assertEquals(1, m.getArgumentCount());
        ResolvedType argType = m.getArgumentType(0);
        assertEquals(expectedType, argType);
        assertSame(Wrapper.class, m.getDeclaringType().getErasedType()); // declared in Wrapper

        ResolvedType returnType = m.getReturnType();
        assertEquals(expectedType, returnType);

        // Then field:
        ResolvedField f = fields[0];
        assertEquals("value", f.getName());
        assertEquals(f.getType(), expectedType);
        assertSame(Wrapper.class, f.getDeclaringType().getErasedType()); // declared in Wrapper

        ResolvedConstructor ctor = ctors[0];
        assertEquals(0, ctor.getArgumentCount());
        assertSame(StringListWrapper.class, ctor.getDeclaringType().getErasedType());
    }

    /**
     * Tests handling of incomplete information for intermediate class.
     */
    public void testGenericWrappersForIntermediate()
    {
        MemberResolver mr = new MemberResolver(typeResolver);
        ResolvedType mainType = typeResolver.resolve(ListWrapper.class);
        // for now, use default annotation settings (== ignore), overrides (none), filtering (none)
        ResolvedTypeWithMembers bean = mr.resolve(mainType, null, null);
        
        // First let's verify that number of members is as expected
        ResolvedMethod[] statics = bean.getStaticMethods();
        assertEquals(0, statics.length);
        ResolvedMethod[] members = bean.getMemberMethods();
        assertEquals(1, members.length);
        ResolvedField[] fields = bean.getMemberFields();
        assertEquals(1, fields.length);
        ResolvedConstructor[] ctors = bean.getConstructors();
        // actually, compiler will auto-generate default ctor, so:
        assertEquals(1, ctors.length);

        // Type checking: here we only know it's List<?>, which we see as List<Object>
        
        // First, method:
        ResolvedType expectedType = typeResolver.resolve(List.class, Object.class);
        ResolvedMethod m = members[0];
        assertEquals("memberMethod", m.getName());
        assertEquals(1, m.getArgumentCount());
        ResolvedType argType = m.getArgumentType(0);
        assertEquals(expectedType, argType);
        assertSame(Wrapper.class, m.getDeclaringType().getErasedType()); // declared in Wrapper

        ResolvedType returnType = m.getReturnType();
        assertEquals(expectedType, returnType);

        // Then field:
        ResolvedField f = fields[0];
        assertEquals("value", f.getName());
        assertEquals(f.getType(), expectedType);
        assertSame(Wrapper.class, f.getDeclaringType().getErasedType()); // declared in Wrapper

        ResolvedConstructor ctor = ctors[0];
        assertEquals(0, ctor.getArgumentCount());
        assertSame(ListWrapper.class, ctor.getDeclaringType().getErasedType());
    }

    /**
     * Tests handling of incomplete information for base type
     */
    public void testGenericWrappersForBaseType()
    {
        MemberResolver mr = new MemberResolver(typeResolver);
        ResolvedType mainType = typeResolver.resolve(Wrapper.class);
        // for now, use default annotation settings (== ignore), overrides (none), filtering (none)
        ResolvedTypeWithMembers bean = mr.resolve(mainType, null, null);
        
        // First let's verify that number of members is as expected
        ResolvedMethod[] statics = bean.getStaticMethods();
        assertEquals(1, statics.length);
        ResolvedMethod[] members = bean.getMemberMethods();
        assertEquals(1, members.length);
        ResolvedField[] fields = bean.getMemberFields();
        assertEquals(1, fields.length);
        ResolvedConstructor[] ctors = bean.getConstructors();
        assertEquals(2, ctors.length);

        // Type checking: here we only know it's ?, which we see as Object
        
        // First, method:
        ResolvedType expectedType = typeResolver.resolve(Object.class);
        ResolvedMethod m = members[0];
        assertEquals("memberMethod", m.getName());
        assertEquals(1, m.getArgumentCount());
        ResolvedType argType = m.getArgumentType(0);
        assertEquals(expectedType, argType);
        assertSame(Wrapper.class, m.getDeclaringType().getErasedType()); // declared in Wrapper

        ResolvedType returnType = m.getReturnType();
        assertEquals(expectedType, returnType);

        // Then field:
        ResolvedField f = fields[0];
        assertEquals("value", f.getName());
        assertEquals(f.getType(), expectedType);
        assertSame(Wrapper.class, f.getDeclaringType().getErasedType()); // declared in Wrapper

        // then constructors...
        for (ResolvedConstructor ctor : ctors) {
            assertSame(Wrapper.class, ctor.getDeclaringType().getErasedType());
            switch (ctor.getArgumentCount()) {
            case 0:
                break;
            case 1:
                assertEquals(ctor.getArgumentType(0), expectedType);
                break;
            default:
                fail("Unexpected number of ctor args: "+ctor.getArgumentCount());
            }
        }
    }

    /**
     * Unit test for Issue#3; ensuring that we can handle "local" declarations
     */
    public void testLocalGenerics()
    {
        MemberResolver mr = new MemberResolver(typeResolver);
        ResolvedType mainType = typeResolver.resolve(WithLocals.class);
        ResolvedTypeWithMembers bean = mr.resolve(mainType, null, null);

        // should have one static, one member method
        ResolvedMethod[] statics = bean.getStaticMethods();
        assertEquals(1, statics.length);
        ResolvedMethod[] members = bean.getMemberMethods();
        assertEquals(1, members.length);

        assertEquals("memberValue", members[0].getName());
        ResolvedType returnType = members[0].getReturnType();
        assertEquals(Serializable.class, returnType.getErasedType());
        assertEquals(0, members[0].getArgumentCount());

        assertEquals("staticValue", statics[0].getName());
        assertEquals(1, statics[0].getArgumentCount());
        ResolvedType arg = statics[0].getArgumentType(0);
        assertEquals(Serializable.class, arg.getErasedType());
    }
}
