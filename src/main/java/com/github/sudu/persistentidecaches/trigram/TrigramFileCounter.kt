package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.records.TrigramFile
import com.github.sudu.persistentidecaches.utils.Counter
import com.github.sudu.persistentidecaches.utils.TriConsumer
import java.nio.file.Path

class TrigramFileCounter : Counter<TrigramFile> {
    constructor()

    constructor(counter: Map<TrigramFile, Int>) : super(counter)

    fun add(trigram: Trigram?, file: Path?, delta: Int) {
        add(TrigramFile(trigram!!, file!!), delta)
    }

    fun decrease(trigram: Trigram?, file: Path?, delta: Int) {
        add(trigram, file, -delta)
    }

    fun add(file: Path?, counter: TrigramCounter) {
        counter.forEach { trigram: Trigram?, integer: Int -> add(trigram, file, integer) }
    }

    fun decrease(file: Path?, counter: TrigramCounter) {
        counter.forEach { trigram: Trigram?, integer: Int -> decrease(trigram, file, integer) }
    }

    fun forEach(consumer: TriConsumer<Trigram, Path, Int>) {
        forEach { trigramFile: TrigramFile, integer: Int ->
            consumer.accept(
                trigramFile.trigram,
                trigramFile.file,
                integer
            )
        }
    }

    fun get(trigram: Trigram?, file: Path?): Int {
        return get(TrigramFile(trigram!!, file!!))
    }

    companion object {
        val EMPTY_COUNTER: TrigramFileCounter = TrigramFileCounter()
    }
}