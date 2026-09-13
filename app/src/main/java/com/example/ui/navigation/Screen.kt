package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.ReceiptLong)
    object Reports : Screen("reports", "Reports", Icons.Default.BarChart)
    object Budgets : Screen("budgets", "Budgets", Icons.Default.AccountBalanceWallet)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    object AddEditTransaction : Screen(
        "add_edit_transaction?id={id}&type={type}",
        "Add Transaction"
    ) {
        fun createRoute(id: String? = null, type: String = "EXPENSE"): String {
            val txId = id ?: ""
            return "add_edit_transaction?id=$txId&type=$type"
        }
    }

    object ManageCategories : Screen("manage_categories", "Categories")
    object ManageRecurring : Screen("manage_recurring", "Recurring Payments")
    object ManagePaymentMethods : Screen("manage_payment_methods", "Payment Methods")
    object ShareDetected : Screen("share_detected", "Transaction Detected")

    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Transactions,
            Reports,
            Budgets,
            Settings
        )
    }
}
