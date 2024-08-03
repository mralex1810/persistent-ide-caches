package com.github.sudu.persistentidecaches.ccsearch

import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.symbols.Symbols
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.stream.Stream

internal class CamelCaseIndexTest {
    @Test
    fun symbolsFromStringTest() {
        val javaAClass = """
            public class MyBestClass {
                public final int myCuteInt;
                public void myBarMethod() {
                }
            }
        """.trimIndent()

        val expectedSymbols = Symbols(
            mutableListOf("MyBestClass"),
            mutableListOf("myCuteInt"),
            mutableListOf("myBarMethod")
        )

        Assertions.assertEquals(expectedSymbols, CamelCaseIndex.getSymbolsFromString(javaAClass))
    }

    @Test
    fun symbolsFromStringTestSomeRealFile() {
        val javaAClass = """
            package info.kgeorgiy.ja.chulkov.walk;
                            
            import java.io.Closeable;
            import java.io.IOException;
            import java.io.Writer;
            import java.nio.file.Path;
            import java.util.HexFormat;
                            
            public class HashResultsHandler implements Closeable {
                            
                private static final String ERROR_HASH_HEX = "0".repeat(64);
                private final Writer writer;
                            
                public HashResultsHandler(final Writer writer) {
                    this.writer = writer;
                }
                            
                public void processSuccess(final Path file, final byte[] hash) throws IOException {
                    processResult(HexFormat.of().formatHex(hash), file.toString());
                }
                            
                public void processError(final String path) throws IOException {
                    processResult(ERROR_HASH_HEX, path);
                }
                            
                private void processResult(final String hexHash, final String path) throws IOException {
                    writer.write(hexHash + " " + path + System.lineSeparator());
                }
                            
                @Override
                public void close() throws IOException {
                    writer.close();
                }
            }
        """.trimIndent()

        val expectedSymbols = Symbols(
            mutableListOf("HashResultsHandler"),
            mutableListOf("ERROR_HASH_HEX", "writer"),
            mutableListOf("processSuccess", "processError", "processResult", "close")
        )

        Assertions.assertEquals(expectedSymbols, CamelCaseIndex.getSymbolsFromString(javaAClass))
    }

    @Test
    fun isCamelCaseTest() {
        Assertions.assertTrue(CamelCaseIndex.isCamelCase("Test"))
        Assertions.assertTrue(CamelCaseIndex.isCamelCase("True"))
        Assertions.assertTrue(CamelCaseIndex.isCamelCase("TestThisCamel"))
        Assertions.assertTrue(CamelCaseIndex.isCamelCase("CamelCaseSearch"))
        Assertions.assertTrue(CamelCaseIndex.isCamelCase("CamelCaseField"))
        Assertions.assertFalse(CamelCaseIndex.isCamelCase("NOT_CAMEL_CASE"))
        Assertions.assertFalse(CamelCaseIndex.isCamelCase("Bad\$Symbol"))
    }

    @Test
    fun interestTrigramsTest() {
        val actualTrigrams = CamelCaseIndex.getInterestTrigrams("ItCamelCase").stream().sorted().toList()
        val expectedTrigrams = Stream.of(
            "\$It",
            "\$IC",
            "ItC",
            "ICa",
            "ICC",
            "tCa",
            "tCC",
            "Cam",
            "CaC",
            "CCa",
            "ame",
            "amC",
            "aCa",
            "mel",
            "meC",
            "mCa",
            "elC",
            "eCa",
            "lCa",
            "Cas",
            "ase"
        )
            .map { Trigram(it.toByteArray()) }
            .sorted()
            .toList()

        Assertions.assertEquals(expectedTrigrams, actualTrigrams)
    }

    @Test
    fun interestTrigramsTest2() {
        val actualTrigrams = CamelCaseIndex.getInterestTrigrams("writer").stream().sorted().toList()
        val expectedTrigrams = Stream.of(
            "\$wr", "wri", "rit", "ite", "ter"
        )
            .map { obj: String -> obj.toByteArray() }
            .map { trigram: ByteArray? -> Trigram(trigram!!) }
            .sorted()
            .toList()

        Assertions.assertEquals(expectedTrigrams, actualTrigrams)
    }
}