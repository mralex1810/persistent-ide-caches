package com.github.sudu.persistentidecaches

import com.github.sudu.persistentidecaches.changes.AddChange
import com.github.sudu.persistentidecaches.records.FilePointer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.function.Consumer

class IntegrationIndexesTest {
    @TempDir
    var tmpDataDir: Path? = null
    var indexesManager: IndexesManager? = null

    @BeforeEach
    fun prepare() {
        indexesManager = IndexesManager(true, tmpDataDir!!)
    }

    @Test
    fun testOneChange() {
        addFiles(FILES)
        val trigramIndex = indexesManager!!.addTrigramIndex()
        val addChanges = FILES.stream().map { it: Path -> createAddChange(it, it.toString()) }
            .toList()
        trigramIndex.processChanges(addChanges)
        FILES.forEach(
            Consumer { file: Path ->
                Assertions.assertEquals(
                    java.util.List.of(file),
                    trigramIndex.trigramIndexUtils.filesForString(file.toString())
                )
            }
        )
    }

    private fun addFiles(paths: List<Path>) {
        paths.forEach(Consumer { it: Path? ->
            indexesManager!!.fileCache.tryRegisterNewObj(
                it!!
            )
        })
    }

    private fun createAddChange(path: Path, text: String): AddChange {
        return AddChange(System.currentTimeMillis(), FilePointer(path, 0), text)
    }


    @AfterEach
    fun close() {
        indexesManager!!.close()
    }

    companion object {
        val EMPTY_PATH: Path = Path.of("")
        val FILES: List<Path> = java.util.List.of(
            EMPTY_PATH.resolve("file1.java"),
            EMPTY_PATH.resolve("file2.java"),
            EMPTY_PATH.resolve("file3.java"),
            EMPTY_PATH.resolve("dir1").resolve("dir2").resolve("file121.java"),
            EMPTY_PATH.resolve("dir1").resolve("dir2").resolve("file122.java"),
            EMPTY_PATH.resolve("dir1").resolve("dir3").resolve("file132.java"),
            EMPTY_PATH.resolve("dir1").resolve("file13.java"),
            EMPTY_PATH.resolve("😊😊😊😊😊😊.java")
        )
    }
}