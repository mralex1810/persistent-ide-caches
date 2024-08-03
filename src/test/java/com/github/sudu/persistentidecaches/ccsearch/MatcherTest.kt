package com.github.sudu.persistentidecaches.ccsearch

import com.github.sudu.persistentidecaches.ccsearch.Matcher.NEG_INF
import com.github.sudu.persistentidecaches.ccsearch.Matcher.match
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class MatcherTest {
    fun assertMatches(pattern: String, symbol: String) {
        val res = match(pattern, symbol)
        println("$pattern $symbol $res")
        Assertions.assertTrue(res > NEG_INF)
    }

    fun assertDoesntMatch(pattern: String, symbol: String) {
        val res = match(pattern, symbol)
        println("$pattern $symbol $res")
        Assertions.assertTrue(res <= NEG_INF)
    }

    fun assertPreference(pattern1: String, pattern2: String, symbol: String) {
        val res1 = match(pattern1, symbol)
        val res2 = match(pattern2, symbol)
        println("$pattern1 $pattern2 $symbol $res1 $res2")
        Assertions.assertTrue(res1 >= res2)
    }

    fun assertNoPreference(pattern1: String, pattern2: String, symbol: String) {
        val res1 = match(pattern1, symbol)
        val res2 = match(pattern2, symbol)
        println("$pattern1 $pattern2 $symbol $res1 $res2")
        Assertions.assertEquals(res1, res2)
    }

    @Test
    fun testSimpleCases() {
        assertMatches("visit", "visitFile")
        assertMatches("N", "NameUtilTest")
        assertMatches("NU", "NameUtilTest")
        assertMatches("NUT", "NameUtilTest")
        assertMatches("NaUT", "NameUtilTest")
        assertDoesntMatch("NeUT", "NameUtilTest")
        assertDoesntMatch("NaUTa", "NameUtilTest")
        assertMatches("NaUtT", "NameUtilTest")
        assertMatches("NaUtT", "NameUtilTest")
        assertMatches("NaUtTe", "NameUtilTest")
        assertMatches("AACl", "AAClass")
        assertMatches("ZZZ", "ZZZZZZZZZZ")
    }

    @Test
    fun testEmptyPrefix() {
        assertMatches("", "")
        assertMatches("", "asdfs")
    }

    @Test
    fun testSkipWords() {
        assertMatches("nt", "NameUtilTest")
        //		assertMatches("repl map", "ReplacePathToMacroMap");
        assertMatches("replmap", "ReplacePathToMacroMap")
        assertMatches("CertificateEx", "CertificateEncodingException")

        //		assertDoesntMatch("ABCD", "AbstractButton.DISABLED_ICON_CHANGED_PROPERTY");
        assertMatches("templipa", "template_impl_template_list_panel")
        assertMatches("templistpa", "template_impl_template_list_panel")
    }

    @Test
    fun testSimpleCasesWithFirstLowercased() {
        assertMatches("N", "nameUtilTest")
        assertDoesntMatch("N", "anameUtilTest")
        assertMatches("NU", "nameUtilTest")
        assertDoesntMatch("NU", "anameUtilTest")
        assertMatches("NUT", "nameUtilTest")
        assertMatches("NaUT", "nameUtilTest")
        assertDoesntMatch("NeUT", "nameUtilTest")
        assertDoesntMatch("NaUTa", "nameUtilTest")
        assertMatches("NaUtT", "nameUtilTest")
        assertMatches("NaUtT", "nameUtilTest")
        assertMatches("NaUtTe", "nameUtilTest")
    }

    @Test
    fun testUnderscoreStyle() {
//		assertMatches("N_U_T", "NAME_UTIL_TEST");
        assertMatches("NUT", "NAME_UTIL_TEST")
        assertDoesntMatch("NUT", "NameutilTest")
    }

    @Test
    fun testAllUppercase() {
        assertMatches("NOS", "NetOutputStream")
    }

    @Test
    fun testLowerCaseWords() {
        assertMatches("uct", "unit_controller_test")
        assertMatches("unictest", "unit_controller_test")
        assertMatches("uc", "unit_controller_test")
        assertDoesntMatch("nc", "unit_controller_test")
        assertDoesntMatch("utc", "unit_controller_test")
    }

    @Test
    fun testPreferCamelHumpsToAllUppers() {
        assertPreference("ProVi", "PROVIDER", "ProjectView")
    }

    @Test
    fun testLong() {
        assertMatches(
            "Product.findByDateAndNameGreaterThanEqualsAndQualityGreaterThanEqual",
            "Product.findByDateAndNameGreaterThanEqualsAndQualityGreaterThanEqualsIntellijIdeaRulezzz"
        )
    }

    @Test
    fun testUpperCaseMatchesLowerCase() {
        assertMatches("ABC_B.C", "abc_b.c")
    }

    @Test
    fun testLowerCaseHumps() {
        assertMatches("foo", "foo")
        assertDoesntMatch("foo", "fxoo")
        assertMatches("foo", "fOo")
        assertMatches("foo", "fxOo")
        assertMatches("foo", "fXOo")
        assertMatches("fOo", "foo")
        //	    assertDoesntMatch("fOo", "FaOaOaXXXX");
        assertMatches("ncdfoe", "NoClassDefFoundException")
        assertMatches("fob", "FOO_BAR")
        assertMatches("fo_b", "FOO_BAR")
        assertMatches("fob", "FOO BAR")
        assertMatches("fo b", "FOO BAR")
        assertMatches("AACl", "AAClass")
        assertMatches("ZZZ", "ZZZZZZZZZZ")
        assertMatches("em", "emptyList")
        assertMatches("bui", "BuildConfig.groovy")
        assertMatches("buico", "BuildConfig.groovy")

        //	    assertMatches("buico.gr", "BuildConfig.groovy");
//	    assertMatches("bui.gr", "BuildConfig.groovy");
//	    assertMatches("*fz", "azzzfzzz");
        assertMatches("WebLogic", "Weblogic")
        assertMatches("WebLOgic", "WebLogic")
        assertMatches("WEbLogic", "WebLogic")
        assertDoesntMatch("WebLogic", "Webologic")

        assertMatches("Wlo", "WebLogic")
    }

    @Test
    fun testDigits() {
        assertMatches("foba4", "FooBar4")
        assertMatches("foba", "Foo4Bar")
        //		assertMatches("*TEST-* ", "TEST-001");
//		assertMatches("*TEST-0* ", "TEST-001");
//		assertMatches("*v2 ", "VARCHAR2");
        assertMatches("smart8co", "SmartType18CompletionTest")
        assertMatches("smart8co", "smart18completion")
    }

    @Test
    fun testMatchingDegree() {
        assertPreference("jscote", "JsfCompletionTest", "JSCompletionTest")
        assertPreference("OCO", "OneCoolObject", "OCObject")
        assertPreference("MUp", "MavenUmlProvider", "MarkUp")
        assertPreference("MUP", "MarkUp", "MavenUmlProvider")
        assertPreference("CertificateExce", "CertificateEncodingException", "CertificateException")
        assertPreference("boo", "Boolean", "boolean")
        assertPreference("Boo", "boolean", "Boolean")
        assertPreference("getCU", "getCurrentSomething", "getCurrentUser")
        assertPreference("cL", "class", "coreLoader")
        assertPreference("cL", "class", "classLoader")
        assertPreference("inse", "InstrumentationError", "intSet")
        assertPreference("String", "STRING", "String")
    }

    @Test
    fun testPreferNoWordSkipping() {
        assertPreference("CBP", "CustomProcessBP", "ComputationBatchProcess")
    }

    @Test
    fun testMatchStartDoesntMatterForDegree() {
        assertNoPreference(" path", "getAbsolutePath", "findPath")
    }

    @Test
    fun testCapsMayMatchNonCaps() {
        assertMatches("PDFRe", "PdfRenderer")
    }

    @Test
    fun testACapitalAfterAnotherCapitalMayMatchALowercaseLetterBecauseShiftWasAccidentallyHeldTooLong() {
        assertMatches("USerDefa", "UserDefaults")
        assertMatches("NSUSerDefa", "NSUserDefaults")
        assertMatches("NSUSER", "NSUserDefaults")
        assertMatches("NSUSD", "NSUserDefaults")
        assertMatches("NSUserDEF", "NSUserDefaults")
    }

    @Test
    fun testCamelHumpWinsOverConsecutiveCaseMismatch() {
        assertPreference("GEN", "GetName", "GetExtendedName")
    }

    @Test
    fun testLowerCaseAfterCamels() {
        assertMatches("LSTMa", "LineStatusTrackerManager")
    }
}