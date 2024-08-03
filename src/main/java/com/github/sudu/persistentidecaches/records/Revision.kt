package com.github.sudu.persistentidecaches.records

@JvmRecord
data class Revision(@JvmField val revision: Int) {
    override fun hashCode(): Int {
        return revision
    }

    companion object {
        @JvmField
        val NULL: Revision = Revision(-1)
    }
}
