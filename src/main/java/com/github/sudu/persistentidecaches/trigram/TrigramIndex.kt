package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.Index
import com.github.sudu.persistentidecaches.Revisions
import com.github.sudu.persistentidecaches.changes.*
import com.github.sudu.persistentidecaches.lmdb.CountingCacheImpl
import com.github.sudu.persistentidecaches.lmdb.maps.LmdbInt2Bytes
import com.github.sudu.persistentidecaches.records.Revision
import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.records.TrigramFile
import org.lmdbjava.Env
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream

class TrigramIndex(
    private val env: Env<ByteBuffer>,
    pathCache: CountingCacheImpl<Path>,
    private val revisions: Revisions
) : Index<TrigramFile, Int> {
    private val cache = TrigramCache(revisions, LmdbInt2Bytes(env, "trigram_deltas"), pathCache)
    @JvmField
    val counter: TrigramFileCounterLmdb = TrigramFileCounterLmdb(this.env, pathCache)
    @JvmField
    val trigramIndexUtils: TrigramIndexUtils = TrigramIndexUtils(this)

    override fun prepare(changes: List<Change>) {
        process(changes)
    }

    override fun processChanges(changes: List<Change>) {
        process(changes)
    }

    private fun pushActions(deltas: TrigramFileCounter, timestamp: Long) {
        cache.pushCluster(timestamp, deltas)
    }

    override fun getValue(trigramFile: TrigramFile, revision: Revision): Int {
        val currentRevision = revisions.currentRevision
        if (revision == currentRevision) {
            return counter.get(trigramFile.trigram, trigramFile.file)
        } else {
            checkout(revision)
            val ans = counter.get(trigramFile.trigram, trigramFile.file)
            checkout(currentRevision)
            return ans
        }
    }

    override fun checkout(targetRevision: Revision) {
        var targetRevision = targetRevision
        var currentRevision = revisions.currentRevision
        env.txnWrite().use { txn ->
//            final var deltasList = new ArrayList<ByteArrIntInt>();
            while (currentRevision != targetRevision) {
                if (currentRevision.revision > targetRevision.revision) {
                    cache.processDataCluster(
                        currentRevision
                    ) { bytes: ByteArray, file: Int, d: Int -> counter.decreaseIt(txn, bytes, file, d) }
                    currentRevision = revisions.getParent(currentRevision)
                } else {
                    cache.processDataCluster(
                        targetRevision
                    ) { bytes: ByteArray, file: Int, d: Int -> counter.addIt(txn, bytes, file, d) }
                    targetRevision = revisions.getParent(targetRevision)
                }
                //                counter.add(txn, deltasList);
//                deltasList.clear();
            }
            txn.commit()
        }
    }


    fun process(changes: List<Change>) {
        val delta = TrigramFileCounter()
        changes.forEach { it: Change -> countChange(it, delta) }

        delta.asMap.entries.stream()
            .filter { it: Map.Entry<TrigramFile?, Int> -> it.value == 0 }
            .map { obj: Map.Entry<TrigramFile?, Int?> -> obj.key }
            .toList()
            .forEach(Consumer { it: TrigramFile? -> delta.asMap.remove(it) })
        counter.add(delta)
        if (!changes.isEmpty()) {
            pushActions(delta, changes[0].timestamp)
        }
    }

    private fun validateFilename(filename: String): Boolean {
        return Stream.of(".java" /*, ".txt", ".kt", ".py"*/).anyMatch { suffix: String? ->
            filename.endsWith(
                suffix!!
            )
        }
    }

    private fun countChange(change: Change, delta: TrigramFileCounter) {
        Objects.requireNonNull(change)
        if (change is AddChange) {
            delta.add(change.place.file, getTrigramsCount(change.addedString))
        } else if (change is ModifyChange) {
            delta.decrease(change.oldFileName, getTrigramsCount(change.oldFileContent))
            delta.add(change.newFileName, getTrigramsCount(change.newFileContent))
        } else if (change is CopyChange) {
            delta.add(change.newFileName, getTrigramsCount(change.newFileContent))
        } else if (change is RenameChange) {
            delta.decrease(change.oldFileName, getTrigramsCount(change.oldFileContent))
            delta.add(change.newFileName, getTrigramsCount(change.newFileContent))
        } else if (change is DeleteChange) {
            delta.decrease(change.place.file, getTrigramsCount(change.deletedString))
        } else {
            throw AssertionError()
        }
    }
    companion object {
        private fun getTrigramsCount(str: String): TrigramCounter {
            val bytes = str.toByteArray()
            val result = TrigramCounter()
            for (i in 2 until bytes.size) {
                val trigram = Trigram(byteArrayOf(bytes[i - 2], bytes[i - 1], bytes[i]))
                result.add(trigram)
            }
            return result
        }
    }
}
