package x.domainpersistencemodeling.child

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import x.domainpersistencemodeling.workAroundArrayTypeForPostgresWrite
import java.time.OffsetDateTime
import java.util.Optional

@Repository
interface ChildRepository : CrudRepository<ChildRecord, Long> {
    @Query(
        """
        SELECT *
        FROM child
        WHERE natural_id = :naturalId
        """
    )
    fun findByNaturalId(@Param("naturalId") naturalId: String)
            : Optional<ChildRecord>

    @Query(
        """
        SELECT *
        FROM child
        WHERE parent_natural_id = :parentNaturalId
        """
    )
    fun findByParentNaturalId(
        @Param("parentNaturalId") parentNaturalId: String
    )
            : Iterable<ChildRecord>

    @Query(
        """
        SELECT *
        FROM upsert_child(:naturalId, :otherNaturalId, :parentNaturalId,
        :state, :at, :value, :defaultSideValues, :sideValues, :version)
        """
    )
    fun upsert(
        @Param("naturalId") naturalId: String,
        @Param("otherNaturalId") otherNaturalId: String?,
        @Param("parentNaturalId") parentNaturalId: String?,
        @Param("state") state: String,
        @Param("at") at: OffsetDateTime, // UTC
        @Param("value") value: String?,
        @Param("defaultSideValues") defaultSideValues: String,
        @Param("sideValues") sideValues: String,
        @Param("version") version: Int
    )
            : Optional<ChildRecord>
}

fun ChildRepository.upsert(entity: ChildRecord) =
    upsert(
        entity.naturalId,
        entity.otherNaturalId,
        entity.parentNaturalId,
        entity.state,
        entity.at,
        entity.value,
        entity.defaultSideValues.workAroundArrayTypeForPostgresWrite(),
        entity.sideValues.workAroundArrayTypeForPostgresWrite(),
        entity.version
    ).map {
        entity.upsertedWith(it)
    }
