package com.github.sudu.persistentidecaches.lmdb.maps

import org.lmdbjava.Dbi
import org.lmdbjava.Env
import java.nio.ByteBuffer

abstract class LmdbAbstractInt2Smth : LmdbAbstractMap {
    private val keyBuffer: ByteBuffer

    constructor(env: Env<ByteBuffer>, dbName: String) : super(env, dbName) {
        keyBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
    }

    protected constructor(env: Env<ByteBuffer>, db: Dbi<ByteBuffer>) : super(env, db) {
        keyBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
    }

    protected fun getKey(key: Int): ByteBuffer {
        return keyBuffer.putInt(key).flip()
    }
}
