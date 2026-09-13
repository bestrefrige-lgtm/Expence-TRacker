package com.example.parser

import com.example.data.model.CategoryEntity

object PaytmTransactionParser : SingleAppParser {

    override val name: String = "Paytm"

    override fun canParse(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("paytm") || lower.contains("paytm order")
    }

    override fun parse(text: String, existingCategories: List<CategoryEntity>): ParsedTransaction? {
        val amountResult = IndianNumberParser.extractAmount(text) ?: return null
        val (type, typeConfidence) = TransactionParser.detectType(text)
        val dateResult = DateExtractor.extractDateTime(text)
        val (txnId, utr) = TransactionParser.extractIdentifiers(text)
        val merchant = TransactionParser.extractMerchant(text, type)

        val categorySuggestion = CategoryDetectionHelper.suggestCategory(
            merchant = merchant,
            description = text,
            transactionType = type,
            existingCategories = existingCategories
        )

        val confidenceMap = mutableMapOf(
            "amount" to amountResult.confidence,
            "type" to typeConfidence,
            "date" to dateResult.confidence,
            "merchant" to (if (merchant.isNotBlank()) 0.93f else 0.40f),
            "category" to categorySuggestion.confidence,
            "paymentMethod" to 0.90f
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
            description = "Shared via Paytm",
            suggestedCategory = categorySuggestion.categoryName,
            suggestedCategoryId = categorySuggestion.categoryId,
            confidence = confidenceMap,
            rawSource = text,
            parserSource = "Paytm"
        )
    }
}
