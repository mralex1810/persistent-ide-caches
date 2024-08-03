package com.github.sudu.persistentidecaches

import java.util.function.BiConsumer

interface CountingCache<V> {
    fun getNumber(obj: V): Int

    fun getObject(objNum: Int): V?

    fun tryRegisterNewObj(obj: V)

    fun restoreObjectsFromDB()

    fun init()

    fun forEach(consumer: BiConsumer<V, Number>)
}
