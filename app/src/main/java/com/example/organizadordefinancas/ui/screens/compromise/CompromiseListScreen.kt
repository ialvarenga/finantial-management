package com.example.organizadordefinancas.ui.screens.compromise

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
import com.example.organizadordefinancas.data.model.CompromiseCategory
import com.example.organizadordefinancas.data.model.CompromiseFrequency
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.data.model.getDisplayName
import com.example.organizadordefinancas.ui.components.DeleteConfirmationDialog
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.CreditCardViewModel
import com.example.organizadordefinancas.ui.viewmodel.FinancialCompromiseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompromiseListScreen(
    viewModel: FinancialCompromiseViewModel,
    creditCardViewModel: CreditCardViewModel,
    onNavigateToAddEdit: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val compromises by viewModel.allCompromises.collectAsState()
    val totalCompromises by viewModel.totalMonthlyCompromises.collectAsState()
    var compromiseToDelete by remember { mutableStateOf<FinancialCompromise?>(null) }

    // Filter compromises by linked status
    val nonLinkedCompromises = compromises.filter { it.linkedCreditCardId == null }
    val linkedCompromises = compromises.filter { it.linkedCreditCardId != null }

    val paidCount = nonLinkedCompromises.count { it.isPaid }
    val totalCount = nonLinkedCompromises.size


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contas Fixas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEdit(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar conta")
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
                    containerColor = Color(0xFFFF9800)
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
                        text = formatCurrency(totalCompromises),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) paidCount.toFloat() / totalCount else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$paidCount de $totalCount contas pagas",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            if (compromises.isEmpty()) {
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
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Nenhuma conta cadastrada",
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
                    // Regular compromises (not linked to credit cards)
                    if (nonLinkedCompromises.isNotEmpty()) {
                        item {
                            Text(
                                text = "Contas Fixas",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(nonLinkedCompromises) { compromise ->
                            CompromiseItem(
                                compromise = compromise,
                                onTogglePaid = { viewModel.togglePaidStatus(compromise) },
                                onEdit = { onNavigateToAddEdit(compromise.id) },
                                onDelete = { compromiseToDelete = compromise }
                            )
                        }
                    }

                    // Compromises linked to credit cards
                    if (linkedCompromises.isNotEmpty()) {
                        item {
                            if (nonLinkedCompromises.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Text(
                                text = "Vinculadas a Cartões de Crédito",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(linkedCompromises) { compromise ->
                            LinkedCompromiseItem(
                                compromise = compromise,
                                onEdit = { onNavigateToAddEdit(compromise.id) },
                                onDelete = { compromiseToDelete = compromise }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    compromiseToDelete?.let { compromise ->
        DeleteConfirmationDialog(
            title = "Excluir Conta",
            message = "Tem certeza que deseja excluir ${compromise.name}?",
            onConfirm = {
                viewModel.deleteCompromise(compromise)
                compromiseToDelete = null
            },
            onDismiss = { compromiseToDelete = null }
        )
    }
}

@Composable
private fun CompromiseItem(
    compromise: FinancialCompromise,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val nextDueDate = remember(compromise) {
        Date(compromise.getNextDueDate())
    }

    // Build the subtitle text based on frequency
    val subtitleText = remember(compromise) {
        val frequencyText = compromise.frequency.getDisplayName()
        when (compromise.frequency) {
            CompromiseFrequency.WEEKLY, CompromiseFrequency.BIWEEKLY -> {
                val dayName = getDayOfWeekName(compromise.dayOfWeek ?: 1)
                "$frequencyText • $dayName"
            }
            CompromiseFrequency.MONTHLY -> {
                "${getCategoryName(compromise.category)} • Dia ${compromise.getEffectiveDayOfMonth()}"
            }
            else -> {
                "$frequencyText • Próx: ${dateFormat.format(nextDueDate)}"
            }
        }
    }

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
            // Checkbox for paid status
            Checkbox(
                checked = compromise.isPaid,
                onCheckedChange = { onTogglePaid() }
            )

            // Category icon
            Icon(
                imageVector = getCategoryIcon(compromise.category),
                contentDescription = null,
                tint = getCategoryColor(compromise.category),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = compromise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (compromise.isPaid) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (compromise.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                // Show monthly equivalent for non-monthly frequencies
                if (compromise.frequency != CompromiseFrequency.MONTHLY) {
                    Text(
                        text = "≈ ${formatCurrency(compromise.getMonthlyEquivalent())}/mês",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(compromise.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (compromise.isPaid) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (compromise.isPaid) MaterialTheme.colorScheme.outline else Color(0xFFE91E63)
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

fun getCategoryIcon(category: CompromiseCategory): ImageVector {
    return when (category) {
        CompromiseCategory.RENT -> Icons.Default.Bathtub
        CompromiseCategory.ENERGY -> Icons.Default.Bolt
        CompromiseCategory.WATER -> Icons.Default.WaterDrop
        CompromiseCategory.INTERNET -> Icons.Default.Wifi
        CompromiseCategory.PHONE -> Icons.Default.Phone
        CompromiseCategory.INSURANCE -> Icons.Default.Security
        CompromiseCategory.STREAMING -> Icons.Default.Tv
        CompromiseCategory.GYM -> Icons.Default.FitnessCenter
        CompromiseCategory.EDUCATION -> Icons.Default.School
        CompromiseCategory.HEALTH -> Icons.Default.LocalHospital
        CompromiseCategory.TRANSPORT -> Icons.Default.DirectionsCar
        CompromiseCategory.COMPANY -> Icons.Default.Business
        CompromiseCategory.HOUSEHOLD -> Icons.Default.Kitchen
        CompromiseCategory.TECHNOLOGY -> Icons.Default.Computer
        CompromiseCategory.FOOD -> Icons.Default.Restaurant
        CompromiseCategory.OTHER -> Icons.Default.Receipt
    }
}

fun getCategoryColor(category: CompromiseCategory): Color {
    return when (category) {
        CompromiseCategory.RENT -> Color(0xFF795548)
        CompromiseCategory.ENERGY -> Color(0xFFFFEB3B)
        CompromiseCategory.WATER -> Color(0xFF2196F3)
        CompromiseCategory.INTERNET -> Color(0xFF9C27B0)
        CompromiseCategory.PHONE -> Color(0xFF4CAF50)
        CompromiseCategory.INSURANCE -> Color(0xFF607D8B)
        CompromiseCategory.STREAMING -> Color(0xFFE91E63)
        CompromiseCategory.GYM -> Color(0xFFFF5722)
        CompromiseCategory.EDUCATION -> Color(0xFF3F51B5)
        CompromiseCategory.HEALTH -> Color(0xFFF44336)
        CompromiseCategory.TRANSPORT -> Color(0xFF00BCD4)
        CompromiseCategory.COMPANY -> Color(0xFF8BC34A)
        CompromiseCategory.HOUSEHOLD -> Color(0xFFFF9800)
        CompromiseCategory.TECHNOLOGY -> Color(0xFF673AB7)
        CompromiseCategory.FOOD -> Color(0xFFCDDC39)
        CompromiseCategory.OTHER -> Color(0xFF9E9E9E)
    }
}

fun getCategoryName(category: CompromiseCategory): String {
    return when (category) {
        CompromiseCategory.RENT -> "Aluguel"
        CompromiseCategory.ENERGY -> "Energia"
        CompromiseCategory.WATER -> "Água"
        CompromiseCategory.INTERNET -> "Internet"
        CompromiseCategory.PHONE -> "Telefone"
        CompromiseCategory.INSURANCE -> "Seguro"
        CompromiseCategory.STREAMING -> "Streaming"
        CompromiseCategory.GYM -> "Academia"
        CompromiseCategory.EDUCATION -> "Educação"
        CompromiseCategory.HEALTH -> "Saúde"
        CompromiseCategory.TRANSPORT -> "Transporte"
        CompromiseCategory.TECHNOLOGY -> "Tecnologia"
        CompromiseCategory.FOOD -> "Alimentação"
        CompromiseCategory.COMPANY -> "Empresa"
        CompromiseCategory.HOUSEHOLD -> "Casa"
        CompromiseCategory.OTHER -> "Outros"
    }
}

fun getDayOfWeekName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Segunda"
        2 -> "Terça"
        3 -> "Quarta"
        4 -> "Quinta"
        5 -> "Sexta"
        6 -> "Sábado"
        7 -> "Domingo"
        else -> "Segunda"
    }
}

@Composable
private fun LinkedCompromiseItem(
    compromise: FinancialCompromise,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Icon(
                imageVector = getCategoryIcon(compromise.category),
                contentDescription = null,
                tint = getCategoryColor(compromise.category),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "Vinculado a cartão",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = compromise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${getCategoryName(compromise.category)} • No cartão de crédito",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(compromise.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63)
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
