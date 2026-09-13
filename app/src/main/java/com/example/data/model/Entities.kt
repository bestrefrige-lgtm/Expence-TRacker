package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["categoryId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // "EXPENSE" or "INCOME"
    val amount: Long, // in minor currency units (e.g., 10000 = ₹100.00)
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val date: Long, // timestamp in millis
    val time: String, // format "HH:mm"
    val paymentMethod: String,
    val note: String = "",
    val merchant: String = "",
    val tags: String = "",
    val receiptUri: String? = null,
    val recurringId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type", "orderIndex"])]
)
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val color: Long,
    val type: String, // "EXPENSE" or "INCOME"
    val budgetAmount: Long? = null,
    val orderIndex: Int = 0,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["month", "categoryId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val month: String, // "YYYY-MM"
    val categoryId: String? = null, // null means overall monthly budget
    val categoryName: String? = null,
    val amount: Long, // in minor units
    val notify75: Boolean = true,
    val notify90: Boolean = true,
    val notify100: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // "EXPENSE" or "INCOME"
    val amount: Long,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val paymentMethod: String,
    val merchant: String = "",
    val note: String = "",
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val startDate: Long,
    val endDate: Long? = null,
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)
