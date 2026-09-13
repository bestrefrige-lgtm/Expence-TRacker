package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.PaymentMethodEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        PaymentMethodEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            if (categoryDao.getCategoryCount() == 0) {
                categoryDao.insertCategories(getDefaultCategories())
            }

            val paymentMethodDao = database.paymentMethodDao()
            if (paymentMethodDao.getCount() == 0) {
                paymentMethodDao.insertPaymentMethods(getDefaultPaymentMethods())
            }

            val appSettingsDao = database.appSettingsDao()
            if (appSettingsDao.getSettingSync("currency") == null) {
                appSettingsDao.setSetting(AppSettingsEntity("currency", "INR"))
            }
            if (appSettingsDao.getSettingSync("theme") == null) {
                appSettingsDao.setSetting(AppSettingsEntity("theme", "SYSTEM"))
            }
            if (appSettingsDao.getSettingSync("budget_alerts") == null) {
                appSettingsDao.setSetting(AppSettingsEntity("budget_alerts", "true"))
            }
            if (appSettingsDao.getSettingSync("recurring_reminders") == null) {
                appSettingsDao.setSetting(AppSettingsEntity("recurring_reminders", "true"))
            }
        }

        fun getDefaultCategories(): List<CategoryEntity> {
            val expenseCategories = listOf(
                Pair("Food", "restaurant") to 0xFFE57373,
                Pair("Groceries", "shopping_cart") to 0xFFFFB74D,
                Pair("Shopping", "shopping_bag") to 0xFFBA68C8,
                Pair("Transportation", "directions_bus") to 0xFF4DD0E1,
                Pair("Fuel", "local_gas_station") to 0xFFFF8A65,
                Pair("Rent", "home") to 0xFF7986CB,
                Pair("Electricity", "bolt") to 0xFFFFD54F,
                Pair("Water", "water_drop") to 0xFF4FC3F7,
                Pair("Internet", "wifi") to 0xFF4DB6AC,
                Pair("Mobile", "smartphone") to 0xFF81C784,
                Pair("EMI", "account_balance") to 0xFFA1887F,
                Pair("Insurance", "verified_user") to 0xFF90A4AE,
                Pair("Health", "favorite") to 0xFFF06292,
                Pair("Medicine", "medication") to 0xFFE57373,
                Pair("Education", "school") to 0xFF64B5F6,
                Pair("Entertainment", "movie") to 0xFFBA68C8,
                Pair("Travel", "flight") to 0xFF4DD0E1,
                Pair("Subscriptions", "subscriptions") to 0xFFFF8A65,
                Pair("Personal Care", "spa") to 0xFFAED581,
                Pair("Other", "more_horiz") to 0xFF9E9E9E
            )

            val incomeCategories = listOf(
                Pair("Salary", "payments") to 0xFF4CAF50,
                Pair("Business", "storefront") to 0xFF2E7D32,
                Pair("Freelance", "laptop") to 0xFF66BB6A,
                Pair("Interest", "trending_up") to 0xFF81C784,
                Pair("Bonus", "card_giftcard") to 0xFFA5D6A7,
                Pair("Other Income", "attach_money") to 0xFF81C784
            )

            val list = mutableListOf<CategoryEntity>()
            var order = 0
            for ((item, color) in expenseCategories) {
                list.add(
                    CategoryEntity(
                        id = UUID.randomUUID().toString(),
                        name = item.first,
                        icon = item.second,
                        color = color,
                        type = "EXPENSE",
                        orderIndex = order++,
                        isDefault = true
                    )
                )
            }

            for ((item, color) in incomeCategories) {
                list.add(
                    CategoryEntity(
                        id = UUID.randomUUID().toString(),
                        name = item.first,
                        icon = item.second,
                        color = color,
                        type = "INCOME",
                        orderIndex = order++,
                        isDefault = true
                    )
                )
            }
            return list
        }

        fun getDefaultPaymentMethods(): List<PaymentMethodEntity> {
            val methods = listOf(
                "Cash",
                "UPI",
                "Credit Card",
                "Debit Card",
                "Bank Transfer",
                "Net Banking",
                "Wallet",
                "Other"
            )
            return methods.mapIndexed { index, name ->
                PaymentMethodEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    isDefault = index == 0
                )
            }
        }
    }
}
