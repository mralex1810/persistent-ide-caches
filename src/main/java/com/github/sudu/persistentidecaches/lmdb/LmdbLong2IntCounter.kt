package com.github.sudu.persistentidecaches.lmdb

import com.github.sudu.persistentidecaches.lmdb.maps.LmdbLong2Int
import com.github.sudu.persistentidecaches.records.LongInt
import org.lmdbjava.Env
import org.lmdbjava.KeyRange
import org.lmdbjava.Txn
import java.nio.ByteBuffer
import java.util.function.BiConsumer
import java.util.function.Consumer

class LmdbLong2IntCounter(env: Env<ByteBuffer>, dbName: String) : LmdbLong2Int(env, dbName) {
    fun countGet(key: Long): Int {
        val res = getImpl(getKey(key))
        return res?.getInt() ?: 0
    }

    fun addAll(list: List<LongInt>) {
        env.txnWrite().use { txn ->
            addAll(txn, list)
            txn.commit()
        }
    }

    fun addAll(txn: Txn<ByteBuffer>, list: List<LongInt>) {
        list.forEach(Consumer { it: LongInt -> add(txn, it.l, it.i) })
    }

    fun decreaseAll(list: List<LongInt>) {
        env.txnWrite().use { txn ->
            list.forEach(Consumer { it: LongInt -> add(txn, it.l, -it.i) })
            txn.commit()
        }
    }

    fun add(txn: Txn<ByteBuffer>, key: Long, delta: Int) {
        val keyBytes = getKey(key)
        val found = db[txn, keyBytes]
        val `val` = if (found == null) 0 else txn.`val`().getInt()
        db.put(txn, keyBytes, getValue(`val` + delta))
    }

    fun decrease(txn: Txn<ByteBuffer>, key: Long, delta: Int) {
        add(txn, key, -delta)
    }

    fun forEachFromTo(consumer: BiConsumer<Long, Int>, from: Long, to: Long) {
        env.txnRead().use { txn ->
            db.iterate(
                txn,
                KeyRange.closedOpen(allocateLong(from), allocateLong(to))
            ).use { ci ->
                for (kv in ci) {
                    val key = kv.key().getLong()
                    consumer.accept(key, kv.`val`().getInt())
                }
            }
        }
    }

    fun forEach(consumer: BiConsumer<Long, Int>) {
        forEachFromTo(consumer, 0L, Long.MAX_VALUE)
    }
}
