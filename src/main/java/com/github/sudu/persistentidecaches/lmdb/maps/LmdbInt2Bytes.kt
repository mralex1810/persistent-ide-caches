package com.github.sudu.persistentidecaches.lmdb.maps

import org.lmdbjava.Dbi
import org.lmdbjava.Env
import java.nio.ByteBuffer

class LmdbInt2Bytes : LmdbAbstractInt2Smth, LmdbMap {
    constructor(env: Env<ByteBuffer>, dbName: String) : super(env, dbName)

    protected constructor(env: Env<ByteBuffer>, db: Dbi<ByteBuffer>) : super(env, db)

    fun put(key: Int, value: ByteArray) {
        putImpl(getKey(key), ByteBuffer.allocateDirect(value.size).put(value).flip())
    }

    fun get(key: Int): ByteArray? {
        val value = getImpl(getKey(key))
        if (value != null) {
            val data = ByteArray(value.remaining())
            value[data]
            return data
        }
        return null
    }
}
