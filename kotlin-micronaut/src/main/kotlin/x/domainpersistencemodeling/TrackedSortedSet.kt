package x.domainpersistencemodeling

import x.domainpersistencemodeling.TrackingArity.MANY
import x.domainpersistencemodeling.TrackingArity.OPTIONAL_ONE
import java.util.TreeSet
import kotlin.reflect.KProperty

internal enum class TrackingArity { OPTIONAL_ONE, MANY }

internal class TrackedManyToOne<T : Comparable<T>>(
    initial: Set<T>,
    addOne: (T, MutableSet<T>) -> Unit,
    removeOne: (T, MutableSet<T>) -> Unit
) : TrackedSortedSet<T>(MANY, initial, addOne, removeOne)

internal class TrackedOptionalOne<T : Comparable<T>>(
    initial: T?,
    addOne: (T, MutableSet<T>) -> Unit,
    removeOne: (T, MutableSet<T>) -> Unit
) : TrackedSortedSet<T>(
    OPTIONAL_ONE,
    if (null == initial) setOf() else setOf(initial), addOne, removeOne
) {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T? {
        return firstOrNull()
    }

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T?
    ) {
        clear()
        value?.run { add(value) }
    }
}

internal sealed class TrackedSortedSet<T : Comparable<T>>(
    private var arity: TrackingArity,
    private var initial: Set<T>,
    private val addOne: (T, MutableSet<T>) -> Unit,
    private val removeOne: (T, MutableSet<T>) -> Unit
) : AbstractMutableSet<T>() {
    private val current: MutableSet<T> = TreeSet(initial)

    init {
        checkArity()
    }

    override val size: Int
        get() = current.size

    override fun add(element: T): Boolean {
        if (!current.add(element))
            throw DomainException("Already present: $element")
        addOne(element, current)
        checkArity()
        return true
    }

    override fun remove(element: T): Boolean {
        if (!super.remove(element))
            throw DomainException("Not present: $element")
        removeOne(element, current)
        return true
    }

    override fun iterator() = object : MutableIterator<T> {
        private val it = current.iterator()
        private var last: T? = null

        override fun hasNext() = it.hasNext()

        override fun next(): T {
            val next = it.next()
            last = next
            return next
        }

        override fun remove() {
            it.remove()
            removeOne(last!!, current)
        }
    }

    fun reset() {
        initial = TreeSet(current)
    }

    fun added(mutator: (T) -> Boolean): Boolean {
        val added = TreeSet(current)
        added.removeAll(initial)
        return added.mutated(mutator)
    }

    fun removed(mutator: (T) -> Boolean): Boolean {
        val removed = TreeSet(initial)
        removed.removeAll(current)
        return removed.mutated(mutator)
    }

    fun changed(mutator: (T) -> Boolean): Boolean {
        val changed = TreeSet(initial)
        changed.retainAll(current)
        return changed.mutated(mutator)
    }

    private fun checkArity() {
        if (OPTIONAL_ONE == arity && 1 < current.size)
            throw Bug("Wrong initial for arity: $arity: $current")
    }
}
