package com.example.parser

import com.example.data.model.CategoryEntity
import java.util.regex.Pattern

interface SingleAppParser {
    val name: String
    fun canParse(text: String): Boolean
    fun parse(text: String, existingCategories: List<CategoryEntity>): ParsedTransaction?
}

object TransactionParser {

    private val parsers: List<SingleAppParser> = listOf(
        PhonePeTransactionParser,
        GooglePayTransactionParser,
        PaytmTransactionParser,
        GenericUPITransactionParser
    )

    /**
     * Sanitizes incoming untrusted text.
     * Strips potential script/HTML markup and control characters, keeping printable text.
     */
    fun sanitize(input: String?): String {
        if (input == null) return ""
        return input
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "") // Remove dangerous control characters
            .trim()
    }

    /**
     * Parses the incoming shared text by running through available parsers
     * and picking the highest-confidence match, or falling back to GenericUPI.
     */
    fun parse(
        rawText: String?,
        existingCategories: List<CategoryEntity> = emptyList()
    ): ParsedTransaction? {
        val cleanText = sanitize(rawText)
        if (cleanText.isBlank()) return null

        // 1. Try specialized parsers that declare canParse = true
        for (parser in parsers) {
            if (parser.canParse(cleanText)) {
                val result = parser.parse(cleanText, existingCategories)
                if (result != null && result.amount > 0) {
                    return result
                }
            }
        }

        // 2. Fallback to generic parser
        return GenericUPITransactionParser.parse(cleanText, existingCategories)
    }

    /**
     * Common helper to extract Transaction IDs, UTRs, and Reference numbers
     */
    fun extractIdentifiers(text: String): Pair<String, String> {
        var txnId = ""
        var utr = ""

        // Transaction ID patterns
        val txnPattern = Pattern.compile(
            "(?:Transaction\\s*ID|Txn\\s*ID|Transaction\\s*No|Txn\\s*Ref)\\s*[:#-]?\\s*([A-Za-z0-9_-]{6,30})",
            Pattern.CASE_INSENSITIVE
        )
        val txnMatcher = txnPattern.matcher(text)
        if (txnMatcher.find()) {
            txnId = txnMatcher.group(1)?.trim() ?: ""
        }

        // UTR / UPI Ref patterns
        val utrPattern = Pattern.compile(
            "(?:UTR|UPI\\s*Ref(?:erence)?(?:\\s*No)?|Reference\\s*(?:Number|No)?)\\s*[:#-]?\\s*([A-Za-z0-9]{6,25})",
            Pattern.CASE_INSENSITIVE
        )
        val utrMatcher = utrPattern.matcher(text)
        if (utrMatcher.find()) {
            utr = utrMatcher.group(1)?.trim() ?: ""
        }

        // If txnId is empty but utr found or vice-versa
        if (txnId.isEmpty() && utr.isNotEmpty()) {
            txnId = utr
        }

        return Pair(txnId, utr)
    }

    /**
     * Common helper to detect transaction type (EXPENSE or INCOME)
     */
    fun detectType(text: String): Pair<String, Float> {
        val lower = text.lowercase()

        val incomeKeywords = listOf(
            "received from", "received", "credited to", "credited", "money received",
            "refund", "cashback", "deposited"
        )
        for (kw in incomeKeywords) {
            if (lower.contains(kw)) {
                return Pair("INCOME", 0.95f)
            }
        }

        val expenseKeywords = listOf(
            "paid to", "paid", "payment to", "payment of", "sent to", "sent",
            "debited from", "debited", "transferred to", "spent", "purchase"
        )
        for (kw in expenseKeywords) {
            if (lower.contains(kw)) {
                return Pair("EXPENSE", 0.95f)
            }
        }

        // Default to EXPENSE as expense tracker primary use-case with lower confidence
        return Pair("EXPENSE", 0.60f)
    }

    /**
     * Common helper to extract Merchant or Person Name
     */
    fun extractMerchant(text: String, type: String): String {
        // Look for "paid to <Merchant>", "payment to <Merchant>", "sent to <Person>", "to <Merchant>"
        val toPatterns = listOf(
            Pattern.compile("(?:paid\\s+(?:(?:[₹]|Rs\\.?|INR)\\s*[0-9,.]+\\s+)?to)\\s+([A-Za-z0-9&'._ -]{2,40})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:payment\\s+(?:of\\s+(?:[₹]|Rs\\.?|INR)\\s*[0-9,.]+\\s+)?to)\\s+([A-Za-z0-9&'._ -]{2,40})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:sent\\s+(?:(?:[₹]|Rs\\.?|INR)\\s*[0-9,.]+\\s+)?to)\\s+([A-Za-z0-9&'._ -]{2,40})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:transferred\\s+(?:(?:[₹]|Rs\\.?|INR)\\s*[0-9,.]+\\s+)?to)\\s+([A-Za-z0-9&'._ -]{2,40})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bto\\s+([A-Za-z0-9&'._ -]{2,30})\\b", Pattern.CASE_INSENSITIVE)
        )

        val fromPatterns = listOf(
            Pattern.compile("(?:received\\s+(?:(?:[₹]|Rs\\.?|INR)\\s*[0-9,.]+\\s+)?from)\\s+([A-Za-z0-9&'._ -]{2,40})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:credited\\s+by|from)\\s+([A-Za-z0-9&'._ -]{2,40})", Pattern.CASE_INSENSITIVE)
        )

        val targetPatterns = if (type == "INCOME") fromPatterns else toPatterns

        for (pattern in targetPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: ""
                val cleaned = cleanMerchantCandidate(candidate)
                if (cleaned.isNotBlank() && cleaned.length > 1) {
                    return cleaned
                }
            }
        }

        // Secondary search: "Paid ₹500 to ABC Store on 12 Sep 2026"
        val onPattern = Pattern.compile("to\\s+([^\\n\\r,]+?)(?:\\s+on\\s+|\\s*,|\\s*\\n|$)", Pattern.CASE_INSENSITIVE)
        val onMatcher = onPattern.matcher(text)
        if (onMatcher.find()) {
            val candidate = onMatcher.group(1)?.trim() ?: ""
            val cleaned = cleanMerchantCandidate(candidate)
            if (cleaned.isNotBlank() && cleaned.length > 1) {
                return cleaned
            }
        }

        return ""
    }

    private fun cleanMerchantCandidate(candidate: String): String {
        // Strip trailing words like "on", "using", "successful", dates, etc.
        var result = candidate
        val stopWords = listOf(" on ", " using ", " successful", " via ", " for ", " at ", " upi", " ref", " txn", " dated")
        for (stop in stopWords) {
            val idx = result.indexOf(stop, ignoreCase = true)
            if (idx != -1) {
                result = result.substring(0, idx)
            }
        }
        // Remove trailing punctuation
        result = result.trimEnd('.', ',', '-', ':', ' ')
        // Exclude if it looks like a currency amount or pure numbers
        if (result.matches(Regex("^[0-9,.]+$")) || result.startsWith("₹") || result.startsWith("Rs")) {
            return ""
        }
        return result.trim()
    }
}
