package com.github.SuduIDE.persistentidecaches;

import com.github.SuduIDE.persistentidecaches.lmdb.LmdbInt2Path;
import com.github.SuduIDE.persistentidecaches.lmdb.LmdbString2Int;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class FileCache {

    private final String FILES = "files";
    private final LmdbInt2Path filesInProject;
    private final Map<Path, Integer> reverseFilesInProject = new HashMap<>();
    private final LmdbString2Int variables;

    public FileCache(final LmdbInt2Path filesInProject, final LmdbString2Int variables) {
        this.filesInProject = filesInProject;
        this.variables = variables;
    }

    public int getNumber(final Path path) {
        return reverseFilesInProject.get(path);
    }

    public Path getFile(final int fileNum) {
        return filesInProject.get(fileNum);
    }

    public void tryRegisterNewFile(final Path path) {
        if (reverseFilesInProject.get(path) == null) {
            final var fileNum = variables.get(FILES);
            filesInProject.put(fileNum, path);
            reverseFilesInProject.put(path, fileNum);
            variables.put(FILES, fileNum + 1);
        }
    }

    public void restoreFilesFromDB() {
        filesInProject.forEach((integer, file) -> reverseFilesInProject.put(file, integer));
    }

    public void initFiles() {
        if (variables.get(FILES) == -1) {
            variables.put(FILES, 0);
        }
    }

    public void forEach(final BiConsumer<Path, Number> consumer) {
        reverseFilesInProject.forEach(consumer);
    }
}
