package com.example.parser

import java.util.regex.Pattern

object IndianNumberParser {

    // Regex matching currency prefixes: ₹, Rs., Rs, INR followed by an Indian or standard formatted number
    // Handles commas in Indian format e.g. 1,25,000 or standard 125,000
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?:[₹]|Rs\\.?|INR)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Secondary pattern for standalone amounts e.g. "Paid 500" or "Amount: 1250.00"
    private val AMOUNT_STANDALONE_PATTERN = Pattern.compile(
        "(?:amount|paid|sent|received|debited|credited)\\s*(?:of|is|:)?\\s*(?:[₹]|Rs\\.?|INR)?\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    data class AmountResult(
        val amountMinor: Long,
        val matchedString: String,
        val confidence: Float
    )

    /**
     * Extracts amount from text and converts to minor units (multiplied by 100 for INR).
     * Returns null if no valid amount found.
     */
    fun extractAmount(text: String, minorUnitFactor: Int = 100): AmountResult? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val numStr = matcher.group(1) ?: return null
            val parsedDouble = parseNumber(numStr) ?: return null
            val minor = Math.round(parsedDouble * minorUnitFactor)
            return AmountResult(
                amountMinor = minor,
                matchedString = matcher.group(0) ?: numStr,
                confidence = 0.98f
            )
        }

        val fallbackMatcher = AMOUNT_STANDALONE_PATTERN.matcher(text)
        if (fallbackMatcher.find()) {
            val numStr = fallbackMatcher.group(1) ?: return null
            val parsedDouble = parseNumber(numStr) ?: return null
            val minor = Math.round(parsedDouble * minorUnitFactor)
            return AmountResult(
                amountMinor = minor,
                matchedString = fallbackMatcher.group(0) ?: numStr,
                confidence = 0.85f
            )
        }

        return null
    }

    /**
     * Parses number string removing Indian and international commas.
     * e.g. "1,25,000.50" -> 125000.50
     */
    fun parseNumber(input: String): Double? {
        val clean = input.replace(",", "").trim()
        return clean.toDoubleOrNull()
    }
}
