package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddEditTransactionScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ManageCategoriesScreen
import com.example.ui.screens.ManagePaymentMethodsScreen
import com.example.ui.screens.ManageRecurringScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShareTransactionDetectedScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.PrefillTransactionData

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSharedIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            MyApplicationTheme(themePreference = themeMode) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return
        var text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            val clipData = intent.clipData
            if (clipData != null && clipData.itemCount > 0) {
                text = clipData.getItemAt(0).text?.toString()
            }
        }
        if (!text.isNullOrBlank()) {
            viewModel.processIncomingSharedText(text)
        }
    }
}

@Composable
fun MainAppScreen(viewModel: ExpenseViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelScreen = Screen.bottomNavItems.any { it.route == currentRoute }

    val parsedSharedTx by viewModel.parsedSharedTransaction.collectAsState()
    val sharedRawText by viewModel.sharedRawText.collectAsState()
    val shareEnabled by viewModel.shareToExpenseEnabled.collectAsState()

    LaunchedEffect(parsedSharedTx, sharedRawText, shareEnabled) {
        if (shareEnabled && (parsedSharedTx != null || sharedRawText != null)) {
            if (currentRoute != Screen.ShareDetected.route) {
                navController.navigate(Screen.ShareDetected.route)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevelScreen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 6.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(it, contentDescription = screen.title)
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Screen.Dashboard.route || currentRoute == Screen.Transactions.route) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.AddEditTransaction.createRoute(type = "EXPENSE"))
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.AddEditTransaction.createRoute(type = "EXPENSE"))
                    },
                    onNavigateToAddIncome = {
                        navController.navigate(Screen.AddEditTransaction.createRoute(type = "INCOME"))
                    },
                    onNavigateToEditTransaction = { txId ->
                        navController.navigate(Screen.AddEditTransaction.createRoute(id = txId))
                    },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route)
                    },
                    onNavigateToBudgets = {
                        navController.navigate(Screen.Budgets.route)
                    }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = viewModel,
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.AddEditTransaction.createRoute(type = "EXPENSE"))
                    },
                    onNavigateToEditTransaction = { txId ->
                        navController.navigate(Screen.AddEditTransaction.createRoute(id = txId))
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = viewModel)
            }

            composable(Screen.Budgets.route) {
                BudgetsScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToCategories = {
                        navController.navigate(Screen.ManageCategories.route)
                    },
                    onNavigateToRecurring = {
                        navController.navigate(Screen.ManageRecurring.route)
                    },
                    onNavigateToPaymentMethods = {
                        navController.navigate(Screen.ManagePaymentMethods.route)
                    }
                )
            }

            composable(
                route = Screen.AddEditTransaction.route,
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    },
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = "EXPENSE"
                    }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"

                AddEditTransactionScreen(
                    viewModel = viewModel,
                    transactionId = id,
                    initialType = type,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ManageCategories.route) {
                ManageCategoriesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ManageRecurring.route) {
                ManageRecurringScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ManagePaymentMethods.route) {
                ManagePaymentMethodsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ShareDetected.route) {
                ShareTransactionDetectedScreen(
                    viewModel = viewModel,
                    onSaved = { savedEntity ->
                        navController.popBackStack()
                    },
                    onEditManually = { parsed, raw ->
                        val prefill = if (parsed != null) {
                            PrefillTransactionData(
                                type = parsed.transactionType,
                                amountMinor = parsed.amount,
                                merchant = parsed.merchant,
                                date = parsed.date,
                                time = parsed.time,
                                paymentMethod = parsed.paymentMethod,
                                note = parsed.description,
                                tags = if (parsed.transactionId.isNotBlank()) "ref:${parsed.transactionId}" else ""
                            )
                        } else {
                            PrefillTransactionData(
                                note = raw
                            )
                        }
                        viewModel.setPrefillData(prefill)
                        viewModel.clearSharedTransaction()
                        navController.popBackStack()
                        navController.navigate(Screen.AddEditTransaction.createRoute(type = prefill.type))
                    },
                    onDismiss = {
                        viewModel.clearSharedTransaction()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
