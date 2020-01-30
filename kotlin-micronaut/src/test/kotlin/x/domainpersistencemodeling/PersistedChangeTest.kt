package x.domainpersistencemodeling

import ch.tutteli.atrium.api.cc.en_GB.toBe
import ch.tutteli.atrium.verbs.expect
import org.junit.jupiter.api.Test
import javax.inject.Inject

internal class PersistedChangeTest
    : LiveTestBase() {
    // TODO: Ick.  Can this be made useful in the factory?
    @Inject
    lateinit var changeRepository: ChangeRepository

    @Test
    fun `should have nothing to see here`() {
        expect(changeRepository).toBe(1001)
    }

    @Test
    fun `should not die`() {
        changes.groupAs("A test change") { }
    }
}
