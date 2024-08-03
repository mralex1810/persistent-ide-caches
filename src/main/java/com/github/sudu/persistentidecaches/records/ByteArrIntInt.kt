package com.github.sudu.persistentidecaches.records

@JvmRecord
data class ByteArrIntInt(@JvmField val trigram: ByteArray, @JvmField val num: Int, @JvmField val delta: Int)