package com.github.sudu.persistentidecaches.lmdb.maps

import java.util.function.BiConsumer

interface LmdbInt2Obj<V> {
    fun put(key: Int, value: V)

    /**
     * @return value for key or null
     */
    operator fun get(key: Int): V?

    fun forEach(consumer: BiConsumer<Int, V>)
}
