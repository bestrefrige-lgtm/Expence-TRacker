package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.parser.ParsedTransaction
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.CurrencyHelper
import com.example.util.DateUtils
import com.example.util.IconHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTransactionDetectedScreen(
    viewModel: ExpenseViewModel,
    onSaved: (TransactionEntity) -> Unit,
    onEditManually: (ParsedTransaction?, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency by viewModel.currency.collectAsState()
    val parsedTx by viewModel.parsedSharedTransaction.collectAsState()
    val rawText by viewModel.sharedRawText.collectAsState()
    val duplicateTx by viewModel.duplicateTransaction.collectAsState()
    val isProcessing by viewModel.isProcessingShare.collectAsState()

    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDuplicateDetailsDialog by remember { mutableStateOf(false) }
    var ignoreDuplicate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (parsedTx != null) "Transaction Detected" else "Shared Transaction",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.clearSharedTransaction()
                        onDismiss()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isProcessing) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing shared transaction text...", style = MaterialTheme.typography.titleMedium)
                }
            } else if (parsedTx == null || parsedTx!!.amount <= 0) {
                // Parsing failed or could not extract valid amount
                ParsingFailedView(
                    rawText = rawText ?: "",
                    onManualEntry = {
                        onEditManually(null, rawText ?: "")
                    },
                    onCancel = {
                        viewModel.clearSharedTransaction()
                        onDismiss()
                    }
                )
            } else {
                val tx = parsedTx!!
                val isExpense = tx.transactionType == "EXPENSE"
                val activeCategories = if (isExpense) expenseCategories else incomeCategories

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Possible Duplicate Warning Banner
                    if (duplicateTx != null && !ignoreDuplicate) {
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color(0xFFFEF2F2) // Red 50
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFEF4444), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "Possible Duplicate Transaction",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF991B1B)
                                            )
                                            Text(
                                                "A transaction matching this amount & details already exists.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFB91C1C)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showDuplicateDetailsDialog = true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View Existing")
                                        }

                                        Button(
                                            onClick = { ignoreDuplicate = true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                        ) {
                                            Text("Add Anyway")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Source & Confidence Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Imported from ${tx.parserSource}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "${(tx.overallConfidence * 100).toInt()}% Match",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main Transaction Card
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Amount & Type selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Detected Amount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = CurrencyHelper.formatMinorUnits(tx.amount, currency),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isExpense) Color(0xFFDC2626) else Color(0xFF16A34A)
                                        )
                                    }

                                    // Expense / Income Selector Chips
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = isExpense,
                                            onClick = { viewModel.updateParsedType("EXPENSE") },
                                            leadingIcon = {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                            },
                                            label = { Text("Expense") }
                                        )
                                        FilterChip(
                                            selected = !isExpense,
                                            onClick = { viewModel.updateParsedType("INCOME") },
                                            leadingIcon = {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                            },
                                            label = { Text("Income") }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Merchant / Payee
                                OutlinedTextField(
                                    value = tx.merchant,
                                    onValueChange = { viewModel.updateParsedMerchant(it) },
                                    label = { Text(if (isExpense) "Paid to (Merchant / Person)" else "Received from (Sender)") },
                                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Category selector
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showCategoryPicker = true },
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("Suggested Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text(tx.suggestedCategory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        TextButton(onClick = { showCategoryPicker = true }) {
                                            Text("Change")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Date & Time display
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(DateUtils.formatDate(tx.date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                    if (tx.time.isNotBlank()) {
                                                        Text(" • ${tx.time}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                                    }
                                                }
                                                Text(
                                                    text = if (tx.dateIsDetected) "Detected from receipt" else "Auto-assigned (Today)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (tx.dateIsDetected) Color(0xFF16A34A) else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Payment Method & Reference ID
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                val next = if (tx.paymentMethod == "UPI") "Cash" else "UPI"
                                                viewModel.updateParsedPaymentMethod(next)
                                            },
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Payment Method", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(tx.paymentMethod, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (tx.transactionId.isNotBlank() || tx.utr.isNotBlank()) {
                                        val idToShow = tx.transactionId.ifBlank { tx.utr }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("Transaction ID / UTR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(idToShow, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Field Confidence Breakdown
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Parser Confidence Details", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ConfidenceChip("Amount", tx.confidence["amount"] ?: 0.98f)
                                    ConfidenceChip("Date", tx.confidence["date"] ?: 0.95f)
                                    ConfidenceChip("Merchant", tx.confidence["merchant"] ?: 0.90f)
                                    ConfidenceChip("Category", tx.confidence["category"] ?: 0.85f)
                                }
                            }
                        }
                    }

                    // Action Buttons
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Primary Save Button
                            Button(
                                onClick = {
                                    viewModel.saveParsedSharedTransaction { savedEntity ->
                                        onSaved(savedEntity)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isExpense) "Save Expense" else "Save Income",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Secondary: Edit Full Form
                            OutlinedButton(
                                onClick = {
                                    onEditManually(tx, rawText ?: "")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit in Full Form")
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    // Category Selector Dialog
    if (showCategoryPicker && parsedTx != null) {
        val isExpense = parsedTx!!.transactionType == "EXPENSE"
        val activeCategories = if (isExpense) expenseCategories else incomeCategories

        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Select Category") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeCategories.size) { idx ->
                        val cat = activeCategories[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateParsedCategory(cat.name, cat.id)
                                    showCategoryPicker = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(cat.color).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    IconHelper.getIcon(cat.icon),
                                    contentDescription = null,
                                    tint = Color(cat.color),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryPicker = false }) { Text("Close") }
            }
        )
    }

    // Existing Duplicate Details Dialog
    if (showDuplicateDetailsDialog && duplicateTx != null) {
        val dup = duplicateTx!!
        AlertDialog(
            onDismissRequest = { showDuplicateDetailsDialog = false },
            title = { Text("Existing Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Amount: ${CurrencyHelper.formatMinorUnits(dup.amount, currency)}", fontWeight = FontWeight.Bold)
                    Text("Merchant: ${dup.merchant.ifBlank { "N/A" }}")
                    Text("Category: ${dup.categoryName}")
                    Text("Date: ${DateUtils.formatDate(dup.date)} (${dup.time})")
                    Text("Payment: ${dup.paymentMethod}")
                    if (dup.note.isNotBlank()) Text("Notes: ${dup.note}")
                }
            },
            confirmButton = {
                Button(onClick = { showDuplicateDetailsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ConfidenceChip(label: String, score: Float) {
    val pct = (score * 100).toInt()
    val color = when {
        pct >= 90 -> Color(0xFF16A34A)
        pct >= 75 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text("$pct%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ParsingFailedView(
    rawText: String,
    onManualEntry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Couldn't automatically identify this transaction",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The shared text did not contain a recognized financial transaction format. You can create the transaction manually with the shared text preserved in notes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (rawText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Text(
                    text = rawText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onManualEntry,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enter Manually")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}
