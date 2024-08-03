package com.github.sudu.persistentidecaches.symbols

import java.util.stream.Stream

data class Symbols(
    val classOrInterfaceSymbols: List<String>,
    val fieldSymbols: List<String>,
    val methodSymbols: List<String>
) {
    fun concatedStream(): Stream<String> {
        return Stream.concat(
            Stream.concat(
                classOrInterfaceSymbols.stream(), fieldSymbols.stream()
            ),
            methodSymbols.stream()
        )
    }
}
