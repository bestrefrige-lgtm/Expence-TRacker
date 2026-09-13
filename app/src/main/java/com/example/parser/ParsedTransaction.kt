package com.example.parser

/**
 * Normalized model for any parsed transaction shared into the app.
 */
data class ParsedTransaction(
    val amount: Long,                          // In minor units (e.g. 45000 for ₹450.00)
    val currency: String = "INR",              // Default currency
    val transactionType: String,               // "EXPENSE" or "INCOME"
    val merchant: String = "",                 // Payee / Merchant / Description
    val recipient: String = "",                // If sent to a person
    val sender: String = "",                   // If received from a person
    val date: Long,                            // Epoch milliseconds
    val dateIsDetected: Boolean,               // True if date was extracted from text; False if fallback today
    val time: String = "",                     // e.g. "10:35 AM"
    val paymentMethod: String = "UPI",         // "UPI", "Card", "Bank Transfer", "Unknown"
    val transactionId: String = "",            // Transaction ID
    val utr: String = "",                      // UTR / UPI Reference Number
    val description: String = "",              // Notes / remarks
    val suggestedCategory: String = "Other",   // e.g. "Food", "Groceries", "Shopping"
    val suggestedCategoryId: String? = null,
    val confidence: Map<String, Float> = emptyMap(), // Confidence scores per field (0.0 to 1.0)
    val rawSource: String = "",                // Original text for preview / fallback
    val parserSource: String = "Generic"       // "PhonePe", "GooglePay", "Paytm", "GenericUPI", "Generic"
) {
    /**
     * Calculates overall confidence score (0.0 to 1.0)
     */
    val overallConfidence: Float
        get() {
            if (confidence.isEmpty()) return 0.7f
            return confidence.values.average().toFloat()
        }

    val isHighConfidence: Boolean
        get() = overallConfidence >= 0.85f &&
                (confidence["amount"] ?: 0f) >= 0.90f &&
                (confidence["type"] ?: 0f) >= 0.85f
}
