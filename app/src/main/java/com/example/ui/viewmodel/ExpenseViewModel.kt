package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.PaymentMethodEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.ExpenseRepository
import com.example.parser.CategoryDetectionHelper
import com.example.parser.ParsedTransaction
import com.example.parser.TransactionParser
import com.example.service.RecurringTransactionWorker
import com.example.util.BackupData
import com.example.util.BackupValidationResult
import com.example.util.CurrencyHelper
import com.example.util.DateUtils
import com.example.util.RestoreStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class DateFilterType {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM
}

enum class SortOrder {
    NEWEST,
    OLDEST,
    HIGHEST_AMOUNT,
    LOWEST_AMOUNT
}

enum class ReportPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    CUSTOM
}

data class DashboardStats(
    val totalIncomeMonth: Long = 0L,
    val totalExpenseMonth: Long = 0L,
    val netBalanceMonth: Long = 0L,
    val todayExpense: Long = 0L,
    val allTimeBalance: Long = 0L,
    val categorySpendMap: Map<String, Long> = emptyMap(),
    val overallBudget: BudgetEntity? = null,
    val budgetSpent: Long = 0L
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    val database = AppDatabase.getDatabase(application, viewModelScope)

    init {
        repository = ExpenseRepository(database)
        viewModelScope.launch {
            // Populate defaults if first run
            AppDatabase.populateInitialData(database)
            // Process any due recurring transactions
            repository.processDueRecurringTransactions()
            // Schedule recurring periodic worker
            RecurringTransactionWorker.schedulePeriodicWork(application)
        }
    }

    // Settings
    val currency: StateFlow<String> = repository.getSetting("currency")
        .combine(MutableStateFlow("INR")) { set, def -> set ?: def }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    val themeMode: StateFlow<String> = repository.getSetting("theme")
        .combine(MutableStateFlow("SYSTEM")) { set, def -> set ?: def }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val budgetAlertsEnabled: StateFlow<Boolean> = repository.getSetting("budget_alerts")
        .combine(MutableStateFlow("true")) { set, def -> (set ?: def).toBoolean() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val recurringRemindersEnabled: StateFlow<Boolean> = repository.getSetting("recurring_reminders")
        .combine(MutableStateFlow("true")) { set, def -> (set ?: def).toBoolean() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun updateCurrency(newCurrency: String) {
        viewModelScope.launch {
            repository.setSetting("currency", newCurrency)
        }
    }

    fun updateThemeMode(newTheme: String) {
        viewModelScope.launch {
            repository.setSetting("theme", newTheme)
        }
    }

    fun updateBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSetting("budget_alerts", enabled.toString())
        }
    }

    fun updateRecurringReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSetting("recurring_reminders", enabled.toString())
        }
    }

    // All data flows
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.getRecentTransactions(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> = repository.getCategoriesByType("EXPENSE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> = repository.getCategoriesByType("INCOME")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurring: StateFlow<List<RecurringTransactionEntity>> = repository.getAllRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentMethods: StateFlow<List<PaymentMethodEntity>> = repository.getAllPaymentMethods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month for Budgets
    private val _selectedBudgetMonth = MutableStateFlow(DateUtils.getMonthKey(System.currentTimeMillis()))
    val selectedBudgetMonth = _selectedBudgetMonth.asStateFlow()

    fun setSelectedBudgetMonth(monthKey: String) {
        _selectedBudgetMonth.value = monthKey
    }

    fun navigateBudgetMonth(step: Int) {
        try {
            val parts = _selectedBudgetMonth.value.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                add(Calendar.MONTH, step)
            }
            _selectedBudgetMonth.value = DateUtils.getMonthKeyFromCalendar(cal)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val currentMonthBudgets: StateFlow<List<BudgetEntity>> = _selectedBudgetMonth.combine(repository.getAllBudgets()) { month, budgets ->
        budgets.filter { it.month == month }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard calculations
    val dashboardStats: StateFlow<DashboardStats> = combine(
        allTransactions,
        currentMonthBudgets,
        _selectedBudgetMonth
    ) { transactions, budgets, monthKey ->
        val monthRange = DateUtils.getMonthStartAndEndMillis(monthKey)
        val todayStart = DateUtils.getTodayStartMillis()
        val todayEnd = DateUtils.getTodayEndMillis()

        var monthIncome = 0L
        var monthExpense = 0L
        var todayExpense = 0L
        var allTimeIncome = 0L
        var allTimeExpense = 0L
        val catSpend = mutableMapOf<String, Long>()

        for (tx in transactions) {
            if (tx.type == "EXPENSE") {
                allTimeExpense += tx.amount
                if (tx.date in monthRange.first..monthRange.second) {
                    monthExpense += tx.amount
                    val currentCat = catSpend[tx.categoryName] ?: 0L
                    catSpend[tx.categoryName] = currentCat + tx.amount
                }
                if (tx.date in todayStart..todayEnd) {
                    todayExpense += tx.amount
                }
            } else if (tx.type == "INCOME") {
                allTimeIncome += tx.amount
                if (tx.date in monthRange.first..monthRange.second) {
                    monthIncome += tx.amount
                }
            }
        }

        val overallBudget = budgets.find { it.categoryId == null }

        DashboardStats(
            totalIncomeMonth = monthIncome,
            totalExpenseMonth = monthExpense,
            netBalanceMonth = monthIncome - monthExpense,
            todayExpense = todayExpense,
            allTimeBalance = allTimeIncome - allTimeExpense,
            categorySpendMap = catSpend,
            overallBudget = overallBudget,
            budgetSpent = monthExpense
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Transactions Search & Filters
    val searchQuery = MutableStateFlow("")
    val selectedDateFilter = MutableStateFlow(DateFilterType.ALL)
    val customStartDate = MutableStateFlow(DateUtils.getThisMonthStartMillis())
    val customEndDate = MutableStateFlow(DateUtils.getThisMonthEndMillis())
    val selectedTypeFilter = MutableStateFlow<String?>("ALL") // "ALL", "EXPENSE", "INCOME"
    val selectedCategoryFilter = MutableStateFlow<String?>("ALL") // "ALL" or categoryName
    val selectedPaymentFilter = MutableStateFlow<String?>("ALL") // "ALL" or method
    val minAmountFilter = MutableStateFlow<Long?>(null)
    val maxAmountFilter = MutableStateFlow<Long?>(null)
    val sortOrder = MutableStateFlow(SortOrder.NEWEST)

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        selectedDateFilter,
        combine(selectedTypeFilter, selectedCategoryFilter, selectedPaymentFilter, ::Triple),
        combine(minAmountFilter, maxAmountFilter, sortOrder, ::Triple)
    ) { txs, query, dateFilter, typeCatPay, minMaxSort ->
        val (typeFilter, catFilter, payFilter) = typeCatPay
        val (minAmt, maxAmt, sort) = minMaxSort

        val (startMillis, endMillis) = when (dateFilter) {
            DateFilterType.ALL -> Pair(0L, Long.MAX_VALUE)
            DateFilterType.TODAY -> Pair(DateUtils.getTodayStartMillis(), DateUtils.getTodayEndMillis())
            DateFilterType.THIS_WEEK -> Pair(DateUtils.getThisWeekStartMillis(), DateUtils.getTodayEndMillis())
            DateFilterType.THIS_MONTH -> Pair(DateUtils.getThisMonthStartMillis(), DateUtils.getThisMonthEndMillis())
            DateFilterType.CUSTOM -> Pair(customStartDate.value, customEndDate.value)
        }

        val filtered = txs.filter { tx ->
            // Date filter
            if (tx.date !in startMillis..endMillis) return@filter false

            // Type filter
            if (typeFilter != "ALL" && tx.type != typeFilter) return@filter false

            // Category filter
            if (catFilter != "ALL" && tx.categoryName != catFilter) return@filter false

            // Payment filter
            if (payFilter != "ALL" && tx.paymentMethod != payFilter) return@filter false

            // Min / Max Amount
            if (minAmt != null && tx.amount < minAmt) return@filter false
            if (maxAmt != null && tx.amount > maxAmt) return@filter false

            // Query search
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                val matchesMerchant = tx.merchant.lowercase().contains(q)
                val matchesNote = tx.note.lowercase().contains(q)
                val matchesCat = tx.categoryName.lowercase().contains(q)
                val matchesTags = tx.tags.lowercase().contains(q)
                val matchesAmount = (tx.amount.toDouble() / 100).toString().contains(q)
                if (!matchesMerchant && !matchesNote && !matchesCat && !matchesTags && !matchesAmount) {
                    return@filter false
                }
            }
            true
        }

        when (sort) {
            SortOrder.NEWEST -> filtered.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.createdAt })
            SortOrder.OLDEST -> filtered.sortedWith(compareBy<TransactionEntity> { it.date }.thenBy { it.createdAt })
            SortOrder.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amount }
            SortOrder.LOWEST_AMOUNT -> filtered.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grouped filtered transactions for list display
    val groupedFilteredTransactions: StateFlow<Map<String, List<TransactionEntity>>> =
        filteredTransactions.combine(MutableStateFlow(Unit)) { txs, _ ->
            val map = linkedMapOf<String, MutableList<TransactionEntity>>()
            map["Today"] = mutableListOf()
            map["Yesterday"] = mutableListOf()
            map["This Week"] = mutableListOf()
            map["Earlier"] = mutableListOf()

            for (tx in txs) {
                val group = DateUtils.getGroupHeader(tx.date)
                map[group]?.add(tx)
            }
            // Remove empty groups
            map.filterValues { it.isNotEmpty() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Transaction Actions
    fun saveTransaction(
        id: String?,
        type: String,
        amountMinor: Long,
        categoryId: String,
        categoryName: String,
        categoryIcon: String,
        date: Long,
        time: String,
        paymentMethod: String,
        merchant: String,
        note: String,
        tags: String,
        receiptUri: String?,
        isRecurring: Boolean,
        recurringFrequency: String,
        recurringEndDate: Long?
    ) {
        viewModelScope.launch {
            val txId = if (id.isNullOrBlank()) UUID.randomUUID().toString() else id

            var recurringId: String? = null
            if (isRecurring) {
                val recurring = RecurringTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    amount = amountMinor,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    categoryIcon = categoryIcon,
                    paymentMethod = paymentMethod,
                    merchant = merchant,
                    note = note,
                    frequency = recurringFrequency,
                    startDate = date,
                    endDate = recurringEndDate,
                    lastProcessedDate = date,
                    isActive = true
                )
                repository.insertRecurring(recurring)
                recurringId = recurring.id
            }

            val transaction = TransactionEntity(
                id = txId,
                type = type,
                amount = amountMinor,
                categoryId = categoryId,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                date = date,
                time = time,
                paymentMethod = paymentMethod,
                merchant = merchant,
                note = note,
                tags = tags,
                receiptUri = receiptUri,
                recurringId = recurringId,
                updatedAt = System.currentTimeMillis()
            )

            if (id.isNullOrBlank()) {
                repository.insertTransaction(transaction)
            } else {
                repository.updateTransaction(transaction)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun duplicateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val duplicated = transaction.copy(
                id = UUID.randomUUID().toString(),
                date = System.currentTimeMillis(),
                time = DateUtils.formatTime(System.currentTimeMillis()),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertTransaction(duplicated)
        }
    }

    // Category CRUD
    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.insertCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun resetCategoriesToDefaults() {
        viewModelScope.launch {
            repository.resetCategoriesToDefault()
        }
    }

    // Budget CRUD
    fun saveBudget(month: String, categoryId: String?, categoryName: String?, amountMinor: Long) {
        viewModelScope.launch {
            val existing = repository.getBudgetsForMonth(month)
            val budget = BudgetEntity(
                id = UUID.randomUUID().toString(),
                month = month,
                categoryId = categoryId,
                categoryName = categoryName,
                amount = amountMinor,
                notify75 = true,
                notify90 = true,
                notify100 = true
            )
            repository.insertOrUpdateBudget(budget)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // Recurring CRUD
    fun toggleRecurringActive(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurring(recurring.copy(isActive = !recurring.isActive))
        }
    }

    fun deleteRecurring(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(recurring)
        }
    }

    fun processDueRecurringNow() {
        viewModelScope.launch {
            repository.processDueRecurringTransactions()
        }
    }

    // Payment Methods
    fun addPaymentMethod(name: String) {
        viewModelScope.launch {
            repository.insertPaymentMethod(name)
        }
    }

    fun deletePaymentMethod(method: PaymentMethodEntity) {
        viewModelScope.launch {
            repository.deletePaymentMethod(method)
        }
    }

    // Reports calculation
    val reportPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    val reportCustomStart = MutableStateFlow(DateUtils.getThisMonthStartMillis())
    val reportCustomEnd = MutableStateFlow(DateUtils.getThisMonthEndMillis())

    val reportStats = combine(
        allTransactions,
        reportPeriod,
        reportCustomStart,
        reportCustomEnd
    ) { txs, period, customStart, customEnd ->
        val (startMillis, endMillis, label) = when (period) {
            ReportPeriod.TODAY -> Triple(DateUtils.getTodayStartMillis(), DateUtils.getTodayEndMillis(), "Today")
            ReportPeriod.THIS_WEEK -> Triple(DateUtils.getThisWeekStartMillis(), DateUtils.getTodayEndMillis(), "This Week")
            ReportPeriod.THIS_MONTH -> Triple(DateUtils.getThisMonthStartMillis(), DateUtils.getThisMonthEndMillis(), "This Month")
            ReportPeriod.THIS_YEAR -> Triple(DateUtils.getThisYearStartMillis(), DateUtils.getTodayEndMillis(), "This Year")
            ReportPeriod.CUSTOM -> Triple(customStart, customEnd, "${DateUtils.formatDate(customStart)} - ${DateUtils.formatDate(customEnd)}")
        }

        val periodTxs = txs.filter { it.date in startMillis..endMillis }

        var totalIncome = 0L
        var totalExpense = 0L
        var largestExpense = 0L
        val categoryTotals = mutableMapOf<String, Long>()
        val dailyExpenses = mutableMapOf<String, Long>()

        for (tx in periodTxs) {
            if (tx.type == "EXPENSE") {
                totalExpense += tx.amount
                if (tx.amount > largestExpense) largestExpense = tx.amount
                categoryTotals[tx.categoryName] = (categoryTotals[tx.categoryName] ?: 0L) + tx.amount

                val dayKey = DateUtils.formatDate(tx.date)
                dailyExpenses[dayKey] = (dailyExpenses[dayKey] ?: 0L) + tx.amount
            } else {
                totalIncome += tx.amount
            }
        }

        val topCategory = categoryTotals.maxByOrNull { it.value }?.key ?: "None"
        val dayCount = Math.max(1L, (endMillis - startMillis) / (24 * 60 * 60 * 1000L))
        val avgDailySpending = totalExpense / dayCount

        val categoryBreakdown = categoryTotals.toList().sortedByDescending { it.second }

        ReportData(
            periodLabel = label,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = totalIncome - totalExpense,
            largestExpense = largestExpense,
            topCategory = topCategory,
            averageDaily = avgDailySpending,
            categoryBreakdown = categoryBreakdown,
            dailyExpenses = dailyExpenses,
            transactions = periodTxs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportData())

    // Backup & Restore
    private val _backupValidationResult = MutableStateFlow<BackupValidationResult?>(null)
    val backupValidationResult = _backupValidationResult.asStateFlow()

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onReady(json)
        }
    }

    fun validateBackupContent(jsonContent: String) {
        viewModelScope.launch {
            val result = repository.validateBackupJson(jsonContent)
            _backupValidationResult.value = result
        }
    }

    fun clearBackupValidation() {
        _backupValidationResult.value = null
    }

    fun applyRestore(strategy: RestoreStrategy, onComplete: () -> Unit) {
        val data = _backupValidationResult.value?.backupData ?: return
        viewModelScope.launch {
            repository.restoreBackup(data, strategy)
            _backupValidationResult.value = null
            onComplete()
        }
    }

    // ==========================================
    // Share-to-Expense (PhonePe & Others)
    // ==========================================
    private val _sharedRawText = MutableStateFlow<String?>(null)
    val sharedRawText: StateFlow<String?> = _sharedRawText.asStateFlow()

    private val _parsedSharedTransaction = MutableStateFlow<ParsedTransaction?>(null)
    val parsedSharedTransaction: StateFlow<ParsedTransaction?> = _parsedSharedTransaction.asStateFlow()

    private val _duplicateTransaction = MutableStateFlow<TransactionEntity?>(null)
    val duplicateTransaction: StateFlow<TransactionEntity?> = _duplicateTransaction.asStateFlow()

    private val _isProcessingShare = MutableStateFlow(false)
    val isProcessingShare: StateFlow<Boolean> = _isProcessingShare.asStateFlow()

    // Transaction Sharing Preferences
    val shareToExpenseEnabled = repository.getSetting("share_enabled")
        .map { it?.toBoolean() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val confirmBeforeSavingShare = repository.getSetting("share_confirm")
        .map { it?.toBoolean() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoCategoryShare = repository.getSetting("share_auto_category")
        .map { it?.toBoolean() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoSaveHighConfidence = repository.getSetting("share_auto_save")
        .map { it?.toBoolean() ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateShareToExpenseEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSetting("share_enabled", enabled.toString()) }
    }

    fun updateConfirmBeforeSavingShare(enabled: Boolean) {
        viewModelScope.launch { repository.setSetting("share_confirm", enabled.toString()) }
    }

    fun updateAutoCategoryShare(enabled: Boolean) {
        viewModelScope.launch { repository.setSetting("share_auto_category", enabled.toString()) }
    }

    fun updateAutoSaveHighConfidence(enabled: Boolean) {
        viewModelScope.launch { repository.setSetting("share_auto_save", enabled.toString()) }
    }

    fun processIncomingSharedText(text: String, onAutoSaved: ((TransactionEntity) -> Unit)? = null) {
        viewModelScope.launch {
            _isProcessingShare.value = true
            _sharedRawText.value = text
            val currentCats = allCategories.value
            val parsed = TransactionParser.parse(text, currentCats)
            _parsedSharedTransaction.value = parsed

            if (parsed != null && parsed.amount > 0) {
                // Check duplicates
                val duplicate = repository.findPossibleDuplicate(
                    amount = parsed.amount,
                    date = parsed.date,
                    merchant = parsed.merchant,
                    transactionId = parsed.transactionId,
                    utr = parsed.utr
                )
                _duplicateTransaction.value = duplicate

                // Check Fast Save Option:
                val autoSave = autoSaveHighConfidence.value
                val confirm = confirmBeforeSavingShare.value
                if (autoSave && !confirm && duplicate == null && parsed.isHighConfidence) {
                    saveParsedSharedTransaction { savedTx ->
                        onAutoSaved?.invoke(savedTx)
                    }
                }
            } else {
                _duplicateTransaction.value = null
            }
            _isProcessingShare.value = false
        }
    }

    fun updateParsedType(newType: String) {
        val current = _parsedSharedTransaction.value ?: return
        val currentCats = if (newType == "EXPENSE") expenseCategories.value else incomeCategories.value
        val suggestion = CategoryDetectionHelper.suggestCategory(current.merchant, current.description, newType, currentCats)
        _parsedSharedTransaction.value = current.copy(
            transactionType = newType,
            suggestedCategory = suggestion.categoryName,
            suggestedCategoryId = suggestion.categoryId
        )
    }

    fun updateParsedCategory(categoryName: String, categoryId: String?) {
        val current = _parsedSharedTransaction.value ?: return
        _parsedSharedTransaction.value = current.copy(
            suggestedCategory = categoryName,
            suggestedCategoryId = categoryId
        )
    }

    fun updateParsedMerchant(newMerchant: String) {
        val current = _parsedSharedTransaction.value ?: return
        _parsedSharedTransaction.value = current.copy(merchant = newMerchant)
    }

    fun updateParsedPaymentMethod(method: String) {
        val current = _parsedSharedTransaction.value ?: return
        _parsedSharedTransaction.value = current.copy(paymentMethod = method)
    }

    fun saveParsedSharedTransaction(onSuccess: (TransactionEntity) -> Unit) {
        val parsed = _parsedSharedTransaction.value ?: return
        viewModelScope.launch {
            val cats = if (parsed.transactionType == "EXPENSE") expenseCategories.value else incomeCategories.value
            val matchedCategory = cats.firstOrNull { it.id == parsed.suggestedCategoryId || it.name.equals(parsed.suggestedCategory, ignoreCase = true) }
                ?: cats.firstOrNull()

            val categoryId = matchedCategory?.id ?: UUID.randomUUID().toString()
            val categoryName = matchedCategory?.name ?: parsed.suggestedCategory
            val categoryIcon = matchedCategory?.icon ?: "shopping_bag"

            val refTag = when {
                parsed.transactionId.isNotBlank() -> "ref:${parsed.transactionId}"
                parsed.utr.isNotBlank() -> "ref:${parsed.utr}"
                else -> ""
            }

            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = parsed.transactionType,
                amount = parsed.amount,
                categoryId = categoryId,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                date = parsed.date,
                time = parsed.time.ifBlank { "12:00 PM" },
                paymentMethod = parsed.paymentMethod,
                merchant = parsed.merchant,
                note = if (parsed.transactionId.isNotBlank()) "Txn ID: ${parsed.transactionId}" else parsed.description,
                tags = refTag,
                updatedAt = System.currentTimeMillis()
            )

            repository.insertTransaction(tx)
            _sharedRawText.value = null
            _parsedSharedTransaction.value = null
            _duplicateTransaction.value = null
            onSuccess(tx)
        }
    }

    fun clearSharedTransaction() {
        _sharedRawText.value = null
        _parsedSharedTransaction.value = null
        _duplicateTransaction.value = null
    }

    // Prefill data for AddEditTransactionScreen (from manual edit of shared receipt)
    private val _prefillTransactionData = MutableStateFlow<PrefillTransactionData?>(null)
    val prefillTransactionData: StateFlow<PrefillTransactionData?> = _prefillTransactionData.asStateFlow()

    fun setPrefillData(data: PrefillTransactionData?) {
        _prefillTransactionData.value = data
    }

    fun clearPrefillData() {
        _prefillTransactionData.value = null
    }

    // Danger Zone
    fun clearAllTransactions(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllTransactions()
            onComplete()
        }
    }

    fun resetApplicationData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.resetApplication()
            onComplete()
        }
    }
}

data class ReportData(
    val periodLabel: String = "",
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val largestExpense: Long = 0L,
    val topCategory: String = "None",
    val averageDaily: Long = 0L,
    val categoryBreakdown: List<Pair<String, Long>> = emptyList(),
    val dailyExpenses: Map<String, Long> = emptyMap(),
    val transactions: List<TransactionEntity> = emptyList()
)

data class PrefillTransactionData(
    val type: String = "EXPENSE",
    val amountMinor: Long? = null,
    val merchant: String? = null,
    val date: Long? = null,
    val time: String? = null,
    val paymentMethod: String? = null,
    val note: String? = null,
    val tags: String? = null,
    val categoryName: String? = null,
    val categoryId: String? = null
)
