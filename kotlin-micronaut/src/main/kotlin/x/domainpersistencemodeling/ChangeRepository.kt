package x.domainpersistencemodeling

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES
import io.micronaut.data.repository.GenericRepository

@JdbcRepository(dialect = POSTGRES)
interface ChangeRepository : GenericRepository<Long, Long> {
    @Query(
        value = """
SELECT * table_revision.ver_create_revision(:name)        
    """, readOnly = false
    )
    fun updateByChangeBegin(name: String): Int

    @Query(
        value = """
SELECT ver_complete_revision()
""", readOnly = false
    )
    fun updateByChangeCommit()

    @Query(
        value = """
SELECT * table_revision.ver_delete_revision(:id)        
    """, readOnly = false
    )
    fun updateByChangeRollback(id: Int)
}
