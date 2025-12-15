package com.example.organizadordefinancas.ui.screens.income

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.Income
import com.example.organizadordefinancas.data.model.IncomeCategory
import com.example.organizadordefinancas.data.model.IncomeType
import com.example.organizadordefinancas.ui.components.DeleteConfirmationDialog
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.IncomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeListScreen(
    viewModel: IncomeViewModel,
    onNavigateToAddEdit: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val incomes by viewModel.allIncomes.collectAsState()
    val totalIncome by viewModel.totalMonthlyIncome.collectAsState()
    val totalRecurrent by viewModel.totalRecurrentIncome.collectAsState()
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }

    val receivedCount = incomes.count { it.isReceived }
    val recurrentCount = incomes.count { it.type == IncomeType.RECURRENT }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receitas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEdit(null) },
                containerColor = Color(0xFF4CAF50)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar receita")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Total Mensal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatCurrency(totalIncome),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Recorrente",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(totalRecurrent),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$recurrentCount receitas fixas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "$receivedCount recebidas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (incomes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Nenhuma receita cadastrada",
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(incomes) { income ->
                        IncomeItem(
                            income = income,
                            onToggleReceived = { viewModel.toggleReceivedStatus(income) },
                            onEdit = { onNavigateToAddEdit(income.id) },
                            onDelete = { incomeToDelete = income }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    incomeToDelete?.let { income ->
        DeleteConfirmationDialog(
            title = "Excluir Receita",
            message = "Tem certeza que deseja excluir ${income.description}?",
            onConfirm = {
                viewModel.deleteIncome(income)
                incomeToDelete = null
            },
            onDismiss = { incomeToDelete = null }
        )
    }
}

@Composable
private fun IncomeItem(
    income: Income,
    onToggleReceived: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for received status
            Checkbox(
                checked = income.isReceived,
                onCheckedChange = { onToggleReceived() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF4CAF50)
                )
            )

            // Category icon
            Icon(
                imageVector = getIncomeCategoryIcon(income.category),
                contentDescription = null,
                tint = getIncomeCategoryColor(income.category),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = income.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (income.isReceived) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (income.isReceived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        append(getIncomeCategoryName(income.category))
                        if (income.type == IncomeType.RECURRENT) {
                            append(" • Dia ${income.receiveDay}")
                        } else {
                            append(" • ${dateFormat.format(Date(income.date))}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (income.type == IncomeType.RECURRENT) {
                    Text(
                        text = "Recorrente",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(income.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (income.isReceived) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (income.isReceived) MaterialTheme.colorScheme.outline else Color(0xFF4CAF50)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun getIncomeCategoryIcon(category: IncomeCategory): ImageVector {
    return when (category) {
        IncomeCategory.SALARY -> Icons.Default.Work
        IncomeCategory.FREELANCE -> Icons.Default.Computer
        IncomeCategory.INVESTMENT -> Icons.Default.TrendingUp
        IncomeCategory.BONUS -> Icons.Default.Star
        IncomeCategory.GIFT -> Icons.Default.CardGiftcard
        IncomeCategory.RENTAL -> Icons.Default.Home
        IncomeCategory.SALE -> Icons.Default.ShoppingCart
        IncomeCategory.REFUND -> Icons.Default.Replay
        IncomeCategory.OTHER -> Icons.Default.AttachMoney
    }
}

fun getIncomeCategoryColor(category: IncomeCategory): Color {
    return when (category) {
        IncomeCategory.SALARY -> Color(0xFF4CAF50)
        IncomeCategory.FREELANCE -> Color(0xFF2196F3)
        IncomeCategory.INVESTMENT -> Color(0xFFFF9800)
        IncomeCategory.BONUS -> Color(0xFFFFD700)
        IncomeCategory.GIFT -> Color(0xFFE91E63)
        IncomeCategory.RENTAL -> Color(0xFF9C27B0)
        IncomeCategory.SALE -> Color(0xFF00BCD4)
        IncomeCategory.REFUND -> Color(0xFF607D8B)
        IncomeCategory.OTHER -> Color(0xFF795548)
    }
}

fun getIncomeCategoryName(category: IncomeCategory): String {
    return when (category) {
        IncomeCategory.SALARY -> "Salário"
        IncomeCategory.FREELANCE -> "Freelance"
        IncomeCategory.INVESTMENT -> "Investimento"
        IncomeCategory.BONUS -> "Bônus"
        IncomeCategory.GIFT -> "Presente"
        IncomeCategory.RENTAL -> "Aluguel"
        IncomeCategory.SALE -> "Venda"
        IncomeCategory.REFUND -> "Reembolso"
        IncomeCategory.OTHER -> "Outros"
    }
}

fun getIncomeTypeName(type: IncomeType): String {
    return when (type) {
        IncomeType.RECURRENT -> "Recorrente"
        IncomeType.ONE_TIME -> "Único"
    }
}

