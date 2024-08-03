package com.github.sudu.persistentidecaches.utils

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object FileUtils {
    fun createParentDirectories(vararg paths: Path) {
        paths.forEach { createParentDirectories(it) }
    }

    fun createParentDirectories(path: Path) {
        try {
            Files.createDirectories(path)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
