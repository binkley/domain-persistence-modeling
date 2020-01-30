package x.domainpersistencemodeling

import io.micronaut.data.annotation.Repository
import io.micronaut.data.exceptions.DataAccessException
import javax.inject.Singleton
import javax.transaction.Transactional

/** @todo Generic repository needs `@Repository` but crud does not */
@Repository
@Singleton
@Transactional
internal class PersistedChangeFactory(
    private val repository: ChangeRepository
) : ChangeFactory {
    override fun <T> change(name: String, block: () -> T): T {
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
