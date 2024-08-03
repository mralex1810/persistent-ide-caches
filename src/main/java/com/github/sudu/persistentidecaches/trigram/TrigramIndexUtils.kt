package com.github.sudu.persistentidecaches.trigram

import com.github.sudu.persistentidecaches.ccsearch.CamelCaseIndexUtils.Companion.getTrigramsSet
import com.github.sudu.persistentidecaches.records.Trigram
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer

class TrigramIndexUtils(private val trigramIndex: TrigramIndex) {
    fun filesForString(str: String?): List<Path> {
        val trigramSet = getTrigramsSet(str!!)
        if (trigramSet.isEmpty()) {
            return listOf()
        }
        val fileSet: MutableSet<Path> = TreeSet(
            trigramIndex.counter.getObjForTrigram(trigramSet.first())
        )
        trigramSet.pollFirst()
        trigramSet.forEach(Consumer { it: Trigram ->
            fileSet.retainAll(
                trigramIndex.counter.getObjForTrigram(it)
            )
        })
        return ArrayList(fileSet)
    }
}
