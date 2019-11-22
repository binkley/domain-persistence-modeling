package x.domainpersistencemodeling.parent

import x.domainpersistencemodeling.DomainChangedEvent
import x.domainpersistencemodeling.DomainException
import x.domainpersistencemodeling.ScopedMutable
import x.domainpersistencemodeling.UpsertableDomain
import x.domainpersistencemodeling.child.Child

data class ParentResource(
        val naturalId: String,
        val value: String?,
        val version: Int)

interface ParentFactory {
    fun all(): Sequence<Parent>
    fun findExisting(naturalId: String): Parent?
    fun createNew(naturalId: String): Parent
    fun findExistingOrCreateNew(naturalId: String): Parent
}

interface ParentDetails : Comparable<ParentDetails> {
    val naturalId: String
    val value: String?
    val version: Int

    override fun compareTo(other: ParentDetails) =
            naturalId.compareTo(other.naturalId)
}

interface MutableParentDetails : ParentDetails {
    override var value: String?
}

interface MutableParent : MutableParentDetails {
    val children: MutableSet<Child>

    fun assign(child: Child) {
        if (!children.add(child))
            throw DomainException(
                    "Already assigned: $child")
    }

    fun unassign(child: Child) {
        if (!children.remove(child))
            throw DomainException(
                    "Not assigned: $child")
    }
}

interface Parent
    : ParentDetails,
        ScopedMutable<Parent, MutableParent>,
        UpsertableDomain<Parent> {
    val children: Set<Child>

    fun toResource(): ParentResource
}

data class ParentChangedEvent(
        val before: ParentResource?,
        val after: ParentResource?)
    : DomainChangedEvent<ParentResource>(before, after)