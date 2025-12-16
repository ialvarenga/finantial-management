package com.example.organizadordefinancas.ui.screens.installment

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
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.InstallmentDisplayItem
import com.example.organizadordefinancas.ui.viewmodel.InstallmentFilter
import com.example.organizadordefinancas.ui.viewmodel.InstallmentListViewModel
import com.example.organizadordefinancas.ui.viewmodel.InstallmentSortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentListScreen(
    viewModel: InstallmentListViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parcelamentos") },
                actions = {
                    // Filter Button
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtrar")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            InstallmentFilter.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(getFilterLabel(filter)) },
                                    onClick = {
                                        viewModel.setFilter(filter)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.filter == filter) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Sort Button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Ordenar")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            InstallmentSortBy.values().forEach { sortBy ->
                                DropdownMenuItem(
                                    text = { Text(getSortLabel(sortBy)) },
                                    onClick = {
                                        viewModel.setSortBy(sortBy)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortBy == sortBy) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
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
        ) {
            // Summary Card
            InstallmentSummaryCard(
                activeCount = uiState.installments.filter {
                    (it.summary?.expectedCount ?: 0) > 0
                }.size,
                totalRemaining = uiState.installments.sumOf {
                    it.summary?.remainingAmount ?: 0.0
                },
                modifier = Modifier.padding(16.dp)
            )

            // Active Filter Indicator
            if (uiState.filter != InstallmentFilter.ALL) {
                AssistChip(
                    onClick = { viewModel.setFilter(InstallmentFilter.ALL) },
                    label = { Text(getFilterLabel(uiState.filter)) },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Limpar", modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Installment List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.installments.isEmpty()) {
                EmptyInstallmentsState(
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
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.installments) { installment ->
                        InstallmentCard(
                            installment = installment,
                            onClick = { onNavigateToDetail(installment.parentTransaction.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentSummaryCard(
    activeCount: Int,
    totalRemaining: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF7C4DFF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Parcelamentos Ativos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "$activeCount",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Total Restante",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = formatCurrency(totalRemaining),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun InstallmentCard(
    installment: InstallmentDisplayItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transaction = installment.parentTransaction
    val summary = installment.summary
    val isCompleted = (summary?.expectedCount ?: 0) == 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description ?: transaction.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(transaction.category),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Status Badge
                if (isCompleted) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "✓ Quitado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Installment Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = installment.formattedTotal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: ${formatCurrency(summary?.totalAmount ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = installment.formattedProgress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                    if (!isCompleted) {
                        Text(
                            text = installment.formattedRemaining,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { installment.progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pago: ${formatCurrency(summary?.paidAmount ?: 0.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "${installment.progressPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyInstallmentsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Nenhum parcelamento",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Compras parceladas aparecerão aqui",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun getFilterLabel(filter: InstallmentFilter): String {
    return when (filter) {
        InstallmentFilter.ALL -> "Todos"
        InstallmentFilter.IN_PROGRESS -> "Em andamento"
        InstallmentFilter.COMPLETED -> "Quitados"
    }
}

private fun getSortLabel(sortBy: InstallmentSortBy): String {
    return when (sortBy) {
        InstallmentSortBy.REMAINING_AMOUNT -> "Valor restante"
        InstallmentSortBy.PURCHASE_DATE -> "Data da compra"
        InstallmentSortBy.TOTAL_AMOUNT -> "Valor total"
        InstallmentSortBy.INSTALLMENT_COUNT -> "Número de parcelas"
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

