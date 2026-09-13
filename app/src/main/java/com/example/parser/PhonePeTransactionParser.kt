package com.example.parser

import com.example.data.model.CategoryEntity
import java.util.regex.Pattern

object PhonePeTransactionParser : SingleAppParser {

    override val name: String = "PhonePe"

    override fun canParse(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("phonepe") ||
                lower.contains("transaction id: t") ||
                lower.contains("phonepe transaction") ||
                (lower.contains("paid") && lower.contains("utr"))
    }

    override fun parse(text: String, existingCategories: List<CategoryEntity>): ParsedTransaction? {
        val amountResult = IndianNumberParser.extractAmount(text) ?: return null
        val (type, typeConfidence) = TransactionParser.detectType(text)
        val dateResult = DateExtractor.extractDateTime(text)
        val (txnId, utr) = TransactionParser.extractIdentifiers(text)
        val merchant = TransactionParser.extractMerchant(text, type)

        // Suggest category
        val categorySuggestion = CategoryDetectionHelper.suggestCategory(
            merchant = merchant,
            description = text,
            transactionType = type,
            existingCategories = existingCategories
        )

        // Confidence map
        val confidenceMap = mutableMapOf(
            "amount" to amountResult.confidence,
            "type" to typeConfidence,
            "date" to dateResult.confidence,
            "merchant" to (if (merchant.isNotBlank()) 0.94f else 0.40f),
            "category" to categorySuggestion.confidence,
            "paymentMethod" to 0.95f
        )

        return ParsedTransaction(
            amount = amountResult.amountMinor,
            currency = "INR",
            transactionType = type,
            merchant = merchant,
            recipient = if (type == "EXPENSE") merchant else "",
            sender = if (type == "INCOME") merchant else "",
            date = dateResult.timestamp,
            dateIsDetected = dateResult.isDetected,
            time = dateResult.timeString,
            paymentMethod = "UPI",
            transactionId = txnId,
            utr = utr,
            description = "Shared via PhonePe",
            suggestedCategory = categorySuggestion.categoryName,
            suggestedCategoryId = categorySuggestion.categoryId,
            confidence = confidenceMap,
            rawSource = text,
            parserSource = "PhonePe"
        )
    }
}
