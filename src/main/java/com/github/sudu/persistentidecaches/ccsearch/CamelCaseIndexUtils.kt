package com.github.sudu.persistentidecaches.ccsearch

import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.symbols.Symbol
import org.apache.commons.lang3.tuple.Pair
import java.util.*
import java.util.stream.Collectors

class CamelCaseIndexUtils(private val camelCaseIndex: CamelCaseIndex) {
    private fun symbolsForTrigramInCounters(
        trigram: Trigram,
        counters: List<TrigramSymbolCounterLmdb>
    ): List<Symbol> {
        return counters.stream()
            .map { counter: TrigramSymbolCounterLmdb -> counter.getObjForTrigram(trigram) }
            .flatMap { obj: List<Symbol> -> obj.stream() }
            .collect(Collectors.toList())
    }

    private fun getSymbols(
        request: String,
        counters: List<TrigramSymbolCounterLmdb>
    ): List<Symbol> {
        val trigramSet = getTrigramsSet(request.lowercase(Locale.getDefault()))
        if (trigramSet.isEmpty()) {
            return listOf()
        }
        val fileSet = TreeSet(symbolsForTrigramInCounters(trigramSet.first(), counters))
        trigramSet.pollFirst()
        trigramSet.forEach { it: Trigram -> fileSet.retainAll(symbolsForTrigramInCounters(it, counters).toSet()) }
        return fileSet.stream()
            .map { it: Symbol -> Pair.of(it, Matcher.match(request, it.name)) }
            .sorted(Comparator.comparing { pair: Pair<Symbol, Int> -> pair.right }
                .reversed())
            .filter { it: Pair<Symbol, Int> -> it.right > Matcher.NEG_INF }
            .map { obj: Pair<Symbol, Int> -> obj.left }
            .toList()
    }

    fun getSymbolsFromClasses(
        request: String
    ): List<Symbol> {
        return getSymbols(request, listOf(camelCaseIndex.classCounter))
    }

    fun getSymbolsFromAny(request: String): List<Symbol> {
        return getSymbols(
            request, listOf(
                camelCaseIndex.classCounter,
                camelCaseIndex.methodCounter,
                camelCaseIndex.fieldCounter
            )
        )
    }

    companion object {
        @JvmStatic
        fun getTrigramsSet(request: String): NavigableSet<Trigram> {
            val trigramSet: NavigableSet<Trigram> = TreeSet()
            val bytes = request.toByteArray()
            for (i in 2 until bytes.size) {
                val trigram = Trigram(byteArrayOf(bytes[i - 2], bytes[i - 1], bytes[i]))
                trigramSet.add(trigram)
            }
            return trigramSet
        }
    }
}
