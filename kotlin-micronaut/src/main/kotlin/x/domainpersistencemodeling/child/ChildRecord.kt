package x.domainpersistencemodeling.child

import io.micronaut.core.annotation.Introspected
import x.domainpersistencemodeling.KnownState.ENABLED
import x.domainpersistencemodeling.UpsertableRecord
import x.domainpersistencemodeling.atZero
import java.time.OffsetDateTime
import java.util.TreeSet
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Transient

@Entity
@Introspected
@Table(name = "child")
data class ChildRecord(
    @Id var id: Long?,
    override var naturalId: String,
    var otherNaturalId: String?,
    override var parentNaturalId: String?,
    override var state: String,
    override var at: OffsetDateTime, // UTC
    override var value: String?,
    override var defaultSideValues: MutableSet<String>,
    override var sideValues: MutableSet<String>,
    override var version: Int
) : MutableChildSimpleDetails,
    UpsertableRecord<ChildRecord> {
    internal constructor(naturalId: String)
            : this(
        id = null,
        naturalId = naturalId,
        otherNaturalId = null,
        parentNaturalId = null,
        state = ENABLED.name,
        at = atZero,
        value = null,
        defaultSideValues = mutableSetOf(),
        sideValues = mutableSetOf(),
        version = 0
    )

    @get:Transient
    override val assigned: Boolean
        get() = super.assigned

    override fun upsertedWith(upserted: ChildRecord): ChildRecord {
        id = upserted.id
        naturalId = upserted.naturalId
        otherNaturalId = upserted.otherNaturalId
        parentNaturalId = upserted.parentNaturalId
        state = upserted.state
        at = upserted.at
        value = upserted.value
        defaultSideValues = TreeSet(upserted.defaultSideValues)
        sideValues = TreeSet(upserted.sideValues)
        version = upserted.version
        return this
    }
}
