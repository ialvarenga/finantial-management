package com.example.organizadordefinancas.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    creditCardViewModel: CreditCardViewModel,
    bankViewModel: BankViewModel,
    compromiseViewModel: FinancialCompromiseViewModel,
    modifier: Modifier = Modifier
) {
    val banks by bankViewModel.allBanks.collectAsState()
    val totalBalance by bankViewModel.totalBalance.collectAsState()
    val creditCards by creditCardViewModel.allCreditCards.collectAsState()
    val allItems by creditCardViewModel.allItems.collectAsState()
    val compromises by compromiseViewModel.allCompromises.collectAsState()
    val totalNonLinkedCompromises by compromiseViewModel.totalNonLinkedCompromises.collectAsState()
    val totalAllCompromises by compromiseViewModel.totalMonthlyCompromises.collectAsState()

    // Credit card total includes linked compromises
    val totalCreditCardItemsOnly = allItems.sumOf { it.amount }
    val totalLinkedCompromises = totalAllCompromises - totalNonLinkedCompromises
    val totalCreditCardBill = totalCreditCardItemsOnly + totalLinkedCompromises

    // Total expenses = credit cards (with linked compromises) + non-linked compromises
    val totalExpenses = totalCreditCardBill + totalNonLinkedCompromises

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

            SummaryCard(
                title = "Fatura dos Cartões",
                value = totalCreditCardBill,
                icon = Icons.Default.CreditCard,
                backgroundColor = Color(0xFFE91E63),
                isNegative = true
            )

            SummaryCard(
                title = "Contas Fixas",
                value = totalNonLinkedCompromises,
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

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    val availableBalance = totalBalance - totalExpenses
                    StatRow(
                        label = "Saldo disponível após despesas",
                        value = formatCurrency(availableBalance),
                        valueColor = if (availableBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
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
                    .filter { !it.isPaid }
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

