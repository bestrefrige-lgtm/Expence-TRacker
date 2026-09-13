package com.example.parser

import com.example.data.model.CategoryEntity

object CategoryDetectionHelper {

    data class CategorySuggestion(
        val categoryName: String,
        val categoryId: String?,
        val confidence: Float
    )

    private val KEYWORDS_MAP = mapOf(
        "Food & Dining" to listOf(
            "swiggy", "zomato", "restaurant", "cafe", "bistro", "starbucks",
            "mcdonald", "burger", "pizza", "domino", "kfc", "subway", "food",
            "bakery", "dhaba", "dining", "eats", "barbeque", "canteen"
        ),
        "Groceries" to listOf(
            "bigbasket", "dmart", "blinkit", "zepto", "grocery", "supermarket",
            "nature's basket", "instamart", "vegetable", "fruits", "provision",
            "spencer", "more retail", "kirana", "milk", "dairy"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "zara", "h&m",
            "tata cliq", "meesho", "store", "mall", "clothing", "electronics",
            "retail", "croma", "reliance digital", "apple"
        ),
        "Transportation" to listOf(
            "uber", "ola", "rapido", "metro", "auto", "cab", "taxi", "irctc",
            "railway", "train", "redbus", "flight", "makemytrip", "indigo",
            "bus", "toll", "fastag"
        ),
        "Fuel" to listOf(
            "petrol", "diesel", "cng", "hpcl", "bpcl", "indianoil", "iocl",
            "shell", "fuel", "gas station"
        ),
        "Bills & Utilities" to listOf(
            "electricity", "msedcl", "mahavitaran", "bescom", "tata power",
            "adani electricity", "water", "gas", "cylinder", "piped gas",
            "airtel", "jio", "vi", "vodafone", "broadband", "wifi", "recharge",
            "dth", "tata play", "bill", "utility"
        ),
        "Entertainment" to listOf(
            "netflix", "spotify", "youtube", "prime video", "hotstar", "disney",
            "bookmyshow", "pvr", "inox", "cinema", "movie", "gaming", "steam",
            "playstation", "apple music"
        ),
        "Health & Medical" to listOf(
            "pharmacy", "medical", "apollo", "1mg", "pharmeasy", "hospital",
            "clinic", "doctor", "dentist", "diagnostic", "pathology", "medplus"
        ),
        "Investments" to listOf(
            "zerodha", "groww", "kite", "upstox", "coin", "mutual fund", "sip",
            "stocks", "sharekhan", "angelone", "gold", "fd"
        ),
        "Salary" to listOf(
            "salary", "payroll", "wages", "stipend"
        ),
        "Investment Return" to listOf(
            "dividend", "interest", "capital gain", "payout"
        ),
        "Freelance" to listOf(
            "freelance", "upwork", "fiverr", "client payment", "consulting"
        )
    )

    /**
     * Suggests category based on merchant name, description, and available categories in Room.
     */
    fun suggestCategory(
        merchant: String,
        description: String,
        transactionType: String,
        existingCategories: List<CategoryEntity>
    ): CategorySuggestion {
        val combinedText = "$merchant $description".lowercase()

        // Match against keyword definitions
        for ((groupName, keywords) in KEYWORDS_MAP) {
            for (kw in keywords) {
                if (combinedText.contains(kw)) {
                    // Try to find matching category in user's database
                    val matchedCategory = existingCategories.firstOrNull { cat ->
                        cat.type == transactionType && (
                            cat.name.contains(groupName, ignoreCase = true) ||
                            groupName.contains(cat.name, ignoreCase = true) ||
                            cat.name.lowercase().contains(kw)
                        )
                    } ?: existingCategories.firstOrNull { it.type == transactionType && it.name.contains(groupName.take(4), ignoreCase = true) }

                    if (matchedCategory != null) {
                        return CategorySuggestion(
                            categoryName = matchedCategory.name,
                            categoryId = matchedCategory.id,
                            confidence = 0.90f
                        )
                    } else {
                        // User might have standard category "Food" matching "Food & Dining"
                        val fallback = existingCategories.firstOrNull { it.type == transactionType && (it.name.startsWith(groupName.take(4), ignoreCase = true)) }
                        if (fallback != null) {
                            return CategorySuggestion(
                                categoryName = fallback.name,
                                categoryId = fallback.id,
                                confidence = 0.85f
                            )
                        }
                    }
                }
            }
        }

        // Fallback: Pick default category for transaction type (e.g. "General" or "Other")
        val defaultCategory = existingCategories.firstOrNull { it.type == transactionType && (it.name.contains("Other", ignoreCase = true) || it.name.contains("General", ignoreCase = true)) }
            ?: existingCategories.firstOrNull { it.type == transactionType }

        return CategorySuggestion(
            categoryName = defaultCategory?.name ?: if (transactionType == "EXPENSE") "Food & Dining" else "Salary",
            categoryId = defaultCategory?.id,
            confidence = 0.50f
        )
    }
}
