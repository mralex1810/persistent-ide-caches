package com.github.sudu.persistentidecaches.git

import com.github.sudu.persistentidecaches.GitParser
import com.github.sudu.persistentidecaches.IndexesManager
import com.github.sudu.persistentidecaches.IntegrationIndexesTest
import com.github.sudu.persistentidecaches.changes.AddChange
import com.github.sudu.persistentidecaches.changes.Change
import com.github.sudu.persistentidecaches.changes.ModifyChange
import com.github.sudu.persistentidecaches.lmdb.maps.LmdbSha12Int
import com.github.sudu.persistentidecaches.utils.DummyCountingCache
import com.github.sudu.persistentidecaches.utils.DummyRevisions
import com.github.sudu.persistentidecaches.utils.FileUtils.createParentDirectories
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer

class GitParserTest {
    @TempDir
    var gitDir: Path? = null

    @Test
    @Throws(IOException::class, GitAPIException::class)
    fun testParseOneBranch() {
        Git.init().setDirectory(gitDir!!.toFile()).call().use { git ->
            for (c in IntegrationIndexesTest.FILES) {
                val file = gitDir!!.resolve(c)
                createParentDirectories(file.parent)
                Files.writeString(file, c.toString())
                git.add()
                    .addFilepattern(c.toString())
                    .call()
                git.commit()
                    .setMessage(c.toString())
                    .call()
            }
            val indexesManager = mockedIndexManager()
            val db = Mockito.mock(LmdbSha12Int::class.java)
            Mockito.doReturn(-1).`when`(db).get(ArgumentMatchers.any())

            val requestCaptor = ArgumentCaptor.forClass(
                MutableList::class.java
            )

            GitParser(git, indexesManager, db).parseHead()

            Mockito.verify(indexesManager, Mockito.times(IntegrationIndexesTest.FILES.size))
                .applyChanges(requestCaptor.capture() as List<Change>)

            Assertions.assertEquals(requestCaptor.allValues.size, IntegrationIndexesTest.FILES.size)
            requestCaptor.allValues.forEach { Assertions.assertEquals(it.size, 1) }
            Assertions.assertTrue(requestCaptor.allValues.stream().flatMap { it.stream() }
                .allMatch { it is AddChange })
            Assertions.assertEquals(
                requestCaptor.allValues.stream().mapToLong { it.size.toLong() }
                    .sum(),
                IntegrationIndexesTest.FILES.size.toLong())
            IntegrationIndexesTest.FILES.forEach(Consumer { file: Path ->
                val change = requestCaptor.allValues.stream().flatMap { it.stream() }
                    .map { it as AddChange }
                    .filter { it.place.file == file }
                    .toList()
                Assertions.assertEquals(change.size, 1)
                Assertions.assertEquals(change[0].addedString, file.toString())
            })
        }
        Git.open(gitDir!!.toFile()).use { git ->
            for (c in IntegrationIndexesTest.FILES) {
                val file = gitDir!!.resolve(c)
                Files.writeString(file, c.toString().repeat(3))
                git.add()
                    .addFilepattern(c.toString())
                    .call()
                git.commit()
                    .setMessage(c.toString())
                    .call()
            }
            val indexesManager = mockedIndexManager()
            val db = Mockito.mock(LmdbSha12Int::class.java)
            Mockito.doReturn(-1).`when`(db).get(ArgumentMatchers.any())

            val requestCaptor = ArgumentCaptor.forClass(
                MutableList::class.java
            )

            GitParser(git, indexesManager, db).parseHead()

            Mockito.verify(indexesManager, Mockito.times(2 * IntegrationIndexesTest.FILES.size))
                .applyChanges(requestCaptor.capture() as List<Change>)

            Assertions.assertEquals(requestCaptor.allValues.size, 2 * IntegrationIndexesTest.FILES.size)
            requestCaptor.allValues.forEach { Assertions.assertEquals(it.size, 1) }
            Assertions.assertEquals(requestCaptor.allValues.stream().flatMap { it.stream() }
                .filter { it is ModifyChange }.count(), IntegrationIndexesTest.FILES.size.toLong())
            Assertions.assertEquals(
                requestCaptor.allValues.stream().mapToLong { it.size.toLong() }
                    .sum(),
                2L * IntegrationIndexesTest.FILES.size)
            IntegrationIndexesTest.FILES.forEach(Consumer { file: Path ->
                val changes = requestCaptor.allValues.stream().flatMap { it.stream() }
                    .filter { it is ModifyChange }
                    .map { it as ModifyChange }
                    .filter { it.newFileName == file }
                    .toList()
                Assertions.assertEquals(changes.size, 1)
                val change = changes[0]
                Assertions.assertEquals(change!!.oldFileContent, file.toString())
                Assertions.assertEquals(change.newFileContent, file.toString().repeat(3))
                Assertions.assertEquals(change.oldFileName, change.newFileName)
            })
        }
    }

    companion object {
        fun mockedIndexManager(): IndexesManager {
            val indexesManager = Mockito.mock(IndexesManager::class.java)
            Mockito.doReturn(DummyRevisions()).`when`(indexesManager).revisions
            Mockito.doReturn(DummyCountingCache<Any>()).`when`(indexesManager).fileCache
            return indexesManager
        }
    }
}
