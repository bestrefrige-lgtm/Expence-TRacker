package com.example.parser

import com.example.data.model.CategoryEntity

object GenericUPITransactionParser : SingleAppParser {

    override val name: String = "GenericUPI"

    override fun canParse(text: String): Boolean = true // Catch-all fallback

    override fun parse(text: String, existingCategories: List<CategoryEntity>): ParsedTransaction? {
        val amountResult = IndianNumberParser.extractAmount(text) ?: return null
        val (type, typeConfidence) = TransactionParser.detectType(text)
        val dateResult = DateExtractor.extractDateTime(text)
        val (txnId, utr) = TransactionParser.extractIdentifiers(text)
        val merchant = TransactionParser.extractMerchant(text, type)

        // Payment method detection
        val lower = text.lowercase()
        val paymentMethod = when {
            lower.contains("upi") || lower.contains("vpa") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm") -> "UPI"
            lower.contains("credit card") || lower.contains("card ending") -> "Credit Card"
            lower.contains("debit card") -> "Debit Card"
            lower.contains("net banking") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("imps") -> "Bank Transfer"
            else -> "UPI" // Most shared text in India is UPI
        }

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
            "merchant" to (if (merchant.isNotBlank()) 0.90f else 0.40f),
            "category" to categorySuggestion.confidence,
            "paymentMethod" to (if (lower.contains("upi")) 0.95f else 0.75f)
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
            paymentMethod = paymentMethod,
            transactionId = txnId,
            utr = utr,
            description = if (txnId.isNotEmpty()) "Ref: $txnId" else "",
            suggestedCategory = categorySuggestion.categoryName,
            suggestedCategoryId = categorySuggestion.categoryId,
            confidence = confidenceMap,
            rawSource = text,
            parserSource = "GenericUPI"
        )
    }
}
