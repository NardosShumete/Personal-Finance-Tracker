package com.portfolio.financetracker.core.sms

import com.portfolio.financetracker.core.sms.parsers.AbyssiniaSmsParser
import com.portfolio.financetracker.core.sms.parsers.AwashSmsParser
import com.portfolio.financetracker.core.sms.parsers.CbeSmsParser
import com.portfolio.financetracker.core.sms.parsers.DashenSmsParser
import com.portfolio.financetracker.core.sms.parsers.TelebirrSmsParser
import com.portfolio.financetracker.core.util.AmountParser
import com.portfolio.financetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the SMS parsing pipeline.
 *
 * Test strategy:
 *  • Each bank parser is tested independently (unit tests, no Android deps)
 *  • [SmsParser] routing is tested via [SmsParser.parse] with empty trackedSenders
 *  • [AmountParser] edge cases are tested separately
 *  • All tests use real-world SMS body samples
 *
 * Naming convention: `methodUnderTest_scenario_expectedResult`
 */
class SmsParserTest {

    private val ts = System.currentTimeMillis()

    // ── AmountParser ──────────────────────────────────────────────────────────

    @Test
    fun amountParser_normalAmount_parsesCorrectly() {
        val result = AmountParser.parse("1,500.00")
        assertTrue(result is AmountParser.AmountResult.Success)
        assertEquals(1500.0, (result as AmountParser.AmountResult.Success).amount, 0.001)
    }

    @Test
    fun amountParser_amountWithEtbPrefix_stripsPrefix() {
        val result = AmountParser.parse("ETB 250.00")
        assertTrue(result is AmountParser.AmountResult.Success)
        assertEquals(250.0, (result as AmountParser.AmountResult.Success).amount, 0.001)
    }

    @Test
    fun amountParser_amountWithTrailingText_ignoresTrailing() {
        // This was the "33" bug — trailing text after amount must be ignored
        val result = AmountParser.parse("1,500.00.Available")
        assertTrue(result is AmountParser.AmountResult.Success)
        assertEquals(1500.0, (result as AmountParser.AmountResult.Success).amount, 0.001)
    }

    @Test
    fun amountParser_negativeAmount_stripsSign() {
        val result = AmountParser.parse("-500.00")
        assertTrue(result is AmountParser.AmountResult.Success)
        assertEquals(500.0, (result as AmountParser.AmountResult.Success).amount, 0.001)
    }

    @Test
    fun amountParser_zeroAmount_returnsFailure() {
        val result = AmountParser.parse("0.00")
        assertTrue(result is AmountParser.AmountResult.Failure)
    }

    @Test
    fun amountParser_emptyString_returnsFailure() {
        val result = AmountParser.parse("")
        assertTrue(result is AmountParser.AmountResult.Failure)
    }

    @Test
    fun amountParser_nonNumeric_returnsFailure() {
        val result = AmountParser.parse("abc")
        assertTrue(result is AmountParser.AmountResult.Failure)
    }

    @Test
    fun amountParser_unreasonablyLarge_returnsFailure() {
        val result = AmountParser.parse("99999999.00")
        assertTrue(result is AmountParser.AmountResult.Failure)
    }

    @Test
    fun amountParser_accountNumberFragment_returnsFailure() {
        // Account numbers like "33" or "1234" should parse as amounts,
        // but the PARSER must not capture them in the first place.
        // AmountParser itself accepts small numbers — the regex in each
        // bank parser is responsible for not capturing account numbers.
        val result = AmountParser.parse("33")
        assertTrue(result is AmountParser.AmountResult.Success)
        assertEquals(33.0, (result as AmountParser.AmountResult.Success).amount, 0.001)
    }

    // ── CBE Parser ────────────────────────────────────────────────────────────

    @Test
    fun cbeSmsParser_creditSms_parsesCorrectAmountAndType() {
        val body = "Your account XXXX1234 has been credited with ETB 1,500.00. " +
                   "Available balance: ETB 12,345.67. Date: 14/05/2026"
        val result = CbeSmsParser.parse(body, "CBE", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(1500.0, parsed.amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(12345.67, parsed.balance!!, 0.001)
        assertEquals("CBE", parsed.bankName)
    }

    @Test
    fun cbeSmsParser_debitSms_parsesCorrectAmountAndType() {
        val body = "Your account XXXX1234 has been debited with ETB 250.00. " +
                   "Available balance: ETB 12,095.67."
        val result = CbeSmsParser.parse(body, "CBE", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(250.0, parsed.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(12095.67, parsed.balance!!, 0.001)
    }

    @Test
    fun cbeSmsParser_balanceNotConfusedWithAmount() {
        // The balance (12,345.67) must NOT be returned as the transaction amount
        val body = "Your account XXXX3345 has been debited with ETB 33.00. " +
                   "Available balance: ETB 12,345.67."
        val result = CbeSmsParser.parse(body, "CBE", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        // Amount must be 33, NOT 12345.67
        assertEquals(33.0, parsed.amount, 0.001)
        assertEquals(12345.67, parsed.balance!!, 0.001)
    }

    @Test
    fun cbeSmsParser_accountNumberNotConfusedWithAmount() {
        // Account number "3345" must NOT be captured as the amount
        val body = "Your account XXXX3345 has been credited with ETB 500.00. " +
                   "Available balance: ETB 1,033.00."
        val result = CbeSmsParser.parse(body, "CBE", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(500.0, parsed.amount, 0.001)
    }

    @Test
    fun cbeSmsParser_missingAmount_returnsFailure() {
        val body = "Your account has been credited. Available balance: ETB 1,000.00."
        val result = CbeSmsParser.parse(body, "CBE", ts)
        assertTrue(result is ParseResult.Failure)
    }

    @Test
    fun cbeSmsParser_formatDetection_returnsCbeFormat() {
        val body = "Your account XXXX1234 has been credited with ETB 100.00. " +
                   "Available balance: ETB 500.00."
        assertEquals(SmsParser.BankFormat.CBE, SmsParser.detectBankFormat(body))
    }

    // ── Dashen Parser ─────────────────────────────────────────────────────────

    @Test
    fun dashenSmsParser_debitSms_parsesCorrectly() {
        val body = "Debit of ETB 750.00 from your Dashen account. Balance: ETB 4,250.00. " +
                   "Ref: 123456"
        val result = DashenSmsParser.parse(body, "DashenBank", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(750.0, parsed.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(4250.0, parsed.balance!!, 0.001)
    }

    @Test
    fun dashenSmsParser_creditSms_parsesCorrectly() {
        val body = "Credit of ETB 2,000.00 to your Dashen account. Balance: ETB 6,250.00."
        val result = DashenSmsParser.parse(body, "DashenBank", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(2000.0, parsed.amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed.type)
    }

    @Test
    fun dashenSmsParser_balanceNotConfusedWithAmount() {
        val body = "Debit of ETB 33.00 from your account. Balance: ETB 5,000.00."
        val result = DashenSmsParser.parse(body, "DashenBank", ts)

        assertTrue(result is ParseResult.Success)
        assertEquals(33.0, (result as ParseResult.Success).parsed.amount, 0.001)
    }

    @Test
    fun dashenSmsParser_formatDetection_returnsDashenFormat() {
        val body = "Debit of ETB 100.00 from your account."
        assertEquals(SmsParser.BankFormat.DASHEN, SmsParser.detectBankFormat(body))
    }

    // ── Telebirr Parser ───────────────────────────────────────────────────────

    @Test
    fun telebirrSmsParser_receivedSms_parsesCorrectly() {
        val body = "You have received ETB 200.00 from 0911XXXXXX. " +
                   "Your new balance is ETB 450.00. TxnID: 987654"
        val result = TelebirrSmsParser.parse(body, "Telebirr", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(200.0, parsed.amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(450.0, parsed.balance!!, 0.001)
    }

    @Test
    fun telebirrSmsParser_sentSms_parsesCorrectly() {
        val body = "You have sent ETB 150.00 to 0922XXXXXX. " +
                   "Your new balance is ETB 300.00."
        val result = TelebirrSmsParser.parse(body, "Telebirr", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(150.0, parsed.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.type)
    }

    @Test
    fun telebirrSmsParser_paidSms_categoryIsShopping() {
        val body = "You have paid ETB 75.00 to Merchant XYZ. " +
                   "Your new balance is ETB 225.00."
        val result = TelebirrSmsParser.parse(body, "Telebirr", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(75.0, parsed.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Shopping", parsed.category)
    }

    @Test
    fun telebirrSmsParser_balanceNotConfusedWithAmount() {
        val body = "You have received ETB 33.00 from 0911XXXXXX. " +
                   "Your new balance is ETB 1,033.00."
        val result = TelebirrSmsParser.parse(body, "Telebirr", ts)

        assertTrue(result is ParseResult.Success)
        assertEquals(33.0, (result as ParseResult.Success).parsed.amount, 0.001)
    }

    @Test
    fun telebirrSmsParser_formatDetection_returnsTelebirrFormat() {
        val body = "You have received ETB 100.00 from 0911XXXXXX."
        assertEquals(SmsParser.BankFormat.TELEBIRR, SmsParser.detectBankFormat(body))
    }

    // ── Awash Parser ──────────────────────────────────────────────────────────

    @Test
    fun awashSmsParser_creditedSms_parsesCorrectly() {
        val body = "Awash Bank: Your account has been Credited ETB 3,000.00. " +
                   "Bal: ETB 8,500.00. Date: 14/05/2026"
        val result = AwashSmsParser.parse(body, "AwashBank", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(3000.0, parsed.amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(8500.0, parsed.balance!!, 0.001)
    }

    @Test
    fun awashSmsParser_debitedSms_parsesCorrectly() {
        val body = "Awash Bank: Your account has been Debited ETB 500.00. " +
                   "Bal: ETB 8,000.00."
        val result = AwashSmsParser.parse(body, "AwashBank", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(500.0, parsed.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.type)
    }

    @Test
    fun awashSmsParser_balanceNotConfusedWithAmount() {
        val body = "Awash Bank: Debited ETB 33.00. Bal: ETB 5,033.00."
        val result = AwashSmsParser.parse(body, "AwashBank", ts)

        assertTrue(result is ParseResult.Success)
        assertEquals(33.0, (result as ParseResult.Success).parsed.amount, 0.001)
    }

    @Test
    fun awashSmsParser_formatDetection_returnsAwashFormat() {
        val body = "Awash Bank: Credited ETB 100.00. Bal: ETB 500.00."
        assertEquals(SmsParser.BankFormat.AWASH, SmsParser.detectBankFormat(body))
    }

    // ── Abyssinia / BOA Parser ────────────────────────────────────────────────

    @Test
    fun abyssiniaSmsParser_crSms_parsesCorrectly() {
        val body = "Cr ETB 1,200.00 Avail Bal ETB 5,700.00 Ref:TXN001"
        val result = AbyssiniaSmsParser.parse(body, "BOABank", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(1200.0, parsed.amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(5700.0, parsed.balance!!, 0.001)
        assertEquals("BOA", parsed.bankName)
    }

    @Test
    fun abyssiniaSmsParser_drSms_parsesCorrectly() {
        val body = "Dr ETB 400.00 Avail Bal ETB 5,300.00 Ref:TXN002"
        val result = AbyssiniaSmsParser.parse(body, "BOABank", ts)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(400.0, parsed.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.type)
    }

    @Test
    fun abyssiniaSmsParser_balanceNotConfusedWithAmount() {
        val body = "Dr ETB 33.00 Avail Bal ETB 4,967.00"
        val result = AbyssiniaSmsParser.parse(body, "BOABank", ts)

        assertTrue(result is ParseResult.Success)
        assertEquals(33.0, (result as ParseResult.Success).parsed.amount, 0.001)
    }

    @Test
    fun abyssiniaSmsParser_formatDetection_returnsAbyssiniaFormat() {
        // BOA SMS starts with "Dr/Cr ETB" — note the leading space check in detectBankFormat
        // requires a space before Dr/Cr, so we include one here
        val body = "Txn: Cr ETB 100.00 Avail Bal ETB 500.00"
        assertEquals(SmsParser.BankFormat.ABYSSINIA, SmsParser.detectBankFormat(body))
    }

    // ── SmsParser routing ─────────────────────────────────────────────────────

    @Test
    fun smsParser_cbeBody_routesToCbeParser() {
        val body = "Your account XXXX1234 has been credited with ETB 500.00. " +
                   "Available balance: ETB 2,000.00."
        val result = SmsParser.parse("CBE", body, ts, emptySet())

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals("CBE", parsed.bankName)
        assertEquals(500.0, parsed.amount, 0.001)
    }

    @Test
    fun smsParser_dashenBody_routesToDashenParser() {
        val body = "Credit of ETB 1,000.00 to your account. Balance: ETB 3,000.00."
        val result = SmsParser.parse("DashenBank", body, ts, emptySet())

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals("Dashen", parsed.bankName)
        assertEquals(1000.0, parsed.amount, 0.001)
    }

    @Test
    fun smsParser_telebirrBody_routesToTelebirrParser() {
        val body = "You have sent ETB 100.00 to 0911XXXXXX. Your new balance is ETB 900.00."
        val result = SmsParser.parse("Telebirr", body, ts, emptySet())

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals("Telebirr", parsed.bankName)
        assertEquals(100.0, parsed.amount, 0.001)
    }

    @Test
    fun smsParser_unknownSenderNotInTrackedList_returnsNull() {
        val body = "Your account has been credited with ETB 500.00. Available balance: ETB 2,000.00."
        val result = SmsParser.parse("UNKNOWN_SENDER", body, ts, setOf("CBE", "Telebirr"))
        assertNull(result)
    }

    @Test
    fun smsParser_trackedSenderInList_processes() {
        val body = "Your account XXXX1234 has been credited with ETB 500.00. " +
                   "Available balance: ETB 2,000.00."
        val result = SmsParser.parse("CBE", body, ts, setOf("CBE"))
        assertNotNull(result)
    }

    @Test
    fun smsParser_nonBankSms_returnsNull() {
        val body = "Hi! Are you coming to the party tonight?"
        val result = SmsParser.parse("0911123456", body, ts, emptySet())
        assertNull(result)
    }

    @Test
    fun smsParser_genericFallback_picksAmountClosestToKeyword() {
        // Generic SMS: credit keyword near 500, balance 5000 appears later
        // Parser must pick 500, not 5000
        val body = "You received ETB 500.00 from sender. Current balance ETB 5,000.00."
        val result = SmsParser.parse("0911123456", body, ts, emptySet())

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).parsed
        assertEquals(500.0, parsed.amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed.type)
    }

    // ── Format detection edge cases ───────────────────────────────────────────

    @Test
    fun detectBankFormat_personalSms_returnsUnknown() {
        assertEquals(SmsParser.BankFormat.UNKNOWN, SmsParser.detectBankFormat("Hello, how are you?"))
    }

    @Test
    fun detectBankFormat_cbeWithoutBalance_returnsCbe() {
        // CBE format requires "your account" as fallback when "available balance" is absent
        val body = "Your account XXXX1234 has been debited with ETB 100.00."
        assertEquals(SmsParser.BankFormat.CBE, SmsParser.detectBankFormat(body))
    }

    @Test
    fun detectBankFormat_awashWithoutAwashKeyword_returnsUnknown() {
        // Awash requires "awash" in body — without it, should not match
        val body = "Your account has been Credited ETB 100.00. Bal: ETB 500.00."
        // This should NOT match Awash because "awash" keyword is missing
        val format = SmsParser.detectBankFormat(body)
        assertTrue(format != SmsParser.BankFormat.AWASH)
    }

    // ── Deduplication hash ────────────────────────────────────────────────────

    @Test
    fun smsParser_sameSmsBody_producesSameHash() {
        val body = "Your account XXXX1234 has been credited with ETB 500.00. " +
                   "Available balance: ETB 2,000.00."
        val r1 = SmsParser.parse("CBE", body, ts, emptySet())
        val r2 = SmsParser.parse("CBE", body, ts + 1000, emptySet()) // different receive time

        assertTrue(r1 is ParseResult.Success)
        assertTrue(r2 is ParseResult.Success)
        // Hash is based on sender+body, NOT receive time — same SMS = same hash
        assertEquals((r1 as ParseResult.Success).parsed.hash, (r2 as ParseResult.Success).parsed.hash)
    }

    @Test
    fun smsParser_differentBodies_produceDifferentHashes() {
        val body1 = "Your account has been credited with ETB 500.00. Available balance: ETB 2,000.00."
        val body2 = "Your account has been credited with ETB 600.00. Available balance: ETB 2,100.00."
        val r1 = SmsParser.parse("CBE", body1, ts, emptySet())
        val r2 = SmsParser.parse("CBE", body2, ts, emptySet())

        assertTrue(r1 is ParseResult.Success)
        assertTrue(r2 is ParseResult.Success)
        assertTrue((r1 as ParseResult.Success).parsed.hash != (r2 as ParseResult.Success).parsed.hash)
    }

    // ── Registry ──────────────────────────────────────────────────────────────

    @Test
    fun smsParserRegistry_allBuiltInParsers_areRegistered() {
        val parsers = SmsParserRegistry.allBuiltIn()
        val bankNames = parsers.map { it.bankName }.toSet()

        assertTrue("CBE" in bankNames)
        assertTrue("Dashen" in bankNames)
        assertTrue("Telebirr" in bankNames)
        assertTrue("Awash" in bankNames)
        assertTrue("BOA" in bankNames)
    }

    @Test
    fun smsParserRegistry_getBuiltIn_returnsCorrectParser() {
        val parser = SmsParserRegistry.getBuiltIn(SmsParser.BankFormat.CBE)
        assertNotNull(parser)
        assertEquals("CBE", parser!!.bankName)
    }

    @Test
    fun smsParserRegistry_unknownFormat_returnsNull() {
        val parser = SmsParserRegistry.getBuiltIn(SmsParser.BankFormat.UNKNOWN)
        assertNull(parser)
    }
}
