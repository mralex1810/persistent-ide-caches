package com.github.sudu.persistentidecaches.utils

import com.github.sudu.persistentidecaches.utils.Counter.Companion.emptyCounter
import java.util.function.BiConsumer

class DoubleCounter<Key1, Key2> {
    private val counter: MutableMap<Key1, Counter<Key2>>

    constructor(counter: MutableMap<Key1, Counter<Key2>>) {
        this.counter = counter
    }

    constructor() {
        counter = HashMap()
    }

    fun add(key1: Key1, key2: Key2, value: Int) {
        counter.computeIfAbsent(key1) { ignore: Key1 -> Counter() }
            .add(key2, value)
    }

    fun add(other: DoubleCounter<Key1, Key2>) {
        other.forEach { key1: Key1, key2: Key2, value: Int -> this.add(key1, key2, value) }
    }

    fun add(key1: Key1, other: Counter<Key2>?) {
        counter.computeIfAbsent(key1) { ignore: Key1 -> Counter() }.add(
            other!!
        )
    }

    fun decrease(key1: Key1, key2: Key2, value: Int) {
        add(key1, key2, -value)
    }

    fun decrease(other: DoubleCounter<Key1, Key2>) {
        other.forEach { key1: Key1, key2: Key2, value: Int -> this.decrease(key1, key2, value) }
    }

    fun decrease(key1: Key1, other: Counter<Key2>?) {
        counter.computeIfAbsent(key1) { ignore: Key1 -> Counter() }.decrease(
            other!!
        )
    }

    val asMap: Map<Key1, Counter<Key2>>
        get() = counter

    fun forEach(function: TriConsumer<Key1, Key2, Int>) {
        counter.forEach((BiConsumer { key1: Key1, key2IntegerMap: Counter<Key2> ->
            key2IntegerMap.forEach { key2: Key2, value: Int? ->
                function.accept(
                    key1,
                    key2,
                    value
                )
            }
        }))
    }

    fun get(key1: Key1, key2: Key2): Int {
        return counter.getOrDefault(key1, emptyCounter()).get(key2)
    }

    fun copy(): DoubleCounter<Key1, Key2> {
        val result = DoubleCounter<Key1, Key2>()
        counter.forEach { (key1: Key1, other: Counter<Key2>?) -> result.add(key1, other) }
        return result
    }
}
