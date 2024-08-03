package com.github.sudu.persistentidecaches.symbols

private const val DUMMY = "DUMMY"

data class Symbol(val name: String, val pathNum: Int) : Comparable<Symbol> {
    override fun compareTo(o: Symbol): Int {
        val res = pathNum.compareTo(o.pathNum)
        return if (res == 0) name.compareTo(o.name) else res
    }

    companion object {
        val MIN: Symbol = Symbol(DUMMY, Int.MIN_VALUE)
        val MAX: Symbol = Symbol(DUMMY, Int.MAX_VALUE)
    }
}
