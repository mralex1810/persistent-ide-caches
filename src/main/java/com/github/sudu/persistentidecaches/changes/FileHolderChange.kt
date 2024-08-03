package com.github.sudu.persistentidecaches.changes

import java.nio.file.Path
import java.util.function.Supplier

abstract class FileHolderChange(
    timestamp: Long, val oldFileGetter: Supplier<String>, val newFileGetter: Supplier<String>,
    val oldFileName: Path, val newFileName: Path
) : Change(timestamp) {
    val oldFileContent: String
        get() = oldFileGetter.get()

    val newFileContent: String
        get() = newFileGetter.get()
}
