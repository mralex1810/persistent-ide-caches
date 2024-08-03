package com.github.sudu.persistentidecaches.changes

import java.nio.file.Path
import java.util.function.Supplier

class ModifyChange(
    timestamp: Long, oldFileGetter: Supplier<String>, newFileGetter: Supplier<String>, oldFile: Path?,
    newFile: Path?
) : FileHolderChange(timestamp, oldFileGetter, newFileGetter, oldFile!!, newFile!!) {
    override fun toString(): String {
        return "ModifyChange{" +
                "} " + super.toString()
    }
}
