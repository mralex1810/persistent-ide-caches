package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.lmdb.CountingCacheImpl
import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.utils.ByteArrIntIntConsumer
import com.github.sudu.persistentidecaches.utils.ReadUtils
import com.github.sudu.persistentidecaches.utils.TriConsumer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.function.Consumer

@JvmRecord
data class TrigramDataFileCluster(val deltas: TrigramFileCounter, val pathCache: CountingCacheImpl<Path>) {
    fun toBytes(): ByteArray {
        var size = HEADER_BYTE_SIZE
        val groupedDelta: MutableMap<Path, MutableList<TrigramInteger>> = HashMap()
        deltas.forEach((TriConsumer { _, file, _: Int -> groupedDelta.computeIfAbsent(file) { ArrayList() } }))
        deltas.forEach((TriConsumer { trigram, file, integer -> groupedDelta[file]!!.add(TrigramInteger(trigram, integer)) }))
        for ((_, value) in groupedDelta) {
            size += TrigramCounterNode.byteSize(value)
        }
        val bytes = ByteBuffer.wrap(ByteArray(size))
            .putInt(groupedDelta.size)
        for ((key, value) in groupedDelta) {
            TrigramCounterNode.putInBuffer(bytes, pathCache.getNumber(key), value)
        }
        return bytes.array()
    }

    data class TrigramFileDelta(val trigram: Trigram, val file: File, val delta: Int) {
        fun byteSize(): Int {
            return trigram.trigram.size + Integer.BYTES + Integer.BYTES
        }

        private fun putInBuffer(byteBuffer: ByteBuffer) {
            byteBuffer.put(trigram.trigram)
        }
    }

    private data class TrigramCounterNode(val file: File, val trigramCounter: List<TrigramInteger>) {
        companion object {
            fun byteSize(trigramCounter: List<TrigramInteger>): Int {
                return Integer.BYTES + Integer.BYTES +
                        trigramCounter.stream()
                            .mapToInt { obj: TrigramInteger -> obj.sizeOf() }
                            .sum()
            }

            fun putInBuffer(
                byteBuffer: ByteBuffer, fileInt: Int,
                trigramCounter: List<TrigramInteger>
            ) {
                byteBuffer.putInt(fileInt)
                byteBuffer.putInt(trigramCounter.size)
                trigramCounter.forEach((Consumer { it: TrigramInteger -> it.putInBuffer(byteBuffer) }))
            }

            @Throws(IOException::class)
            fun read(`is`: InputStream, consumer: ByteArrIntIntConsumer) {
                val fileInt = ReadUtils.readInt(`is`)
                val size = ReadUtils.readInt(`is`)
                for (i in 0 until size) {
                    TrigramInteger.read(`is`, consumer, fileInt)
                }
            }
        }
    }

    private data class TrigramInteger(val trigram: Trigram, val value: Int) {
        fun sizeOf(): Int {
            return trigram.trigram.size + Integer.BYTES
        }

        fun putInBuffer(byteBuffer: ByteBuffer) {
            byteBuffer.put(trigram.trigram)
            byteBuffer.putInt(value)
        }

        companion object {
            @Throws(IOException::class)
            fun read(`is`: InputStream, consumer: ByteArrIntIntConsumer, fileInt: Int) {
                val trigram = ReadUtils.readBytes(`is`, 3)
                val delta = ReadUtils.readInt(`is`)
                consumer.accept(trigram, fileInt, delta)
            }
        }
    }

    companion object {
        private const val HEADER_BYTE_SIZE = Integer.BYTES

        fun readTrigramDataFileCluster(
            inputStream: InputStream,
            consumer: ByteArrIntIntConsumer
        ) {
            try {
                val size = ReadUtils.readInt(inputStream)
                val deltas = TrigramFileCounter()
                for (i in 0 until size) {
                    TrigramCounterNode.read(inputStream, consumer)
                    //                deltas.add(it.file(), it.trigramCounter());
                }
            } catch (e: IOException) {
                throw RuntimeException("Error on reading node", e)
            }
        }
    }
}
