package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.TransactionItemCard
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.SortOrder
import com.example.util.CurrencyHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency by viewModel.currency.collectAsState()
    val groupedTransactions by viewModel.groupedFilteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.selectedDateFilter.collectAsState()
    val typeFilter by viewModel.selectedTypeFilter.collectAsState()
    val categoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val paymentFilter by viewModel.selectedPaymentFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    val categories by viewModel.allCategories.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search & Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by merchant, note, category...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter Sheet Button
                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "All Filters")
                }

                // Type Chips
                FilterChip(
                    selected = typeFilter == "ALL",
                    onClick = { viewModel.selectedTypeFilter.value = "ALL" },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = typeFilter == "EXPENSE",
                    onClick = { viewModel.selectedTypeFilter.value = "EXPENSE" },
                    label = { Text("Expenses") }
                )
                FilterChip(
                    selected = typeFilter == "INCOME",
                    onClick = { viewModel.selectedTypeFilter.value = "INCOME" },
                    label = { Text("Income") }
                )

                // Date Filter Chips
                FilterChip(
                    selected = dateFilter == DateFilterType.TODAY,
                    onClick = {
                        viewModel.selectedDateFilter.value =
                            if (dateFilter == DateFilterType.TODAY) DateFilterType.ALL else DateFilterType.TODAY
                    },
                    label = { Text("Today") }
                )
                FilterChip(
                    selected = dateFilter == DateFilterType.THIS_WEEK,
                    onClick = {
                        viewModel.selectedDateFilter.value =
                            if (dateFilter == DateFilterType.THIS_WEEK) DateFilterType.ALL else DateFilterType.THIS_WEEK
                    },
                    label = { Text("This Week") }
                )
                FilterChip(
                    selected = dateFilter == DateFilterType.THIS_MONTH,
                    onClick = {
                        viewModel.selectedDateFilter.value =
                            if (dateFilter == DateFilterType.THIS_MONTH) DateFilterType.ALL else DateFilterType.THIS_MONTH
                    },
                    label = { Text("This Month") }
                )

                // Sort Dropdown
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest first") },
                            onClick = {
                                viewModel.sortOrder.value = SortOrder.NEWEST
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest first") },
                            onClick = {
                                viewModel.sortOrder.value = SortOrder.OLDEST
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Highest amount") },
                            onClick = {
                                viewModel.sortOrder.value = SortOrder.HIGHEST_AMOUNT
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lowest amount") },
                            onClick = {
                                viewModel.sortOrder.value = SortOrder.LOWEST_AMOUNT
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Transactions List Grouped by Today, Yesterday, This Week, Earlier
        if (groupedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No matching transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try adjusting your search terms or filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.searchQuery.value = ""
                            viewModel.selectedDateFilter.value = DateFilterType.ALL
                            viewModel.selectedTypeFilter.value = "ALL"
                            viewModel.selectedCategoryFilter.value = "ALL"
                            viewModel.selectedPaymentFilter.value = "ALL"
                            viewModel.minAmountFilter.value = null
                            viewModel.maxAmountFilter.value = null
                        }
                    ) {
                        Text("Reset All Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedTransactions.forEach { (groupTitle, txList) ->
                    item(key = "header_$groupTitle") {
                        Text(
                            text = groupTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(txList, key = { it.id }) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            currencyCode = currency,
                            onClick = { onNavigateToEditTransaction(tx.id) },
                            onEdit = { onNavigateToEditTransaction(tx.id) },
                            onDuplicate = { viewModel.duplicateTransaction(tx) },
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        }
    }

    // Comprehensive Filter Modal BottomSheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            viewModel.selectedTypeFilter.value = "ALL"
                            viewModel.selectedDateFilter.value = DateFilterType.ALL
                            viewModel.selectedCategoryFilter.value = "ALL"
                            viewModel.selectedPaymentFilter.value = "ALL"
                            viewModel.minAmountFilter.value = null
                            viewModel.maxAmountFilter.value = null
                        }
                    ) {
                        Text("Reset")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Filter
                Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoryFilter == "ALL",
                        onClick = { viewModel.selectedCategoryFilter.value = "ALL" },
                        label = { Text("All Categories") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = categoryFilter == cat.name,
                            onClick = { viewModel.selectedCategoryFilter.value = cat.name },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Filter
                Text("Payment Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paymentFilter == "ALL",
                        onClick = { viewModel.selectedPaymentFilter.value = "ALL" },
                        label = { Text("All Methods") }
                    )
                    paymentMethods.forEach { pm ->
                        FilterChip(
                            selected = paymentFilter == pm.name,
                            onClick = { viewModel.selectedPaymentFilter.value = pm.name },
                            label = { Text(pm.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Filters")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
