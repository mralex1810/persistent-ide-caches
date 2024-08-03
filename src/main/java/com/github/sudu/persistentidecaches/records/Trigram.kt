package com.github.sudu.persistentidecaches.records

import kotlin.math.abs

data class Trigram(val trigram: ByteArray) : Comparable<Trigram> {
    constructor(i: Int) : this(i.toLong())

    constructor(l: Long) : this(byteArrayOf((l shr 16).toByte(), (l shr 8).toByte(), l.toByte()))

    override fun hashCode(): Int {
        return toInt(trigram) * 31 + 25
    }

    override fun compareTo(o: Trigram): Int {
        return Integer.compare(toInt(trigram), toInt(o.trigram))
    }

    override fun toString(): String {
        return "Trigram" + trigram.contentToString()
    }

    fun toPrettyString(): String {
        return (Char(trigram[0].toUShort()).toString() + Char(trigram[1].toUShort()) + Char(
            trigram[2].toUShort()
        )).replace("\n", "\\n")
    }

    fun toLowerCase(): Trigram {
        return Trigram(
            byteArrayOf(
                trigram[0].toInt().toChar().lowercaseChar().code.toByte(),
                trigram[1].toInt().toChar().lowercaseChar().code.toByte(),
                trigram[2].toInt().toChar().lowercaseChar().code.toByte()
            )
        )
    }

    companion object {
        fun toLong(bytes: ByteArray): Long {
            return toInt(bytes).toLong()
        }

        fun toInt(bytes: ByteArray): Int {
            return (((abs(bytes[0].toInt()) shl 8) +
                    abs(bytes[1].toInt())) shl 8) +
                    abs(bytes[2].toInt())
        }
    }
}
