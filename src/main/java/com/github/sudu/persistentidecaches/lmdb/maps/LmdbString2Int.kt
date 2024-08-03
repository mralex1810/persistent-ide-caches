package com.github.sudu.persistentidecaches.lmdb.maps

import org.lmdbjava.Env
import java.nio.ByteBuffer

class LmdbString2Int(env: Env<ByteBuffer>, dbName: String) : LmdbAbstractMap(env, dbName) {
    private val valueBuffer: ByteBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    protected fun getValue(key: Int): ByteBuffer {
        return valueBuffer.putInt(key).flip()
    }

    fun put(key: String, value: Int) {
        putImpl(
            allocateString(key),
            getValue(value)
        )
    }

    /**
     * @return value for key or -1
     */
    operator fun get(key: String?): Int {
        val res = getImpl(allocateString(key!!))
        return res?.getInt() ?: -1
    }
}
