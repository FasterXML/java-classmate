## Overview

ClassMate is a zero-dependency Java library for accurately introspecting type information, including reliable resolution of generic type declarations for both classes ("types") and members (fields, methods and constructors).

Project is licensed under [Apache 2](http://www.apache.org/licenses/LICENSE-2.0.txt).

## Status

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fasterxml/classmate/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fasterxml/classmate/)
[![Javadoc](https://javadoc.io/badge/com.fasterxml/classmate.svg)](http://www.javadoc.io/doc/com.fasterxml/classmate)
[![Tidelift](https://tidelift.com/badges/package/maven/com.fasterxml:classmate)](https://tidelift.com/subscription/pkg/maven-com-fasterxml-classmate?utm_source=maven-com-fasterxml-classmate&utm_medium=referral&utm_campaign=readme)

## Support

### Community support

Classmate is supported by the community via the mailing list: [java-classmate-user](https://groups.google.com/forum/#!forum/java-classmate-users)

### Enterprise support

Available as part of the Tidelift Subscription.

The maintainers of `classmate` and thousands of other packages are working with Tidelift to deliver commercial support and maintenance for the open source dependencies you use to build your applications. Save time, reduce risk, and improve code health, while paying the maintainers of the exact dependencies you use. [Learn more.](https://tidelift.com/subscription/pkg/maven-com-fasterxml-classmate?utm_source=maven-com-fasterxml-classmate&utm_medium=referral&utm_campaign=enterprise&utm_term=repo)

## Contributing

Contributions are welcome (of course!); we require a simple one-page CLA to help simplify distribution
(corporate users want to know how contributions are handled), one per contributor. Feel free to submit
Pull Requests and we will get you through this formality.

One special case is that for reporting possible security issues ("vulnerabilities"), we recommend filing a
[Tidelift security contact](https://tidelift.com/security) (NOTE: you do NOT have to be a subscriber to do this).

## Documentation

[Project wiki](/../../wiki) has Javadocs.

External links that may help include:

* [Resolving Generic Types with Classmate](http://www.cowtowncoder.com/blog/archives/2012/04/entry_471.html) (some simple usage examples)
* [Problem with java.lang.reflect.Type](http://www.cowtowncoder.com/blog/archives/2010/12/entry_436.html) (explanation of issues ClassMate was written to solve)

-----

## Usage

### Maven dependency

To use ClassMate via Maven, include following dependency:

```xml
<dependency>
  <groupId>com.fasterxml</groupId>
  <artifactId>classmate</artifactId>
  <version>1.5.0</version>
</dependency>
```

### Java 9 module

Module name to use for Java 9 and above is `com.fasterxml.classmate`; `module-info` included
from version `1.5.0` on.

### Non-Maven

Downloads available from [Project wiki](../../wiki).

### Resolving Class type information

Main class used for fully resolving type information for classes is `com.fasterxml.classmate.TypeResolver`.
TypeResolver does simple caching for resolved supertypes (since many subtypes resolve to smaller set of supertypes, typically). Since all access to shared data is synchronized, a single `TypeResolver` instance is typically shared for a single system (as a plain old static singleton): there are no benefits to instantiating more instances.

Its main resolution methods are:

* `resolve(Class cls)`: given a plain old class, will use generic type information that super type declarations (extends, implements) may have.
* `resolve(GenericType<T>)`: given a subtype of `GenericType` (which uses ["Super-type Token" pattern](http://gafter.blogspot.com/2006/12/super-type-tokens.html)), fully resolve type information
* `resolve(Class<?> baseType, Class<?> typeParameter1, ... , Class<?> typeParameter2)`: given base type (like `List.class`) and zero or more type parameters (either as `Class` es to resolve, or as `ResolvedType` s), resolves type information

Result in all these cases is an instance of `ResolvedType`, which you can think of as generic type information containing replacement for `java.lang.Class`. It is also the starting point for resolving member (constructor, field, method) information.

### Resolving Type parameters for a class

While finding type parameters for specific class is relatively easy (using `getTypeParameters`), what you more commonly need to know is type parameters for a type implemented or extended.
Specifically, consider case of:

    public class StringIntMap extends HashMap<String,Integer> { }

where you would want to know `key` and `value` types of your Map sub-type.
The first step is the same:

    ResolvedType type = typeResolver.resolve(StringIntMap.class);

and to find parameter bindings for `java.util.Map`, you will use:

    List<ResolvedType> mapParams = type.typeParametersFor(Map.class);
    ResolvedType keyType = mapParams.get(0);
    ResolvedType valueType = mapParams.get(1);

Note: if types were left unspecified (like, say, `public class MyMap<K,V> extends Map<K,V>`), you will always get resolved types based on bounds: in this case, it would be equivalent to parameterization of `Map<Object,Object`).

### Resolving Member information

Member information resolution is done by `com.fasterxml.classmate.MemberResolver`, which takes a `ResolvedType` and produces `ResolvedTypeWithMembers`. As with `TypeResolver`, a single instance is typically shared by all code; but since no reuse of information is done, creating new instances is cheap and need not be avoided.

There are a few configuration options that can be used to determine things like:

* Whether to include information from `java.lang.Object` (default: ignore and do not include)
* Which members to filter out before aggregation (default: no filtering, include all members)
* Which annotations to include in resolved members (default: include nothing)
* For method annotations included, whether annotations from overridden methods are be inherited by overriding methods.
* Which annotation overrides (aka "mix-ins") to use for which classes (default: no overrides)

`ResolvedTypeWithMembers` has simple accessors for:

* Constructors: only constructors of the resolved type itself included (no constructors of superclasses)
* Fields: all fields from resolved type and its superclasses are included; expect in cases where fields are masked, in which case masked fields (super-class field with same name as a field on its sub-class) are not included.
* Static methods: only methods declared by resolved type itself
* Member methods: all methods from resolved type and its supertypes are included; except for overriding in which case only overriding method is included (which also means that methods from interfaces are typically not included, when implementing class has overriding method intance)

Annotations of all member types can be overridden by annotation overrides; annotation value defaulting only works for members that use inheritance, meaning just member methods.

Member information is lazily constructed. Access to member information is synchronized such that it is safe to share `ResolvedTypeWithMembers` instances.

## Examples

The following examples are all backed by an accompanying _junit_ [test](java-classmate/blob/master/src/test/java/com/fasterxml/classmate/TestReadme.java)

### Resolving Classes

##### Resolve `List.class`

```java
TypeResolver typeResolver = new TypeResolver();
// listType => List<Object>
ResolvedType listType = typeResolver.resolve(List.class);
```

##### Resolve `List<String>.class`

```java
// listType => List<String>
ResolvedType listType = typeResolver.resolve(List.class, String.class);
```

##### Resolve `List<String>.class` (leveraging already ResolvedType object)

```java
ResolvedType stringType = typeResolver.resolve(String.class);
// listType => List<String>
ResolvedType listType = typeResolver.resolve(List.class, stringType);
```

##### Resolve `List<String>.class` using ["super type token"](http://gafter.blogspot.com/2006/12/super-type-tokens.html)

```java
// listType => List<String>
ResolvedType listType = typeResolver.resolve(new GenericType<List<String>>() {});
```

### Resolving Members (i.e., Field/Method/Constructor)

#### Resolving All Members

##### Resolve `ArrayList<String>` static/instance Methods

```java
ResolvedType arrayListType = typeResolver.resolve(ArrayList.class, String.class);
MemberResolver memberResolver = new MemberResolver(typeResolver);
ResolvedTypeWithMembers arrayListTypeWithMembers = memberResolver.resolve(arrayListType, null, null);
// get static methods
ResolvedMethod[] staticArrayListMethods = arrayListTypeWithMembers.getStaticMethods();
// get instance methods
ResolvedMethod[] arrayListMethods = arrayListTypeWithMembers.getMemberMethods();
```

##### Resolve `ArrayList<String>` Fields

```java
ResolvedType arrayListType = typeResolver.resolve(ArrayList.class, String.class);
MemberResolver memberResolver = new MemberResolver(typeResolver);
ResolvedTypeWithMembers arrayListTypeWithMembers = memberResolver.resolve(arrayListType, null, null);
// get static/instance fields
ResolvedField[] arrayListFields = arrayListTypeWithMembers.getMemberFields();
```

##### Resolve `ArrayList<String>` Constructors

```java
ResolvedType arrayListType = typeResolver.resolve(ArrayList.class, String.class);
MemberResolver memberResolver = new MemberResolver(typeResolver);
ResolvedTypeWithMembers arrayListTypeWithMembers = memberResolver.resolve(arrayListType, null, null);
// get static/instance fields
ResolvedConstructor[] arrayListConstructors = arrayListTypeWithMembers.getConstructors();
```

#### Resolving Particular Members (i.e., Filtering)

##### Resolve `ArrayList<String>#size()` Method

```java
ResolvedType arrayListType = typeResolver.resolve(ArrayList.class, String.class);
MemberResolver memberResolver = new MemberResolver(typeResolver);
memberResolver.setMethodFilter(new Filter<RawMethod>() {
    @Override public boolean include(RawMethod element) {
        return "size".equals(element.getName());
    }
});
ResolvedTypeWithMembers arrayListTypeWithMembers = memberResolver.resolve(arrayListType, null, null);
ResolvedMethod sizeMethod = arrayListTypeWithMembers.getMemberMethods()[0];
```

##### Resolve `ArrayList<String>.size` Field

```java
ResolvedType arrayListType = typeResolver.resolve(ArrayList.class, String.class);
MemberResolver memberResolver = new MemberResolver(typeResolver);
memberResolver.setFieldFilter(new Filter<RawField>() {
    @Override public boolean include(RawField element) {
        return "size".equals(element.getName());
    }
});
ResolvedTypeWithMembers arrayListTypeWithMembers = memberResolver.resolve(arrayListType, null, null);
ResolvedField sizeField = arrayListTypeWithMembers.getMemberFields()[0];
```

#### Resolving Members and their Annotations

Classes for reference in the examples below:
```java
@Retention(RetentionPolicy.RUNTIME)
public @interface Marker { }

@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MarkerA { }

public class SomeClass {
    @Marker @MarkerA
    public void someMethod() { }
}
public class SomeSubclass extends SomeClass {
    @Override
    public void someMethod() { }
}
```

##### Resolve `SomeClass#someMethod()`'s Annotations

```java
ResolvedType someType = typeResolver.resolve(SomeClass.class);
MemberResolver memberResolver = new MemberResolver(typeResolver);
memberResolver.setMethodFilter(new Filter<RawMethod>() {
    @Override public boolean include(RawMethod element) {
        return "someMethod".equals(element.getName());
    }
});
AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
ResolvedTypeWithMembers someTypeWithMembers = memberResolver.resolve(someType, annConfig, null);
ResolvedMethod someMethod = someTypeWithMembers.getMemberMethods()[0];
Marker marker = someMethod.get(Marker.class);  // marker != null
MarkerA markerA = someMethod.get(MarkerA.class); // markerA != null
```

##### Resolve `SomeSubclass#someMethod()`'s Annotations

```java

// setup removed for brevity; same as above only using SomeSubclass instead of SomeClass

AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_BUT_DONT_INHERIT);
ResolvedTypeWithMembers someSubclassTypeWithMembers = memberResolver.resolve(someSubclassType, annConfig, null);
ResolvedMethod someMethod = someSubclassTypeWithMembers.getMemberMethods()[0];
Marker marker = someMethod.get(Marker.class);  // marker == null
MarkerA markerA = someMethod.get(MarkerA.class); // markerA == null
Override override = someMethod.get(Override.class); // override == null (RetentionPolicy = SOURCE)
```

##### Resolve `SomeSubclass#someMethod()`'s Annotations including @Inherited

```java

// setup removed for brevity; same as above

AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED);
ResolvedTypeWithMembers someSubclassTypeWithMembers = memberResolver.resolve(someSubclassType, annConfig, null);
ResolvedMethod someMethod = someSubclassTypeWithMembers.getMemberMethods()[0];
Marker marker = someMethod.get(Marker.class);  // marker == null
MarkerA markerA = someMethod.get(MarkerA.class); // markerA != null
Override override = someMethod.get(Override.class); // override == null (RetentionPolicy = SOURCE)
```

##### Resolve `SomeSubclass#someMethod()`'s Annotations including all super class's Annotations

```java

// setup removed for brevity; same as above

AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT);
ResolvedTypeWithMembers someSubclassTypeWithMembers = memberResolver.resolve(someSubclassType, annConfig, null);
ResolvedMethod someMethod = someSubclassTypeWithMembers.getMemberMethods()[0];
Marker marker = someMethod.get(Marker.class);  // marker != null
MarkerA markerA = someMethod.get(MarkerA.class); // markerA != null
Override override = someMethod.get(Override.class); // override == null (RetentionPolicy = SOURCE)
```

#### Using Annotation "mix-ins"

Types with the same method signature, field definition or constructor signature but which aren't explicitly related to one another (i.e., _extend_ each other or _implement_ the same interface) can have their annotations "mixed in" to others' resolved types.  For example, using the `SomeClass` from above, let's add another class definition.

```java
public class SomeOtherClass {
    public void someMethod() { }
}
```

The `someMethod` signature on `SomeOtherClass` is the same as `SomeClass` however `SomeOtherClass` does not extend from `SomeClass`.  Member resolution for `SomeOtherClass`, like we've done above, will (of course) result in no Annotations.

```java

// setup removed for brevity; similar to above but using SomeOtherClass

AnnotationConfiguration annConfig = new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT);
ResolvedTypeWithMembers someOtherClassTypeWithMembers = memberResolver.resolve(someOtherClassType, annConfig, null);
ResolvedMethod someMethod = someOtherClassTypeWithMembers.getMemberMethods()[0];
Marker marker = someMethod.get(Marker.class);  // marker == null, of course
MarkerA markerA = someMethod.get(MarkerA.class); // markerA == null, of course
```

We can augment the annotations returned by `SomeOtherClass` with "mix-ins"

```java
// setup removed for brevity; same as above, using SomeOtherClass

// MIX-IN -> take SomeClass and apply to SomeOtherClass
AnnotationOverrides annOverrides = AnnotationOverrides.builder().add(SomeOtherClass.class, SomeClass.class).build();

ResolvedTypeWithMembers someOtherTypeWithMembers = memberResolver.resolve(someOtherType, annConfig, annOverrides);
ResolvedMethod someMethod = someOtherTypeWithMembers.getMemberMethods()[0];
Marker marker = someMethod.get(Marker.class);  // marker != null
MarkerA markerA = someMethod.get(MarkerA.class); // markerA != null
```

Now the `ResolvedMethod` for `SomeOtherClass` also contains the `Marker` and `MarkerA` annotations!
