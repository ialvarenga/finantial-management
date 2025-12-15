package com.example.organizadordefinancas.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.ui.components.SummaryCard
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.BankViewModel
import com.example.organizadordefinancas.ui.viewmodel.CreditCardViewModel
import com.example.organizadordefinancas.ui.viewmodel.FinancialCompromiseViewModel
import com.example.organizadordefinancas.ui.viewmodel.IncomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    creditCardViewModel: CreditCardViewModel,
    bankViewModel: BankViewModel,
    compromiseViewModel: FinancialCompromiseViewModel,
    incomeViewModel: IncomeViewModel,
    modifier: Modifier = Modifier
) {
    val banks by bankViewModel.allBanks.collectAsState()
    val totalBalance by bankViewModel.totalBalance.collectAsState()
    val totalSavingsBalance by bankViewModel.totalSavingsBalance.collectAsState()
    val creditCards by creditCardViewModel.allCreditCards.collectAsState()
    val allItems by creditCardViewModel.allItems.collectAsState()
    val compromises by compromiseViewModel.allCompromises.collectAsState()
    val totalAllCompromises by compromiseViewModel.totalMonthlyCompromises.collectAsState()
    val incomes by incomeViewModel.allIncomes.collectAsState()
    val totalMonthlyIncome by incomeViewModel.totalMonthlyIncome.collectAsState()

    // Credit card items only (purchases)
    val totalCreditCardItemsOnly = allItems.sumOf { it.amount }

    // Total expenses = credit card items + ALL fixed compromises
    val totalExpenses = totalCreditCardItemsOnly + totalAllCompromises

    val currentDate = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("pt", "BR"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Organizador Financeiro")
                        Text(
                            text = currentDate.format(dateFormatter).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            Text(
                text = "Resumo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            SummaryCard(
                title = "Saldo em Bancos",
                value = totalBalance,
                icon = Icons.Default.AccountBalance,
                backgroundColor = Color(0xFF4CAF50)
            )

            if (totalSavingsBalance > 0) {
                SummaryCard(
                    title = "Reserva de Emergência",
                    value = totalSavingsBalance,
                    icon = Icons.Default.Savings,
                    backgroundColor = Color(0xFF673AB7)
                )
            }

            SummaryCard(
                title = "Renda Mensal",
                value = totalMonthlyIncome,
                icon = Icons.Default.TrendingUp,
                backgroundColor = Color(0xFF2196F3)
            )

            SummaryCard(
                title = "Fatura dos Cartões",
                value = totalCreditCardItemsOnly,
                icon = Icons.Default.CreditCard,
                backgroundColor = Color(0xFFE91E63),
                isNegative = true
            )

            SummaryCard(
                title = "Contas Fixas",
                value = totalAllCompromises,
                icon = Icons.Default.Receipt,
                backgroundColor = Color(0xFFFF9800),
                isNegative = true
            )

            SummaryCard(
                title = "Total de Despesas",
                value = totalExpenses,
                icon = Icons.Default.TrendingDown,
                backgroundColor = Color(0xFFF44336),
                isNegative = true
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Quick Stats
            Text(
                text = "Estatísticas Rápidas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatRow("Bancos cadastrados", banks.size.toString())
                    StatRow("Cartões de crédito", creditCards.size.toString())
                    StatRow("Contas fixas", compromises.size.toString())
                    StatRow("Itens na fatura", allItems.size.toString())
                    StatRow("Fontes de renda", incomes.size.toString())

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    val availableBalance = totalBalance - totalExpenses
                    StatRow(
                        label = "Saldo disponível após despesas",
                        value = formatCurrency(availableBalance),
                        valueColor = if (availableBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )

                    // Money left after paying all debts (income - expenses)
                    val remainingAfterDebts = totalMonthlyIncome - totalExpenses
                    StatRow(
                        label = "Sobra do próximo mês",
                        value = formatCurrency(remainingAfterDebts),
                        valueColor = if (remainingAfterDebts >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )

                    if (totalSavingsBalance > 0) {
                        StatRow(
                            label = "Reserva de emergência",
                            value = formatCurrency(totalSavingsBalance),
                            valueColor = Color(0xFF673AB7)
                        )
                    }
                }
            }

            // Upcoming compromises
            if (compromises.isNotEmpty()) {
                Text(
                    text = "Próximas Contas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                val today = LocalDate.now().dayOfMonth
                val upcomingCompromises = compromises
                    .filter { !it.isPaid && it.linkedCreditCardId == null }
                    .sortedBy {
                        val daysUntil = if (it.dueDay >= today) it.dueDay - today else 30 - today + it.dueDay
                        daysUntil
                    }
                    .take(5)

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (upcomingCompromises.isEmpty()) {
                            Text(
                                text = "Todas as contas foram pagas! 🎉",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            upcomingCompromises.forEach { compromise ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${compromise.name} (dia ${compromise.dueDay})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = formatCurrency(compromise.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE91E63)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

