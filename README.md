<a href="LICENSE.md">
<img src="https://unlicense.org/pd-icon.png" alt="Public Domain" align="right"/>
</a>

# Domain persistence modeling examples

* Ports
  * [Java Spring Boot](java-spring-boot)
  * [Kotlin Spring Boot](kotlin-spring-boot)
  * [Kotlin Micronaut](kotlin-micronaut) (most up-to-date)
* [Goals](#goals)
  * [Non-goals](#non-goals)
* [Concepts](#concepts)
  * [Separation of concerns](#separation-of-concerns)
  * [Scoped mutation](#scoped-mutation)
  * [Reversal of roles](#reversal-of-roles)
  * [Distinct types](#distinct-types)
* [Implementation](#implementation)
  * [Domain patterns](#domain-patterns)
  * [Tracked sets](#tracked-sets)
  * [Persistence](#persistence)
  * [Coverage](#coverage)
* [Open questions](#open-questions)
* [Spring-recommended documentation](#spring-recommended-documentation)
  * [Reference documentation](#reference-documentation)

These examples explores _domain persistence modeling_, that is, how the domain
model and persistence models relate, and good ways to express those relations
in code.  It is _not_ a mini-ORM, but uses Spring Data to abstract away
persistence operations.

## Goals

1. Correct: The code is correct, mostly expressed through testing.
2. Reasoning: I can reason about the code, and make changes with
   understanding.
3. Clean: The code is readable, concise, and I can make changes with
   confidence.
   
## Non-goals

- The tests define the code; more effort required to run this code in
  production.

## Concepts

The key concepts are:

- [_Separation of concerns_](#separation-of-concerns)
- [_Scoped mutation_](#scoped-mutation)
- [_Reversal of roles_](#reversal-of-roles)
- [_Distinct types_](#distinct-types)

### Separation of concerns

A classic programming thought, _separation of concerns_ is important for clean
code, often expressed in the OOP community as the
[Single Responsibility Principle](https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html).

That entails dividing properties of an entity at different levels:

- Simple values common to all levels
- Representations specific to persistence
- Representations specific to domain modeling
- Dependent values in the domain implementation
- Dependent values provided by another domain

I want to make changes in the right places, and have confidence of no surprise
side effects.

### Scoped mutation

Goals:

1. Use _immutable domain objects_ as the default.
2. Provide a simple way to mutate domain objects.
3. Make mutation obvious.
4. Make saving and deleting obvious.

To achieve these goals, each domain object implements
[`ScopedMutable`](src/main/kotlin/x/domainpersistencemodeling/ScopedMutable.kt),
which provides three methods:

* `ScopedMutable.update(block)` runs the mutations of `block` against a
  mutable version of the domain object, and returns an updated and immutable
  domain object

In the method, the return is the result of the block.

In addition, there are several specialized scoped-mutation methods:

* `PersistableDomain.save()` saves the domain object in persistence, and
  returns an updated and immutable domain object including any changes made
  by persistence (eg, versioning).
* `PersistableDomain.delete()` deletes the domain object in persistence.
  After this call, the domain object is _unusable_.

A domain object may have specialized scoped-mutation methods particular to
itself:

* `Parent.assign(Child)` mutates both parent and child, but does not persist
* `Parent.unassign(Child)` mutates both parent and child, but does not persist

### Reversal of roles

Commonly domain objects refer to objects from other domains, however, at the
persistence level, the relationship represented differently.  In this code,
the key example is:

* (Domain) A parent owns zero or more children, and children do not know about
  parents
* (Persistence) A child record knows to which parent record it belongs (if
  any), and parents do not know about children

Why this reversal?  At the domain level, activities center around the parent,
and one wishes to manage parent-child relationships through them.  However,
these domain objects are _aggregate roots_ at the persistence level.

Using this reversal of roles, saving a parent is an *O(N)* operation,
proportional to the count of children, and with some simple optimizations,
can be *O(1)* if changing only satellite data on the parent, or changing only
a single parent-child relationship (adding or removing).  (Without this
reversal, saving can be an *O(N^2)* operation&mdash;ouch.)

A **natural** persistence representation: child records track their parent
natural id:

```kotlin
data class ParentRecord( // Natural case
    var naturalKey: String,
    var someValue: Int)
data class ChildRecord(
    var naturalKey: String,
    var parentNaturalKey: String?, // Children reference parent
    var others: Set<AnotherRecordType>)
```

And the **contrary** persistence representation: parent records track their
child records:

```kotlin
data class ParentRecord( // Contrary case -- **do not do this**
    var naturalKey: String,
    var someValue: Int,
    var children: Set<ChildRecord>) // Parents own children
data class ChildRecord(
    var naturalKey: String,
    var others: Set<AnotherRecordType>)
```

When saving `ParentRecord`, a framework like Spring Data or Micronaut Data
performs these operations, likely in this order:

1. Deletes all existing `ChildRecord` rows.  This is a recursive operation, so
   any record references held by children are themselves recursively deleted.
   This clears the parent record of any previous child record references,
   including: children added, children removed, children modified. 
2. Saves the `ParentRecord` row to a store (such as an SQL database).
3. Inserts all `ChildRecord` rows for the current `ParentRecord`.  Again, this
   is a recursive operation (as above).  This updates the persistence store to
   match current parent-child relationships.

In this contrary case, we are ignoring that `ChildRecord` is itself an
_aggregate root_, creating large amounts of persistence churn.  In a scenario
when adding children to a parent one at a time (say from an external source
that does not add all children in a single operation), the overall
operation of saving a parent has become an *O(N<sup>2</sup>)* operation.
A parent with roughly 3,000 children results in a multiple of 10MM persistence
operations.

In this **contrary** persistence representation, saving a parent bears this
cost even when updating satellite data on the parent record.

#### Examples

For relationships formally exposed on the domain objects:

| Relationship |                                    | Domain property type | Persistence and field type    |
|--------------|------------------------------------|----------------------|-------------------------------|
| Many-to-one  | Parent **&#8666;** Child<em>*</em> | `Set<Child>`         | Child record, parent ID field |
| Optional-one | Parent **&rarr;** Other<em>?</em>  | `Other?`             | Parent record, other ID field |
| Optional-one | Child **&rarr;** Other<em>?</em>   | `Other?`             | Child record, other ID field  |

### Distinct types

(Only expressed in Kotlin ports, for now.)

To aid in avoiding programming mistakes, and catch such errors in complilation
rather than at runtime, we divide child domain objects into two types:
"unassigned" (`UnassignedChild`) and "assigned" (`AssignedChild`).  As the JVM
and JVM-based languages such as Kotlin and Java do not support casting based
on memory layout (unlike "C" and C++), the code needs to construct distinct
objects when converting between "unassigned" and "assigned".

There is an upside: This prevents misuse of the wrong type through their
common base type as conversion creates a new object.

One downside: This violates the rule that only `update` mutates objects.
Rather, for children, `update`, `assignTo`, and `unassignFromAny` all mutate.

## Implementation

### Domain patterns

The general pattern for a domain type is:

1. A _Factory_ type, responsible for creating instances, and coordinating with
   other factories and external dependencies (_e.g._, the framework
   application event publisher)
2. A _Record_ type, representing persisted data
3. A _Repository_ type, representing interactions with persistence (generally
   queries for records, and deletion)
4. A _Snapshot_ type, representing domain state as of creation

The _Domain_ type knows **only** about (directly or indirectly):

* Its factory
* Its record
* Its snapshot (and _Domain_ does not use the snapshot, but hands it back to
  the _Factory_ or helper methods as needed)

(There may be other details, but this is the general approach.)

A goal here is to minimize coupling, and follow an easily reproduced pattern
without falling back to a full-on ORM (like Hibernate).

The domain type implementation itself is split:

* Simple details
* Dependent details

_Simple_ details are those without dependencies, such as the database ID and
version, or simple properties.  _Dependent_ details are those which interact
with other domain types, such as _Parent_ and _Child_ relationships to
 _Other_.

### Tracked sets

A key data structure is the 
[_Tracked Set_](kotlin-micronaut/src/main/kotlin/x/domainpersistencemodeling/TrackedSortedSet.kt).
Tracked sets manage many-to-one (_eg_, children-to-parent) and optional-one
(_eg_, parent-to-other and child-to-other) relationships.

Essentially the tracked set has callbacks for adding/removing elements, so
domain objects can react to changes in relationships.  It also has minimal
bug-detection for misuse (_ie_, the tracked set for an optional-one
relationship should contain exactly zero or one elements).

### Persistence

Intentionally avoid more complex persistence features (_eg_, 
framework-managed relationships, lazy loading).  Expose most persistence
details directly in Kotlin/Java code, or in SQL.

The purpose of these examples is not to maximize framework usage, but to
demonstrate code-side domain-persistence patterns.

### Coverage

Maintain _full_ test coverage (presently only the Kotlin Micronaut version).
This has unexpected benefits:

* Discovery and removal of "dead" (unused) code
* Better design (_eg_, more discipline in inheritance _vs_ composition)
* Cleaner production code (_eg_, fewer branches, more concise design)
* Improved readability (_eg_, mark code used only in testing or debugging)

## Open questions

0. Soft delete
1. Should snapshots be deep or shallow?
2. Should snapshots generally follow the persistence representation or the
   domain representation?  If snapshots are purely implementation
   representations, it makes more sense to follow persistence, and let another
   layer translate (eg, REST)
3. It is hard or confusing to add new functionality to domain objects.
   Ideally, one could edit extension functions, test them separately, etc.,
   however, that does not address interface implementation concerns.  And the
   programmer needs to decide between "simple" and "dependent
   "functionalities".
4. Rule that root of aggregate always has version greater or equal to any
   descendants.

## Spring-recommended documentation

### Reference documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
  &mdash; After much trial with both Gradle and Maven, these examples uses
  Maven for:
  1. More repeatable builds
  2. Simplicity of build configuration
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.2.0.RC1/maven-plugin/)
* [Spring Data JDBC](https://docs.spring.io/spring-data/jdbc/docs/current/reference/html/)
  &mdash; Simple persistence based on _aggregate roots_
* [Flyway Migration](https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/reference/htmlsingle/#howto-execute-flyway-database-migrations-on-startup)
  &mdash; Simple installation of SQL schemas (unclear if it can be even
  simpler)
* [Atrium](https://docs.atriumlib.org) &mdash; `expect`-style assertions for
  Kotlin
* [Test Containers](https://www.testcontainers.org) &mdash; runs tests using
  Postgres in a Docker container
