package com.myexpense.tracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.ui.screens.accounts.AccountsScreen
import com.myexpense.tracker.ui.screens.addedit.AddEditTransactionScreen
import com.myexpense.tracker.ui.screens.backup.BackupRestoreScreen
import com.myexpense.tracker.ui.screens.budgets.BudgetsScreen
import com.myexpense.tracker.ui.screens.categories.CategoriesScreen
import com.myexpense.tracker.ui.screens.home.HomeScreen
import com.myexpense.tracker.ui.screens.search.SearchScreen
import com.myexpense.tracker.ui.screens.settings.SettingsScreen
import com.myexpense.tracker.ui.screens.stats.StatsScreen
import com.myexpense.tracker.ui.screens.transactions.TransactionsScreen

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "Home", Icons.Filled.Home),
    BottomTab(Routes.TRANSACTIONS, "Transactions", Icons.Filled.ReceiptLong),
    BottomTab(Routes.STATS, "Stats", Icons.Filled.BarChart),
    BottomTab(Routes.BUDGETS, "Budgets", Icons.Filled.Savings),
    BottomTab(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun MoneyMateNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            val showBottomBar = bottomTabs.any { tab ->
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
            }
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                    onNavigateToStats = { navController.navigate(Routes.STATS) },
                    onNavigateToBudgets = { navController.navigate(Routes.BUDGETS) },
                    onNavigateToAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    onNavigateToCategories = { navController.navigate(Routes.CATEGORIES) },
                    onAddTransaction = { type ->
                        navController.navigate(Routes.ADD_TRANSACTION) {
                            launchSingleTop = true
                        }
                    },
                    onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) },
                )
            }

            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    onAddTransaction = { navController.navigate(Routes.ADD_TRANSACTION) },
                    onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                )
            }

            composable(Routes.STATS) {
                StatsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.BUDGETS) {
                BudgetsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                )
            }

            composable(Routes.ACCOUNTS) {
                AccountsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CATEGORIES) {
                CategoriesScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) },
                )
            }

            composable(Routes.BACKUP) {
                BackupRestoreScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.ADD_TRANSACTION) {
                AddEditTransactionScreen(onDone = { navController.popBackStack() })
            }

            composable(
                route = Routes.EDIT_TRANSACTION,
                arguments = listOf(navArgument("id") { androidx.navigation.NavType.LongType }),
            ) {
                AddEditTransactionScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}
