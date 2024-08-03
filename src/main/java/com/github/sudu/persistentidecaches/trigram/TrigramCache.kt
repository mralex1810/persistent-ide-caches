package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.Revisions
import com.github.sudu.persistentidecaches.lmdb.CountingCacheImpl
import com.github.sudu.persistentidecaches.lmdb.maps.LmdbInt2Bytes
import com.github.sudu.persistentidecaches.records.Revision
import com.github.sudu.persistentidecaches.utils.ByteArrIntIntConsumer
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.nio.file.Path

class TrigramCache(
    private val revisions: Revisions,
    private val pointers: LmdbInt2Bytes,
    private val pathCache: CountingCacheImpl<Path>
) {
    fun pushCluster(timestamp: Long, deltas: TrigramFileCounter?) {
        val revision = revisions.currentRevision
        pointers.put(revision.revision, TrigramDataFileCluster(deltas, pathCache).toBytes())
    }

    fun processDataCluster(revision: Revision, consumer: ByteArrIntIntConsumer?) {
        val data = pointers.get(revision.revision) ?: return
        val bufferedInputStream =
            BufferedInputStream(ByteArrayInputStream(pointers.get(revision.revision)))
        TrigramDataFileCluster.readTrigramDataFileCluster(bufferedInputStream, consumer)
    }
}
