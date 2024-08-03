package com.github.sudu.persistentidecaches.lmdb.maps

import org.lmdbjava.DbiFlags
import org.lmdbjava.Env
import java.nio.ByteBuffer

class LmdbInt2Int(env: Env<ByteBuffer>, dbName: String?) :
    LmdbAbstractInt2Smth(env, env.openDbi(dbName, DbiFlags.MDB_CREATE, DbiFlags.MDB_INTEGERKEY)) {
    private val valueBuffer: ByteBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    fun put(key: Int, value: Int) {
        putImpl(
            getKey(key),
            getValue(value)
        )
    }

    protected fun getValue(value: Int): ByteBuffer {
        return valueBuffer.putInt(value).flip()
    }

    /**
     * @return value for key or -1
     */
    fun get(key: Int): Int {
        val res = getImpl(getKey(key))
        return res?.getInt() ?: -1
    }
}
