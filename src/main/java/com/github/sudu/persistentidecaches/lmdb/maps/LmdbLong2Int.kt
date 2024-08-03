package com.github.sudu.persistentidecaches.lmdb.maps

import org.lmdbjava.DbiFlags
import org.lmdbjava.Env
import java.nio.ByteBuffer

open class LmdbLong2Int(env: Env<ByteBuffer>, dbName: String) :
    LmdbAbstractMap(env, env.openDbi(dbName, DbiFlags.MDB_CREATE)) {
    protected val keyBuffer: ByteBuffer = ByteBuffer.allocateDirect(java.lang.Long.BYTES)
    protected val valueBuffer: ByteBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    protected fun getKey(key: Long): ByteBuffer {
        return keyBuffer.putLong(key).flip()
    }

    protected fun getValue(key: Int): ByteBuffer {
        return valueBuffer.putInt(key).flip()
    }

    fun put(key: Long, value: Int) {
        putImpl(
            getKey(key),
            getValue(value)
        )
    }

    /**
     * @return value for key or -1
     */
    operator fun get(key: Long): Int {
        val res = getImpl(getKey(key))
        return res?.getInt() ?: -1
    }
}
