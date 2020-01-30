package x.domainpersistencemodeling

import io.micronaut.data.annotation.Repository
import io.micronaut.data.exceptions.DataAccessException
import javax.inject.Singleton
import javax.transaction.Transactional

/**
 *  @todo Generic repository needs `@Repository` but crud does not
 *  @todo Why does kotlin open plugin not fix this?
 */
@Repository
@Singleton
internal open class PersistedChangeFactory(
    private val repository: ChangeRepository
) : ChangeFactory {
    @Transactional
    override fun <T> groupAs(name: String, block: () -> T): T {
        val revision = repository.updateByChangeBegin(name)
        try {
            val result = block()
            repository.updateByChangeCommit()
            return result
        } catch (e: DataAccessException) {
            // TODO: Loses `e` if rollback fails
            repository.updateByChangeRollback(revision)
            throw e
        }
    }
}
