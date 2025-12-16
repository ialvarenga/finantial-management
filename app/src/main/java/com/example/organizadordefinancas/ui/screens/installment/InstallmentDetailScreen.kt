package com.example.organizadordefinancas.ui.screens.installment

import androidx.compose.foundation.background
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
import com.example.organizadordefinancas.ui.components.DeleteConfirmationDialog
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.InstallmentDetailUiState
import com.example.organizadordefinancas.ui.viewmodel.InstallmentDetailViewModel
import com.example.organizadordefinancas.ui.viewmodel.InstallmentScheduleItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentDetailScreen(
    parentTransactionId: Long,
    viewModel: InstallmentDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(parentTransactionId) {
        viewModel.loadInstallment(parentTransactionId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    val hasExpectedInstallments = uiState.scheduleItems.any { it.isExpected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Parcelamento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (hasExpectedInstallments) {
                        IconButton(onClick = { showCancelDialog = true }) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Cancelar parcelas restantes",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF7C4DFF),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
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
                // Header Card
                item {
                    InstallmentHeaderCard(uiState = uiState)
                }

                // Summary Card
                item {
                    InstallmentSummaryCard(uiState = uiState)
                }

                // Payment Schedule Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cronograma de Pagamentos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Payment Schedule Items
                items(uiState.scheduleItems) { scheduleItem ->
                    PaymentScheduleCard(
                        item = scheduleItem,
                        onMarkAsPaid = {
                            viewModel.markInstallmentAsPaid(scheduleItem.transaction.id)
                        },
                        onClick = { onNavigateToTransaction(scheduleItem.transaction.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
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

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        val remainingCount = uiState.scheduleItems.count { it.isExpected }
        val remainingAmount = uiState.summary?.remainingAmount ?: 0.0

        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Cancelar Parcelas Restantes?") },
            text = {
                Column {
                    Text("Esta ação cancelará as $remainingCount parcelas restantes.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Valor total a ser cancelado: ${formatCurrency(remainingAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "As parcelas já pagas não serão afetadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelRemainingInstallments()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancelar Parcelas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Voltar")
                }
            }
        )
    }
}

@Composable
private fun InstallmentHeaderCard(
    uiState: InstallmentDetailUiState,
    modifier: Modifier = Modifier
) {
    val transaction = uiState.parentTransaction ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF7C4DFF).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = transaction.description ?: transaction.category,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getCategoryIcon(transaction.category),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Comprado em ${uiState.purchaseDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total and Installment Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Valor Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = uiState.formattedTotalAmount,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Parcelas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${uiState.summary?.totalInstallments ?: 0}x",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C4DFF)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallmentSummaryCard(
    uiState: InstallmentDetailUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Progress
            Text(
                text = uiState.progressText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { uiState.progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = Color(0xFF4CAF50),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Row
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
                        text = uiState.formattedPaidAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Progresso",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${uiState.progressPercentage.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C4DFF)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Restante",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = uiState.formattedRemainingAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if ((uiState.summary?.remainingAmount ?: 0.0) > 0) Color(0xFFFF9800) else Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentScheduleCard(
    item: InstallmentScheduleItem,
    onMarkAsPaid: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        item.isPaid -> Color(0xFF4CAF50)
        item.isCancelled -> Color(0xFF9E9E9E)
        item.isExpected -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                item.isPaid -> Color(0xFFE8F5E9)
                item.isCancelled -> Color(0xFFFAFAFA)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Installment Number Badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${item.installmentNumber}/${item.totalInstallments}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Date
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            ) {
                Text(
                    text = item.formattedDate.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Amount
            Text(
                text = item.formattedAmount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Status/Action
            when {
                item.isPaid -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Pago",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
                item.isCancelled -> {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Cancelado",
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(24.dp)
                    )
                }
                item.isExpected -> {
                    IconButton(
                        onClick = onMarkAsPaid,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Marcar como pago",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Status Text at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusColor.copy(alpha = 0.1f))
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = item.statusDisplayText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
        }
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
        else -> Icons.Default.Receipt
    }
}

