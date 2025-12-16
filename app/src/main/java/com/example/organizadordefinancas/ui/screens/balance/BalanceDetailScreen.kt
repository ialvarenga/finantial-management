package com.example.organizadordefinancas.ui.screens.balance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.BalanceTypes
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.ui.components.DeleteConfirmationDialog
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.BalanceDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceDetailScreen(
    balanceId: Long,
    viewModel: BalanceDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransaction: (Long) -> Unit,
    onNavigateToTransfer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(balanceId) {
        viewModel.loadBalance(balanceId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val balance = uiState.balance
    val isPool = balance?.balanceType == BalanceTypes.POOL

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(balance?.name ?: "Detalhes do Saldo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Edit balance */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    if (isPool) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = balance?.color?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { balance?.id?.let { onNavigateToTransfer(it) } },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Transferir")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Balance Header
                item {
                    BalanceHeaderCard(
                        balance = balance,
                        isPool = isPool
                    )
                }

                // Goal Progress (only for pools)
                if (isPool && balance?.goalAmount != null && balance.goalAmount > 0) {
                    item {
                        GoalProgressCard(
                            currentBalance = balance.currentBalance,
                            goalAmount = balance.goalAmount,
                            balanceColor = balance.color
                        )
                    }
                }

                // Quick Actions
                item {
                    QuickActionsRow(
                        onDeposit = { /* Add deposit */ },
                        onWithdraw = { /* Add withdraw */ },
                        onTransfer = { balance?.id?.let { onNavigateToTransfer(it) } }
                    )
                }

                // Transactions Section
                item {
                    TransactionsSectionHeader(
                        transactionCount = uiState.transactions.size
                    )
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        EmptyTransactionsState()
                    }
                } else {
                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                    val groupedTransactions = uiState.transactions.groupBy { transaction ->
                        dateFormatter.format(Date(transaction.date))
                    }

                    groupedTransactions.forEach { (date, transactions) ->
                        item {
                            Text(
                                text = formatDateHeader(date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(transactions) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                onClick = { onNavigateToTransaction(transaction.id) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Error handling
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show error snackbar
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && balance != null) {
        DeleteConfirmationDialog(
            title = "Excluir Caixinha",
            message = "Tem certeza que deseja excluir ${balance.name}? O saldo de ${formatCurrency(balance.currentBalance)} será transferido para a conta principal.",
            onConfirm = {
                viewModel.deleteBalance()
                showDeleteDialog = false
                onNavigateBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun BalanceHeaderCard(
    balance: Balance?,
    isPool: Boolean,
    modifier: Modifier = Modifier
) {
    if (balance == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = balance.color?.let { Color(it).copy(alpha = 0.1f) }
                ?: MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = if (isPool) Icons.Default.Savings else Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = balance.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Balance Type Label
            Text(
                text = if (isPool) "Caixinha" else "Saldo da Conta",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            // Current Balance
            Text(
                text = formatCurrency(balance.currentBalance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = balance.color?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
            )

            // Currency
            Text(
                text = balance.currency,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun GoalProgressCard(
    currentBalance: Double,
    goalAmount: Double,
    balanceColor: Long?,
    modifier: Modifier = Modifier
) {
    val progress = (currentBalance / goalAmount).toFloat().coerceIn(0f, 1f)
    val isGoalReached = currentBalance >= goalAmount
    val color = balanceColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Meta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isGoalReached) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "✓ Meta atingida!",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = if (isGoalReached) Color(0xFF4CAF50) else color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Atual",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatCurrency(currentBalance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isGoalReached) Color(0xFF4CAF50) else color
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Progresso",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Meta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatCurrency(goalAmount),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Remaining amount
            if (!isGoalReached) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Faltam ${formatCurrency(goalAmount - currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onDeposit,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Depositar")
        }
        OutlinedButton(
            onClick = onWithdraw,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Retirar")
        }
        OutlinedButton(
            onClick = onTransfer,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Transferir")
        }
    }
}

@Composable
private fun TransactionsSectionHeader(
    transactionCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Movimentações ($transactionCount)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale("pt", "BR")) }
    val isExpense = transaction.type == "expense"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description ?: transaction.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nenhuma movimentação",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun formatDateHeader(dateString: String): String {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    val today = dateFormatter.format(Date())
    val yesterday = dateFormatter.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))

    return when (dateString) {
        today -> "Hoje"
        yesterday -> "Ontem"
        else -> dateString
    }
}
