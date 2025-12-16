package com.example.organizadordefinancas.ui.screens.bill

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
import com.example.organizadordefinancas.data.model.BillStatus
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionType
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.BillDetailUiState
import com.example.organizadordefinancas.ui.viewmodel.BillDetailViewModel
import com.example.organizadordefinancas.ui.viewmodel.PaymentStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    billId: Long,
    viewModel: BillDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (Long) -> Unit,
    onNavigateToTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(billId) {
        viewModel.loadBill(billId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Fatura") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = uiState.creditCard?.color?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (uiState.remainingAmount > 0) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToPayment(billId) },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pagar ${formatCurrency(uiState.remainingAmount)}")
                }
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
                // Bill Header
                item {
                    BillHeader(
                        uiState = uiState
                    )
                }

                // Payment Status Card
                item {
                    PaymentStatusCard(
                        paymentStatus = uiState.paymentStatus,
                        totalAmount = uiState.totalAmount,
                        paidAmount = uiState.paidAmount,
                        remainingAmount = uiState.remainingAmount
                    )
                }

                // Transactions Section
                item {
                    TransactionsSectionHeader(
                        transactionCount = uiState.transactions.size,
                        totalAmount = uiState.totalAmount
                    )
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        EmptyTransactionsState()
                    }
                } else {
                    // Group transactions by date
                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                    val groupedTransactions = uiState.transactions.groupBy { transaction ->
                        dateFormatter.format(Date(transaction.date))
                    }

                    groupedTransactions.forEach { (date, transactions) ->
                        item {
                            Text(
                                text = date,
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
}

@Composable
private fun BillHeader(
    uiState: BillDetailUiState,
    modifier: Modifier = Modifier
) {
    val bill = uiState.bill ?: return
    val card = uiState.creditCard

    val statusColor = when (bill.status) {
        BillStatus.OPEN -> Color(0xFFFF9800)
        BillStatus.OVERDUE -> Color(0xFFF44336)
        BillStatus.PAID -> Color(0xFF4CAF50)
        BillStatus.PARTIAL -> Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Credit Card color bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(card?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary)
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = card?.name ?: "Cartão",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }
                        Text(
                            text = monthFormatter.format(Date(bill.closingDate)).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = getStatusText(bill.status),
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dates Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fecha: ${uiState.formattedClosingDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (bill.status == BillStatus.OVERDUE) Color(0xFFF44336) else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Vence: ${uiState.formattedDueDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (bill.status == BillStatus.OVERDUE) Color(0xFFF44336) else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentStatusCard(
    paymentStatus: PaymentStatus,
    totalAmount: Double,
    paidAmount: Double,
    remainingAmount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (paymentStatus) {
                is PaymentStatus.FullyPaid -> Color(0xFFE8F5E9)
                is PaymentStatus.Overdue -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total da Fatura",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatCurrency(totalAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (paidAmount > 0 && paymentStatus !is PaymentStatus.FullyPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Pago",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = formatCurrency(paidAmount),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Restante",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = formatCurrency(remainingAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (paymentStatus) {
                                is PaymentStatus.Overdue -> Color(0xFFF44336)
                                else -> Color(0xFFFF9800)
                            }
                        )
                    }
                }
            }

            // Payment Progress
            if (totalAmount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { (paidAmount / totalAmount).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF4CAF50),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (paymentStatus) {
                        is PaymentStatus.FullyPaid -> "✓ Fatura paga"
                        is PaymentStatus.PartiallyPaid -> "${((paidAmount / totalAmount) * 100).toInt()}% pago"
                        is PaymentStatus.Overdue -> "⚠ Fatura atrasada"
                        else -> "Aguardando pagamento"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (paymentStatus) {
                        is PaymentStatus.FullyPaid -> Color(0xFF4CAF50)
                        is PaymentStatus.Overdue -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionsSectionHeader(
    transactionCount: Int,
    totalAmount: Double,
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
                text = "Transações ($transactionCount)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = formatCurrency(totalAmount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInstallment = transaction.installmentNumber != null

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
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${transaction.installmentNumber}/${transaction.totalInstallments}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = formatCurrency(transaction.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF44336)
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
                text = "Nenhuma transação nesta fatura",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun getStatusText(status: String): String {
    return when (status) {
        BillStatus.OPEN -> "Aberta"
        BillStatus.OVERDUE -> "Atrasada"
        BillStatus.PAID -> "Paga"
        BillStatus.PARTIAL -> "Parcial"
        else -> status
    }
}

