package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.CurrencyHelper
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val currency by viewModel.currency.collectAsState()
    val selectedMonth by viewModel.selectedBudgetMonth.collectAsState()
    val currentBudgets by viewModel.currentMonthBudgets.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()

    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var budgetAmountInput by remember { mutableStateOf("") }
    var selectedCategoryForBudget by remember { mutableStateOf<CategoryEntity?>(null) } // null = Overall monthly
    var isOverallBudgetSelected by remember { mutableStateOf(true) }

    val overallBudget = currentBudgets.find { it.categoryId == null }
    val categoryBudgets = currentBudgets.filter { it.categoryId != null }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateBudgetMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = DateUtils.formatMonthKeyForDisplay(selectedMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Budgeting Period",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.navigateBudgetMonth(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }
        }

        // Overall Monthly Budget Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Overall Monthly Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total spending cap for all categories",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (overallBudget != null) {
                            IconButton(
                                onClick = {
                                    editingBudget = overallBudget
                                    budgetAmountInput = CurrencyHelper.minorUnitsToInputString(overallBudget.amount, currency)
                                    isOverallBudgetSelected = true
                                    showAddBudgetDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Overall Budget")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (overallBudget != null) {
                        val spent = stats.budgetSpent
                        val total = overallBudget.amount
                        val fraction = (spent.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        val percent = ((spent.toDouble() / total.toDouble()) * 100).toInt()
                        val remaining = total - spent

                        val isExceeded = percent >= 100
                        val isWarning = percent in 75..99

                        val progressColor = when {
                            isExceeded -> Color(0xFFDC2626)
                            isWarning -> Color(0xFFF59E0B)
                            else -> Color(0xFF16A34A)
                        }

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    CurrencyHelper.formatMinorUnits(spent, currency),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Budget Limit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    CurrencyHelper.formatMinorUnits(total, currency),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (remaining >= 0) "Remaining" else "Over Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    CurrencyHelper.formatMinorUnits(Math.abs(remaining), currency),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                        }

                        if (isExceeded || isWarning) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isExceeded) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isExceeded) Color(0xFFDC2626) else Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isExceeded) "Alert: Monthly budget limit exceeded by ${CurrencyHelper.formatMinorUnits(Math.abs(remaining), currency)}!"
                                        else "Caution: You have used $percent% of your budget.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isExceeded) Color(0xFF991B1B) else Color(0xFF92400E),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                editingBudget = null
                                budgetAmountInput = ""
                                isOverallBudgetSelected = true
                                showAddBudgetDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Monthly Budget Limit")
                        }
                    }
                }
            }
        }

        // Category Budgets Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        editingBudget = null
                        budgetAmountInput = ""
                        isOverallBudgetSelected = false
                        selectedCategoryForBudget = expenseCategories.firstOrNull()
                        showAddBudgetDialog = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Budget")
                }
            }
        }

        // Category Budgets List
        if (categoryBudgets.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No category budgets for ${DateUtils.formatMonthKeyForDisplay(selectedMonth)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set category-level limits for dining, groceries, entertainment, etc.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(categoryBudgets, key = { it.id }) { budget ->
                val spent = stats.categorySpendMap[budget.categoryName] ?: 0L
                val total = budget.amount
                val fraction = (spent.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                val percent = ((spent.toDouble() / total.toDouble()) * 100).toInt()
                val remaining = total - spent

                val isExceeded = percent >= 100
                val isWarning = percent in 75..99

                val progressColor = when {
                    isExceeded -> Color(0xFFDC2626)
                    isWarning -> Color(0xFFF59E0B)
                    else -> Color(0xFF16A34A)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = budget.categoryName ?: "Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        editingBudget = budget
                                        budgetAmountInput = CurrencyHelper.minorUnitsToInputString(budget.amount, currency)
                                        isOverallBudgetSelected = false
                                        selectedCategoryForBudget = expenseCategories.find { it.id == budget.categoryId }
                                        showAddBudgetDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteBudget(budget) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${CurrencyHelper.formatMinorUnits(spent, currency)} / ${CurrencyHelper.formatMinorUnits(total, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = if (isExceeded) "Exceeded by ${CurrencyHelper.formatMinorUnits(Math.abs(remaining), currency)}"
                                else "${CurrencyHelper.formatMinorUnits(remaining, currency)} left ($percent%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Add/Edit Budget Dialog
    if (showAddBudgetDialog) {
        var expandedCatDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            title = {
                Text(
                    if (editingBudget != null) "Edit Budget"
                    else if (isOverallBudgetSelected) "Set Overall Monthly Budget"
                    else "Add Category Budget"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "For month: ${DateUtils.formatMonthKeyForDisplay(selectedMonth)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isOverallBudgetSelected) {
                        ExposedDropdownMenuBox(
                            expanded = expandedCatDropdown,
                            onExpandedChange = { expandedCatDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCategoryForBudget?.name ?: "Select Category",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCatDropdown) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                label = { Text("Category") }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCatDropdown,
                                onDismissRequest = { expandedCatDropdown = false }
                            ) {
                                expenseCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategoryForBudget = cat
                                            expandedCatDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = budgetAmountInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                budgetAmountInput = input
                            }
                        },
                        label = { Text("Budget Amount (${CurrencyHelper.getCurrencyInfo(currency).symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountMinor = CurrencyHelper.parseToMinorUnits(budgetAmountInput, currency)
                        if (amountMinor > 0) {
                            val catId = if (isOverallBudgetSelected) null else selectedCategoryForBudget?.id
                            val catName = if (isOverallBudgetSelected) null else selectedCategoryForBudget?.name
                            viewModel.saveBudget(selectedMonth, catId, catName, amountMinor)
                            showAddBudgetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
