package com.github.sudu.persistentidecaches.utils

import com.github.sudu.persistentidecaches.Revisions
import com.github.sudu.persistentidecaches.records.Revision

class DummyRevisions : Revisions {
    private val parents: MutableMap<Revision, Revision> = HashMap()
    override var currentRevision: Revision = Revision(0)
    private var revisions = 1

    override fun getParent(revision: Revision): Revision {
        return parents[revision]!!
    }

    override fun addRevision(parent: Revision): Revision {
        val rev = Revision(revisions++)
        parents[rev] = parent
        return rev
    }

    override fun addLastRevision(): Revision {
        currentRevision = addRevision(currentRevision)
        return currentRevision
    }
}


