package com.github.sudu.persistentidecaches.lmdb.maps

import org.lmdbjava.Env
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.function.BiConsumer

class LmdbInt2Path(env: Env<ByteBuffer>, dbName: String) : LmdbMap, LmdbInt2Obj<Path> {
    private val db = LmdbInt2String(env, dbName)

    override fun put(key: Int, value: Path) {
        db.put(key, value.toString())
    }

    override fun get(key: Int): Path? {
        val value = db.get(key) ?: return null
        return Path.of(value)
    }

    override fun forEach(consumer: BiConsumer<Int, Path>) {
        db.forEach { integer: Int, s: String -> consumer.accept(integer, Path.of(s)) }
    }
}
