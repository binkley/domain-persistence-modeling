package x.domainpersistencemodeling

interface ChangeFactory {
    fun <T> change(name: String, block: () -> T): T
}
