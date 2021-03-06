package x.domainpersistencemodeling

import ch.tutteli.atrium.api.cc.en_GB.isNotEmpty
import org.junit.jupiter.api.Test

internal class PersistencePerformanceTest
    : LiveTestBase() {
    @Test
    fun `should save only one mutated child`() {
        val parent = newSavedParent()
        for (i in 1..2) {
            val uniqueUnassignedChild =
                children.createNewUnassigned("c$i")
            parent.assign(uniqueUnassignedChild)
        }
        parent.save()

        expectSqlQueryCountsByType(select = 1, upsert = 3)
        expectDomainChangedEvents().isNotEmpty()

        parent.children.first().update {
            value += "X"
        }

        parent.save()

        expectSqlQueryCountsByType(select = 1, upsert = 1)
        expectDomainChangedEvents().isNotEmpty()
    }
}
