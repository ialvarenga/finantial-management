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
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.BillListUiState
import com.example.organizadordefinancas.ui.viewmodel.BillListViewModel
import com.example.organizadordefinancas.ui.viewmodel.BillWithCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillListScreen(
    viewModel: BillListViewModel,
    onNavigateToBillDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Faturas") },
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
        ) {
            // Summary Card
            BillSummaryCard(
                summary = uiState.summary,
                modifier = Modifier.padding(16.dp)
            )

            // Status Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Abertas") },
                    icon = {
                        Badge {
                            Text("${uiState.summary.openBills.size}")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Atrasadas") },
                    icon = {
                        if (uiState.summary.overdueBills.isNotEmpty()) {
                            Badge(containerColor = Color(0xFFF44336)) {
                                Text("${uiState.summary.overdueBills.size}")
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Pagas") }
                )
            }

            // Bill List
            val billsToShow = when (selectedTab) {
                0 -> uiState.summary.openBills
                1 -> uiState.summary.overdueBills
                else -> uiState.summary.paidBills
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (billsToShow.isEmpty()) {
                EmptyBillsState(
                    message = when (selectedTab) {
                        0 -> "Nenhuma fatura aberta"
                        1 -> "Nenhuma fatura atrasada"
                        else -> "Nenhuma fatura paga"
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(billsToShow) { billWithCard ->
                        BillCard(
                            billWithCard = billWithCard,
                            onClick = { onNavigateToBillDetail(billWithCard.bill.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillSummaryCard(
    summary: com.example.organizadordefinancas.ui.viewmodel.BillSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total Unpaid
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF9800)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "A Pagar",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = formatCurrency(summary.totalUnpaid),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Total Overdue
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = if (summary.totalOverdue > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (summary.totalOverdue > 0) "Atrasado" else "Em dia!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = if (summary.totalOverdue > 0) formatCurrency(summary.totalOverdue) else "✓",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun BillCard(
    billWithCard: BillWithCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bill = billWithCard.bill
    val card = billWithCard.creditCard
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }

    val statusColor = when (bill.status) {
        BillStatus.OPEN -> Color(0xFFFF9800)
        BillStatus.OVERDUE -> Color(0xFFF44336)
        BillStatus.PAID -> Color(0xFF4CAF50)
        BillStatus.PARTIAL -> Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Color bar from credit card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(card?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary)
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = card?.name ?: "Cartão",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = monthFormatter.format(Date(bill.closingDate)).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = getStatusText(bill.status),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = formatCurrency(bill.totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (billWithCard.remainingAmount > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Restante",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = formatCurrency(billWithCard.remainingAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Vence ${dateFormatter.format(Date(bill.dueDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Paid progress
                    if (bill.paidAmount > 0 && bill.status != BillStatus.PAID) {
                        Text(
                            text = "${((bill.paidAmount / bill.totalAmount) * 100).toInt()}% pago",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                // Payment progress bar
                if (bill.totalAmount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (bill.paidAmount / bill.totalAmount).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBillsState(
    message: String,
    modifier: Modifier = Modifier
) {
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
                text = message,
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

