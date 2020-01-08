package x.domainpersistencemodeling

import ch.tutteli.atrium.api.cc.en_GB.isEmpty
import ch.tutteli.atrium.api.cc.en_GB.toBe
import ch.tutteli.atrium.api.cc.en_GB.toThrow
import ch.tutteli.atrium.verbs.expect
import org.junit.jupiter.api.Test

private val doNothing: (String, MutableSet<String>) -> Unit = { _, _ -> }

internal class TrackedSortedSetTest {
    @Test
    fun `should do nothing when empty`() {
        val tracked = TrackedManyToOne(setOf(), doNothing, doNothing)

        expect(tracked.mutated { true }).toBe(false)
    }

    @Test
    fun `should do a little when changed`() {
        val tracked = TrackedManyToOne(setOf("ABC"), doNothing, doNothing)

        expect(tracked.mutated { true }).toBe(true)
    }

    @Test
    fun `should notice first element changed`() {
        val tracked =
            TrackedManyToOne(setOf("ABC", "DEF"), doNothing, doNothing)

        expect(tracked.mutated { it == "ABC" }).toBe(true)
    }

    @Test
    fun `should notice second element changed`() {
        val tracked =
            TrackedManyToOne(setOf("ABC", "DEF"), doNothing, doNothing)

        expect(tracked.mutated { it == "DEF" }).toBe(true)
    }

    @Test
    fun `should notify when deleting through iterator`() {
        var removed = false
        val tracked = TrackedManyToOne(setOf("ABC"), doNothing) { _, _ ->
            removed = true
        }

        tracked.clear()

        expect(tracked).isEmpty()
        expect(removed).toBe(true)
    }

    @Test
    fun `should complain on misuse for optional-one`() {
        val tracked = TrackedOptionalOne("ABC", doNothing, doNothing)

        expect {
            tracked.add("BOB")
        }.toThrow<Bug> { }
    }

    @Test
    fun `should complain on misuse for many-to-one`() {
        val tracked = TrackedManyToOne(setOf("BOB"), doNothing, doNothing)

        expect {
            tracked.add("BOB")
        }.toThrow<DomainException> { }

        expect {
            tracked.remove("SALLY")
        }.toThrow<DomainException> { }
    }
}
