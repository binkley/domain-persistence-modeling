package x.domainpersistencemodeling

import ch.tutteli.atrium.api.cc.en_GB.isEmpty
import ch.tutteli.atrium.api.cc.en_GB.toBe
import ch.tutteli.atrium.verbs.expect
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.Instant.EPOCH
import java.time.ZoneOffset.UTC

internal const val parentNaturalId = "a"
internal const val childNaturalId = "p"
internal val atZero = EPOCH.atOffset(UTC)

@AutoConfigureTestDatabase(replace = NONE)
@SpringBootTest
@Transactional
internal abstract class LiveTestBase {
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
     * change events.
     */
    @BeforeEach
    internal fun setUp() {
        sqlQueries.expectNext.isEmpty()
        testListener.expectNext.isEmpty()
    }

    internal fun expectSqlQueries() = sqlQueries.expectNext
    internal fun expectSqlQueriesByType() = sqlQueries.expectNextByType { it }
    internal fun <V> expectSqlQueriesByType(toValue: (List<String>) -> V) =
            sqlQueries.expectNextByType(toValue)

    internal fun resetSqlQueries() = sqlQueries.reset()

    internal fun expectDomainChangedEvents() = testListener.expectNext
    internal fun resetDomainChangedEvents() = testListener.reset()

    internal fun expectAllParents() = expect(parents.all().toList()).also {
        resetSqlQueries()
    }

    internal fun createNewParent(naturalId: String = parentNaturalId) =
            parents.createNew(naturalId)

    internal fun findExistingOrCreateNewParent(
            naturalId: String = parentNaturalId) =
            parents.findExistingOrCreateNew(naturalId).also {
                resetSqlQueries()
            }

    internal fun newSavedParent(): Parent {
        val saved = createNewParent().save()
        expect(saved.changed).toBe(true)
        val parent = saved.domain
        resetSqlQueries()
        resetDomainChangedEvents()
        return parent
    }

    internal fun currentPersistedParent(
            naturalId: String = parentNaturalId) =
            parents.findExisting(naturalId)!!.also {
                resetSqlQueries()
            }

    internal fun expectAllChildren() = expect(children.all().toList()).also {
        resetSqlQueries()
    }

    internal fun newSavedUnassignedChild(): UnassignedChild {
        val saved = createNewUnassignedChild().save()
        expect(saved.changed).toBe(true)
        val child = saved.domain
        resetSqlQueries()
        resetDomainChangedEvents()
        return child as UnassignedChild
    }

    internal fun currentPersistedChild(
            naturalId: String = childNaturalId) =
            children.findExisting(naturalId)!!.also {
                resetSqlQueries()
            }

    internal fun createNewUnassignedChild(
            naturalId: String = childNaturalId) =
            children.createNewUnassigned(naturalId)

    internal fun findExistingOrCreateNewUnassignedChild(
            naturalId: String = childNaturalId) =
            children.findExistingOrCreateNewUnassigned(naturalId).also {
                resetSqlQueries()
            }
}
