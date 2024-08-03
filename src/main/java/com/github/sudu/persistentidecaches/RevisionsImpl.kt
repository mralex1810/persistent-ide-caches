package com.github.sudu.persistentidecaches

import com.github.sudu.persistentidecaches.lmdb.maps.LmdbInt2Int
import com.github.sudu.persistentidecaches.lmdb.maps.LmdbString2Int
import com.github.sudu.persistentidecaches.records.Revision

class RevisionsImpl(private val variables: LmdbString2Int, private val parents: LmdbInt2Int) : Revisions {
    override var currentRevision: Revision = Revision(variables[CURRENT_REVISION])
        set(value) {
            field = value
            updateCurrentRevision()
        }
    private var revisionsCount: Int = variables[REVISIONS_COUNT]

    init {
        if (revisionsCount == -1) {
            variables.put(REVISIONS_COUNT, 0)
        }
        if (currentRevision.revision == -1) {
            variables.put(CURRENT_REVISION, 0)
        }
    }

    override fun getParent(revision: Revision): Revision? {
        return Revision(parents.get(revision.revision))
    }

    override fun addRevision(parent: Revision): Revision? {
        val rev = Revision(revisionsCount++)
        parents.put(rev.revision, parent.revision)
        updateRevisionsCount()
        return rev
    }

    override fun addLastRevision(): Revision {
        currentRevision = addRevision(currentRevision)!!
        updateCurrentRevision()
        return currentRevision
    }

    private fun updateCurrentRevision() {
        variables.put(CURRENT_REVISION, currentRevision.revision)
    }

    private fun updateRevisionsCount() {
        variables.put(REVISIONS_COUNT, revisionsCount)
    }

    companion object {
        const val REVISIONS_COUNT: String = "revisions_count"
        const val CURRENT_REVISION: String = "current_revision"
    }
}
