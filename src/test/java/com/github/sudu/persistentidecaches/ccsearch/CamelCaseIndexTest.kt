package com.github.sudu.persistentidecaches.ccsearch

import com.github.sudu.persistentidecaches.records.Trigram
import com.github.sudu.persistentidecaches.symbols.Symbols
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.stream.Stream

internal class CamelCaseIndexTest {
    @get:Test
    val symbolsFromStringTest: Unit
        get() {
            val javaAClass = """
                public class MyBestClass {
                    public final int myCuteInt;
                    public void myBarMethod() {
                    }
                }
                
                """.trimIndent()
            Assertions.assertEquals(
                CamelCaseIndex.getSymbolsFromString(javaAClass), Symbols(
                    listOf("MyBestClass"),
                    listOf("myCuteInt"),
                    listOf("myBarMethod")
                )
            )
        }

    @get:Test
    val symbolsFromStringTestSomeRealFile: Unit
        get() {
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
            Assertions.assertEquals(
                CamelCaseIndex.getSymbolsFromString(javaAClass), Symbols(
                    listOf("HashResultsHandler"),
                    listOf("ERROR_HASH_HEX", "writer"),
                    listOf("processSuccess", "processError", "processResult", "close")
                )
            )
        }


    @get:Test
    val isCamelCaseTest: Unit
        get() {
            Assertions.assertTrue(CamelCaseIndex.isCamelCase("Test"))
            Assertions.assertTrue(CamelCaseIndex.isCamelCase("True"))
            Assertions.assertTrue(CamelCaseIndex.isCamelCase("TestThisCamel"))
            Assertions.assertTrue(CamelCaseIndex.isCamelCase("CamelCaseSearch"))
            Assertions.assertTrue(CamelCaseIndex.isCamelCase("CamelCaseField"))
            Assertions.assertFalse(CamelCaseIndex.isCamelCase("NOT_CAMEL_CASE"))
            Assertions.assertFalse(CamelCaseIndex.isCamelCase("Bad\$Symbol"))
        }

    @get:Test
    val interestTrigramsTest: Unit
        get() {
            Assertions.assertEquals(CamelCaseIndex.getInterestTrigrams("ItCamelCase").stream().sorted().toList(),
                Stream.of(
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
                ).map { obj: String -> obj.toByteArray() }
                    .map { trigram: ByteArray? -> Trigram(trigram!!) }.sorted().toList()
            )
        }

    @get:Test
    val interestTrigramsTest2: Unit
        get() {
            Assertions.assertEquals(CamelCaseIndex.getInterestTrigrams("writer").stream().sorted().toList(),
                Stream.of(
                    "\$wr",
                    "wri",
                    "rit",
                    "ite",
                    "ter"
                ).map { obj: String -> obj.toByteArray() }
                    .map { trigram: ByteArray? -> Trigram(trigram!!) }.sorted().toList()
            )
        }
}