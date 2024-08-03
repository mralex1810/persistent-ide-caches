package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.records.Revision
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

@JvmRecord
data class TrigramNode(val revision: Revision, val parent: Revision, val pointer: Long) {
    fun toBytes(): ByteArray {
        val bytes = ByteBuffer.allocate(BYTE_SIZE)
            .putInt(revision.revision)
            .putInt(parent.revision)
            .putLong(pointer)
        return bytes.array()
    }

    companion object {
        private const val BYTE_SIZE = Integer.BYTES + Integer.BYTES + java.lang.Long.BYTES

        fun readTrigramNode(raf: RandomAccessFile): TrigramNode {
            try {
                val revision = Revision(raf.readInt())
                val parent = Revision(raf.readInt())
                val pointer = raf.readLong()
                return TrigramNode(revision, parent, pointer)
            } catch (e: IOException) {
                throw RuntimeException("Error on reading node", e)
            }
        }
    }
}
