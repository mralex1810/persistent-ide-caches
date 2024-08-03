package com.github.sudu.persistentidecaches.lmdb

import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.utils.TriConsumer
import org.lmdbjava.Env
import org.lmdbjava.Txn
import java.nio.ByteBuffer

abstract class TrigramObjCounterLmdb<U>(
    protected val cache: CountingCacheImpl<U>,
    env: Env<ByteBuffer>,
    dbName: String
) {
    @JvmField
    protected val db: LmdbLong2IntCounter = LmdbLong2IntCounter(env, dbName)

    fun get(trigram: Trigram, obj: U): Int {
        return db.countGet(getKey(trigram, obj))
    }

    protected fun getKey(trigram: Trigram, num: U): Long {
        return getKey(trigram.trigram, cache.getNumber(num))
    }

    protected fun getKey(trigram: ByteArray, num: Int): Long {
        return (Trigram.toLong(trigram) shl Integer.SIZE) + num
    }

    fun addIt(txn: Txn<ByteBuffer>, bytes: ByteArray, num: Int, delta: Int) {
        db.add(txn, getKey(bytes, num), delta)
    }

    fun decreaseIt(txn: Txn<ByteBuffer>, bytes: ByteArray, num: Int, delta: Int) {
        db.decrease(txn, getKey(bytes, num), delta)
    }

    fun getObjForTrigram(trigram: Trigram): List<U> {
        val list: MutableList<U> = ArrayList()
        db.forEachFromTo(
            { trigramFileLong: Long, value: Int ->
                if (value > 0) {
                    list.add(cache.getObject(trigramFileLong.toInt())!!)
                }
            },
            trigram.toLong() shl Integer.SIZE,
            (trigram.toLong() + 1) shl Integer.SIZE
        )
        return list
    }

    fun forEach(consumer: TriConsumer<Trigram, U, Int>) {
        db.forEach { l: Long, i: Int ->
            consumer.accept(
                Trigram(l shr Integer.SIZE),
                cache.getObject(l.toInt())!!,
                i
            )
        }
    }
}
