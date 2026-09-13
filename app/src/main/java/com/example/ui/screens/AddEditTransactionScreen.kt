package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.TransactionEntity
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.CurrencyHelper
import com.example.util.DateUtils
import com.example.util.IconHelper
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: ExpenseViewModel,
    transactionId: String?,
    initialType: String,
    initialAmount: Long? = null,
    initialMerchant: String? = null,
    initialDate: Long? = null,
    initialTime: String? = null,
    initialPaymentMethod: String? = null,
    initialNote: String? = null,
    initialTags: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currency by viewModel.currency.collectAsState()
    val currencyInfo = CurrencyHelper.getCurrencyInfo(currency)

    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()

    var type by remember { mutableStateOf(initialType) }
    var amountInput by remember {
        mutableStateOf(
            if (initialAmount != null && initialAmount > 0) CurrencyHelper.minorUnitsToInputString(initialAmount, currency) else ""
        )
    }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedDateMillis by remember { mutableLongStateOf(initialDate ?: System.currentTimeMillis()) }
    var selectedTime by remember { mutableStateOf(initialTime ?: DateUtils.formatTime(initialDate ?: System.currentTimeMillis())) }
    var selectedPaymentMethod by remember { mutableStateOf(initialPaymentMethod ?: "UPI") }
    var merchant by remember { mutableStateOf(initialMerchant ?: "") }
    var note by remember { mutableStateOf(initialNote ?: "") }
    var tags by remember { mutableStateOf(initialTags ?: "") }
    var receiptUri by remember { mutableStateOf<String?>(null) }

    // Recurring
    var isRecurring by remember { mutableStateOf(false) }
    var recurringFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY.name) }
    var recurringEndDate by remember { mutableStateOf<Long?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Existing transaction if editing
    var existingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(transactionId) {
        if (!transactionId.isNullOrBlank()) {
            val tx = viewModel.database.transactionDao().getTransactionById(transactionId)
            if (tx != null) {
                existingTransaction = tx
                type = tx.type
                amountInput = CurrencyHelper.minorUnitsToInputString(tx.amount, currency)
                selectedDateMillis = tx.date
                selectedTime = tx.time
                selectedPaymentMethod = tx.paymentMethod
                merchant = tx.merchant
                note = tx.note
                tags = tx.tags
                receiptUri = tx.receiptUri
            }
        }
    }

    val prefillData by viewModel.prefillTransactionData.collectAsState()
    LaunchedEffect(prefillData) {
        val prefill = prefillData
        if (transactionId.isNullOrBlank() && prefill != null) {
            type = prefill.type
            if (prefill.amountMinor != null && prefill.amountMinor > 0) {
                amountInput = CurrencyHelper.minorUnitsToInputString(prefill.amountMinor, currency)
            }
            if (!prefill.merchant.isNullOrBlank()) {
                merchant = prefill.merchant
            }
            if (prefill.date != null) {
                selectedDateMillis = prefill.date
            }
            if (!prefill.time.isNullOrBlank()) {
                selectedTime = prefill.time
            }
            if (!prefill.paymentMethod.isNullOrBlank()) {
                selectedPaymentMethod = prefill.paymentMethod
            }
            if (!prefill.note.isNullOrBlank()) {
                note = prefill.note
            }
            if (!prefill.tags.isNullOrBlank()) {
                tags = prefill.tags
            }
        }
    }

    // Set default category when categories load if none selected
    val activeCategories = if (type == "EXPENSE") expenseCategories else incomeCategories
    LaunchedEffect(activeCategories, selectedCategory) {
        if (selectedCategory == null && activeCategories.isNotEmpty()) {
            val matched = if (existingTransaction != null) {
                activeCategories.find { it.id == existingTransaction?.categoryId || it.name == existingTransaction?.categoryName }
            } else null
            selectedCategory = matched ?: activeCategories.firstOrNull()
        }
    }

    // Photo picker launcher for receipts
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            // Copy file to internal app storage
            val savedPath = saveReceiptToInternalStorage(context, sourceUri)
            receiptUri = savedPath
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingTransaction != null) "Edit Transaction" else if (type == "EXPENSE") "Add Expense" else "Add Income",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existingTransaction != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Expense / Income Tabs (if creating new)
            if (existingTransaction == null) {
                TabRow(
                    selectedTabIndex = if (type == "EXPENSE") 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Tab(
                        selected = type == "EXPENSE",
                        onClick = {
                            type = "EXPENSE"
                            selectedCategory = expenseCategories.firstOrNull()
                        },
                        text = { Text("Expense", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = type == "INCOME",
                        onClick = {
                            type = "INCOME"
                            selectedCategory = incomeCategories.firstOrNull()
                        },
                        text = { Text("Income", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Amount Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ENTER AMOUNT (${currencyInfo.symbol})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currencyInfo.symbol,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "EXPENSE") Color(0xFFDC2626) else Color(0xFF16A34A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    amountInput = input
                                    errorMessage = null
                                }
                            },
                            modifier = Modifier.width(220.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("0.00", fontSize = 28.sp) },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (type == "EXPENSE") Color(0xFFDC2626) else Color(0xFF16A34A)
                            )
                        )
                    }
                }
            }

            // Category Selector
            Column {
                Text(
                    text = "Category *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    activeCategories.forEach { cat ->
                        val isSelected = selectedCategory?.id == cat.id
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(cat.color).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconHelper.getIcon(cat.icon),
                                    contentDescription = cat.name,
                                    tint = Color(cat.color),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Date & Time Pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(DateUtils.formatDate(selectedDateMillis))
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedTime)
                }
            }

            // Payment Method Selector
            Column {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { pm ->
                        FilterChip(
                            selected = selectedPaymentMethod == pm.name,
                            onClick = { selectedPaymentMethod = pm.name },
                            label = { Text(pm.name) }
                        )
                    }
                }
            }

            // Merchant / Payee
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text(if (type == "EXPENSE") "Merchant / Store" else "Payer / Source") },
                placeholder = { Text(if (type == "EXPENSE") "e.g., Walmart, Amazon, Star Cafe" else "e.g., ACME Corp") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Notes
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add any extra details here...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            // Tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated)") },
                placeholder = { Text("e.g. personal, trip, essentials") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Receipt Photo Attachment
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Receipt / Bill Photo", fontWeight = FontWeight.SemiBold)
                        }

                        if (receiptUri == null) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Attach")
                            }
                        }
                    }

                    if (receiptUri != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = File(receiptUri!!),
                                contentDescription = "Receipt photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { receiptUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Recurring Transaction Option (for new entries)
            if (existingTransaction == null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Repeat this transaction", fontWeight = FontWeight.SemiBold)
                                Text("Automatically creates future entries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it }
                            )
                        }

                        if (isRecurring) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { freq ->
                                    FilterChip(
                                        selected = recurringFrequency == freq,
                                        onClick = { recurringFrequency = freq },
                                        label = { Text(freq.lowercase().replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error Message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Large Save Button
            Button(
                onClick = {
                    val amountMinor = CurrencyHelper.parseToMinorUnits(amountInput, currency)
                    if (amountMinor <= 0) {
                        errorMessage = "Please enter an amount greater than zero."
                        return@Button
                    }
                    val cat = selectedCategory
                    if (cat == null) {
                        errorMessage = "Please select a category."
                        return@Button
                    }

                    viewModel.saveTransaction(
                        id = existingTransaction?.id,
                        type = type,
                        amountMinor = amountMinor,
                        categoryId = cat.id,
                        categoryName = cat.name,
                        categoryIcon = cat.icon,
                        date = selectedDateMillis,
                        time = selectedTime,
                        paymentMethod = selectedPaymentMethod,
                        merchant = merchant.trim(),
                        note = note.trim(),
                        tags = tags.trim(),
                        receiptUri = receiptUri,
                        isRecurring = isRecurring,
                        recurringFrequency = recurringFrequency,
                        recurringEndDate = recurringEndDate
                    )
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "EXPENSE") MaterialTheme.colorScheme.primary else Color(0xFF16A34A)
                )
            ) {
                Text(
                    text = if (existingTransaction != null) "Update Transaction" else if (type == "EXPENSE") "Save Expense" else "Save Income",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val cal = Calendar.getInstance()
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = timePickerState.hour
                        val minute = timePickerState.minute
                        val amPm = if (hour >= 12) "PM" else "AM"
                        val displayHour = if (hour % 12 == 0) 12 else hour % 12
                        selectedTime = String.format("%02d:%02d %s", displayHour, minute, amPm)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm && existingTransaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction permanently?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteTransaction(existingTransaction!!)
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun saveReceiptToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val receiptsDir = File(context.filesDir, "receipts")
        if (!receiptsDir.exists()) receiptsDir.mkdirs()
        val file = File(receiptsDir, "receipt_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
