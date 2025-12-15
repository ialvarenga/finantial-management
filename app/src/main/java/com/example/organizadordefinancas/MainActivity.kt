package com.example.organizadordefinancas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.organizadordefinancas.ui.navigation.FinanceNavHost
import com.example.organizadordefinancas.ui.navigation.bottomNavItems
import com.example.organizadordefinancas.ui.theme.OrganizadorDeFinancasTheme
import com.example.organizadordefinancas.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as FinanceApplication

        setContent {
            OrganizadorDeFinancasTheme {
                FinanceApp(application)
            }
        }
    }
}

@Composable
fun FinanceApp(application: FinanceApplication) {
    val navController = rememberNavController()

    val creditCardViewModel: CreditCardViewModel = viewModel(
        factory = CreditCardViewModelFactory(application.creditCardRepository)
    )
    val bankViewModel: BankViewModel = viewModel(
        factory = BankViewModelFactory(application.bankRepository)
    )
    val compromiseViewModel: FinancialCompromiseViewModel = viewModel(
        factory = FinancialCompromiseViewModelFactory(
            application.financialCompromiseRepository,
            application.compromiseOccurrenceRepository
        )
    )
    val incomeViewModel: IncomeViewModel = viewModel(
        factory = IncomeViewModelFactory(application.incomeRepository)
    )
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(
            application.capturedNotificationRepository,
            application.creditCardRepository,
            application.bankRepository
        )
    )
    val analyticsViewModel: AnalyticsViewModel = viewModel(
        factory = AnalyticsViewModelFactory(application.analyticsRepository)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if bottom bar should be shown
    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let { Icon(it, contentDescription = screen.title) }
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        content = { innerPadding ->
            FinanceNavHost(
                navController = navController,
                creditCardViewModel = creditCardViewModel,
                bankViewModel = bankViewModel,
                compromiseViewModel = compromiseViewModel,
                incomeViewModel = incomeViewModel,
                notificationViewModel = notificationViewModel,
                analyticsViewModel = analyticsViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    )
}