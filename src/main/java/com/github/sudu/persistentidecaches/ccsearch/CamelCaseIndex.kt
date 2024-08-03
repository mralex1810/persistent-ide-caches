package com.github.sudu.persistentidecaches.ccsearch

import com.github.sudu.persistentidecaches.Index
import com.github.sudu.persistentidecaches.changes.AddChange
import com.github.sudu.persistentidecaches.changes.Change
import com.github.sudu.persistentidecaches.changes.DeleteChange
import com.github.sudu.persistentidecaches.changes.ModifyChange
import com.github.sudu.persistentidecaches.lmdb.CountingCacheImpl
import com.github.sudu.persistentidecaches.records.FilePointer
import com.github.sudu.persistentidecaches.records.Revision
import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.symbols.Symbol
import com.github.sudu.persistentidecaches.symbols.Symbols
import org.apache.commons.lang3.tuple.Pair
import org.lmdbjava.Env
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream

class CamelCaseIndex(
    env: Env<ByteBuffer>, private val symbolCache: CountingCacheImpl<Symbol>,
    private val pathCache: CountingCacheImpl<Path>
) : Index<String, String> {
    val classCounter: TrigramSymbolCounterLmdb = TrigramSymbolCounterLmdb(env, symbolCache, "class")
    val fieldCounter: TrigramSymbolCounterLmdb = TrigramSymbolCounterLmdb(env, symbolCache, "field")
    val methodCounter: TrigramSymbolCounterLmdb = TrigramSymbolCounterLmdb(env, symbolCache, "method")

    override fun prepare(changes: List<Change>) {
        processChanges(changes)
    }

    private fun processModifyChange(modifyChange: ModifyChange) {
        processDeleteChange(
            DeleteChange(
                modifyChange.timestamp,
                FilePointer(modifyChange.oldFileName, 0),
                modifyChange.oldFileContent
            )
        )
        processAddChange(
            AddChange(
                modifyChange.timestamp,
                FilePointer(modifyChange.newFileName, 0),
                modifyChange.newFileContent
            )
        )
    }

    private fun processDeleteChange(change: DeleteChange) {
        val symbolsFile = getSymbolsFromString(change.deletedString)
        val fileNum = pathCache.getNumber(change.place.file)
        classCounter.decrease(collectCounter(symbolsFile.classOrInterfaceSymbols, fileNum))
        fieldCounter.decrease(collectCounter(symbolsFile.fieldSymbols, fileNum))
        methodCounter.decrease(collectCounter(symbolsFile.methodSymbols, fileNum))
    }

    override fun getValue(key: String, revision: Revision): String? {
        return null
    }

    private fun processAddChange(change: AddChange) {
        val symbolsFile = getSymbolsFromString(change.addedString)
        val fileNum = pathCache.getNumber(change.place.file)
        symbolsFile.concatedStream().forEach { it: String? ->
            symbolCache.tryRegisterNewObj(
                Symbol(it!!, pathCache.getNumber(change.place.file))
            )
        }
        classCounter.add(collectCounter(symbolsFile.classOrInterfaceSymbols, fileNum))
        fieldCounter.add(collectCounter(symbolsFile.fieldSymbols, fileNum))
        methodCounter.add(collectCounter(symbolsFile.methodSymbols, fileNum))
    }

    override fun processChanges(changes: List<Change>) {
        changes.forEach {
            Objects.requireNonNull(it)
            when (it) {
                is ModifyChange -> processModifyChange(it)

                is AddChange -> processAddChange(it)

                is DeleteChange -> processDeleteChange(it)
            }
        }
    }

    override fun checkout(revision: Revision) {
//        throw new UnsupportedOperationException();
    }

    val utils: CamelCaseIndexUtils
        get() = CamelCaseIndexUtils(this)

    companion object {
        var CAMEL_CASE_PATTERN: Pattern = Pattern.compile("[A-Za-z][a-z0-9]*([A-Z][a-z0-9]*)*")
        fun getSymbolsFromString(javaFile: String): Symbols {
            return JavaSymbolListener.getSymbolsFromString(javaFile)
        }

        fun isCamelCase(name: String): Boolean {
            return CAMEL_CASE_PATTERN.matcher(name).matches()
        }

        fun getInterestTrigrams(symbolName: String): List<Trigram> {
            val parts = symbolName.split(Pattern.compile("(?=[A-Z])"))
                    .dropWhile { it.isEmpty() }
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val trigrams: MutableList<Trigram> = ArrayList()
            val normalizedParts = Stream.concat(Stream.of("$"), Arrays.stream(parts)).map { it.toByteArray() }
                .toList()
            for (partIndex in normalizedParts.indices) {
                val part = normalizedParts[partIndex]
                for (indexInPart in normalizedParts[partIndex].indices) {
                    val thisByte = part[indexInPart]
                    if (indexInPart + 2 < normalizedParts[partIndex].size) {
                        trigrams.add(
                            Trigram(
                                byteArrayOf(
                                    thisByte,
                                    part[indexInPart + 1],
                                    part[indexInPart + 2]
                                )
                            )
                        )
                    }
                    if (indexInPart + 1 < normalizedParts[partIndex].size && partIndex + 1 < normalizedParts.size) {
                        trigrams.add(
                            Trigram(
                                byteArrayOf(
                                    thisByte,
                                    part[indexInPart + 1],
                                    normalizedParts[partIndex + 1][0]
                                )
                            )
                        )
                    }
                    if (partIndex + 1 < normalizedParts.size && normalizedParts[partIndex + 1].size >= 2) {
                        trigrams.add(
                            Trigram(
                                byteArrayOf(
                                    thisByte,
                                    normalizedParts[partIndex + 1][0],
                                    normalizedParts[partIndex + 1][1]
                                )
                            )
                        )
                    }
                    if (partIndex + 2 < normalizedParts.size) {
                        trigrams.add(
                            Trigram(
                                byteArrayOf(
                                    thisByte,
                                    normalizedParts[partIndex + 1][0],
                                    normalizedParts[partIndex + 2][0]
                                )
                            )
                        )
                    }
                }
            }
            return trigrams
        }

        fun getPriority(trigram: Trigram?, word: String?): Int {
            return 1
        }

        private fun collectCounter(symbols: List<String>, fileNum: Int): Map<TrigramSymbol, Int> {
            return symbols.stream()
                .map { it: String -> Pair.of(it, getInterestTrigrams(it)) }
                .map { it: Pair<String, List<Trigram>> ->
                    Pair.of(it.left, it.right.stream().map { obj: Trigram -> obj.toLowerCase() }
                        .toList())
                }
                .flatMap { pair: Pair<String, List<Trigram>> ->
                    pair.value.stream()
                        .map { trigram: Trigram? ->
                            TrigramSymbol(
                                trigram!!, Symbol(pair.key, fileNum)
                            )
                        }
                }
                .collect(Collectors.groupingBy({ it }, Collectors.summingInt { 1 }))
        }
    }
}
