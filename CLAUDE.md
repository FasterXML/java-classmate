# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ClassMate is a zero-dependency Java library for accurately introspecting type information, including reliable resolution of generic type declarations for both classes ("types") and members (fields, methods and constructors). It exists to work around the shortcomings of `java.lang.reflect.Type`.

## Build Commands

Use the Maven wrapper (`./mvnw`), not `mvn`.

```bash
./mvnw clean verify                          # full build + tests
./mvnw test                                  # tests only
./mvnw test -Dtest=TestClassName             # single test class
./mvnw test -Dtest=TestClassName#methodName  # single test method
./mvnw clean package -DskipTests             # build without tests
```

- JaCoCo coverage report is generated in the `test` phase: `target/site/jacoco/index.html`.
- Surefire excludes `**/failing/*.java`; put known-failing reproduction tests in a `com.fasterxml.classmate.failing` test package (none exist currently).
- Build targets Java 8 (`version.jdk=1.8`); no language features newer than Java 8 in `src/main`. CI (`.github/workflows/main.yml`) runs on Java 8, 17, 21 and 25.
- Parent POM is `com.fasterxml:oss-parent`. Release notes live in `VERSION.txt`.

## Module / Packaging

- JPMS `module-info` is **hand-maintained** in `src/moditect/module-info.java` and injected into the jar by the moditect plugin at `package`. Adding a new package requires updating it.
- OSGi metadata (in `pom.xml`) treats `com.fasterxml.classmate.util` as private, while JPMS exports it — keep both in mind when moving classes between packages.

## Architecture

### Core API flow

1. `TypeResolver.resolve(...)` → `ResolvedType` (full generic info for a class, including supertypes and `TypeBindings`)
2. `MemberResolver.resolve(ResolvedType, AnnotationConfiguration, AnnotationOverrides)` → `ResolvedTypeWithMembers`
3. `ResolvedTypeWithMembers` exposes resolved fields, member/static methods and constructors with fully bound generic types.

`TypeResolver` is stateful (caches resolved types), thread-safe, and meant to be shared. `MemberResolver` holds only configuration (filters, whether to include `Object` members, etc.) and is cheap to create.

### Packages

- `com.fasterxml.classmate` — public API (`TypeResolver`, `MemberResolver`, `ResolvedType`, `TypeBindings`, `GenericType<T>` super-type token, annotation configuration/overrides ("mix-ins"), `Filter`).
- `types` — `ResolvedType` implementations: object/interface/array/primitive types, plus `ResolvedRecursiveType` and `TypePlaceHolder` for self-referential definitions like `Enum<E extends Enum<E>>`.
- `members` — `Raw*` (unresolved) vs. `Resolved*` (generic-bound) members; `HierarchicType` represents a type in the flattened hierarchy used during member resolution.
- `util` — caching and resolution helpers. `ResolvedTypeCache` is abstract with two implementations: `LRUTypeCache` (default, synchronized, via `ResolvedTypeCache.lruCache(200)`) and `ConcurrentTypeCache` (clears all entries when full). `ClassStack` tracks the in-progress resolution stack to detect recursion.

### Member resolution semantics

- Fields: from the type and all supertypes, minus fields masked by a same-named subclass field.
- Member methods: from the type and supertypes, minus overridden ones; interface methods are dropped when a class provides the implementation. Annotations from overridden methods are merged according to `AnnotationInclusion`.
- Constructors and static methods: only from the resolved type itself.
- `java.lang.Object` members are excluded by default (configurable on `MemberResolver`).
- Only `RetentionPolicy.RUNTIME` annotations are visible.

### Type resolution notes

- Unbound type parameters resolve to their bounds (often `Object`), not to raw types.
- `TypeResolver.resolve(Class baseType, Class... typeParams)` and `resolve(GenericType<T>)` are the main ways to obtain parameterized types.

## Testing

Tests mirror the main package layout under `src/test/java/com/fasterxml/classmate/`. `TestReadme.java` contains runnable versions of the README usage examples — keep it in sync when changing README examples.
