package com.github.sudu.persistentidecaches.utils

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets

object ReadUtils {
    @Throws(IOException::class)
    fun readInt(inputStream: InputStream): Int {
        return ByteBuffer.wrap(inputStream.readNBytes(Integer.BYTES)).getInt()
    }

    @Throws(IOException::class)
    fun readShort(inputStream: InputStream): Short {
        return ByteBuffer.wrap(inputStream.readNBytes(java.lang.Short.BYTES)).getShort()
    }

    @Throws(IOException::class)
    fun readUTF(inputStream: InputStream): String {
        val bytes = inputStream.readNBytes(readShort(inputStream).toInt())
        return String(bytes)
    }

    @Throws(IOException::class)
    fun readNSymbols(inputStream: InputStream, n: Int): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until n) {
            try {
                stringBuilder.append(readOneUTF8(inputStream))
            } catch (e: RuntimeException) {
                throw RuntimeException("InputStream hasn't $n symbols", e)
            }
        }
        return stringBuilder.toString()
    }

    @Throws(IOException::class)
    fun readOneUTF8(`is`: InputStream): CharBuffer {
        val first = `is`.read()
        val bytes = ByteArray(4)
        bytes[0] = first.toByte()
        var len = 1
        if (first == -1) {
            throw RuntimeException("Expected UTF8 char, actual: EOF")
        }
        if ((first and (1 shl 7)) != 0) {
            len = if ((first and (1 shl 5)) != 0) {
                if ((first and (1 shl 4)) != 0) {
                    4
                } else {
                    3
                }
            } else {
                2
            }
        }
        if (len != 1) {
            val read = `is`.read(bytes, 1, len - 1)
            if (read != len - 1) {
                throw RuntimeException("Expected UTF8 char, actual: EOF")
            }
        }
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes, 0, len))
    }

    @Throws(IOException::class)
    fun readBytes(`is`: InputStream, size: Int): ByteArray {
        val res = ByteArray(size)
        if (`is`.read(res) != size) {
            throw RuntimeException("InputStream hasn't 3 bytes")
        }
        return res
        //        return new Trigram(readNSymbols(is, 3));
    }
}
