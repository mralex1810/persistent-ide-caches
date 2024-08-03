package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.records.Trigram
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer

class TrigramCounterTest {
    private var counter: TrigramCounter? = null

    @BeforeEach
    fun resetCounter() {
        counter = TrigramCounter()
    }

    @Test
    fun testAdd() {
        Assertions.assertEquals(0, counter!!.get(trigrams[0]))
        counter!!.add(trigrams[0], 2)
        Assertions.assertEquals(2, counter!!.get(trigrams[0]))
        counter!!.add(trigrams[1], 10)
        Assertions.assertEquals(10, counter!!.get(trigrams[1]))
        counter!!.add(trigrams[1], 10)
        Assertions.assertEquals(2, counter!!.get(trigrams[0]))
        Assertions.assertEquals(20, counter!!.get(trigrams[1]))
    }

    @Test
    fun testDecrease() {
        Assertions.assertEquals(0, counter!!.get(trigrams[0]))
        counter!!.decrease(trigrams[0], 2)
        Assertions.assertEquals(-2, counter!!.get(trigrams[0]))
        counter!!.decrease(trigrams[1], 10)
        Assertions.assertEquals(-10, counter!!.get(trigrams[1]))
        counter!!.decrease(trigrams[1], 10)
        Assertions.assertEquals(-2, counter!!.get(trigrams[0]))
        Assertions.assertEquals(-20, counter!!.get(trigrams[1]))
    }

    @Test
    fun testForEach() {
        counter!!.add(trigrams[0], 2)
        counter!!.add(trigrams[1], 10)
        counter!!.add(trigrams[1], 15)
        counter!!.add(trigrams[2], -3)
        counter!!.add(trigrams[4], -5)
        counter!!.decrease(trigrams[4], 10)
        val map = Map.of(
            trigrams[0], 2,
            trigrams[1], 25,
            trigrams[2], -3,
            trigrams[4], -15
        )
        val count = AtomicInteger()
        counter!!.forEach((BiConsumer { trigram: Trigram, integer: Int? ->
            count.addAndGet(1)
            Assertions.assertEquals(map[trigram], integer)
        }))
        Assertions.assertEquals(count.get(), map.size)
    }

    companion object {
        private val trigrams: List<Trigram> = java.util.List.of(
            Trigram(byteArrayOf(1, 2, 3)),
            Trigram(byteArrayOf(1, 4, 5)),
            Trigram(byteArrayOf(2, 2, 2)),
            Trigram(byteArrayOf(3, 3, 3)),
            Trigram(byteArrayOf(4, 5, 1))
        )
    }
}
