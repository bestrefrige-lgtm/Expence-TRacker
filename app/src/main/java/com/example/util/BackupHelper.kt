package com.example.util

import com.example.data.model.AppSettingsEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.PaymentMethodEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val app: String = "ExpenseTracker",
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity>,
    val recurring: List<RecurringTransactionEntity>,
    val paymentMethods: List<PaymentMethodEntity>,
    val settings: List<AppSettingsEntity>
)

data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val backupData: BackupData? = null,
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    val categoryCount: Int = 0,
    val budgetCount: Int = 0,
    val recurringCount: Int = 0,
    val duplicateTransactionCount: Int = 0
)

enum class RestoreStrategy {
    SKIP_DUPLICATES,
    IMPORT_NEW_ONLY,
    REPLACE_EXISTING
}

object BackupHelper {

    fun createBackupJson(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>,
        recurring: List<RecurringTransactionEntity>,
        paymentMethods: List<PaymentMethodEntity>,
        settings: List<AppSettingsEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "ExpenseTracker")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type)
                put("amount", tx.amount)
                put("categoryId", tx.categoryId)
                put("categoryName", tx.categoryName)
                put("categoryIcon", tx.categoryIcon)
                put("date", tx.date)
                put("time", tx.time)
                put("paymentMethod", tx.paymentMethod)
                put("note", tx.note)
                put("merchant", tx.merchant)
                put("tags", tx.tags)
                put("receiptUri", tx.receiptUri ?: "")
                put("recurringId", tx.recurringId ?: "")
                put("createdAt", tx.createdAt)
                put("updatedAt", tx.updatedAt)
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val catArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("icon", cat.icon)
                put("color", cat.color)
                put("type", cat.type)
                if (cat.budgetAmount != null) put("budgetAmount", cat.budgetAmount)
                put("orderIndex", cat.orderIndex)
                put("isDefault", cat.isDefault)
            }
            catArray.put(obj)
        }
        root.put("categories", catArray)

        val budgetArray = JSONArray()
        for (b in budgets) {
            val obj = JSONObject().apply {
                put("id", b.id)
                put("month", b.month)
                put("categoryId", b.categoryId ?: "")
                put("categoryName", b.categoryName ?: "")
                put("amount", b.amount)
                put("notify75", b.notify75)
                put("notify90", b.notify90)
                put("notify100", b.notify100)
                put("updatedAt", b.updatedAt)
            }
            budgetArray.put(obj)
        }
        root.put("budgets", budgetArray)

        val recArray = JSONArray()
        for (r in recurring) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("type", r.type)
                put("amount", r.amount)
                put("categoryId", r.categoryId)
                put("categoryName", r.categoryName)
                put("categoryIcon", r.categoryIcon)
                put("paymentMethod", r.paymentMethod)
                put("merchant", r.merchant)
                put("note", r.note)
                put("frequency", r.frequency)
                put("startDate", r.startDate)
                if (r.endDate != null) put("endDate", r.endDate)
                if (r.lastProcessedDate != null) put("lastProcessedDate", r.lastProcessedDate)
                put("isActive", r.isActive)
                put("createdAt", r.createdAt)
            }
            recArray.put(obj)
        }
        root.put("recurring", recArray)

        val pmArray = JSONArray()
        for (pm in paymentMethods) {
            val obj = JSONObject().apply {
                put("id", pm.id)
                put("name", pm.name)
                put("isDefault", pm.isDefault)
            }
            pmArray.put(obj)
        }
        root.put("paymentMethods", pmArray)

        val settingsArray = JSONArray()
        for (s in settings) {
            val obj = JSONObject().apply {
                put("key", s.key)
                put("value", s.value)
            }
            settingsArray.put(obj)
        }
        root.put("settings", settingsArray)

        return root.toString(2)
    }

    fun parseAndValidateBackup(jsonString: String, existingTransactionIds: Set<String>): BackupValidationResult {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("transactions") && !root.has("categories")) {
                return BackupValidationResult(isValid = false, errorMessage = "Invalid file structure: Missing transactions or categories.")
            }

            val transactions = mutableListOf<TransactionEntity>()
            var expenseCount = 0
            var incomeCount = 0
            var duplicates = 0

            val txArray = root.optJSONArray("transactions")
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val type = obj.getString("type")
                    if (existingTransactionIds.contains(id)) {
                        duplicates++
                    }
                    if (type == "EXPENSE") expenseCount++ else incomeCount++

                    transactions.add(
                        TransactionEntity(
                            id = id,
                            type = type,
                            amount = obj.getLong("amount"),
                            categoryId = obj.optString("categoryId", ""),
                            categoryName = obj.optString("categoryName", "Uncategorized"),
                            categoryIcon = obj.optString("categoryIcon", "receipt"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            time = obj.optString("time", "12:00"),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            note = obj.optString("note", ""),
                            merchant = obj.optString("merchant", ""),
                            tags = obj.optString("tags", ""),
                            receiptUri = obj.optString("receiptUri").ifEmpty { null },
                            recurringId = obj.optString("recurringId").ifEmpty { null },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val categories = mutableListOf<CategoryEntity>()
            val catArray = root.optJSONArray("categories")
            if (catArray != null) {
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    categories.add(
                        CategoryEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            icon = obj.optString("icon", "receipt"),
                            color = obj.optLong("color", 0xFF888888),
                            type = obj.optString("type", "EXPENSE"),
                            budgetAmount = if (obj.has("budgetAmount")) obj.getLong("budgetAmount") else null,
                            orderIndex = obj.optInt("orderIndex", i),
                            isDefault = obj.optBoolean("isDefault", false)
                        )
                    )
                }
            }

            val budgets = mutableListOf<BudgetEntity>()
            val budgetArray = root.optJSONArray("budgets")
            if (budgetArray != null) {
                for (i in 0 until budgetArray.length()) {
                    val obj = budgetArray.getJSONObject(i)
                    budgets.add(
                        BudgetEntity(
                            id = obj.getString("id"),
                            month = obj.getString("month"),
                            categoryId = obj.optString("categoryId").ifEmpty { null },
                            categoryName = obj.optString("categoryName").ifEmpty { null },
                            amount = obj.getLong("amount"),
                            notify75 = obj.optBoolean("notify75", true),
                            notify90 = obj.optBoolean("notify90", true),
                            notify100 = obj.optBoolean("notify100", true),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val recurring = mutableListOf<RecurringTransactionEntity>()
            val recArray = root.optJSONArray("recurring")
            if (recArray != null) {
                for (i in 0 until recArray.length()) {
                    val obj = recArray.getJSONObject(i)
                    recurring.add(
                        RecurringTransactionEntity(
                            id = obj.getString("id"),
                            type = obj.optString("type", "EXPENSE"),
                            amount = obj.getLong("amount"),
                            categoryId = obj.optString("categoryId", ""),
                            categoryName = obj.optString("categoryName", "Recurring"),
                            categoryIcon = obj.optString("categoryIcon", "receipt"),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            merchant = obj.optString("merchant", ""),
                            note = obj.optString("note", ""),
                            frequency = obj.optString("frequency", "MONTHLY"),
                            startDate = obj.optLong("startDate", System.currentTimeMillis()),
                            endDate = if (obj.has("endDate")) obj.getLong("endDate") else null,
                            lastProcessedDate = if (obj.has("lastProcessedDate")) obj.getLong("lastProcessedDate") else null,
                            isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val paymentMethods = mutableListOf<PaymentMethodEntity>()
            val pmArray = root.optJSONArray("paymentMethods")
            if (pmArray != null) {
                for (i in 0 until pmArray.length()) {
                    val obj = pmArray.getJSONObject(i)
                    paymentMethods.add(
                        PaymentMethodEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            isDefault = obj.optBoolean("isDefault", false)
                        )
                    )
                }
            }

            val settings = mutableListOf<AppSettingsEntity>()
            val settingsArray = root.optJSONArray("settings")
            if (settingsArray != null) {
                for (i in 0 until settingsArray.length()) {
                    val obj = settingsArray.getJSONObject(i)
                    settings.add(
                        AppSettingsEntity(
                            key = obj.getString("key"),
                            value = obj.getString("value")
                        )
                    )
                }
            }

            val data = BackupData(
                version = root.optInt("version", 1),
                exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
                transactions = transactions,
                categories = categories,
                budgets = budgets,
                recurring = recurring,
                paymentMethods = paymentMethods,
                settings = settings
            )

            BackupValidationResult(
                isValid = true,
                backupData = data,
                expenseCount = expenseCount,
                incomeCount = incomeCount,
                categoryCount = categories.size,
                budgetCount = budgets.size,
                recurringCount = recurring.size,
                duplicateTransactionCount = duplicates
            )
        } catch (e: Exception) {
            BackupValidationResult(isValid = false, errorMessage = "Failed to parse backup: ${e.localizedMessage}")
        }
    }
}
