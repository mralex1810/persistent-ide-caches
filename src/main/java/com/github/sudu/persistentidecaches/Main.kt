package com.github.sudu.persistentidecaches

import com.github.sudu.persistentidecaches.ccsearch.TrigramSymbolCounterLmdb
import com.github.sudu.persistentidecaches.records.Revision
import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.symbols.Symbol
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Stream


object Main {
    const val SEPARATOR: String = "-----"

    // needs java options:
    /*
    --add-opens java.base/java.nio=ALL-UNNAMED
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED
    */
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 1) {
            throw RuntimeException("Needs path to repository as first arg")
        }
        IndexesManager(true).use { manager ->
//            final var trigramHistoryIndex = manager.addTrigramIndex();
//            final var trigramIndexUtils = trigramHistoryIndex.getTrigramIndexUtils();
            val camelCaseIndex = manager.addCamelCaseIndex()
            val camelCaseIndexUtils = camelCaseIndex.utils
            //            final int LIMIT = 10;
            val sizeCounterIndex = manager.addSizeCounterIndex()
            val LIMIT = Int.MAX_VALUE
            benchmark { manager.parseGitRepository(Path.of(args[0]), LIMIT) }

            println("Sum size " + sizeCounterIndex.summarySize + " bytes")
            val map: MutableMap<String, Int> = HashMap()
            Stream.of(
                camelCaseIndex.classCounter, camelCaseIndex.methodCounter,
                camelCaseIndex.fieldCounter
            )
                .forEach { it: TrigramSymbolCounterLmdb ->
                    it.forEach { trigram: Trigram, symbol: Symbol, integer: Int ->
                        map.merge(trigram.toPrettyString(), integer) { a: Int, b: Int -> a + b }
                    }
                }
            try {
                Files.writeString(
                    Path.of("res.csv"), map.entries.stream()
                        .sorted(java.util.Map.Entry.comparingByValue())
                        .map { it: Map.Entry<String, Int> -> "\"" + it.key + "\"," + it.value }
                        .collect(Collectors.joining("\n"))
                )
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    fun benchmark(runnable: Runnable) {
        val start = System.currentTimeMillis()
        runnable.run()
        println("Benchmarked: " + ((System.currentTimeMillis() - start) / 1000) + " second")
    }

    fun benchmarkCheckout(
        targetRevision: Revision, manager: IndexesManager,
        revisions: Revisions
    ) {
        benchmark {
            System.out.printf(
                "checkout from %d to %d\n", revisions.currentRevision.revision,
                targetRevision.revision
            )
            manager.checkout(targetRevision)
            revisions.currentRevision = targetRevision
        }
    }
}
