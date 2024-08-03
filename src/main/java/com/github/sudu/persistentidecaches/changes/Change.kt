package com.github.sudu.persistentidecaches.changes

abstract class Change(@JvmField val timestamp: Long) {
    override fun toString(): String {
        return "Change{" +
                "timestamp=" + timestamp +
                '}'
    }
}
