package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.lmdb.CountingCacheImpl
import com.github.sudu.persistentidecaches.lmdb.TrigramObjCounterLmdb
import com.github.sudu.persistentidecaches.records.ByteArrIntInt
import com.github.sudu.persistentidecaches.records.LongInt
import com.github.sudu.persistentidecaches.records.Trigram
import org.lmdbjava.Env
import org.lmdbjava.Txn
import java.nio.ByteBuffer
import java.nio.file.Path

class TrigramFileCounterLmdb(env: Env<ByteBuffer>, pathCache: CountingCacheImpl<Path>) :
    TrigramObjCounterLmdb<Path>(
        pathCache, env, "trigram_file_counter"
    ) {
    fun add(counter: TrigramFileCounter) {
        db.addAll(counterToList(counter))
    }

    fun add(txn: Txn<ByteBuffer>, counter: List<ByteArrIntInt>) {
        db.addAll(txn, counter.stream()
            .map { it: ByteArrIntInt -> LongInt(getKey(it.trigram, it.num), it.delta) }
            .toList())
    }

    fun decrease(counter: TrigramFileCounter) {
        db.decreaseAll(counterToList(counter))
    }

    private fun counterToList(counter: TrigramFileCounter): List<LongInt> {
        val list: MutableList<LongInt> = ArrayList()
        counter.forEach { trigram: Trigram, file: Path, integer: Int ->
            list.add(
                LongInt(
                    getKey(
                        trigram, file
                    ), integer
                )
            )
        }
        return list
    }
}
