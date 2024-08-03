package com.github.sudu.persistentidecaches.lmdb

import com.github.sudu.persistentidecaches.lmdb.maps.*
import org.apache.commons.lang3.tuple.Pair
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.lmdbjava.Env
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.nio.file.Path

class LmdbMapsTest {
    @TempDir
    lateinit var directory: Path
    lateinit var env: Env<ByteBuffer>

    @BeforeEach
    fun prepare() {
        this.env = Env.create()
            .setMapSize(10485760)
            .setMaxDbs(1)
            .setMaxReaders(1)
            .open(directory.toFile())
    }

    @Test
    fun testInt2Int() {
        val map = LmdbInt2Int(env!!, "a")
        val list = java.util.List.of(
            Pair.of(1, 2),
            Pair.of(3, 4),
            Pair.of(5, 6),
            Pair.of(10000, 2)
        )
        testMap(map, list, listOf(100, 4, 0), -1, Integer.TYPE, Integer.TYPE)
    }

    @Test
    fun testInt2Long() {
        val map = LmdbInt2Long(env!!, "a")
        val list = java.util.List.of(
            Pair.of(1, 2L),
            Pair.of(3, 4L),
            Pair.of(5, 6L),
            Pair.of(10000, 2L)
        )
        testMap(map, list, listOf(100, 4, 0), -1L, Integer.TYPE, java.lang.Long.TYPE)
    }

    @Test
    fun testLong2Int() {
        val map = LmdbLong2Int(env!!, "a")
        val list = java.util.List.of(
            Pair.of(1L, 2),
            Pair.of(3L, 4),
            Pair.of(5L, 6),
            Pair.of(10000L, 2),
            Pair.of(100000000000000L, 2)
        )
        testMap(map, list, listOf(100L, 4L, 0L, 100000000000001L), -1, java.lang.Long.TYPE, Integer.TYPE)
    }

    @Test
    fun testInt2File() {
        val map = LmdbInt2Path(env!!, "a")
        val list = java.util.List.of(
            Pair.of(1, Path.of("1")),
            Pair.of(3, Path.of("3")),
            Pair.of(5, Path.of("5")),
            Pair.of(10000, Path.of("1"))
        )
        testMap(map, list, listOf(100, 4, 0), null, Integer.TYPE, Path::class.java)
    }

    @Test
    @Throws(InvocationTargetException::class, NoSuchMethodException::class, IllegalAccessException::class)
    fun testString2Int() {
        val map = LmdbString2Int(env!!, "a")
        val list = java.util.List.of(
            Pair.of("1", 1),
            Pair.of("3", 3),
            Pair.of("5", 5),
            Pair.of("1000", 1)
        )
        testMap(map, list, listOf("100", "4", "0"), -1, String::class.java, Integer.TYPE)
    }

    @Test
    fun testInt2String() {
        val map = LmdbInt2String(env!!, "a")
        val list = java.util.List.of(
            Pair.of(1, "1"),
            Pair.of(3, "3"),
            Pair.of(5, "5"),
            Pair.of(10000, "1")
        )
        testMap(map, list, listOf(100, 4, 0), null, Integer.TYPE, String::class.java)
    }

    @Test
    fun testSha12String() {
        val map = LmdbInt2String(env!!, "a")
        val list = java.util.List.of(
            Pair.of(1, "1"),
            Pair.of(3, "3"),
            Pair.of(5, "5"),
            Pair.of(10000, "1")
        )
        testMap(map, list, listOf(100, 4, 0), null, Integer.TYPE, String::class.java)
    }


    fun <T, V> testMap(
        map: LmdbMap,
        list: List<Pair<T, V>>,
        missingKeys: List<T>,
        defaultValue: V,
        keyToken: Class<*>?,
        valueToken: Class<*>?
    ) {
        val putMethod = map.javaClass.getMethod(PUT, keyToken, valueToken)
        val getMethod = map.javaClass.getMethod(GET, keyToken)
        for (pair in list) {
            for (key in missingKeys) {
                Assertions.assertNotEquals(pair.left, pair.right)
            }
        }
        for (pair in list) {
            putMethod.invoke(map, pair.left, pair.right)
        }
        for (pair in list) {
            Assertions.assertEquals(getMethod.invoke(map, pair.left), pair.right)
        }
        for (key in missingKeys) {
            Assertions.assertEquals(getMethod.invoke(map, key), defaultValue)
        }
    }

    companion object {
        const val GET: String = "get"
        const val PUT: String = "put"
    }
}
