package com.github.sudu.persistentidecaches.utils

import com.github.sudu.persistentidecaches.CountingCache
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer

class DummyCountingCache<V> : CountingCache<V> {
    val counter: AtomicInteger = AtomicInteger()
    val numToObj: MutableMap<Int, V> = HashMap()
    val objToNum: MutableMap<V, Int?> = HashMap()

    override fun getNumber(obj: V): Int {
        return objToNum[obj]!!
    }

    override fun getObject(objNum: Int): V? {
        return numToObj[objNum]
    }

    override fun tryRegisterNewObj(obj: V) {
        if (objToNum[obj] == null) {
            numToObj[counter.get()] = obj
            objToNum[obj] = counter.getAndIncrement()
        }
    }

    override fun restoreObjectsFromDB() {
    }

    override fun init() {
    }

    override fun forEach(consumer: BiConsumer<V, Number?>?) {
        objToNum.forEach(consumer!!)
    }
}
