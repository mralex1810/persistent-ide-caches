package com.github.sudu.persistentidecaches.records

@JvmRecord
data class LongInt(val l: Long, val i: Int) {
    override fun hashCode(): Int {
        return (31 * l + i).toInt()
    }
}
