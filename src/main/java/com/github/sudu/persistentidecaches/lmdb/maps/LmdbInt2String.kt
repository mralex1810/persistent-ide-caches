package com.github.sudu.persistentidecaches.lmdb.maps

import org.lmdbjava.DbiFlags
import org.lmdbjava.Env
import org.lmdbjava.KeyRange
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.function.BiConsumer

class LmdbInt2String(env: Env<ByteBuffer>, dbName: String) :
    LmdbAbstractInt2Smth(env, env.openDbi(dbName, DbiFlags.MDB_CREATE, DbiFlags.MDB_INTEGERKEY)), LmdbInt2Obj<String> {
    override fun put(key: Int, value: String) {
        putImpl(
            getKey(key),
            allocateString(value)
        )
    }

    /**
     * @return value for key or null
     */
    override fun get(key: Int): String? {
        val res = getImpl(getKey(key))
        return if (res == null) null else StandardCharsets.UTF_8.decode(res).toString()
    }

    override fun forEach(consumer: BiConsumer<Int, String>) {
        env.txnRead().use { txn ->
            db.iterate(txn, KeyRange.all()).use { ci ->
                for (kv in ci) {
                    consumer.accept(kv.key().getInt(), StandardCharsets.UTF_8.decode(kv.`val`()).toString())
                }
            }
        }
    }
}
