package com.github.sudu.persistentidecaches.lmdb.maps

import org.eclipse.jgit.util.Hex
import org.lmdbjava.Env
import org.lmdbjava.KeyRange
import java.nio.ByteBuffer
import java.util.*
import java.util.function.BiConsumer

class LmdbSha12Int(env: Env<ByteBuffer>, dbName: String) : LmdbAbstractMap(env, dbName) {
    fun put(hash: String, value: Int) {
        val bytes = HexFormat.of().parseHex(hash)
        putImpl(
            ByteBuffer.allocateDirect(bytes.size).put(bytes).flip(),
            allocateInt(value)
        )
    }

    /**
     * @return value for key or -1
     */
    fun get(hash: String): Int {
        val bytes = HexFormat.of().parseHex(hash)
        val res = getImpl(ByteBuffer.allocateDirect(bytes.size).put(bytes).flip())
        return res?.getInt() ?: -1
    }

    fun forEach(consumer: BiConsumer<String, Int>) {
        env.txnRead().use { txn ->
            db.iterate(txn, KeyRange.all()).use { ci ->
                for (kv in ci) {
                    val bytes = ByteArray(kv.key().capacity())
                    kv.key()[bytes]
                    consumer.accept(Hex.toHexString(bytes), kv.`val`().getInt())
                }
            }
        }
    }
}
