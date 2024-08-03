package com.github.sudu.persistentidecaches

import com.github.sudu.persistentidecaches.records.Revision

interface Index<Key, Value> : ChangeProcessor {
    fun getValue(key: Key, revision: Revision): Value?

    fun checkout(revision: Revision)
}
