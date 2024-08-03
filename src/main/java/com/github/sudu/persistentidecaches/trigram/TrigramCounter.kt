package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.utils.Counter

class TrigramCounter : Counter<Trigram> {
    constructor() : super()

    constructor(counter: Map<Trigram, Int>) : super(counter)
}
