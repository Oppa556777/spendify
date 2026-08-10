package com.myexpense.tracker.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.myexpense.tracker.ui.screens.accounts.AccountDetailScreen
import com.myexpense.tracker.ui.screens.addedit.AddEditTransactionScreen
import com.myexpense.tracker.ui.screens.backup.BackupRestoreScreen
import com.myexpense.tracker.ui.screens.budgets.BudgetsScreen
import com.myexpense.tracker.ui.screens.budgets.BudgetDetailScreen
import com.myexpense.tracker.ui.screens.achievements.AchievementsScreen
import com.myexpense.tracker.ui.screens.assets.AssetsScreen
import com.myexpense.tracker.ui.screens.billsplits.BillSplitsScreen
import com.myexpense.tracker.ui.screens.calendar.CalendarScreen
import com.myexpense.tracker.ui.screens.categories.CategoriesScreen
import com.myexpense.tracker.ui.screens.more.MoreScreen
import com.myexpense.tracker.ui.screens.people.PeopleScreen
import com.myexpense.tracker.ui.screens.people.PersonDetailScreen
import com.myexpense.tracker.ui.screens.recurring.RecurringScreen
import com.myexpense.tracker.ui.screens.tags.TagsManagerScreen
import com.myexpense.tracker.ui.screens.goals.GoalsScreen
import com.myexpense.tracker.ui.screens.loans.LoansScreen
import com.myexpense.tracker.ui.screens.subscriptions.SubscriptionsScreen
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
    Routes.MORE,
)

@Composable
fun MoneyMateNavHost(initialDestination: String? = null) {
    val navController = rememberNavController()
    var addMenuOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialDestination) {
        if (initialDestination == "reports") {
            navController.navigate(Routes.STATS) {
                launchSingleTop = true
            }
        }
    }

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
                enterTransition = {
                    androidx.compose.animation.fadeIn(tween(300)) +
                        androidx.compose.animation.slideInHorizontally(tween(300)) { it / 4 }
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(tween(300)) +
                        androidx.compose.animation.slideOutHorizontally(tween(300)) { -it / 4 }
                },
                popEnterTransition = { androidx.compose.animation.fadeIn(tween(300)) },
                popExitTransition = { androidx.compose.animation.fadeOut(tween(300)) },
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
                        onBudgetClick = { id -> navController.navigate(Routes.budgetDetail(id)) },
                    )
                }

                composable(
                    route = Routes.BUDGET_DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) {
                    BudgetDetailScreen(
                        onBack = { navController.popBackStack() },
                        onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) },
                    )
                }

                composable(Routes.MORE) {
                    MoreScreen(
                        onAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                        onSplitBill = { navController.navigate(Routes.BILL_SPLITS) },
                        onAssets = { navController.navigate(Routes.ASSETS) },
                        onPeople = { navController.navigate(Routes.PEOPLE) },
                        onTags = { navController.navigate(Routes.TAGS) },
                        onRecurring = { navController.navigate(Routes.RECURRING) },
                        onReports = { navController.navigate(Routes.STATS) },
                        onCalendar = { navController.navigate(Routes.CALENDAR) },
                        onExport = { navController.navigate(Routes.STATS) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onCurrencies = { navController.navigate(Routes.SETTINGS) },
                        onCategories = { navController.navigate(Routes.CATEGORIES) },
                        onAccounts = { navController.navigate(Routes.ACCOUNTS) },
                        onBackup = { navController.navigate(Routes.BACKUP) },
                        onRestore = { navController.navigate(Routes.BACKUP) },
                        onClearAll = { navController.navigate(Routes.SETTINGS) },
                        onAbout = { navController.navigate(Routes.SETTINGS) },
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                        onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                        onOpenBudgets = { navController.navigate(Routes.BUDGETS) },
                        onOpenGoals = { navController.navigate(Routes.GOALS) },
                        onOpenLoans = { navController.navigate(Routes.LOANS) },
                        onOpenSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) },
                        onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                        onOpenSplitBill = { navController.navigate(Routes.BILL_SPLITS) },
                        onOpenAssets = { navController.navigate(Routes.ASSETS) },
                        onOpenBackup = { navController.navigate(Routes.BACKUP) },
                        onClearAll = { navController.navigate(Routes.SETTINGS) },
                    )
                }

                composable(Routes.PEOPLE) {
                    PeopleScreen(
                        onBack = { navController.popBackStack() },
                        onPersonTap = { id -> navController.navigate(Routes.personDetail(id)) },
                    )
                }

                composable(
                    route = Routes.PERSON_DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) {
                    PersonDetailScreen(
                        onBack = { navController.popBackStack() },
                        onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) },
                    )
                }

                composable(Routes.TAGS) {
                    TagsManagerScreen(
                        onBack = { navController.popBackStack() },
                        onTagTap = { navController.navigate(Routes.SEARCH) },
                    )
                }

                composable(Routes.RECURRING) {
                    RecurringScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.CALENDAR) {
                    CalendarScreen(
                        onBack = { navController.popBackStack() },
                        onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) },
                    )
                }

                composable(Routes.ACCOUNTS) {
                    AccountsScreen(
                        onBack = { navController.popBackStack() },
                        onAccountClick = { id -> navController.navigate(Routes.accountDetail(id)) },
                    )
                }

                composable(
                    route = Routes.ACCOUNT_DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) {
                    AccountDetailScreen(
                        onBack = { navController.popBackStack() },
                        onTransfer = { accountId ->
                            navController.navigate(Routes.transferFrom(accountId)) {
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(Routes.CATEGORIES) {
                    CategoriesScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.GOALS) {
                    GoalsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.ACHIEVEMENTS) {
                    AchievementsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.BILL_SPLITS) {
                    BillSplitsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.ASSETS) {
                    AssetsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.LOANS) {
                    LoansScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.SUBSCRIPTIONS) {
                    SubscriptionsScreen(onBack = { navController.popBackStack() })
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
                        },
                        navArgument("from") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    ),
                ) {
                    AddEditTransactionScreen(
                        onDone = { navController.popBackStack() },
                        onSplitBill = { navController.navigate(Routes.BILL_SPLITS) },
                    )
                }

                composable(
                    route = Routes.EDIT_TRANSACTION,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) {
                    AddEditTransactionScreen(
                        onDone = { navController.popBackStack() },
                        onSplitBill = { navController.navigate(Routes.BILL_SPLITS) },
                    )
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
