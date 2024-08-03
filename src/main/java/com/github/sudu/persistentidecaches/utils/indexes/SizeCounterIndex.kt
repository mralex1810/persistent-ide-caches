package com.github.sudu.persistentidecaches.utils.indexes

import com.github.sudu.persistentidecaches.Index
import com.github.sudu.persistentidecaches.changes.AddChange
import com.github.sudu.persistentidecaches.changes.Change
import com.github.sudu.persistentidecaches.changes.CopyChange
import com.github.sudu.persistentidecaches.changes.ModifyChange
import com.github.sudu.persistentidecaches.records.Revision

class SizeCounterIndex : Index<String, String> {
    var summarySize: Long = 0
        private set

    override fun prepare(changes: List<Change>) {
        processChanges(changes)
    }

    override fun processChanges(changes: List<Change>) {
        changes.forEach { change: Change -> this.processChange(change) }
    }

    private fun processChange(change: Change) {
        if (change is AddChange) {
            summarySize += change.addedString.toByteArray().size.toLong()
        } else if (change is ModifyChange) {
            summarySize += change.newFileContent.toByteArray().size.toLong()
        } else if (change is CopyChange) {
            summarySize += change.newFileContent.toByteArray().size.toLong()
        }
    }

    override fun getValue(key: String, revision: Revision): String? {
        return null
    }

    override fun checkout(revision: Revision) {
    }
}
