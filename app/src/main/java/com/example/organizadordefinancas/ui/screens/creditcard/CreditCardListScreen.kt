package com.example.organizadordefinancas.ui.screens.creditcard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.CreditCardItem
import com.example.organizadordefinancas.ui.components.DeleteConfirmationDialog
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.CreditCardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardListScreen(
    viewModel: CreditCardViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddEdit: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val creditCards by viewModel.allCreditCards.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    var cardToDelete by remember { mutableStateOf<CreditCard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cartões de Crédito") },
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
                Icon(Icons.Default.Add, contentDescription = "Adicionar cartão")
            }
        }
    ) { paddingValues ->
        if (creditCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Nenhum cartão cadastrado",
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
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(creditCards) { card ->
                    val cardTotal = allItems.filter { it.cardId == card.id }.sumOf { it.amount }
                    CreditCardItem(
                        card = card,
                        total = cardTotal,
                        onClick = { onNavigateToDetail(card.id) },
                        onDelete = { cardToDelete = card },
                        onEdit = { onNavigateToAddEdit(card.id) }
                    )
                }
            }
        }
    }

    cardToDelete?.let { card ->
        DeleteConfirmationDialog(
            title = "Excluir Cartão",
            message = "Tem certeza que deseja excluir o cartão ${card.name}? Todos os itens da fatura também serão excluídos.",
            onConfirm = {
                viewModel.deleteCreditCard(card)
                cardToDelete = null
            },
            onDismiss = { cardToDelete = null }
        )
    }
}

@Composable
private fun CreditCardItem(
    card: CreditCard,
    total: Double,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Card header with color
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(card.color))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vencimento: dia ${card.dueDay}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Fechamento: dia ${card.closingDay}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Limite: ${formatCurrency(card.cardLimit)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Fatura",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatCurrency(total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (total > 0) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

