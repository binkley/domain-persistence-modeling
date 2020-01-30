package x.domainpersistencemodeling

import io.micronaut.data.exceptions.DataAccessException
import javax.transaction.Transactional

// TODO: Method to:
//  1) Begin txn using Micronaut -- is `@Transactional` good enough?
//  2) Start new named table revision
//  3) Execute block for rest of work
//  4) Complete table revision (or delete empty revision)
//  5) Commit/rollback txn using Micronaut

@Transactional
fun <T> change(changes: ChangeRepository, name: String, block: () -> T): T {
    val revisionId = changes.updateByChangeBegin(name)
    try {
        val result = block()
        changes.updateByChangeCommit()
        return result
    } catch (e: DataAccessException) {
        // TODO: Loses `e` if rollback fails
        changes.updateByChangeRollback(revisionId)
        throw e
    }
}
