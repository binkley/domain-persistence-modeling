package x.domainpersistencemodeling

import ch.tutteli.atrium.api.cc.en_GB.isEmpty
import ch.tutteli.atrium.api.cc.en_GB.toBe
import ch.tutteli.atrium.verbs.expect
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import x.domainpersistencemodeling.KnownState.ENABLED
import java.time.Instant.EPOCH
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

internal const val otherNaturalId = "o"
internal const val parentNaturalId = "p"
internal const val childNaturalId = "c"
internal val atZero = EPOCH.atOffset(UTC)

/**
 * Provides testing help for the full stack application with a Dockerized
 * Postgres on a random port.  Tests are transactional, so forgets any
 * persistence changes made during tests.
 *
 * The general pattern for "newFoo" methods:
 * - Resets the appropriate SQL or domain changed event tracker
 *
 * The general pattern for "expectFoo" methods:
 * - Returns an Atrium expectation natural to the methods
 * - Resets the appropriate SQL or domain changed event tracker
 */
@AutoConfigureTestDatabase(replace = NONE)
@SpringBootTest
@Transactional
internal abstract class LiveTestBase {
    @Autowired
    lateinit var others: OtherFactory
    @Autowired
    lateinit var parents: ParentFactory
    @Autowired
    lateinit var children: ChildFactory
    @Autowired
    lateinit var sqlQueries: SqlQueries
    @Autowired
    lateinit var testListener: TestListener<DomainChangedEvent<*>>

    /**
     * Aggressively checks that tests asserted against SQL queries and domain
     * change events.  Use [@AfterEach] to tie failures to tests that do not
     * check.
     */
    @AfterEach
    internal fun setUp() {
        sqlQueries.expectNext.isEmpty()
        testListener.expectNext.isEmpty()
    }

    internal fun expectSqlQueries() = sqlQueries.expectNext
    internal fun <V> expectSqlQueriesByType(toValue: (List<String>) -> V) =
            sqlQueries.expectNextByType(toValue)

    internal fun expectSqlQueryCountsByType(
            delete: Int = 0,
            insert: Int = 0,
            select: Int = 0,
            update: Int = 0,
            upsert: Int = 0) {
        val expected = mutableMapOf<String, Int>()
        if (0 != delete) expected["DELETE"] = delete
        if (0 != insert) expected["INSERT"] = insert
        if (0 != select) expected["SELECT"] = select
        if (0 != update) expected["UPDATE"] = update
        if (0 != upsert) expected["UPSERT"] = upsert
        expectSqlQueriesByType {
            it.size
        }.toBe(expected)
    }

    internal fun expectDomainChangedEvents() = testListener.expectNext

    internal fun expectAllOthers() = expect(others.all().toList()).also {
        sqlQueries.reset()
    }

    internal fun createNewOther(naturalId: String = otherNaturalId) =
            others.createNew(naturalId)

    internal fun newSavedOther(): Other {
        val saved = createNewOther().save()
        expect(saved.changed).toBe(true)
        val other = saved.domain
        sqlQueries.reset()
        testListener.reset()
        return other
    }

    internal fun currentPersistedOther(naturalId: String = otherNaturalId) =
            others.findExisting(naturalId)!!.also {
                sqlQueries.reset()
            }

    internal fun expectAllParents() = expect(parents.all().toList()).also {
        sqlQueries.reset()
    }

    internal fun createNewParent(naturalId: String = parentNaturalId) =
            parents.createNew(naturalId)

    internal fun newSavedParent(): Parent {
        val saved = createNewParent().save()
        expect(saved.changed).toBe(true)
        val parent = saved.domain
        sqlQueries.reset()
        testListener.reset()
        return parent
    }

    internal fun currentPersistedParent(naturalId: String = parentNaturalId) =
            parents.findExisting(naturalId)!!.also {
                sqlQueries.reset()
            }

    internal fun expectAllChildren() = expect(children.all().toList()).also {
        sqlQueries.reset()
    }

    internal fun newSavedUnassignedChild(): UnassignedChild {
        val saved = createNewUnassignedChild().save()
        expect(saved.changed).toBe(true)
        val child = saved.domain
        sqlQueries.reset()
        testListener.reset()
        return child
    }

    internal fun currentPersistedChild(
            naturalId: String = childNaturalId) =
            children.findExisting(naturalId)!!.also {
                sqlQueries.reset()
            }

    internal fun createNewUnassignedChild(
            naturalId: String = childNaturalId) =
            children.createNewUnassigned(naturalId)
}

internal fun anOtherChangedEvent( // TODO: Tie defaults to record defaults
        noBefore: Boolean = false,
        beforeValue: String? = null,
        beforeOtherVersion: Int = 0,
        noAfter: Boolean = false,
        afterValue: String? = null,
        afterVersion: Int = 0) = OtherChangedEvent(
        if (noBefore) null else OtherSnapshot(otherNaturalId, beforeValue,
                beforeOtherVersion),
        if (noAfter) null else OtherSnapshot(otherNaturalId, afterValue,
                afterVersion))

internal fun aParentChangedEvent( // TODO: Tie defaults to record defaults
        noBefore: Boolean = false,
        beforeOtherNaturalId: String? = null,
        beforeState: String = ENABLED.name,
        beforeAt: OffsetDateTime? = null,
        beforeValue: String? = null,
        beforeSideValues: Set<String> = setOf(),
        beforeVersion: Int = 0,
        noAfter: Boolean = false,
        afterOtherNaturalId: String? = null,
        afterState: String = ENABLED.name,
        afterAt: OffsetDateTime? = null,
        afterValue: String? = null,
        afterSideValues: Set<String> = setOf(),
        afterVersion: Int = 0) = ParentChangedEvent(
        if (noBefore) null else ParentSnapshot(parentNaturalId,
                beforeOtherNaturalId, beforeState, beforeAt, beforeValue,
                beforeSideValues, beforeVersion),
        if (noAfter) null else ParentSnapshot(parentNaturalId,
                afterOtherNaturalId, afterState, afterAt, afterValue,
                afterSideValues, afterVersion))

internal fun aChildChangedEvent( // TODO: Tie defaults to record defaults
        noBefore: Boolean = false,
        beforeOtherNaturalId: String? = null,
        beforeParentNaturalId: String? = null,
        beforeState: String = ENABLED.name,
        beforeAt: OffsetDateTime = atZero,
        beforeValue: String? = null,
        beforeSideValues: Set<String> = setOf(),
        beforeVersion: Int = 0,
        noAfter: Boolean = false,
        afterOtherNaturalId: String? = null,
        afterParentNaturalId: String? = null,
        afterState: String = ENABLED.name,
        afterAt: OffsetDateTime = atZero,
        afterValue: String? = null,
        afterSideValues: Set<String> = setOf(),
        afterVersion: Int = 0) = ChildChangedEvent(
        if (noBefore) null else ChildSnapshot(childNaturalId,
                beforeOtherNaturalId, beforeParentNaturalId, beforeState,
                beforeAt, beforeValue, beforeSideValues, beforeVersion),
        if (noAfter) null else ChildSnapshot(childNaturalId,
                afterOtherNaturalId, afterParentNaturalId, afterState,
                afterAt, afterValue, afterSideValues, afterVersion))
