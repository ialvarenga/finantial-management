package com.example.organizadordefinancas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.organizadordefinancas.ui.screens.analytics.AnalyticsScreen
import com.example.organizadordefinancas.ui.screens.account.AccountDetailScreen
import com.example.organizadordefinancas.ui.screens.account.AccountListScreen
import com.example.organizadordefinancas.ui.screens.account.AddEditAccountScreen
import com.example.organizadordefinancas.ui.screens.bank.AddEditBankScreen
import com.example.organizadordefinancas.ui.screens.bank.BankListScreen
import com.example.organizadordefinancas.ui.screens.compromise.AddEditCompromiseScreen
import com.example.organizadordefinancas.ui.screens.compromise.CompromiseListScreen
import com.example.organizadordefinancas.ui.screens.creditcard.AddCreditCardItemScreen
import com.example.organizadordefinancas.ui.screens.creditcard.AddEditCreditCardScreen
import com.example.organizadordefinancas.ui.screens.creditcard.CreditCardDetailScreen
import com.example.organizadordefinancas.ui.screens.creditcard.CreditCardListScreen
import com.example.organizadordefinancas.ui.screens.creditcard.EditCreditCardItemScreen
import com.example.organizadordefinancas.ui.screens.creditcard.ImportStatementScreen
import com.example.organizadordefinancas.ui.screens.home.HomeScreen
import com.example.organizadordefinancas.ui.screens.income.AddEditIncomeScreen
import com.example.organizadordefinancas.ui.screens.income.IncomeListScreen
import com.example.organizadordefinancas.ui.screens.notification.NotificationSettingsScreen
import com.example.organizadordefinancas.ui.screens.notification.PendingNotificationsScreen
import com.example.organizadordefinancas.ui.screens.transaction.TransactionListScreen
import com.example.organizadordefinancas.ui.viewmodel.AnalyticsViewModel
import com.example.organizadordefinancas.ui.viewmodel.AccountDetailViewModel
import com.example.organizadordefinancas.ui.viewmodel.AccountListViewModel
import com.example.organizadordefinancas.ui.viewmodel.BankViewModel
import com.example.organizadordefinancas.ui.viewmodel.CreditCardViewModel
import com.example.organizadordefinancas.ui.viewmodel.FinancialCompromiseViewModel
import com.example.organizadordefinancas.ui.viewmodel.IncomeViewModel
import com.example.organizadordefinancas.ui.viewmodel.NotificationViewModel
import com.example.organizadordefinancas.ui.viewmodel.TransactionListViewModel

@Composable
fun FinanceNavHost(
    navController: NavHostController,
    creditCardViewModel: CreditCardViewModel,
    bankViewModel: BankViewModel,
    compromiseViewModel: FinancialCompromiseViewModel,
    incomeViewModel: IncomeViewModel,
    notificationViewModel: NotificationViewModel,
    analyticsViewModel: AnalyticsViewModel,
    accountListViewModel: AccountListViewModel,
    accountDetailViewModel: AccountDetailViewModel,
    transactionListViewModel: TransactionListViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // Home
        composable(Screen.Home.route) {
            HomeScreen(
                creditCardViewModel = creditCardViewModel,
                bankViewModel = bankViewModel,
                compromiseViewModel = compromiseViewModel,
                incomeViewModel = incomeViewModel,
                notificationViewModel = notificationViewModel,
                onNavigateToPendingNotifications = {
                    navController.navigate(Screen.PendingNotifications.route)
                }
            )
        }

        // Credit Cards
        composable(Screen.CreditCards.route) {
            CreditCardListScreen(
                viewModel = creditCardViewModel,
                compromiseViewModel = compromiseViewModel,
                onNavigateToDetail = { cardId ->
                    navController.navigate(Screen.CreditCardDetail.createRoute(cardId))
                },
                onNavigateToAddEdit = { cardId ->
                    navController.navigate(Screen.AddEditCreditCard.createRoute(cardId))
                }
            )
        }

        composable(
            route = Screen.CreditCardDetail.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
            CreditCardDetailScreen(
                cardId = cardId,
                viewModel = creditCardViewModel,
                compromiseViewModel = compromiseViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddItem = { id ->
                    navController.navigate(Screen.AddCreditCardItem.createRoute(id))
                },
                onNavigateToEditItem = { itemId ->
                    navController.navigate(Screen.EditCreditCardItem.createRoute(itemId))
                },
                onNavigateToEditCard = { id ->
                    navController.navigate(Screen.AddEditCreditCard.createRoute(id))
                },
                onNavigateToImport = { id ->
                    navController.navigate(Screen.ImportStatement.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.AddEditCreditCard.route,
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId")?.takeIf { it != -1L }
            AddEditCreditCardScreen(
                cardId = cardId,
                viewModel = creditCardViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddCreditCardItem.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
            AddCreditCardItemScreen(
                cardId = cardId,
                viewModel = creditCardViewModel,
                compromiseViewModel = compromiseViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditCreditCardItem.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
            EditCreditCardItemScreen(
                itemId = itemId,
                viewModel = creditCardViewModel,
                compromiseViewModel = compromiseViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ImportStatement.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
            ImportStatementScreen(
                cardId = cardId,
                viewModel = creditCardViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Banks
        composable(Screen.Banks.route) {
            BankListScreen(
                viewModel = bankViewModel,
                onNavigateToAddEdit = { bankId ->
                    navController.navigate(Screen.AddEditBank.createRoute(bankId))
                }
            )
        }

        composable(
            route = Screen.AddEditBank.route,
            arguments = listOf(
                navArgument("bankId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val bankId = backStackEntry.arguments?.getLong("bankId")?.takeIf { it != -1L }
            AddEditBankScreen(
                bankId = bankId,
                viewModel = bankViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Compromises
        composable(Screen.Compromises.route) {
            CompromiseListScreen(
                viewModel = compromiseViewModel,
                creditCardViewModel = creditCardViewModel,
                onNavigateToAddEdit = { compromiseId ->
                    navController.navigate(Screen.AddEditCompromise.createRoute(compromiseId))
                }
            )
        }

        composable(
            route = Screen.AddEditCompromise.route,
            arguments = listOf(
                navArgument("compromiseId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val compromiseId = backStackEntry.arguments?.getLong("compromiseId")?.takeIf { it != -1L }
            AddEditCompromiseScreen(
                compromiseId = compromiseId,
                viewModel = compromiseViewModel,
                creditCardViewModel = creditCardViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Incomes
        composable(Screen.Incomes.route) {
            IncomeListScreen(
                viewModel = incomeViewModel,
                onNavigateToAddEdit = { incomeId ->
                    navController.navigate(Screen.AddEditIncome.createRoute(incomeId))
                }
            )
        }

        composable(
            route = Screen.AddEditIncome.route,
            arguments = listOf(
                navArgument("incomeId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val incomeId = backStackEntry.arguments?.getLong("incomeId")?.takeIf { it != -1L }
            AddEditIncomeScreen(
                incomeId = incomeId,
                viewModel = incomeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Analytics
        composable(Screen.Analytics.route) {
            AnalyticsScreen(viewModel = analyticsViewModel)
        }

        // Accounts
        composable(Screen.Accounts.route) {
            AccountListScreen(
                viewModel = accountListViewModel,
                onNavigateToDetail = { accountId ->
                    navController.navigate(Screen.AccountDetail.createRoute(accountId))
                },
                onNavigateToAddEdit = { accountId ->
                    navController.navigate(Screen.AddEditAccount.createRoute(accountId))
                }
            )
        }

        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            AccountDetailScreen(
                accountId = accountId,
                viewModel = accountDetailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPool = { poolId ->
                    navController.navigate(Screen.BalanceDetail.createRoute(poolId))
                },
                onNavigateToTransaction = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                },
                onNavigateToTransfer = { balanceId ->
                    navController.navigate(Screen.Transfer.createRoute(balanceId))
                },
                onNavigateToAddPool = {
                    navController.navigate(Screen.AddPool.createRoute(accountId))
                }
            )
        }

        composable(
            route = Screen.AddEditAccount.route,
            arguments = listOf(
                navArgument("accountId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId")?.takeIf { it != -1L }
            AddEditAccountScreen(
                accountId = accountId,
                viewModel = accountListViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Transactions
        composable(Screen.Transactions.route) {
            TransactionListScreen(
                viewModel = transactionListViewModel,
                onNavigateToDetail = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                },
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.TransactionForm.createRoute(null))
                }
            )
        }

        // Notification screens
        composable(Screen.PendingNotifications.route) {
            PendingNotificationsScreen(
                viewModel = notificationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                }
            )
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

