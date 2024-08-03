package com.github.sudu.persistentidecaches.lmdb.maps

import org.lmdbjava.Dbi
import org.lmdbjava.DbiFlags
import org.lmdbjava.Env
import java.io.Closeable
import java.nio.ByteBuffer

abstract class LmdbAbstractMap : Closeable, LmdbMap {
    protected val env: Env<ByteBuffer>
    protected val db: Dbi<ByteBuffer>

    constructor(env: Env<ByteBuffer>, dbName: String) {
        this.env = env
        db = env.openDbi(dbName, DbiFlags.MDB_CREATE)
    }

    protected constructor(env: Env<ByteBuffer>, db: Dbi<ByteBuffer>) {
        this.env = env
        this.db = db
    }

    protected fun putImpl(key: ByteBuffer, value: ByteBuffer) {
        db.put(key, value)
    }

    protected fun getImpl(key: ByteBuffer): ByteBuffer? {
        env.txnRead().use { txn ->
            val found = db[txn, key] ?: return null
            //                throw new RuntimeException(key + " key not found in DB " + new String(db.getName()));
            return txn.`val`()
        }
    }

    override fun close() {
        db.close()
    }

    companion object {
        @JvmStatic
        fun allocateInt(it: Int): ByteBuffer {
            return ByteBuffer.allocateDirect(Integer.BYTES).putInt(it).flip()
        }

        fun allocateLong(it: Long): ByteBuffer {
            return ByteBuffer.allocateDirect(java.lang.Long.BYTES).putLong(it).flip()
        }

        @JvmStatic
        fun allocateString(it: String): ByteBuffer {
            val bytes = it.toByteArray()
            return ByteBuffer.allocateDirect(bytes.size).put(bytes).flip()
        }
    }
}
