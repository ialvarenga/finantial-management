package com.example.organizadordefinancas.ui.screens.bill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.BillDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPaymentScreen(
    billId: Long,
    viewModel: BillDetailViewModel,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(billId) {
        viewModel.loadBill(billId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var paymentAmount by remember { mutableStateOf("") }
    var selectedBalanceId by remember { mutableStateOf<Long?>(null) }
    var isFullPayment by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initialize payment amount with remaining amount
    LaunchedEffect(uiState.remainingAmount) {
        if (paymentAmount.isEmpty()) {
            paymentAmount = uiState.remainingAmount.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pagar Fatura") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bill Summary Card
                BillSummaryCard(
                    cardName = uiState.creditCard?.name ?: "Cartão",
                    month = uiState.formattedDueDate,
                    totalAmount = uiState.totalAmount,
                    remainingAmount = uiState.remainingAmount,
                    dueDate = uiState.formattedDueDate
                )

                // Payment Type Selection
                PaymentTypeSelector(
                    isFullPayment = isFullPayment,
                    remainingAmount = uiState.remainingAmount,
                    onFullPaymentSelected = {
                        isFullPayment = true
                        paymentAmount = uiState.remainingAmount.toString()
                    },
                    onPartialPaymentSelected = {
                        isFullPayment = false
                        paymentAmount = ""
                    }
                )

                // Payment Amount Input
                PaymentAmountInput(
                    amount = paymentAmount,
                    onAmountChange = { paymentAmount = it },
                    isFullPayment = isFullPayment,
                    remainingAmount = uiState.remainingAmount,
                    error = errorMessage
                )

                // Balance Selection
                BalanceSelector(
                    balances = uiState.availableBalancesForPayment,
                    selectedBalanceId = selectedBalanceId,
                    paymentAmount = paymentAmount.toDoubleOrNull() ?: 0.0,
                    onBalanceSelected = { selectedBalanceId = it }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Error message
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Pay Button
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedBalanceId != null &&
                             (paymentAmount.toDoubleOrNull() ?: 0.0) > 0 &&
                             !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pagar ${formatCurrency(paymentAmount.toDoubleOrNull() ?: 0.0)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        val selectedBalance = uiState.availableBalancesForPayment.find { it.id == selectedBalanceId }

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Pagamento") },
            text = {
                Column {
                    Text("Você está prestes a pagar:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatCurrency(paymentAmount.toDoubleOrNull() ?: 0.0),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("De: ${selectedBalance?.name ?: ""}")
                    Text("Para: Fatura ${uiState.creditCard?.name}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isProcessing = true
                        selectedBalanceId?.let { balanceId ->
                            val amount = paymentAmount.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                if (isFullPayment || amount >= uiState.remainingAmount) {
                                    viewModel.payFullAmount(balanceId)
                                } else {
                                    viewModel.payPartialAmount(balanceId, amount)
                                }
                                onPaymentSuccess()
                            } else {
                                errorMessage = "Valor inválido"
                                isProcessing = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun BillSummaryCard(
    cardName: String,
    month: String,
    totalAmount: Double,
    remainingAmount: Double,
    dueDate: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = cardName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vence em $dueDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatCurrency(totalAmount),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (remainingAmount < totalAmount) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Já pago: ${formatCurrency(totalAmount - remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Restante: ${formatCurrency(remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentTypeSelector(
    isFullPayment: Boolean,
    remainingAmount: Double,
    onFullPaymentSelected: () -> Unit,
    onPartialPaymentSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Tipo de Pagamento",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                onClick = onFullPaymentSelected,
                label = {
                    Column {
                        Text("Pagamento Total")
                        Text(
                            formatCurrency(remainingAmount),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                selected = isFullPayment,
                leadingIcon = if (isFullPayment) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                onClick = onPartialPaymentSelected,
                label = {
                    Column {
                        Text("Pagamento Parcial")
                        Text("Valor personalizado", style = MaterialTheme.typography.bodySmall)
                    }
                },
                selected = !isFullPayment,
                leadingIcon = if (!isFullPayment) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PaymentAmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    isFullPayment: Boolean,
    remainingAmount: Double,
    error: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Valor do Pagamento",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isFullPayment) {
                Text(
                    text = formatCurrency(remainingAmount),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "R$ ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { value ->
                            if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                onAmountChange(value)
                            }
                        },
                        modifier = Modifier.width(150.dp),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        placeholder = { Text("0.00", style = MaterialTheme.typography.headlineMedium) }
                    )
                }

                // Validate amount
                val amountValue = amount.toDoubleOrNull() ?: 0.0
                if (amountValue > remainingAmount) {
                    Text(
                        text = "Valor maior que o restante (${formatCurrency(remainingAmount)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BalanceSelector(
    balances: List<Balance>,
    selectedBalanceId: Long?,
    paymentAmount: Double,
    onBalanceSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedBalance = balances.find { it.id == selectedBalanceId }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Pagar com",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedBalance?.name ?: "Selecione uma conta",
                onValueChange = {},
                readOnly = true,
                leadingIcon = {
                    Icon(Icons.Default.AccountBalance, contentDescription = null)
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                supportingText = selectedBalance?.let {
                    {
                        val hasEnoughBalance = it.currentBalance >= paymentAmount
                        Text(
                            text = "Saldo: ${formatCurrency(it.currentBalance)}" +
                                   if (!hasEnoughBalance) " (insuficiente)" else "",
                            color = if (hasEnoughBalance) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (balances.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Nenhuma conta com saldo disponível") },
                        onClick = {},
                        enabled = false
                    )
                } else {
                    balances.forEach { balance ->
                        val hasEnoughBalance = balance.currentBalance >= paymentAmount
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(balance.name)
                                    Text(
                                        formatCurrency(balance.currentBalance),
                                        color = if (hasEnoughBalance) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            onClick = {
                                onBalanceSelected(balance.id)
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (hasEnoughBalance) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (hasEnoughBalance) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

