package x.domainpersistencemodeling

interface ChangeFactory {
    fun <T> groupAs(name: String, block: () -> T): T
}
