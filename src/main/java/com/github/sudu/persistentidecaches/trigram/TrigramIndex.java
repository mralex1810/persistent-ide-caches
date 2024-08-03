package com.github.sudu.persistentidecaches.trigram;

import com.github.sudu.persistentidecaches.Index;
import com.github.sudu.persistentidecaches.Revisions;
import com.github.sudu.persistentidecaches.changes.AddChange;
import com.github.sudu.persistentidecaches.changes.Change;
import com.github.sudu.persistentidecaches.changes.CopyChange;
import com.github.sudu.persistentidecaches.changes.DeleteChange;
import com.github.sudu.persistentidecaches.changes.ModifyChange;
import com.github.sudu.persistentidecaches.changes.RenameChange;
import com.github.sudu.persistentidecaches.lmdb.CountingCacheImpl;
import com.github.sudu.persistentidecaches.lmdb.maps.LmdbInt2Bytes;
import com.github.sudu.persistentidecaches.records.Revision;
import com.github.sudu.persistentidecaches.records.Trigram;
import com.github.sudu.persistentidecaches.records.TrigramFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import org.lmdbjava.Env;

public class TrigramIndex implements Index<TrigramFile, Integer> {

    private final Env<ByteBuffer> env;
    private final TrigramCache cache;
    private final Revisions revisions;
    private final TrigramFileCounterLmdb counter;
    private final TrigramIndexUtils trigramIndexUtils;

    public TrigramIndex(final Env<ByteBuffer> env, final CountingCacheImpl<Path> pathCache, final Revisions revisions) {
        this.env = env;
        cache = new TrigramCache(revisions, new LmdbInt2Bytes(env, "trigram_deltas"), pathCache);
        this.revisions = revisions;
        counter = new TrigramFileCounterLmdb(this.env, pathCache);
        trigramIndexUtils = new TrigramIndexUtils(this);
    }

    private static TrigramCounter getTrigramsCount(final String str) {
        final byte[] bytes = str.getBytes();
        final TrigramCounter result = new TrigramCounter();
        for (int i = 2; i < bytes.length; i++) {
            final Trigram trigram = new Trigram(new byte[]{bytes[i - 2], bytes[i - 1], bytes[i]});
            result.add(trigram);
        }
        return result;
    }

    public TrigramIndexUtils getTrigramIndexUtils() {
        return trigramIndexUtils;
    }

    @Override
    public void prepare(final List<? extends Change> changes) {
        process(changes);
    }

    @Override
    public void processChanges(final List<? extends Change> changes) {
        process(changes);
    }

    private void pushActions(final TrigramFileCounter deltas, final long timestamp) {
        cache.pushCluster(timestamp, deltas);
    }

    @Override
    public Integer getValue(final TrigramFile trigramFile, final Revision revision) {
        final var currentRevision = revisions.getCurrentRevision();
        if (revision.equals(currentRevision)) {
            return counter.get(trigramFile.trigram(), trigramFile.file());
        } else {
            checkout(revision);
            final var ans = counter.get(trigramFile.trigram(), trigramFile.file());
            checkout(currentRevision);
            return ans;
        }
    }

    @Override
    public void checkout(Revision targetRevision) {
        var currentRevision = revisions.getCurrentRevision();
        try (final var txn = env.txnWrite()) {
//            final var deltasList = new ArrayList<ByteArrIntInt>();
            while (!currentRevision.equals(targetRevision)) {
                if (currentRevision.revision() > targetRevision.revision()) {
                    cache.processDataCluster(currentRevision,
                        (bytes, file, d) -> counter.decreaseIt(txn, bytes, file, d));
                    currentRevision = revisions.getParent(currentRevision);
                } else {
                    cache.processDataCluster(targetRevision,
                        (bytes, file, d) -> counter.addIt(txn, bytes, file, d));
                    targetRevision = revisions.getParent(targetRevision);
                }
//                counter.add(txn, deltasList);
//                deltasList.clear();
            }
            txn.commit();
        }

    }


    public void process(final List<? extends Change> changes) {
        final var delta = new TrigramFileCounter();
        changes.forEach(it -> countChange(it, delta));

        delta.getAsMap().entrySet().stream()
            .filter(it -> it.getValue() == 0)
            .map(Entry::getKey)
            .toList()
            .forEach(it -> delta.getAsMap().remove(it))         ;
        counter.add(delta);
        if (!changes.isEmpty()) {
            pushActions(delta, changes.get(0).timestamp);
        }
    }

    private boolean validateFilename(final String filename) {
        return Stream.of(".java"/*, ".txt", ".kt", ".py"*/).anyMatch(filename::endsWith);
    }

    private void countChange(final Change change, final TrigramFileCounter delta) {
        if (Objects.requireNonNull(change) instanceof final AddChange addChange) {
            delta.add(addChange.place.file(), getTrigramsCount(addChange.addedString));
        } else if (change instanceof final ModifyChange modifyChange) {
            delta.decrease(modifyChange.oldFileName, getTrigramsCount(modifyChange.getOldFileContent()));
            delta.add(modifyChange.newFileName, getTrigramsCount(modifyChange.getNewFileContent()));
        } else if (change instanceof final CopyChange copyChange) {
            delta.add(copyChange.newFileName, getTrigramsCount(copyChange.getNewFileContent()));
        } else if (change instanceof final RenameChange renameChange) {
            delta.decrease(renameChange.oldFileName, getTrigramsCount(renameChange.getOldFileContent()));
            delta.add(renameChange.newFileName, getTrigramsCount(renameChange.getNewFileContent()));
        } else if (change instanceof final DeleteChange deleteChange) {
            delta.decrease(deleteChange.place.file(), getTrigramsCount(deleteChange.deletedString));
        } else {
            throw new AssertionError();
        }
    }

    public TrigramFileCounterLmdb getCounter() {
        return counter;
    }
}
