package com.github.sudu.persistentidecaches.symbols

import java.util.stream.Stream

data class Symbols(
    val classOrInterfaceSymbols: MutableList<String>,
    val fieldSymbols: MutableList<String>,
    val methodSymbols: MutableList<String>
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
