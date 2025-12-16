package com.example.organizadordefinancas.ui.screens.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.model.TransactionType
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.TransactionFilter
import com.example.organizadordefinancas.ui.viewmodel.TransactionListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transações") },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Badge(
                            modifier = Modifier.padding(4.dp),
                            containerColor = if (hasActiveFilters(uiState.filter))
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Transparent
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar transação")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Cards
            TransactionSummaryCards(
                totalExpenses = uiState.totalExpenses,
                totalIncome = uiState.totalIncome,
                modifier = Modifier.padding(16.dp)
            )

            // Category breakdown chips
            if (uiState.expensesByCategory.isNotEmpty()) {
                CategoryChips(
                    categories = uiState.expensesByCategory.take(5).map { it.category to it.total },
                    onCategoryClick = { category ->
                        viewModel.filterByCategory(category)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active filters indicator
            if (hasActiveFilters(uiState.filter)) {
                ActiveFiltersChip(
                    filter = uiState.filter,
                    onClearFilters = { viewModel.clearFilters() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Transaction List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.transactions.isEmpty()) {
                EmptyTransactionsState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                TransactionList(
                    transactions = uiState.transactions,
                    onTransactionClick = onNavigateToDetail,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = uiState.filter,
            onFilterApply = { filter ->
                viewModel.setFilter(filter)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun TransactionSummaryCards(
    totalExpenses: Double,
    totalIncome: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Income Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Receitas",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(totalIncome),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Expenses Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF44336)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Despesas",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(totalExpenses),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<Pair<String, Double>>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (category, total) ->
            SuggestionChip(
                onClick = { onCategoryClick(category) },
                label = {
                    Text(
                        text = "$category: ${formatCurrency(total)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        }
    }
}

@Composable
private fun ActiveFiltersChip(
    filter: TransactionFilter,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClearFilters,
        label = { Text("Limpar filtros") },
        leadingIcon = {
            Icon(
                Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Limpar",
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
private fun TransactionList(
    transactions: List<Transaction>,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    // Group transactions by date
    val groupedTransactions = transactions.groupBy { transaction ->
        dateFormatter.format(Date(transaction.date))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedTransactions.forEach { (date, dayTransactions) ->
            item {
                Text(
                    text = formatDateHeader(date),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            items(dayTransactions) { transaction ->
                TransactionCard(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isInstallment = transaction.installmentNumber != null
    val isPending = transaction.status == TransactionStatus.EXPECTED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isExpense) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(transaction.category),
                        contentDescription = null,
                        tint = if (isExpense) Color(0xFFFF9800) else Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.description ?: transaction.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Installment indicator
                        if (isInstallment) {
                            Text(
                                text = "(${transaction.installmentNumber}/${transaction.totalInstallments})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Status indicator
                        if (isPending) {
                            Text(
                                text = "• Previsto",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            Text(
                text = "${if (isExpense) "-" else "+"} ${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) Color(0xFFF44336) else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun EmptyTransactionsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Nenhuma transação encontrada",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Toque no + para adicionar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    currentFilter: TransactionFilter,
    onFilterApply: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(currentFilter.type) }
    var selectedStatus by remember { mutableStateOf(currentFilter.status) }
    var selectedCategory by remember { mutableStateOf(currentFilter.category) }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Filtros",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Type Filter
            Text(
                text = "Tipo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    onClick = { selectedType = if (selectedType == null) TransactionType.INCOME else null },
                    label = { Text("Receita") },
                    selected = selectedType == TransactionType.INCOME,
                    leadingIcon = if (selectedType == TransactionType.INCOME) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    onClick = { selectedType = if (selectedType == TransactionType.EXPENSE) null else TransactionType.EXPENSE },
                    label = { Text("Despesa") },
                    selected = selectedType == TransactionType.EXPENSE,
                    leadingIcon = if (selectedType == TransactionType.EXPENSE) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }

            // Status Filter
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    onClick = { selectedStatus = if (selectedStatus == TransactionStatus.COMPLETED) null else TransactionStatus.COMPLETED },
                    label = { Text("Concluído") },
                    selected = selectedStatus == TransactionStatus.COMPLETED,
                    leadingIcon = if (selectedStatus == TransactionStatus.COMPLETED) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    onClick = { selectedStatus = if (selectedStatus == TransactionStatus.EXPECTED) null else TransactionStatus.EXPECTED },
                    label = { Text("Previsto") },
                    selected = selectedStatus == TransactionStatus.EXPECTED,
                    leadingIcon = if (selectedStatus == TransactionStatus.EXPECTED) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onFilterApply(TransactionFilter())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Limpar")
                }
                Button(
                    onClick = {
                        onFilterApply(
                            currentFilter.copy(
                                type = selectedType,
                                status = selectedStatus,
                                category = selectedCategory
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Aplicar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun hasActiveFilters(filter: TransactionFilter): Boolean {
    return filter.type != null ||
           filter.status != null ||
           filter.category != null ||
           filter.startDate != null ||
           filter.endDate != null ||
           filter.balanceId != null ||
           filter.billId != null
}

private fun formatDateHeader(dateString: String): String {
    val today = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
    val yesterday = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(
        Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
    )

    return when (dateString) {
        today -> "Hoje"
        yesterday -> "Ontem"
        else -> dateString
    }
}

private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "alimentação" -> Icons.Default.Restaurant
        "transporte" -> Icons.Default.DirectionsCar
        "moradia" -> Icons.Default.Home
        "saúde" -> Icons.Default.LocalHospital
        "educação" -> Icons.Default.School
        "lazer" -> Icons.Default.SportsEsports
        "compras" -> Icons.Default.ShoppingBag
        "eletrônicos" -> Icons.Default.Devices
        "vestuário" -> Icons.Default.Checkroom
        "serviços" -> Icons.Default.Build
        "assinaturas" -> Icons.Default.Subscriptions
        "salário" -> Icons.Default.Work
        "freelance" -> Icons.Default.Laptop
        "investimentos" -> Icons.Default.TrendingUp
        "transferência" -> Icons.Default.SwapHoriz
        "cartão de crédito" -> Icons.Default.CreditCard
        else -> Icons.Default.Receipt
    }
}

