package com.github.sudu.persistentidecaches

import com.github.sudu.persistentidecaches.ccsearch.CamelCaseIndex
import com.github.sudu.persistentidecaches.changes.Change
import com.github.sudu.persistentidecaches.lmdb.CountingCacheImpl
import com.github.sudu.persistentidecaches.lmdb.maps.*
import com.github.sudu.persistentidecaches.records.Revision
import com.github.sudu.persistentidecaches.symbols.Symbol
import com.github.sudu.persistentidecaches.trigram.TrigramIndex
import com.github.sudu.persistentidecaches.utils.FileUtils.createParentDirectories
import com.github.sudu.persistentidecaches.utils.indexes.EchoIndex
import com.github.sudu.persistentidecaches.utils.indexes.SizeCounterIndex
import org.eclipse.jgit.api.Git
import org.lmdbjava.Env
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.Consumer


class IndexesManager @JvmOverloads constructor(resetDBs: Boolean = false, dataPath: Path = Path.of("")) :
    AutoCloseable {

    private val logger = LoggerFactory.getLogger(IndexesManager::class.java.name)
    private val lmdbGlobalPath: Path = dataPath.resolve(".lmdb")
    private val lmdbTrigramPath: Path = dataPath.resolve(".lmdb.trigrams")
    private val lmdbCamelCaseSearchPath: Path = dataPath.resolve(".lmdb.camelCaseSearch")
    private val indexes: MutableMap<Class<*>, Index<*, *>> = HashMap()

    val revisions: Revisions
    private val pathCache: CountingCacheImpl<Path>
    val variables: LmdbString2Int
    private val globalEnv: Env<ByteBuffer>
    private val envs: MutableList<Env<ByteBuffer>> = ArrayList()
    private val symbolCache: CountingCacheImpl<Symbol>
    private var lmdbSha12Int: LmdbSha12Int? = null


    init {
        if (resetDBs) {
            try {
                if (Files.exists(lmdbGlobalPath)) {
                    Files.walkFileTree(lmdbGlobalPath, DELETE)
                }
                if (Files.exists(lmdbTrigramPath)) {
                    Files.walkFileTree(lmdbTrigramPath, DELETE)
                }
                if (Files.exists(lmdbCamelCaseSearchPath)) {
                    Files.walkFileTree(lmdbCamelCaseSearchPath, DELETE)
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        createParentDirectories(lmdbTrigramPath, lmdbGlobalPath, lmdbCamelCaseSearchPath)

        globalEnv = initGlobalEnv()
        variables = initVariables(globalEnv)
        revisions = initRevisions(globalEnv, variables)
        pathCache = initFileCache(globalEnv, variables)
        symbolCache = initSymbolCache(globalEnv, variables)
    }

    private fun initGlobalEnv(): Env<ByteBuffer> {
        logger.info(lmdbGlobalPath.toFile().toString())
        return Env.create()
            .setMapSize(10485760)
            .setMaxDbs(7)
            .setMaxReaders(2)
            .open(lmdbGlobalPath.toFile())
    }

    private fun initVariables(env: Env<ByteBuffer>): LmdbString2Int {
        return LmdbString2Int(env, "variables")
    }

    private fun initRevisions(env: Env<ByteBuffer>, variables: LmdbString2Int): Revisions {
        return RevisionsImpl(variables, LmdbInt2Int(globalEnv, "revisions"))
    }

    private fun initFileCache(globalEnv: Env<ByteBuffer>, variables: LmdbString2Int): CountingCacheImpl<Path> {
        val pathCache = CountingCacheImpl(
            "files",
            LmdbInt2Path(globalEnv, "files"),
            variables
        )
        pathCache.init()
        pathCache.restoreObjectsFromDB()
        return pathCache
    }

    private fun initSymbolCache(
        globalEnv: Env<ByteBuffer>, variables: LmdbString2Int
    ): CountingCacheImpl<Symbol> {
        val symbolCache =
            CountingCacheImpl(
                "symbols",
                LmdbInt2Symbol(globalEnv, "symbols"),
                variables
            )
        symbolCache.init()
        symbolCache.restoreObjectsFromDB()
        return symbolCache
    }

    fun addEchoIndex(): EchoIndex {
        val echoIndex = EchoIndex()
        indexes[EchoIndex::class.java] = echoIndex
        return echoIndex
    }

    fun addSizeCounterIndex(): SizeCounterIndex {
        val sizeCounterIndex = SizeCounterIndex()
        indexes[SizeCounterIndex::class.java] = sizeCounterIndex
        return sizeCounterIndex
    }

    fun addTrigramIndex(): TrigramIndex {
        val trigramEnv = Env.create()
            .setMapSize(10485760000L)
            .setMaxDbs(3)
            .setMaxReaders(2)
            .open(lmdbTrigramPath.toFile())
        envs.add(trigramEnv)
        val trigramHistoryIndex = TrigramIndex(trigramEnv, pathCache, revisions)
        indexes[TrigramIndex::class.java] = trigramHistoryIndex
        return trigramHistoryIndex
    }

    fun addCamelCaseIndex(): CamelCaseIndex {
        val camelCaseEnv = Env.create()
            .setMapSize(1048576000)
            .setMaxDbs(3)
            .setMaxReaders(2)
            .open(lmdbCamelCaseSearchPath.toFile())
        envs.add(camelCaseEnv)
        val camelCaseIndex = CamelCaseIndex(camelCaseEnv, symbolCache, pathCache)
        indexes[CamelCaseIndex::class.java] = camelCaseIndex
        return camelCaseIndex
    }

    @JvmOverloads
    fun parseGitRepository(pathToRepository: Path, parseOnlyHead: Boolean = true) {
        parseGitRepository(pathToRepository, Int.MAX_VALUE, true)
    }

    @JvmOverloads
    fun parseGitRepository(pathToRepository: Path, limit: Int, parseOnlyHead: Boolean = true) {
        try {
            Git.open(pathToRepository.toFile()).use { git ->
                lmdbSha12Int = LmdbSha12Int(globalEnv, "git_commits_to_revision")
                val parser = GitParser(
                    git, this,
                    lmdbSha12Int!!,
                    limit
                )
                if (revisions.currentRevision == Revision.NULL) {
                    if (parseOnlyHead) {
                        parser.parseHead()
                    } else {
                        parser.parseAll()
                    }
                    checkoutToGitRevision(parser.head)
                }
                System.err.println("Parsed")
            }
        } catch (ioException: IOException) {
            throw RuntimeException(ioException)
        }
    }

    fun checkout(targetRevision: Revision) {
        indexes.values.forEach(Consumer { index: Index<*, *> ->
            index.checkout(targetRevision)
        })
        revisions.currentRevision = targetRevision
    }

    fun checkoutToGitRevision(commitHashName: String) {
        val revision = lmdbSha12Int!!.get(commitHashName)
        require(revision != -1)
        checkout(Revision(revision))
    }

    override fun close() {
        envs.forEach(Consumer { obj: Env<ByteBuffer> -> obj.close() })
        globalEnv.close()
    }

    val fileCache: CountingCache<Path>
        get() = pathCache

    fun applyChanges(changes: List<Change>) {
        indexes.values.forEach(Consumer { it: Index<*, *> -> it.processChanges(changes) })
    }

    fun <T, U> getIndex(indexClass: Class<out Index<T?, U?>?>): Index<*, *> {
        return indexes[indexClass]!!
    }

    fun nextRevision() {
        revisions.currentRevision = revisions.addRevision(
            revisions.currentRevision
        )
    }

    companion object {
        private val DELETE: SimpleFileVisitor<Path> = object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            @Throws(IOException::class)
            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        }
    }
}
