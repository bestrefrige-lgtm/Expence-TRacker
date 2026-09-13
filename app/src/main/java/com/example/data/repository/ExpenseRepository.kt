package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.PaymentMethodEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.util.BackupData
import com.example.util.BackupHelper
import com.example.util.BackupValidationResult
import com.example.util.DateUtils
import com.example.util.RestoreStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class ExpenseRepository(private val database: AppDatabase) {

    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val recurringDao = database.recurringDao()
    private val paymentMethodDao = database.paymentMethodDao()
    private val appSettingsDao = database.appSettingsDao()

    // Transactions
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(startMillis, endMillis)

    suspend fun getTransactionsBetweenSync(startMillis: Long, endMillis: Long): List<TransactionEntity> =
        transactionDao.getTransactionsBetweenSync(startMillis, endMillis)

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    suspend fun getTransactionById(id: String): TransactionEntity? =
        transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun findPossibleDuplicate(
        amount: Long,
        date: Long,
        merchant: String,
        transactionId: String,
        utr: String
    ): TransactionEntity? = withContext(Dispatchers.IO) {
        val identifier = if (transactionId.isNotBlank()) transactionId else utr
        // 1. Check by explicit transaction identifier if present
        if (identifier.isNotBlank()) {
            val all = transactionDao.getAllTransactionsSync()
            val match = all.firstOrNull {
                it.tags.contains(identifier, ignoreCase = true) ||
                it.note.contains(identifier, ignoreCase = true) ||
                it.merchant.contains(identifier, ignoreCase = true)
            }
            if (match != null) return@withContext match
        }

        // 2. If no identifier or no identifier match, compare amount, same day (within 24 hours), and merchant
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val candidates = transactionDao.getTransactionsBetweenSync(date - oneDayMillis, date + oneDayMillis)
        return@withContext candidates.firstOrNull { candidate ->
            candidate.amount == amount && (
                merchant.isBlank() ||
                candidate.merchant.contains(merchant, ignoreCase = true) ||
                merchant.contains(candidate.merchant, ignoreCase = true)
            )
        }
    }

    suspend fun deleteTransactionById(id: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun deleteAllTransactions() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
    }

    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    suspend fun insertCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
    }

    suspend fun resetCategoriesToDefault() = withContext(Dispatchers.IO) {
        categoryDao.deleteAllCategories()
        categoryDao.insertCategories(AppDatabase.getDefaultCategories())
    }

    // Budgets
    fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(month)

    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun getBudgetById(id: String): BudgetEntity? = budgetDao.getBudgetById(id)

    suspend fun insertOrUpdateBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.insertOrUpdateBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudget(budget)
    }

    // Recurring Transactions
    fun getAllRecurring(): Flow<List<RecurringTransactionEntity>> =
        recurringDao.getAllRecurring()

    suspend fun insertRecurring(recurring: RecurringTransactionEntity) = withContext(Dispatchers.IO) {
        recurringDao.insertRecurring(recurring)
    }

    suspend fun updateRecurring(recurring: RecurringTransactionEntity) = withContext(Dispatchers.IO) {
        recurringDao.updateRecurring(recurring)
    }

    suspend fun deleteRecurring(recurring: RecurringTransactionEntity) = withContext(Dispatchers.IO) {
        recurringDao.deleteRecurring(recurring)
    }

    /**
     * Checks all active recurring transactions and creates due transactions automatically.
     */
    suspend fun processDueRecurringTransactions(): Int = withContext(Dispatchers.IO) {
        val activeList = recurringDao.getActiveRecurring()
        val now = System.currentTimeMillis()
        var createdCount = 0

        for (rec in activeList) {
            val start = rec.startDate
            if (now < start) continue

            val lastProcessed = rec.lastProcessedDate ?: (start - 1000)
            val cal = Calendar.getInstance().apply { timeInMillis = lastProcessed }
            val nextDate = Calendar.getInstance().apply { timeInMillis = lastProcessed }

            when (rec.frequency) {
                RecurringFrequency.DAILY.name -> nextDate.add(Calendar.DAY_OF_YEAR, 1)
                RecurringFrequency.WEEKLY.name -> nextDate.add(Calendar.WEEK_OF_YEAR, 1)
                RecurringFrequency.MONTHLY.name -> nextDate.add(Calendar.MONTH, 1)
                RecurringFrequency.YEARLY.name -> nextDate.add(Calendar.YEAR, 1)
                else -> nextDate.add(Calendar.MONTH, 1)
            }

            var nextTime = nextDate.timeInMillis
            var lastSuccessfulTime: Long? = null

            // Generate transactions up to now (max 12 intervals at once to prevent runaway loops)
            var iterations = 0
            while (nextTime <= now && iterations < 12) {
                if (rec.endDate != null && nextTime > rec.endDate) {
                    break
                }

                val transaction = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = rec.type,
                    amount = rec.amount,
                    categoryId = rec.categoryId,
                    categoryName = rec.categoryName,
                    categoryIcon = rec.categoryIcon,
                    date = nextTime,
                    time = DateUtils.formatTime(nextTime),
                    paymentMethod = rec.paymentMethod,
                    note = if (rec.note.isNotBlank()) "${rec.note} (Auto-recurring)" else "Recurring: ${rec.frequency}",
                    merchant = rec.merchant,
                    recurringId = rec.id,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                transactionDao.insertTransaction(transaction)
                createdCount++
                lastSuccessfulTime = nextTime

                when (rec.frequency) {
                    RecurringFrequency.DAILY.name -> nextDate.add(Calendar.DAY_OF_YEAR, 1)
                    RecurringFrequency.WEEKLY.name -> nextDate.add(Calendar.WEEK_OF_YEAR, 1)
                    RecurringFrequency.MONTHLY.name -> nextDate.add(Calendar.MONTH, 1)
                    RecurringFrequency.YEARLY.name -> nextDate.add(Calendar.YEAR, 1)
                }
                nextTime = nextDate.timeInMillis
                iterations++
            }

            if (lastSuccessfulTime != null) {
                recurringDao.updateRecurring(rec.copy(lastProcessedDate = lastSuccessfulTime))
            }
        }
        createdCount
    }

    // Payment Methods
    fun getAllPaymentMethods(): Flow<List<PaymentMethodEntity>> =
        paymentMethodDao.getAllPaymentMethods()

    suspend fun insertPaymentMethod(name: String) = withContext(Dispatchers.IO) {
        paymentMethodDao.insertPaymentMethod(PaymentMethodEntity(name = name.trim()))
    }

    suspend fun deletePaymentMethod(method: PaymentMethodEntity) = withContext(Dispatchers.IO) {
        paymentMethodDao.deletePaymentMethod(method)
    }

    // App Settings
    fun getSetting(key: String): Flow<String?> = appSettingsDao.getSetting(key)

    suspend fun getSettingSync(key: String): String? = withContext(Dispatchers.IO) {
        appSettingsDao.getSettingSync(key)
    }

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        appSettingsDao.setSetting(AppSettingsEntity(key, value))
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val txs = transactionDao.getAllTransactionsSync()
        val cats = categoryDao.getAllCategoriesSync()
        val budgets = budgetDao.getAllBudgetsSync()
        val recs = recurringDao.getAllRecurringSync()
        val pms = paymentMethodDao.getAllPaymentMethodsSync()
        val settings = appSettingsDao.getAllSettingsSync()

        BackupHelper.createBackupJson(txs, cats, budgets, recs, pms, settings)
    }

    suspend fun validateBackupJson(jsonString: String): BackupValidationResult = withContext(Dispatchers.IO) {
        val existingIds = transactionDao.getAllTransactionsSync().map { it.id }.toSet()
        BackupHelper.parseAndValidateBackup(jsonString, existingIds)
    }

    suspend fun restoreBackup(backupData: BackupData, strategy: RestoreStrategy) = withContext(Dispatchers.IO) {
        when (strategy) {
            RestoreStrategy.REPLACE_EXISTING -> {
                // Clear all existing data and replace entirely
                transactionDao.deleteAllTransactions()
                categoryDao.deleteAllCategories()
                budgetDao.deleteAllBudgets()
                recurringDao.deleteAllRecurring()
                paymentMethodDao.deleteAllPaymentMethods()
                appSettingsDao.deleteAllSettings()

                transactionDao.insertTransactions(backupData.transactions)
                categoryDao.insertCategories(backupData.categories)
                budgetDao.insertBudgets(backupData.budgets)
                recurringDao.insertAllRecurring(backupData.recurring)
                paymentMethodDao.insertPaymentMethods(backupData.paymentMethods)
                appSettingsDao.insertSettings(backupData.settings)
            }
            RestoreStrategy.SKIP_DUPLICATES, RestoreStrategy.IMPORT_NEW_ONLY -> {
                val existingIds = transactionDao.getAllTransactionsSync().map { it.id }.toSet()
                val newTransactions = backupData.transactions.filter { !existingIds.contains(it.id) }
                transactionDao.insertTransactions(newTransactions)

                // For categories, budgets, recurring, payment methods, insert on conflict replace
                categoryDao.insertCategories(backupData.categories)
                budgetDao.insertBudgets(backupData.budgets)
                recurringDao.insertAllRecurring(backupData.recurring)
                paymentMethodDao.insertPaymentMethods(backupData.paymentMethods)
                appSettingsDao.insertSettings(backupData.settings)
            }
        }
    }

    suspend fun resetApplication() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
        categoryDao.deleteAllCategories()
        budgetDao.deleteAllBudgets()
        recurringDao.deleteAllRecurring()
        paymentMethodDao.deleteAllPaymentMethods()
        appSettingsDao.deleteAllSettings()

        AppDatabase.populateInitialData(database)
    }
}
