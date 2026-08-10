package com.myexpense.tracker.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
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
import com.myexpense.tracker.ui.screens.reports.ReportsScreen
import com.myexpense.tracker.ui.screens.transactions.TransactionsScreen

/** Routes shown in the floating pill bottom bar. */
private val tabRoutes = listOf(
    Routes.HOME,
    Routes.ACCOUNTS,
    Routes.STATS,
    Routes.SETTINGS,
)

@Composable
fun MoneyMateNavHost() {
    val navController = rememberNavController()
    var addMenuOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val destination = backStackEntry?.destination
            val isTab = tabRoutes.any { tab ->
                destination?.hierarchy?.any { it.route == tab } == true
            }
            if (isTab) {
                FloatingPillNavBar(
                    selectedRoute = destination?.route,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddClick = { addMenuOpen = !addMenuOpen },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onSeeAllTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                        onSeeAllBudgets = { navController.navigate(Routes.BUDGETS) },
                        onSeeAllStats = { navController.navigate(Routes.STATS) },
                        onOpenSearch = { navController.navigate(Routes.SEARCH) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onAddExpense = {
                            navController.navigate(Routes.addTransaction(TransactionType.EXPENSE)) {
                                launchSingleTop = true
                            }
                        },
                        onAddIncome = {
                            navController.navigate(Routes.addTransaction(TransactionType.INCOME)) {
                                launchSingleTop = true
                            }
                        },
                        onTransfer = {
                            navController.navigate(Routes.addTransaction(TransactionType.TRANSFER)) {
                                launchSingleTop = true
                            }
                        },
                        onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) },
                    )
                }

                composable(Routes.TRANSACTIONS) {
                    TransactionsScreen(
                        onAddTransaction = {
                            navController.navigate(Routes.addTransaction(TransactionType.EXPENSE))
                        },
                        onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) },
                        onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    )
                }

                composable(Routes.STATS) {
                    ReportsScreen(
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
                        onOpenBudgets = { navController.navigate(Routes.BUDGETS) },
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

                composable(
                    route = Routes.ADD_TRANSACTION,
                    arguments = listOf(
                        navArgument("type") {
                            type = NavType.StringType
                            defaultValue = TransactionType.EXPENSE.name
                        }
                    ),
                ) {
                    AddEditTransactionScreen(onDone = { navController.popBackStack() })
                }

                composable(
                    route = Routes.EDIT_TRANSACTION,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) {
                    AddEditTransactionScreen(onDone = { navController.popBackStack() })
                }
            }

            if (addMenuOpen) {
                SpeedDialOverlay(
                    onDismiss = { addMenuOpen = false },
                    onExpense = {
                        addMenuOpen = false
                        navController.navigate(Routes.addTransaction(TransactionType.EXPENSE)) {
                            launchSingleTop = true
                        }
                    },
                    onIncome = {
                        addMenuOpen = false
                        navController.navigate(Routes.addTransaction(TransactionType.INCOME)) {
                            launchSingleTop = true
                        }
                    },
                    onTransfer = {
                        addMenuOpen = false
                        navController.navigate(Routes.addTransaction(TransactionType.TRANSFER)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
