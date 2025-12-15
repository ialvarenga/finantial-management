package com.example.organizadordefinancas.ui.screens.notification

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.Bank
import com.example.organizadordefinancas.data.model.BankAppConfig
import com.example.organizadordefinancas.data.model.CapturedNotification
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.service.FinanceNotificationListener
import com.example.organizadordefinancas.ui.viewmodel.NotificationViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingNotificationsScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val pendingNotifications by viewModel.pendingNotifications.collectAsState()
    val creditCards by viewModel.creditCards.collectAsState()
    val banks by viewModel.banks.collectAsState()
    val context = LocalContext.current

    val hasNotificationAccess = remember {
        FinanceNotificationListener.isNotificationAccessGranted(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transações Pendentes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasNotificationAccess) {
                NotificationAccessBanner(context)
            }

            if (pendingNotifications.isEmpty()) {
                EmptyPendingState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingNotifications, key = { it.id }) { notification ->
                        PendingNotificationCard(
                            notification = notification,
                            creditCards = creditCards,
                            banks = banks,
                            viewModel = viewModel,
                            onIgnore = {
                                viewModel.ignoreNotification(notification.id)
                            },
                            onDelete = {
                                viewModel.deleteNotification(notification.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationAccessBanner(context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Permissão Necessária",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Para capturar transações automaticamente, habilite o acesso às notificações.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { FinanceNotificationListener.openNotificationAccessSettings(context) }
            ) {
                Text("Habilitar Acesso")
            }
        }
    }
}

@Composable
private fun EmptyPendingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nenhuma transação pendente",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Transações capturadas aparecerão aqui",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun PendingNotificationCard(
    notification: CapturedNotification,
    creditCards: List<CreditCard>,
    banks: List<Bank>,
    viewModel: NotificationViewModel,
    onIgnore: () -> Unit,
    onDelete: () -> Unit
) {
    var showProcessDialog by remember { mutableStateOf(false) }
    val suggestedCard = viewModel.findMatchingCard(notification, creditCards)
    val isPix = viewModel.isPixTransaction(notification)

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: App name, transaction type, and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = BankAppConfig.getDisplayName(notification.packageName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isPix) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = { },
                            label = { Text("PIX", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                Text(
                    text = dateFormat.format(Date(notification.capturedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount
            notification.extractedAmount?.let { amount ->
                Text(
                    text = currencyFormat.format(amount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPix) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                )
            }

            // Merchant/Recipient
            notification.extractedMerchant?.let { merchant ->
                Text(
                    text = if (isPix) "Para: $merchant" else merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Card last four digits (if available and not PIX)
            if (!isPix) {
                notification.extractedCardLastFour?.let { lastFour ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cartão •••• $lastFour",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        suggestedCard?.let { card ->
                            Text(
                                text = " → ${card.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Original notification content (collapsed)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notification.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excluir")
                }
                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ignorar")
                }
                Button(
                    onClick = { showProcessDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirmar")
                }
            }
        }
    }

    if (showProcessDialog) {
        if (isPix) {
            ProcessPixDialog(
                notification = notification,
                banks = banks,
                onDismiss = { showProcessDialog = false },
                onProcess = { bankId, description, amount ->
                    viewModel.processAsPixTransaction(
                        notificationId = notification.id,
                        bankId = bankId,
                        description = description,
                        amount = amount
                    )
                    showProcessDialog = false
                }
            )
        } else {
            ProcessCreditCardDialog(
                notification = notification,
                creditCards = creditCards,
                suggestedCard = suggestedCard,
                onDismiss = { showProcessDialog = false },
                onProcess = { cardId, description, amount, category, installments ->
                    viewModel.processAsCreditCardPurchase(
                        notificationId = notification.id,
                        cardId = cardId,
                        description = description,
                        amount = amount,
                        category = category,
                        installments = installments
                    )
                    showProcessDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessPixDialog(
    notification: CapturedNotification,
    banks: List<Bank>,
    onDismiss: () -> Unit,
    onProcess: (Long, String, Double) -> Unit
) {
    var selectedBankId by remember { mutableStateOf(banks.firstOrNull()?.id ?: 0L) }
    val isPixReceived = notification.transactionType == "PIX_RECEIVED"
    val defaultDescription = if (isPixReceived) "Pix Recebido" else "Pix Enviado"
    var description by remember { mutableStateOf(notification.extractedMerchant ?: defaultDescription) }
    var amount by remember { mutableStateOf(notification.extractedAmount?.toString() ?: "") }
    var expanded by remember { mutableStateOf(false) }

    val transactionTypeLabel = when (notification.transactionType) {
        "PIX_RECEIVED" -> "Pix recebido"
        "PIX_SENT" -> "Pix enviado"
        else -> "Pix"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
        title = { Text("Confirmar PIX") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Transaction type label
                SuggestionChip(
                    onClick = { },
                    label = { Text(transactionTypeLabel) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isPixReceived)
                            Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else
                            Color(0xFFE91E63).copy(alpha = 0.2f)
                    )
                )

                Text(
                    text = if (isPixReceived)
                        "Este valor será adicionado ao saldo do banco selecionado."
                    else
                        "Este valor será deduzido do saldo do banco selecionado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                // Bank selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = banks.find { it.id == selectedBankId }?.name ?: "Selecione um banco",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Banco") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        banks.forEach { bank ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(bank.name)
                                        Text(
                                            "Saldo: R$ ${String.format("%.2f", bank.balance)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                onClick = {
                                    selectedBankId = bank.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Valor (R$)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (selectedBankId > 0 && parsedAmount > 0) {
                        onProcess(selectedBankId, description, parsedAmount)
                    }
                },
                enabled = selectedBankId > 0 && amount.isNotBlank()
            ) {
                Text("Atualizar Saldo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessCreditCardDialog(
    notification: CapturedNotification,
    creditCards: List<CreditCard>,
    suggestedCard: CreditCard?,
    onDismiss: () -> Unit,
    onProcess: (Long, String, Double, String, Int) -> Unit
) {
    var selectedCardId by remember { mutableStateOf(suggestedCard?.id ?: creditCards.firstOrNull()?.id ?: 0L) }
    var description by remember { mutableStateOf(notification.extractedMerchant ?: "") }
    var amount by remember { mutableStateOf(notification.extractedAmount?.toString() ?: "") }
    var category by remember { mutableStateOf("Outros") }
    var installments by remember { mutableStateOf("1") }
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Alimentação", "Transporte", "Compras", "Lazer", "Saúde",
        "Educação", "Moradia", "Serviços", "Outros"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
        title = { Text("Confirmar Compra") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = creditCards.find { it.id == selectedCardId }?.name ?: "Selecione um cartão",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cartão") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        creditCards.forEach { card ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(card.name)
                                        card.lastFourDigits?.let {
                                            Text(
                                                "•••• $it",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCardId = card.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Valor (R$)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category dropdown
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Installments
                OutlinedTextField(
                    value = installments,
                    onValueChange = { installments = it.filter { c -> c.isDigit() } },
                    label = { Text("Parcelas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val parsedInstallments = installments.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    if (selectedCardId > 0 && parsedAmount > 0) {
                        onProcess(selectedCardId, description, parsedAmount, category, parsedInstallments)
                    }
                },
                enabled = selectedCardId > 0 && amount.isNotBlank()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

