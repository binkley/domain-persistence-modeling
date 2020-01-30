package x.domainpersistencemodeling

import org.junit.jupiter.api.Test

internal class PersistedChangeTest
    : LiveTestBase() {
    @Test
    fun `should not die`() {
        changes.groupAs("A test change") { }
    }
}
