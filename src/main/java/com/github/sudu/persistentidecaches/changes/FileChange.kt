package com.github.sudu.persistentidecaches.changes

import com.github.sudu.persistentidecaches.records.FilePointer

abstract class FileChange(timestamp: Long, @JvmField val place: FilePointer) : Change(timestamp) {
    override fun toString(): String {
        return "FileChange{" +
                "place=" + place +
                "} " + super.toString()
    }
}
