package com.github.sudu.persistentidecaches.records

import java.nio.file.Path

data class TrigramFile(val trigram: Trigram, val file: Path) {
}
