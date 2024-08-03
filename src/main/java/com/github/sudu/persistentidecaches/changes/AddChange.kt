package com.github.sudu.persistentidecaches.changes

import com.github.sudu.persistentidecaches.records.FilePointer

open class AddChange(timestamp: Long, place: FilePointer, val addedString: String) : FileChange(timestamp, place) {
    override fun toString(): String {
        return "AddChange{" +
                "addedString='" + addedString + '\'' +
                "} " + super.toString()
    }
}
