package com.github.sudu.persistentidecaches.utils.indexes

import com.github.sudu.persistentidecaches.Index
import com.github.sudu.persistentidecaches.changes.Change
import com.github.sudu.persistentidecaches.records.Revision

class EchoIndex : Index<String, String> {
    override fun prepare(changes: List<Change?>) {
        println("Echo: prepare")
        changes.forEach { x: Any? -> println(x) }
        println(SEP)
    }

    override fun processChanges(changes: List<Change?>) {
        println("Echo: process")
        changes.forEach { x: Any? -> println(x) }
        println(SEP)
    }

    override fun getValue(o: String, revision: Revision): String? {
        println("Echo: get $o from revision: $revision")
        return null
    }

    override fun checkout(revision: Revision) {
        println("Echo: checkout to $revision")
    }

    companion object {
        const val SEP: String = "-------------------"
    }
}
