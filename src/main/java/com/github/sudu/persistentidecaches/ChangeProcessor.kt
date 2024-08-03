package com.github.sudu.persistentidecaches

import com.github.sudu.persistentidecaches.changes.Change

interface ChangeProcessor {
    fun prepare(changes: List<Change?>?)


    fun processChanges(changes: List<Change?>?)
}
