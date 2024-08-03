package com.github.sudu.persistentidecaches.ccsearch

import com.github.sudu.persistentidecaches.lmdb.CountingCacheImpl
import com.github.sudu.persistentidecaches.lmdb.TrigramObjCounterLmdb
import com.github.sudu.persistentidecaches.records.ByteArrIntInt
import com.github.sudu.persistentidecaches.records.LongInt
import com.github.sudu.persistentidecaches.symbols.Symbol
import org.lmdbjava.Env
import org.lmdbjava.Txn
import java.nio.ByteBuffer

class TrigramSymbolCounterLmdb(
    env: Env<ByteBuffer?>?, symbolCache: CountingCacheImpl<Symbol?>?,
    dbNameSuffix: String
) : TrigramObjCounterLmdb<Symbol?>(symbolCache, env, "trigram_symbol_counter_$dbNameSuffix") {
    fun add(counter: Map<TrigramSymbol, Int>) {
        db.addAll(counterToList(counter))
    }

    fun add(txn: Txn<ByteBuffer?>?, counter: List<ByteArrIntInt>) {
        db.addAll(txn, counter.stream()
            .map { it: ByteArrIntInt -> LongInt(getKey(it.trigram, it.num), it.delta) }
            .toList())
    }

    fun decrease(counter: Map<TrigramSymbol, Int>) {
        db.decreaseAll(counterToList(counter))
    }

    private fun counterToList(counter: Map<TrigramSymbol, Int>): List<LongInt> {
        val list: MutableList<LongInt> = ArrayList()
        counter.forEach { (trigramSymbol: TrigramSymbol, integer: Int?) ->
            list.add(
                LongInt(
                    getKey(
                        trigramSymbol.trigram,
                        trigramSymbol.word
                    ), integer
                )
            )
        }
        return list
    }
}
