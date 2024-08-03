package com.github.sudu.persistentidecaches.ccsearch

import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.symbols.Symbol

data class TrigramSymbol(val trigram: Trigram, val word: Symbol) : Comparable<TrigramSymbol> {
    override fun compareTo(o: TrigramSymbol): Int {
        val res = trigram.compareTo(o.trigram)
        return if (res == 0) word.compareTo(o.word) else res
    }
}
