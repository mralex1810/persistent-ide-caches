package com.github.sudu.persistentidecaches.changes

import com.github.sudu.persistentidecaches.records.FilePointer

class DeleteChange(timestamp: Long, place: FilePointer, val deletedString: String) : FileChange(timestamp, place) {
    override fun toString(): String {
        return "DeleteChange{" +
                "deletedString='" + deletedString + '\'' +
                "} " + super.toString()
    }
}
