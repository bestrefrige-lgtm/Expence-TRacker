package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.BackupValidationResult
import com.example.util.CsvExportHelper
import com.example.util.CurrencyHelper
import com.example.util.PdfExportHelper
import com.example.util.RestoreStrategy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currency by viewModel.currency.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val budgetAlerts by viewModel.budgetAlertsEnabled.collectAsState()
    val recurringReminders by viewModel.recurringRemindersEnabled.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    val shareEnabled by viewModel.shareToExpenseEnabled.collectAsState()
    val shareConfirm by viewModel.confirmBeforeSavingShare.collectAsState()
    val shareAutoCategory by viewModel.autoCategoryShare.collectAsState()
    val shareAutoSave by viewModel.autoSaveHighConfidence.collectAsState()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showResetDefaultsConfirm by remember { mutableStateOf(false) }
    var showResetAppConfirm by remember { mutableStateOf(false) }

    val backupValidation by viewModel.backupValidationResult.collectAsState()
    var selectedRestoreStrategy by remember { mutableStateOf(RestoreStrategy.SKIP_DUPLICATES) }

    // Backup Save Launcher (SAF)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            viewModel.exportBackup { json ->
                writeJsonToUri(context, targetUri, json)
            }
        }
    }

    // Restore File Picker Launcher (SAF)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            val json = readJsonFromUri(context, sourceUri)
            if (json != null) {
                viewModel.validateBackupContent(json)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // UNINSTALL WARNING BANNER (Crucial offline-first requirement)
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFFFEF3C7) // Amber 100
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Your Data is Stored Locally",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Create a backup before uninstalling or switching phones.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            createDocumentLauncher.launch("ExpenseTracker_Backup_${System.currentTimeMillis()}.json")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BACKUP NOW", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Appearance & Localization
        item {
            SettingsSectionHeader(title = "Preferences")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Paid,
                        title = "Currency",
                        subtitle = CurrencyHelper.getCurrencyInfo(currency).name,
                        onClick = { showCurrencyDialog = true }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.FormatPaint,
                        title = "App Theme",
                        subtitle = themeMode.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showThemeDialog = true }
                    )
                }
            }
        }

        // Backup & Restore Section
        item {
            SettingsSectionHeader(title = "Backup & Data Export")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.CloudUpload,
                        title = "Backup Data (JSON)",
                        subtitle = "Save a complete database snapshot to your device storage",
                        onClick = {
                            createDocumentLauncher.launch("ExpenseTracker_Backup_${System.currentTimeMillis()}.json")
                        }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.CloudDownload,
                        title = "Restore Backup",
                        subtitle = "Import and restore financial records from a backup file",
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.TableChart,
                        title = "Export to CSV",
                        subtitle = "Share spreadsheet with date, category, amount, notes",
                        onClick = {
                            val csv = CsvExportHelper.generateCsv(allTransactions, currency)
                            CsvExportHelper.shareCsv(context, csv)
                        }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Export PDF Report",
                        subtitle = "Generate structured financial summary report and share",
                        onClick = {
                            val report = viewModel.reportStats.value
                            PdfExportHelper.generateAndSharePdf(
                                context = context,
                                dateRangeLabel = "All Time",
                                totalIncomeMinor = report.totalIncome,
                                totalExpenseMinor = report.totalExpense,
                                currencyCode = currency,
                                categoryBreakdown = report.categoryBreakdown,
                                transactions = allTransactions
                            )
                        }
                    )
                }
            }
        }

        // Data Management
        item {
            SettingsSectionHeader(title = "Manage Entities")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Category,
                        title = "Manage Categories",
                        subtitle = "Add custom categories, edit icons, set order",
                        onClick = onNavigateToCategories
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.Repeat,
                        title = "Recurring Transactions",
                        subtitle = "View recurring rules, pause, resume or delete",
                        onClick = onNavigateToRecurring
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Payment Methods",
                        subtitle = "Cash, UPI, Credit Card, Bank Accounts",
                        onClick = onNavigateToPaymentMethods
                    )
                }
            }
        }

        // Transaction Sharing (PhonePe & UPI)
        item {
            SettingsSectionHeader(title = "Transaction Sharing (Share-to-Expense)")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Enable Share-to-Expense", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Receive and parse transactions shared from PhonePe & UPI apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = shareEnabled,
                            onCheckedChange = { viewModel.updateShareToExpenseEnabled(it) }
                        )
                    }

                    if (shareEnabled) {
                        SettingsDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Confirm Before Saving", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Always show transaction preview dialog before adding to database", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = shareConfirm,
                                onCheckedChange = { viewModel.updateConfirmBeforeSavingShare(it) }
                            )
                        }

                        SettingsDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Category Suggestion", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Automatically identify merchant categories (e.g. Swiggy → Food)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = shareAutoCategory,
                                onCheckedChange = { viewModel.updateAutoCategoryShare(it) }
                            )
                        }

                        SettingsDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Save High-Confidence", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Bypass preview only when 100% matched without duplicates (Default: Off)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = shareAutoSave,
                                onCheckedChange = { viewModel.updateAutoSaveHighConfidence(it) }
                            )
                        }
                    }
                }
            }
        }

        // Notifications
        item {
            SettingsSectionHeader(title = "Alerts & Notifications")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Budget Limit Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Warn at 75%, 90%, and 100% budget usage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = budgetAlerts,
                            onCheckedChange = { viewModel.updateBudgetAlerts(it) }
                        )
                    }
                    SettingsDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Recurring Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Notify when auto-recurring payments execute", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = recurringReminders,
                            onCheckedChange = { viewModel.updateRecurringReminders(it) }
                        )
                    }
                }
            }
        }

        // Danger Zone
        item {
            SettingsSectionHeader(title = "Danger Zone", color = MaterialTheme.colorScheme.error)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Clear Transaction History",
                        subtitle = "Deletes all expenses and incomes, keeps categories & budgets",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showClearConfirm = true }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.RestartAlt,
                        title = "Reset Categories to Default",
                        subtitle = "Restores initial built-in expense and income categories",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showResetDefaultsConfirm = true }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Reset All Application Data",
                        subtitle = "Erases all transactions, custom categories, budgets, and settings",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showResetAppConfirm = true }
                    )
                }
            }
        }

        // About
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Expense Tracker v1.0", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("100% Offline & Private", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No cloud, no logins, no tracking. Your finances stay on your device.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Currency Selector Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    CurrencyHelper.supportedCurrencies.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateCurrency(c.code)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currency == c.code,
                                onClick = {
                                    viewModel.updateCurrency(c.code)
                                    showCurrencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(c.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
            }
        )
    }

    // Theme Selector Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    listOf("SYSTEM" to "System Default", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    viewModel.updateThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    // Restore Backup Confirmation Dialog
    if (backupValidation != null) {
        val result = backupValidation!!
        if (result.isValid && result.backupData != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearBackupValidation() },
                title = { Text("Restore Backup") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Backup File Summary:", fontWeight = FontWeight.Bold)
                        Text("• Expenses: ${result.expenseCount}")
                        Text("• Income: ${result.incomeCount}")
                        Text("• Categories: ${result.categoryCount}")
                        Text("• Budgets: ${result.budgetCount}")
                        Text("• Recurring: ${result.recurringCount}")

                        if (result.duplicateTransactionCount > 0) {
                            Text(
                                "⚠️ ${result.duplicateTransactionCount} existing duplicate transactions detected.",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Import Strategy:", fontWeight = FontWeight.Bold)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedRestoreStrategy = RestoreStrategy.SKIP_DUPLICATES }
                        ) {
                            RadioButton(
                                selected = selectedRestoreStrategy == RestoreStrategy.SKIP_DUPLICATES,
                                onClick = { selectedRestoreStrategy = RestoreStrategy.SKIP_DUPLICATES }
                            )
                            Text("Skip duplicates (Safe)")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedRestoreStrategy = RestoreStrategy.REPLACE_EXISTING }
                        ) {
                            RadioButton(
                                selected = selectedRestoreStrategy == RestoreStrategy.REPLACE_EXISTING,
                                onClick = { selectedRestoreStrategy = RestoreStrategy.REPLACE_EXISTING }
                            )
                            Text("Replace all existing records")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.applyRestore(selectedRestoreStrategy) {}
                        }
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearBackupValidation() }) {
                        Text("Cancel")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.clearBackupValidation() },
                title = { Text("Invalid Backup File") },
                text = { Text(result.errorMessage ?: "The selected file is not a valid Expense Tracker backup.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearBackupValidation() }) { Text("OK") }
                }
            )
        }
    }

    // Clear Transactions Confirmation
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Transactions?") },
            text = { Text("Are you sure you want to delete all transaction history? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearAllTransactions {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Reset Defaults Confirmation
    if (showResetDefaultsConfirm) {
        AlertDialog(
            onDismissRequest = { showResetDefaultsConfirm = false },
            title = { Text("Reset Categories?") },
            text = { Text("Reset all categories back to default built-in categories?") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDefaultsConfirm = false
                        viewModel.resetCategoriesToDefaults()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDefaultsConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Reset App Confirmation
    if (showResetAppConfirm) {
        AlertDialog(
            onDismissRequest = { showResetAppConfirm = false },
            title = { Text("Reset Entire Application?") },
            text = { Text("This will permanently delete all transactions, budgets, custom categories, recurring rules, and preferences. Make sure you have created a backup first!") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetAppConfirm = false
                        viewModel.resetApplicationData {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAppConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = titleColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = titleColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

fun writeJsonToUri(context: Context, uri: Uri, json: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os).use { writer ->
                writer.write(json)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun readJsonFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
