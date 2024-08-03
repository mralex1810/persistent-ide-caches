package com.github.sudu.persistentidecaches.lmdb.maps

import com.github.sudu.persistentidecaches.symbols.Symbol
import org.lmdbjava.DbiFlags
import org.lmdbjava.Env
import org.lmdbjava.KeyRange
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.function.BiConsumer

class LmdbInt2Symbol(env: Env<ByteBuffer>, dbName: String) :
    LmdbAbstractInt2Smth(env, env.openDbi(dbName, DbiFlags.MDB_CREATE, DbiFlags.MDB_INTEGERKEY)), LmdbInt2Obj<Symbol> {
    override fun put(key: Int, value: Symbol) {
        val nameBytes = value.name.toByteArray()
        putImpl(
            getKey(key),
            ByteBuffer.allocateDirect(nameBytes.size + Integer.BYTES)
                .putInt(value.pathNum)
                .put(nameBytes)
                .flip()
        )
    }

    override fun get(key: Int): Symbol? {
        val res = getImpl(getKey(key)) ?: return null
        val pathNum = res.getInt()
        return Symbol(StandardCharsets.UTF_8.decode(res.slice()).toString(), pathNum)
    }

    private fun decodeSymbol(byteBuffer: ByteBuffer): Symbol {
        val pathNum = byteBuffer.getInt()
        return Symbol(StandardCharsets.UTF_8.decode(byteBuffer.slice()).toString(), pathNum)
    }

    override fun forEach(consumer: BiConsumer<Int, Symbol>) {
        env.txnRead().use { txn ->
            db.iterate(txn, KeyRange.all()).use { ci ->
                for (kv in ci) {
                    consumer.accept(kv.key().getInt(), decodeSymbol(kv.`val`()))
                }
            }
        }
    }
}
