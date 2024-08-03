package com.github.sudu.persistentidecaches.records

import java.nio.file.Path

@JvmRecord
data class FilePointer(@JvmField val file: Path, val offset: Int)
