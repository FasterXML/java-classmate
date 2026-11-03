package com.fasterxml.classmate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import com.fasterxml.classmate.members.RawMethod;
import com.fasterxml.classmate.members.ResolvedMethod;

import static org.junit.jupiter.api.Assertions.*;

public class MemberResolver57Test
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface MethodMark { String value(); }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface ParameterMark { String value(); }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @interface InterfaceOnly { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @interface ClassOnly { }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @interface InheritedMark { String value(); }

    interface Foo { }
    static class Bar implements Foo { }

    interface Contract {
        @MethodMark("interface") @InterfaceOnly @InheritedMark("interface")
        Foo convert(@ParameterMark("interface") @InterfaceOnly
                @InheritedMark("interface") String value);
    }

    static class Parent {
        @MethodMark("class") @ClassOnly @InheritedMark("class")
        public Bar convert(@ParameterMark("class") @ClassOnly
                @InheritedMark("class") String value) { return null; }
    }

    static class Child extends Parent implements Contract { }

    static class DirectChild extends Child {
        @Override
        public Bar convert(String value) { return null; }
    }

    abstract static class ChildMixin {
        @MethodMark("child-mixin")
        abstract Foo convert(@ParameterMark("child-mixin") String value);
    }

    abstract static class ParentMixin {
        @MethodMark("parent-mixin")
        abstract Bar convert(@ParameterMark("parent-mixin") String value);
    }

    interface GenericContract<T> { T convert(T value); }
    static class GenericParent<T> {
        public T convert(T value) { return value; }
    }
    static class GenericChild extends GenericParent<String>
        implements GenericContract<String> { }

    interface GenericReturn<T> { T value(); }
    static class StringParent { public String value() { return "value"; } }
    static class StringChild extends StringParent implements GenericReturn<CharSequence> { }

    interface ValueContract {
        @MethodMark("interface") @InterfaceOnly
        Foo value();
    }
    static class ValueParent {
        @MethodMark("class") @ClassOnly
        public Bar value() { return null; }
    }
    static class ValueChild extends ValueParent implements ValueContract { }
    abstract static class ChildValueMixin {
        @MethodMark("child-mixin")
        abstract Foo value();
    }
    abstract static class ParentValueMixin {
        @MethodMark("parent-mixin")
        abstract Bar value();
    }
    interface DefaultContract {
        default Foo value() { return null; }
    }
    static class DefaultChild extends ValueParent implements DefaultContract { }
    abstract static class AbstractParent { public abstract Bar value(); }
    abstract static class AbstractChild extends AbstractParent implements ValueContract { }
    static class PrivateParent {
        @SuppressWarnings("unused")
        private Bar value() { return null; }
    }
    abstract static class PrivateChild extends PrivateParent implements ValueContract { }

    @Test
    public void inheritedClassMethodHasCovariantReturn()
    {
        ResolvedMethod method = method(Child.class, "convert", null, null);
        assertSame(Parent.class, method.getRawMember().getDeclaringClass());
        assertSame(Parent.class, method.getDeclaringType().getErasedType());
        assertSame(Bar.class, method.getReturnType().getErasedType());
        assertSame(String.class, method.getArgumentType(0).getErasedType());
    }

    @Test
    public void inheritedGenericMethodUsesSuperclassBindings()
    {
        ResolvedMethod method = method(GenericChild.class, "convert", null, null);
        assertSame(GenericParent.class, method.getRawMember().getDeclaringClass());
        assertSame(String.class, method.getReturnType().getErasedType());
        assertSame(String.class, method.getArgumentType(0).getErasedType());
    }

    @Test
    public void inheritedReturnCanBeNarrowerThanGenericInterface()
    {
        ResolvedMethod method = method(StringChild.class, "value", null, null);
        assertSame(StringParent.class, method.getRawMember().getDeclaringClass());
        assertSame(String.class, method.getReturnType().getErasedType());
    }

    @Test
    public void classOnlyMethodIsUnchanged()
    {
        assertSame(Parent.class, method(Parent.class, "convert", null, null)
                .getRawMember().getDeclaringClass());
    }

    @Test
    public void interfaceOnlyMethodIsUnchanged()
    {
        assertSame(Contract.class, method(Contract.class, "convert", null, null)
                .getRawMember().getDeclaringClass());
    }

    @Test
    public void directlyOverriddenMethodKeepsPrecedence()
    {
        assertSame(DirectChild.class, method(DirectChild.class, "convert", null, null)
                .getRawMember().getDeclaringClass());
    }

    @Test
    public void classMethodTakesPrecedenceOverDefaultInterfaceMethod()
    {
        assertSame(ValueParent.class, method(DefaultChild.class, "value", null, null)
                .getRawMember().getDeclaringClass());
    }

    @Test
    public void abstractClassMethodTakesPrecedenceOverInterfaceMethod()
    {
        assertSame(AbstractParent.class, method(AbstractChild.class, "value", null, null)
                .getRawMember().getDeclaringClass());
    }

    @Test
    public void privateClassMethodDoesNotImplementInterface()
    {
        assertSame(ValueContract.class, method(PrivateChild.class, "value", null, null)
                .getRawMember().getDeclaringClass());
    }

    @Test
    public void filteredClassMethodDoesNotReplaceInterface()
    {
        TypeResolver types = new TypeResolver();
        MemberResolver resolver = new MemberResolver(types);
        resolver.setMethodFilter(new Filter<RawMethod>() {
            @Override
            public boolean include(RawMethod method) {
                return method.getDeclaringType().getErasedType() != Parent.class;
            }
        });
        ResolvedMethod[] methods = resolver.resolve(types.resolve(Child.class), null, null)
                .getMemberMethods();
        assertEquals(1, methods.length);
        assertSame(Contract.class, methods[0].getRawMember().getDeclaringClass());
    }

    @Test
    public void inheritableAnnotationsKeepExistingPriority()
    {
        ResolvedMethod method = annotated(Child.class, AnnotationInclusion.INCLUDE_AND_INHERIT, null);
        assertEquals("interface", method.get(MethodMark.class).value());
        assertEquals("interface", method.getParam(0, ParameterMark.class).value());
        assertNotNull(method.get(InterfaceOnly.class));
        assertNotNull(method.get(ClassOnly.class));
        assertNotNull(method.getParam(0, InterfaceOnly.class));
        assertNotNull(method.getParam(0, ClassOnly.class));
        assertSame(Parent.class, method.getRawMember().getDeclaringClass());
    }

    @Test
    public void nonInheritableAnnotationsComeFromSelectedClass()
    {
        ResolvedMethod method = annotated(Child.class, AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT, null);
        assertEquals("class", method.get(MethodMark.class).value());
        assertEquals("class", method.getParam(0, ParameterMark.class).value());
        assertNull(method.get(InterfaceOnly.class));
        assertNull(method.getParam(0, InterfaceOnly.class));
        assertNotNull(method.get(ClassOnly.class));
        assertNotNull(method.getParam(0, ClassOnly.class));
        assertSame(Parent.class, method.getRawMember().getDeclaringClass());
    }

    @Test
    public void inheritedOnlyModeFiltersInterfaceAnnotations()
    {
        ResolvedMethod method = annotated(Child.class,
                AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED, null);
        assertEquals("class", method.get(MethodMark.class).value());
        assertEquals("class", method.getParam(0, ParameterMark.class).value());
        assertEquals("interface", method.get(InheritedMark.class).value());
        assertEquals("interface", method.getParam(0, InheritedMark.class).value());
        assertNull(method.get(InterfaceOnly.class));
        assertNull(method.getParam(0, InterfaceOnly.class));
    }

    @Test
    public void excludedMethodAnnotationsStayExcluded()
    {
        ResolvedMethod method = annotated(Child.class, AnnotationInclusion.DONT_INCLUDE, null);
        assertNull(method.get(MethodMark.class));
        assertNull(method.get(InterfaceOnly.class));
        assertNull(method.get(ClassOnly.class));
        assertSame(Parent.class, method.getRawMember().getDeclaringClass());
    }

    @Test
    public void childMixinKeepsInheritableMethodAndParameterPriority()
    {
        AnnotationOverrides mixins = AnnotationOverrides.builder()
                .add(Child.class, ChildMixin.class).add(Parent.class, ParentMixin.class).build();
        ResolvedMethod method = annotated(Child.class, AnnotationInclusion.INCLUDE_AND_INHERIT, mixins);
        assertEquals("child-mixin", method.get(MethodMark.class).value());
        assertEquals("child-mixin", method.getParam(0, ParameterMark.class).value());
        assertNotNull(method.get(ClassOnly.class));
        assertSame(Parent.class, method.getRawMember().getDeclaringClass());
    }

    @Test
    public void nonInheritableChildMixinStillOverridesSelectedMethod()
    {
        AnnotationOverrides mixins = AnnotationOverrides.builder()
                .add(ValueChild.class, ChildValueMixin.class).build();
        ResolvedMethod method = method(ValueChild.class, "value",
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT), mixins);
        assertEquals("child-mixin", method.get(MethodMark.class).value());
        assertNull(method.get(InterfaceOnly.class));
        assertSame(ValueParent.class, method.getRawMember().getDeclaringClass());
    }

    @Test
    public void nonInheritableParentMixinMatchesClassOnlyResolution()
    {
        AnnotationOverrides mixins = AnnotationOverrides.builder()
                .add(ValueParent.class, ParentValueMixin.class).build();
        AnnotationConfiguration config = new AnnotationConfiguration.StdConfiguration(
                AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
        ResolvedMethod parent = method(ValueParent.class, "value", config, mixins);
        ResolvedMethod child = method(ValueChild.class, "value", config, mixins);
        assertEquals("parent-mixin", parent.get(MethodMark.class).value());
        assertEquals(parent.get(MethodMark.class), child.get(MethodMark.class));
        assertSame(ValueParent.class, child.getRawMember().getDeclaringClass());
    }

    @Test
    public void nonInheritableChildMixinPrecedesParentMixin()
    {
        AnnotationOverrides mixins = AnnotationOverrides.builder()
                .add(ValueChild.class, ChildValueMixin.class).add(ValueParent.class, ParentValueMixin.class).build();
        ResolvedMethod method = method(ValueChild.class, "value",
                new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT), mixins);
        assertEquals("child-mixin", method.get(MethodMark.class).value());
        assertSame(ValueParent.class, method.getRawMember().getDeclaringClass());
    }

    private ResolvedMethod annotated(Class<?> type, AnnotationInclusion inclusion,
            AnnotationOverrides overrides)
    {
        return method(type, "convert", new AnnotationConfiguration.StdConfiguration(inclusion), overrides);
    }

    private ResolvedMethod method(Class<?> type, String name, AnnotationConfiguration config,
            AnnotationOverrides overrides)
    {
        TypeResolver types = new TypeResolver();
        ResolvedMethod found = null;
        for (ResolvedMethod method : new MemberResolver(types)
                .resolve(types.resolve(type), config, overrides).getMemberMethods()) {
            if (method.getName().equals(name)) {
                assertNull(found, "Only one method should be selected for " + name);
                found = method;
            }
        }
        assertNotNull(found, "Expected method " + name);
        return found;
    }
}
