package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.records.TrigramFile
import com.github.sudu.persistentidecaches.utils.TriConsumer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger


class TrigramFileCounterTest {
    private var counter: TrigramFileCounter? = null

    @BeforeEach
    fun resetCounter() {
        counter = TrigramFileCounter()
    }

    @Test
    fun testAdd() {
        Assertions.assertEquals(0, counter!!.get(trigrams[0], files[2]))
        counter!!.add(trigrams[0], files[2], 2)
        Assertions.assertEquals(2, counter!!.get(trigrams[0], files[2]))
        counter!!.add(trigrams[1], files[1], 10)
        Assertions.assertEquals(10, counter!!.get(trigrams[1], files[1]))
        counter!!.add(trigrams[1], files[1], 10)
        Assertions.assertEquals(2, counter!!.get(trigrams[0], files[2]))
        Assertions.assertEquals(20, counter!!.get(trigrams[1], files[1]))
    }

    @Test
    fun testDecrease() {
        Assertions.assertEquals(0, counter!!.get(trigrams[0], files[2]))
        counter!!.decrease(trigrams[0], files[2], 2)
        Assertions.assertEquals(-2, counter!!.get(trigrams[0], files[2]))
        counter!!.decrease(trigrams[1], files[1], 10)
        Assertions.assertEquals(-10, counter!!.get(trigrams[1], files[1]))
        counter!!.decrease(trigrams[1], files[1], 10)
        Assertions.assertEquals(-2, counter!!.get(trigrams[0], files[2]))
        Assertions.assertEquals(-20, counter!!.get(trigrams[1], files[1]))
    }

    @Test
    fun testAdd2() {
        Assertions.assertEquals(0, counter!!.get(trigrams[0], files[2]))
        val trigramCounter = TrigramCounter()
        trigramCounter.add(trigrams[0], 2)
        counter!!.add(files[2], trigramCounter)
        Assertions.assertEquals(2, counter!!.get(trigrams[0], files[2]))
        Assertions.assertEquals(0, counter!!.get(trigrams[1], files[1]))
        trigramCounter.add(trigrams[1], 15)
        counter!!.add(files[1], trigramCounter)
        counter!!.add(trigrams[1], files[1], 10)
        Assertions.assertEquals(2, counter!!.get(trigrams[0], files[1]))
        Assertions.assertEquals(25, counter!!.get(trigrams[1], files[1]))
        counter!!.add(files[1], trigramCounter)
        Assertions.assertEquals(40, counter!!.get(trigrams[1], files[1]))
    }

    @Test
    fun testDecrease2() {
        Assertions.assertEquals(0, counter!!.get(trigrams[0], files[2]))
        val trigramCounter = TrigramCounter()
        trigramCounter.add(trigrams[0], 2)
        counter!!.decrease(files[2], trigramCounter)
        Assertions.assertEquals(-2, counter!!.get(trigrams[0], files[2]))
        Assertions.assertEquals(0, counter!!.get(trigrams[1], files[1]))
        trigramCounter.add(trigrams[1], 15)
        counter!!.decrease(files[1], trigramCounter)
        counter!!.add(trigrams[1], files[1], 10)
        Assertions.assertEquals(-2, counter!!.get(trigrams[0], files[1]))
        Assertions.assertEquals(-5, counter!!.get(trigrams[1], files[1]))
        counter!!.decrease(files[1], trigramCounter)
        Assertions.assertEquals(-4, counter!!.get(trigrams[0], files[1]))
        Assertions.assertEquals(-20, counter!!.get(trigrams[1], files[1]))
    }

    @Test
    fun testForEach() {
        counter!!.add(trigrams[0], files[2], 2)
        counter!!.add(trigrams[1], files[1], 10)
        counter!!.add(trigrams[1], files[1], 15)
        counter!!.add(trigrams[2], files[3], -3)
        counter!!.add(trigrams[4], files[4], -5)
        counter!!.decrease(trigrams[4], files[4], 10)
        val map = Map.of(
            TrigramFile(trigrams[0], files[2]), 2,
            TrigramFile(trigrams[1], files[1]), 25,
            TrigramFile(trigrams[2], files[3]), -3,
            TrigramFile(trigrams[4], files[4]), -15
        )
        val count = AtomicInteger()
        counter!!.forEach((TriConsumer { trigram: Trigram?, file: Path?, integer: Int? ->
            count.addAndGet(1)
            Assertions.assertEquals(map[TrigramFile(trigram!!, file!!)], integer)
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
        private val files: List<Path> = java.util.List.of(
            Path.of("1"),
            Path.of("2"),
            Path.of("3"),
            Path.of("4"),
            Path.of("5")
        )
    }
}
