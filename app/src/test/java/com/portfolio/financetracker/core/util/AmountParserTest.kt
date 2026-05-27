package com.portfolio.financetracker.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AmountParser].
 *
 * These tests are pure JVM — no Android dependencies, no mocking needed.
 * They document the exact contract of the parser and guard against regressions.
 */
class AmountParserTest {

    // ── Success cases ─────────────────────────────────────────────────────────

    @Test fun parse_plainInteger_succeeds() =
        assertAmount(1000.0, "1000")

    @Test fun parse_decimalAmount_succeeds() =
        assertAmount(1500.0, "1500.00")

    @Test fun parse_commaFormatted_succeeds() =
        assertAmount(1500.0, "1,500.00")

    @Test fun parse_largeCommaFormatted_succeeds() =
        assertAmount(12345.67, "12,345.67")

    @Test fun parse_etbPrefix_stripped() =
        assertAmount(250.0, "ETB 250.00")

    @Test fun parse_etbPrefixNoSpace_stripped() =
        assertAmount(250.0, "ETB250.00")

    @Test fun parse_birrPrefix_stripped() =
        assertAmount(100.0, "Birr 100.00")

    @Test fun parse_leadingWhitespace_trimmed() =
        assertAmount(300.0, "  300.00")

    @Test fun parse_trailingWhitespace_trimmed() =
        assertAmount(300.0, "300.00  ")

    @Test fun parse_negativeSign_stripped() =
        assertAmount(500.0, "-500.00")

    @Test fun parse_trailingText_ignored() {
        // This was the root cause of the "33" bug — trailing text after amount
        // must be safely ignored, not cause a parse failure
        assertAmount(1500.0, "1,500.00.Available")
        assertAmount(250.0, "250.00 balance")
        assertAmount(100.0, "100.00ETB")
    }

    @Test fun parse_smallAmount_succeeds() =
        assertAmount(0.01, "0.01")

    @Test fun parse_maxReasonableAmount_succeeds() =
        assertAmount(9_999_999.0, "9,999,999.00")

    // ── Failure cases ─────────────────────────────────────────────────────────

    @Test fun parse_emptyString_fails() =
        assertFailure("")

    @Test fun parse_blankString_fails() =
        assertFailure("   ")

    @Test fun parse_zeroAmount_fails() =
        assertFailure("0.00")

    @Test fun parse_zeroInteger_fails() =
        assertFailure("0")

    @Test fun parse_pureLetters_fails() =
        assertFailure("abc")

    @Test fun parse_unreasonablyLarge_fails() =
        assertFailure("99,999,999.00")

    @Test fun parse_onlyEtbPrefix_fails() =
        assertFailure("ETB")

    // ── toSafeAmount extension ────────────────────────────────────────────────

    @Test fun toSafeAmount_validAmount_returnsDouble() {
        val result = with(AmountParser) { "1,500.00".toSafeAmount() }
        assertEquals(1500.0, result!!, 0.001)
    }

    @Test fun toSafeAmount_invalidAmount_returnsNull() {
        val result = with(AmountParser) { "abc".toSafeAmount() }
        assertNull(result)
    }

    @Test fun toSafeAmount_zeroAmount_returnsNull() {
        val result = with(AmountParser) { "0.00".toSafeAmount() }
        assertNull(result)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun assertAmount(expected: Double, raw: String) {
        val result = AmountParser.parse(raw)
        assertTrue(
            "Expected Success($expected) for input '$raw' but got $result",
            result is AmountParser.AmountResult.Success
        )
        assertEquals(
            "Amount mismatch for input '$raw'",
            expected,
            (result as AmountParser.AmountResult.Success).amount,
            0.001
        )
    }

    private fun assertFailure(raw: String) {
        val result = AmountParser.parse(raw)
        assertTrue(
            "Expected Failure for input '$raw' but got $result",
            result is AmountParser.AmountResult.Failure
        )
    }
}
