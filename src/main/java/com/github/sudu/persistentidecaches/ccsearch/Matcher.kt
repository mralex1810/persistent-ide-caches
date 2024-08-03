package com.github.sudu.persistentidecaches.ccsearch

import java.util.*

object Matcher {
    const val NEG_INF: Int = -100000
    const val FIRST_SYMBOL: Int = 1000
    const val HUMP_CASE_MATCH: Int = 50
    const val HUMP_SKIP: Int = 10

    @JvmStatic
    fun match(pattern: String, symbol: String): Int {
        val symbolLC = symbol.lowercase(Locale.getDefault())
        val patternLC = pattern.lowercase(Locale.getDefault())

        val n = symbol.length
        val m = pattern.length

        val isHump = BooleanArray(n)

        val humps = ArrayList<Int>()
        for (i in 0 until n) {
            if (i == 0 || Character.isUpperCase(symbol[i]) || Character.isDigit(symbol[i])
                || (symbol[i - 1] == '_' && symbol[i] != '_')
            ) {
                humps.add(i)
                isHump[i] = true
            }
        }
        val h = humps.size

        var curH = 0

        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) {
            for (j in 0..m) {
                dp[i][j] = NEG_INF
            }
        }
        dp[0][0] = 0 // dp[i][j] - best score for prefixes of size i and j, where characters i and j

        // are taken
        for (i in 0 until n) {
            for (j in 0 until m) {
                if (dp[i][j] == NEG_INF) {
                    continue
                }
                if (symbolLC[i] == patternLC[j]) {
                    val value =
                        dp[i][j] + (if (symbol[i] == pattern[j]) 1 else 0) * (if (isHump[i]) HUMP_CASE_MATCH else 1) + (if (i == 0) FIRST_SYMBOL else 0)
                    if (value > dp[i + 1][j + 1]) {
                        dp[i + 1][j + 1] = value
                    }
                }
                for (k in curH until h) {
                    val pos = humps[k]
                    if (symbolLC[pos] == patternLC[j]) {
                        val value = (dp[i][j] + (if (symbol[pos] == pattern[j]) HUMP_CASE_MATCH else 0)
                                - HUMP_SKIP * (curH - k))
                        if (value > dp[pos + 1][j + 1]) {
                            dp[pos + 1][j + 1] = value
                        }
                    }
                }
            }
            if (isHump[i]) {
                curH++
            }
        }

        var ans = NEG_INF
        for (i in 0..n) {
            if (dp[i][m] >= ans) {
                ans = dp[i][m]
            }
        }
        return ans - h
    }

    @JvmStatic
    fun letters(pattern: String, symbol: String): IntArray {
        val symbolLC = symbol.lowercase(Locale.getDefault())
        val patternLC = pattern.lowercase(Locale.getDefault())

        val n = symbol.length
        val m = pattern.length

        val isHump = BooleanArray(n)

        val humps = ArrayList<Int>()
        for (i in 0 until n) {
            if (i == 0 || Character.isUpperCase(symbol[i]) || Character.isDigit(symbol[i])
                || (symbol[i - 1] == '_' && symbol[i] != '_')
            ) {
                humps.add(i)
                isHump[i] = true
            }
        }
        val h = humps.size

        var curH = 0

        val dp = Array(n + 1) { IntArray(m + 1) }
        val prev = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) {
            for (j in 0..m) {
                dp[i][j] = NEG_INF
                prev[i][j] = -1
            }
        }
        dp[0][0] = 0 // dp[i][j] - best score for prefixes of size i and j, where characters i and j

        // are taken
        for (i in 0 until n) {
            for (j in 0 until m) {
                if (dp[i][j] == NEG_INF) {
                    continue
                }
                if (symbolLC[i] == patternLC[j]) {
                    val value =
                        dp[i][j] + (if (symbol[i] == pattern[j]) 1 else 0) * (if (isHump[i]) HUMP_CASE_MATCH else 1) + (if (i == 0) FIRST_SYMBOL else 0)
                    if (value > dp[i + 1][j + 1]) {
                        dp[i + 1][j + 1] = value
                        prev[i + 1][j + 1] = i
                    }
                }
                for (k in curH until h) {
                    val pos = humps[k]
                    if (symbolLC[pos] == patternLC[j]) {
                        val value = (dp[i][j] + (if (symbol[pos] == pattern[j]) HUMP_CASE_MATCH else 0)
                                - HUMP_SKIP * (curH - k))
                        if (value > dp[pos + 1][j + 1]) {
                            dp[pos + 1][j + 1] = value
                            prev[pos + 1][j + 1] = i
                        }
                    }
                }
            }
            if (isHump[i]) {
                curH++
            }
        }

        var ans = NEG_INF
        var curI = -1
        for (i in 0..n) {
            if (dp[i][m] >= ans) {
                ans = dp[i][m]
                curI = i
            }
        }
        val answer = ArrayList<Int>()
        var curJ = m
        while (curI > 0) {
            answer.add(curI - 1)
            curI = prev[curI][curJ]
            curJ--
        }
        val res = IntArray(answer.size)
        for (i in res.indices) {
            res[i] = answer[res.size - 1 - i]
        }
        return res // score = ans - h
    }
}
