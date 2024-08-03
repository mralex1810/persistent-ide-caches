package com.github.sudu.persistentidecaches.records

import java.nio.file.Path

data class TrigramFileInteger(val trigram: Trigram, val file: Path, val value: Int)
