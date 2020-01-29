package x.domainpersistencemodeling

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES

@JdbcRepository(dialect = POSTGRES)
interface ChangeRepository {
    @Query(
        value = """
SELECT * table_revision.ver_create_revision(:name)        
    """, readOnly = false
    )
    fun beginChange(name: String): Int

    @Query(
        """
SELECT ver_complete_revision()
"""
    )
    fun commitChange()

    @Query(
        value = """
SELECT * table_revision.ver_delete_revision(:id)        
    """, readOnly = false
    )
    fun rollbackChange(id: Int)
}
