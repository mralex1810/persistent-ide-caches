package com.github.sudu.persistentidecaches

import com.github.sudu.persistentidecaches.GitParser
import com.github.sudu.persistentidecaches.changes.*
import com.github.sudu.persistentidecaches.lmdb.maps.LmdbSha12Int
import com.github.sudu.persistentidecaches.records.FilePointer
import com.github.sudu.persistentidecaches.records.Revision
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.AndTreeFilter
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors
import kotlin.math.min

class GitParser @JvmOverloads constructor(
    git: Git, private val indexesManager: IndexesManager, private val gitCommits2Revisions: LmdbSha12Int,
    private val commitsLimit: Int = Int.MAX_VALUE
) {
    private val repository: Repository = git.repository

    @Throws(IOException::class)
    fun parseAll() {
        val refs = repository.refDatabase.refs
        System.err.println("Parsing " + refs.size + " refs")
        var cnt = 0
        for (ref in refs) {
            parseOne(ref.objectId)
            System.err.println("Parsed " + ref.name + " " + (++cnt) + "/" + refs.size + " refs")
        }
    }

    @Throws(IOException::class)
    fun parseHead() {
        parseOne(repository.resolve(Constants.HEAD))
    }

    @get:Throws(IOException::class)
    val head: String
        get() {
            RevWalk(repository).use { walk ->
                return walk.parseCommit(repository.resolve(Constants.HEAD)).name
            }
        }

    private fun parseOne(head: ObjectId) {
        LOG.info("Parsing ref: " + head.name)
        try {
            RevWalk(repository).use { walk ->
                val commits: Deque<RevCommit> = ArrayDeque()
                var firstCommit: RevCommit? = null
                run {
                    walk.markStart(walk.parseCommit(head))
                    for (commit in walk) {
                        commits.add(commit)
                        if (gitCommits2Revisions.get(commit.name) != -1) {
                            firstCommit = commit
                            break
                        }
                    }
                }
                if (!commits.iterator().hasNext()) {
                    throw RuntimeException("Repository hasn't commits")
                }
                LOG.info(String.format("%d commits found to process", commits.size))

                if (firstCommit == null) {
                    indexesManager.checkout(Revision.NULL)
                    firstCommit = commits.removeLast()
                    parseFirstCommit(firstCommit)
                } else {
                    val rev = Revision(gitCommits2Revisions.get(firstCommit!!.name))
                    indexesManager.checkout(rev)
                }
                var prevCommit = firstCommit

                var commitsParsed = 0
                val totalCommits = min(commitsLimit.toDouble(), commits.size.toDouble()).toInt()
                while (commitsParsed < totalCommits) {
                    if (commitsParsed % 100 == 0) {
                        System.err.printf("Processed %d commits out of %d %n", commitsParsed, totalCommits)
                    }
                    val commit = commits.removeLast()
                    parseCommit(commit, prevCommit)
                    prevCommit = commit
                    commitsParsed++
                }
                System.err.println("Processed $totalCommits commits")
            }
        } catch (e: GitAPIException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }


    @Throws(IOException::class, GitAPIException::class)
    private fun parseCommit(commit: RevCommit, prevCommit: RevCommit?) {
        TreeWalk(repository).use { tw ->
            tw.addTree(prevCommit!!.tree)
            tw.addTree(commit.tree)
            tw.filter = IndexDiffFilter.ANY_DIFF
            tw.filter = AndTreeFilter.create(IndexDiffFilter.ANY_DIFF, PathSuffixFilter.create(".java"))
            tw.isRecursive = true
            val rawChanges = DiffEntry.scan(tw)
            sendChanges(rawChanges.stream()
                .map<List<Change>> { it: DiffEntry ->
                    try {
                        return@map processDiff(it)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
                .flatMap<Change> { obj: List<Change> -> obj.stream() }
                .collect(Collectors.toList()),
                commit
            )
        }
    }

    fun sendChanges(changes: List<Change>, commit: RevCommit) {
        changes.forEach {
            Objects.requireNonNull(it)
            if (it is FileChange) {
                indexesManager.fileCache.tryRegisterNewObj(it.place.file)
            } else if (it is FileHolderChange) {
                indexesManager.fileCache.tryRegisterNewObj(it.oldFileName)
                indexesManager.fileCache.tryRegisterNewObj(it.newFileName)
            }
        }
        val rev = gitCommits2Revisions.get(commit.name)
        if (rev == -1) {
            indexesManager.revisions.currentRevision = indexesManager.revisions.addRevision(
                indexesManager.revisions.currentRevision
            )
            gitCommits2Revisions.put(commit.name, indexesManager.revisions.currentRevision.revision)
            indexesManager.applyChanges(changes)
        } else {
            indexesManager.checkout(Revision(rev))
        }
    }


    fun fileGetter(abbreviatedObjectId: AbbreviatedObjectId): Supplier<String> {
        return Supplier {
            try {
                return@Supplier String(repository.open(abbreviatedObjectId.toObjectId()).bytes)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    @Throws(IOException::class)
    fun processDiff(diffEntry: DiffEntry): List<Change> {
        return when (diffEntry.changeType) {
            DiffEntry.ChangeType.ADD -> java.util.List.of<Change>(
                AddChange(
                    System.currentTimeMillis(),
                    FilePointer(Path.of(diffEntry.newPath), 0),
                    String(repository.open(diffEntry.newId.toObjectId()).bytes)
                )
            )

            DiffEntry.ChangeType.MODIFY -> java.util.List.of<Change>(
                ModifyChange(
                    System.currentTimeMillis(),
                    fileGetter(diffEntry.oldId),
                    fileGetter(diffEntry.newId),
                    Path.of(diffEntry.oldPath),
                    Path.of(diffEntry.newPath)
                )
            )

            DiffEntry.ChangeType.DELETE -> java.util.List.of<Change>(
                DeleteChange(
                    System.currentTimeMillis(),
                    FilePointer(Path.of(diffEntry.oldPath), 0),
                    String(repository.open(diffEntry.oldId.toObjectId()).bytes)
                )
            )

            DiffEntry.ChangeType.RENAME -> java.util.List.of<Change>(
                RenameChange(
                    System.currentTimeMillis(),
                    fileGetter(diffEntry.oldId),
                    fileGetter(diffEntry.newId),
                    Path.of(diffEntry.oldPath),
                    Path.of(diffEntry.newPath)
                )
            )

            DiffEntry.ChangeType.COPY -> java.util.List.of<Change>(
                CopyChange(
                    System.currentTimeMillis(),
                    fileGetter(diffEntry.oldId),
                    fileGetter(diffEntry.newId),
                    Path.of(diffEntry.oldPath),
                    Path.of(diffEntry.newPath)
                )
            )
        }
    }

    private fun parseFirstCommit(first: RevCommit?) {
        val rev = gitCommits2Revisions.get(first!!.name)
        if (rev != -1) {
            indexesManager.revisions.currentRevision = Revision(rev)
            return
        }
        val changes: MutableList<Change> = ArrayList()
        try {
            TreeWalk(repository).use { treeWalk ->
                treeWalk.addTree(first.tree)
                treeWalk.filter = AndTreeFilter.create(IndexDiffFilter.ANY_DIFF, PathSuffixFilter.create(".java"))
                treeWalk.isRecursive = true
                while (treeWalk.next()) {
                    changes.add(
                        AddChange(
                            System.currentTimeMillis(),
                            FilePointer(Path.of(treeWalk.pathString), 0),
                            String(repository.open(treeWalk.getObjectId(0)).bytes)
                        )
                    )
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        sendChanges(changes, first)
    }

    companion object {
        const val PARSE_ONLY_TREE: Boolean = false
        private val LOG: Logger = LoggerFactory.getLogger(GitParser::class.java)
    }
}