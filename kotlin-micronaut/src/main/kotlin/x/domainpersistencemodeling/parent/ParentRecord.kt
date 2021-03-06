package x.domainpersistencemodeling.parent

import io.micronaut.core.annotation.Introspected
import x.domainpersistencemodeling.KnownState.ENABLED
import x.domainpersistencemodeling.UpsertableRecord
import java.time.OffsetDateTime
import java.util.TreeSet
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Introspected
@Table(name = "parent")
data class ParentRecord(
    @Id var id: Long?,
    override var naturalId: String,
    var otherNaturalId: String?,
    override var state: String,
    override var at: OffsetDateTime?,
    override var value: String?,
    override var sideValues: MutableSet<String>,
    override var version: Int
) : MutableParentSimpleDetails,
    UpsertableRecord<ParentRecord> {
    internal constructor(naturalId: String)
            : this(
        id = null,
        naturalId = naturalId,
        otherNaturalId = null,
        state = ENABLED.name,
        at = null,
        value = null,
        sideValues = mutableSetOf(),
        version = 0
    )

    override fun upsertedWith(upserted: ParentRecord): ParentRecord {
        id = upserted.id
        naturalId = upserted.naturalId
        otherNaturalId = upserted.otherNaturalId
        state = upserted.state
        value = upserted.value
        sideValues = TreeSet(upserted.sideValues)
        version = upserted.version
        return this
    }
}
