package com.github.sudu.persistentidecaches.lmdb.maps

import org.lmdbjava.DbiFlags
import org.lmdbjava.Env
import java.nio.ByteBuffer

class LmdbInt2Long(env: Env<ByteBuffer>, dbName: String?) :
    LmdbAbstractInt2Smth(env, env.openDbi(dbName, DbiFlags.MDB_CREATE, DbiFlags.MDB_INTEGERKEY)) {
    private val valueBuffer: ByteBuffer = ByteBuffer.allocateDirect(java.lang.Long.BYTES)

    fun put(key: Int, value: Long) {
        putImpl(
            getKey(key),
            getValue(value)
        )
    }

    protected fun getValue(value: Long): ByteBuffer {
        return valueBuffer.putLong(value).flip()
    }

    /**
     * @return value for key or -1
     */
    fun get(key: Int): Long {
        val res = getImpl(getKey(key))
        return res?.getLong() ?: -1
    }
}
