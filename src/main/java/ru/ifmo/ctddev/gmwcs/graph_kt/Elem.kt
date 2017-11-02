package ru.ifmo.ctddev.gmwcs.graph_kt


open class Elem(val num: Int, protected var w: Double) : Comparable<Elem> {
    private val absorbed: MutableSet<Elem> = mutableSetOf()

    var weight: Double
        get() = w
        set(value) {
            w = value
        }

    fun absorb(what: Elem) {
        weight += what.weight
        absorbed.addAll(what.absorbed)
        what.clear()
        absorbed.add(what)
    }

    private fun clear() {
        weight -= absorbed.sumByDouble { it.weight }
        absorbed.clear()
    }

    override fun hashCode(): Int {
        return num
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Elem -> other.num == num
            else -> false
        }
    }

    override fun compareTo(other: Elem): Int {
        return if (weight != other.weight) {
            weight.compareTo(other.weight)
        } else num.compareTo(other.num)
    }
}
