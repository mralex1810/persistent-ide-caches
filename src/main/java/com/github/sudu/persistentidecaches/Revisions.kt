package com.github.sudu.persistentidecaches

import com.github.sudu.persistentidecaches.records.Revision

interface Revisions {
    fun getParent(revision: Revision): Revision?

    fun addRevision(parent: Revision): Revision

    fun addLastRevision(): Revision

    var currentRevision: Revision
}
