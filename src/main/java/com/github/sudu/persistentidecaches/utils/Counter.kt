package com.github.sudu.persistentidecaches.utils

import java.util.function.BiConsumer


open class Counter<Key> {
    protected val counter: MutableMap<Key, Int>

    // LIMIT=300
    // fast-util.Avl 210 64 1 9 9 64
    // fast-util.OpenHash 98 37 0 5 5 38
    // fast-util.RB 202 68 1 10 9 67
    // fast-util.LinkedOpenHash 96 34 0 4 4 33
    // HashMap 72 28 0 3 4 27
    constructor() {
        counter = HashMap()
    }

    constructor(counter: Map<Key, Int>?) {
        this.counter = HashMap(counter)
    }

    fun decrease(key: Key) {
        add(key, -1)
    }

    fun add(key: Key, value: Int = 1) {
        counter.merge(key, value) { a: Int?, b: Int? ->
            Integer.sum(
                a!!, b!!
            )
        }
    }

    fun decrease(key: Key, value: Int) {
        add(key, -value)
    }

    fun add(other: Counter<Key>) {
        other.counter.forEach { (key: Key, value: Int) -> this.add(key, value) }
    }

    fun decrease(other: Counter<Key>) {
        other.counter.forEach { (key: Key, value: Int) -> this.decrease(key, value) }
    }

    fun plus(other: Counter<Key>?): Counter<Key> {
        val copy = copy()
        copy.add(other!!)
        return copy
    }

    fun minus(other: Counter<Key>?): Counter<Key> {
        val copy = copy()
        copy.decrease(other!!)
        return copy
    }

    fun get(key: Key): Int {
        return counter.getOrDefault(key, 0)
    }

    val asMap: Map<Key, Int>
        get() = counter

    fun copy(): Counter<Key> {
        return Counter(counter)
    }

    fun forEach(function: BiConsumer<Key, Int>?) {
        counter.forEach(function!!)
    }

    companion object {
        @JvmStatic
        fun <Key> emptyCounter(): Counter<Key> {
            return Counter()
        }
    }
}
