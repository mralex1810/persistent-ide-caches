package com.github.sudu.persistentidecaches

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.sudu.persistentidecaches.ccsearch.CamelCaseIndex
import com.github.sudu.persistentidecaches.ccsearch.CamelCaseIndexUtils
import com.github.sudu.persistentidecaches.ccsearch.Matcher.letters
import com.github.sudu.persistentidecaches.changes.*
import com.github.sudu.persistentidecaches.records.FilePointer
import com.github.sudu.persistentidecaches.symbols.Symbol
import com.github.sudu.persistentidecaches.trigram.TrigramIndex
import com.github.sudu.persistentidecaches.trigram.TrigramIndexUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.math.min

object VsCodeClient {
    const val SEARCH: String = "search"
    const val CHANGES: String = "changes"
    const val BUSY_WAITING_MILLIS: Int = 500
    const val CHECKOUT: String = "checkout"
    const val CCSEARCH: String = "ccsearch"
    const val BUCKET_SIZE: Int = 10
    const val NEXT: String = "next"
    const val PREV: String = "prev"
    private val BUFFER = CharArray(16384)
    private var returned: List<String>? = null
    private var currentPos = 0
    private var time: Long = 0

    private fun printUsage() {
        System.err.println(
            """
            Usage:
            java VsCodeClient path_to_repository reset_db parseAll/parseHead trigram_index camel_case_index
            
            path_to_repository -- absolute path to repository
            reset_db -- true/false to reset databases
            parseAll/parseHead -- "parseAll" or "parseHead" to parse all refs or only HEAD
            trigram_index  -- true/false to enable trigram index
            camel_case_index  -- true/false to enable camel case index
            
            """.trimIndent()
        )
    }

    @Throws(IOException::class, InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 5) {
            System.err.println("Wrong usage")
            printUsage()
        }
        IndexesManager(args[1] == "true").use { manager ->
            var trigramHistoryIndex: TrigramIndex? = null
            val trigramIndexUtils: TrigramIndexUtils?
            var camelCaseSearch: CamelCaseIndex? = null
            val camelCaseSearchUtils: CamelCaseIndexUtils?
            if (args[3] == "true") {
                trigramHistoryIndex = manager.addTrigramIndex()
                trigramIndexUtils = trigramHistoryIndex.trigramIndexUtils
            } else {
                trigramIndexUtils = null
            }
            if (args[4] == "true") {
                camelCaseSearch = manager.addCamelCaseIndex()
                camelCaseSearchUtils = camelCaseSearch.utils
            } else {
                camelCaseSearchUtils = null
            }
            val repPath = Path.of(args[0])
            manager.parseGitRepository(repPath, args[3] == "parseHead")

            val objectMapper = ObjectMapper()
            val scanner = BufferedReader(InputStreamReader(System.`in`))
            while (true) {
                var line = scanner.readLine()
                Thread.sleep(BUSY_WAITING_MILLIS.toLong())
                when (line) {
                    CHANGES -> {
                        val read = scanner.read(BUFFER)
                        line = String(BUFFER, 0, read)
                        val changes = objectMapper.readValue(line, Changes::class.java)
                        val processedChangesList: MutableList<Change> = ArrayList()
                        for ((uri, oldText, newText) in changes.modifyChanges) {
                            val path = repPath.relativize(Path.of(uri))
                            val modifyChange = ModifyChange(
                                changes.timestamp,
                                { oldText },
                                { newText },
                                path,
                                path
                            )
                            processedChangesList.add(modifyChange)
                        }
                        for ((uri, text) in changes.addChanges) {
                            val addChange = AddChange(
                                changes.timestamp,
                                FilePointer(repPath.relativize(Path.of(uri)), 0),
                                text
                            )
                            processedChangesList.add(addChange)
                        }
                        for ((uri, text) in changes.deleteChanges) {
                            val deleteChange = DeleteChange(
                                changes.timestamp,
                                FilePointer(repPath.relativize(Path.of(uri)), 0),
                                text
                            )
                            processedChangesList.add(deleteChange)
                        }
                        for ((oldUri, newUri, text) in changes.renameChanges) {
                            val renameChange = RenameChange(
                                changes.timestamp,
                                { text },
                                { text },
                                repPath.relativize(Path.of(oldUri)),
                                repPath.relativize(Path.of(newUri))
                            )
                            processedChangesList.add(renameChange)
                        }
                        manager.nextRevision()
                        manager.applyChanges(processedChangesList)
                    }

                    SEARCH -> {
                        val read = scanner.read(BUFFER)
                        val l = String(BUFFER, 0, read)
                        currentPos = 0
                        checkTime {
                            returned = trigramIndexUtils!!.filesForString(l)
                                .stream()
                                .map { obj: Path -> obj.toString() }
                                .toList()
                        }
                        sendCurrentBucket()
                    }

                    CHECKOUT -> {
                        val read = scanner.read(BUFFER)
                        val l = String(BUFFER, 0, read)
                        checkTime { manager.checkoutToGitRevision(l) }
                        println(time)
                    }

                    CCSEARCH -> {
                        val read = scanner.read(BUFFER)
                        val req = String(BUFFER, 0, read)
                        checkTime {
                            returned =
                                camelCaseSearchUtils!!.getSymbolsFromAny(req).stream()
                                    .map { it: Symbol ->
                                        Stream.of(
                                            Stream.of(it.name),
                                            Stream.of(manager.fileCache.getObject(it.pathNum)),
                                            Arrays.stream(letters(req, it.name)).mapToObj { it.toString() }
                                        )
                                            .flatMap { it }
                                            .map { Objects.toString(it) }
                                            .collect(Collectors.joining(" "))
                                    }
                                    .toList()
                        }
                        currentPos = 0
                        sendCurrentBucket()
                    }

                    NEXT -> {
                        currentPos += BUCKET_SIZE
                        System.err.println("Next " + currentPos + " of " + returned!!.size)
                        sendCurrentBucket()
                    }

                    PREV -> {
                        currentPos -= BUCKET_SIZE
                        System.err.println("Prev " + currentPos + " of " + returned!!.size)
                        sendCurrentBucket()
                    }
                }
            }
        }
    }

    private fun checkTime(runnable: Runnable) {
        val start = System.nanoTime()
        runnable.run()
        time = (System.nanoTime() - start) / 1000000
    }

    private fun sendCurrentBucket() {
        println(
            Stream.concat(
                Stream.of(returned!!.size, time).map { obj: Number -> obj.toString() },
                returned!!.subList(
                    currentPos, min((currentPos + BUCKET_SIZE).toDouble(), returned!!.size.toDouble())
                        .toInt()
                ).stream()
            )
                .collect(Collectors.joining("\n"))
        )
    }

    @JvmRecord
    private data class ModifyChangeFromJSON(val uri: String, val oldText: String, val newText: String)

    @JvmRecord
    private data class CreateFileChangeFromJSON(val uri: String, val text: String)

    @JvmRecord
    private data class DeleteFileChangeFromJSON(val uri: String, val text: String)

    @JvmRecord
    private data class RenameFileChangeFromJSON(val oldUri: String, val newUri: String, val text: String)

    @JvmRecord
    private data class Changes(
        val modifyChanges: List<ModifyChangeFromJSON>,
        val addChanges: List<CreateFileChangeFromJSON>,
        val deleteChanges: List<DeleteFileChangeFromJSON>,
        val renameChanges: List<RenameFileChangeFromJSON>,
        val timestamp: Long
    )
}