package com.github.sudu.persistentidecaches.utils

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object FileUtils {
    @JvmStatic
    fun createParentDirectories(vararg paths: Path) {
        Arrays.stream(paths).forEach { createParentDirectories() }
    }

    @JvmStatic
    fun createParentDirectories(path: Path) {
        try {
            Files.createDirectories(path)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
