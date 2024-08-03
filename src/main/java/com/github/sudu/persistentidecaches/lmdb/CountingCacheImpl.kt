package com.github.sudu.persistentidecaches.lmdb

import com.github.sudu.persistentidecaches.CountingCache
import com.github.sudu.persistentidecaches.lmdb.maps.LmdbInt2Obj
import com.github.sudu.persistentidecaches.lmdb.maps.LmdbString2Int
import java.util.function.BiConsumer

class CountingCacheImpl<V>(
    private val objectsStringName: String,
    private val objInProject: LmdbInt2Obj<V>,
    private val variables: LmdbString2Int
) : CountingCache<V> {
    private val reverseObjInProject: MutableMap<V, Int> = HashMap()

    override fun getNumber(obj: V): Int {
        return reverseObjInProject[obj]!!
    }

    override fun getObject(objNum: Int): V? {
        return objInProject[objNum]
    }

    override fun tryRegisterNewObj(obj: V) {
        if (reverseObjInProject[obj] == null) {
            val fileNum = variables[objectsStringName]
            objInProject.put(fileNum, obj)
            reverseObjInProject[obj] = fileNum
            variables.put(objectsStringName, fileNum + 1)
        }
    }

    override fun restoreObjectsFromDB() {
        objInProject.forEach { integer: Int, file: V -> reverseObjInProject[file] = integer }
    }

    override fun init() {
        if (variables[objectsStringName] == -1) {
            variables.put(objectsStringName, 0)
        }
    }

    override fun forEach(consumer: BiConsumer<V, Number>) {
        reverseObjInProject.forEach(consumer)
    }
}
