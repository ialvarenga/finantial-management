package com.example.organizadordefinancas.ui.screens.transaction

import androidx.compose.foundation.clickable
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
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.model.TransactionType
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.TransactionCategories
import com.example.organizadordefinancas.ui.viewmodel.TransactionFormUiState
import com.example.organizadordefinancas.ui.viewmodel.TransactionFormViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    viewModel: TransactionFormViewModel,
    transactionId: Long?, // null for new transaction
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditing = transactionId != null

    // Initialize for editing if needed
    LaunchedEffect(transactionId) {
        transactionId?.let { viewModel.loadTransaction(it) }
    }

    // Handle save success
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Transação" else "Nova Transação") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveTransaction() },
                        enabled = !uiState.isSaving && uiState.validationErrors.isEmpty()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Salvar")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Selector
            TransactionTypeSelector(
                selectedType = uiState.formData.type,
                onTypeSelected = { viewModel.updateType(it) }
            )

            // Amount Input
            AmountInput(
                amount = uiState.formData.amount,
                onAmountChange = { viewModel.updateAmount(it) },
                error = uiState.validationErrors["amount"],
                isExpense = uiState.formData.type == TransactionType.EXPENSE
            )

            // Date Picker
            DatePicker(
                selectedDate = uiState.formData.date,
                onDateSelected = { viewModel.updateDate(it) }
            )

            // Category Selector
            CategorySelector(
                selectedCategory = uiState.formData.category,
                categories = if (uiState.formData.type == TransactionType.INCOME)
                    TransactionCategories.INCOME_CATEGORIES
                else
                    TransactionCategories.EXPENSE_CATEGORIES,
                onCategorySelected = { viewModel.updateCategory(it) },
                error = uiState.validationErrors["category"]
            )

            // Description
            OutlinedTextField(
                value = uiState.formData.description ?: "",
                onValueChange = { viewModel.updateDescription(it.ifBlank { null }) },
                label = { Text("Descrição (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Status Selector
            StatusSelector(
                selectedStatus = uiState.formData.status,
                onStatusSelected = { viewModel.updateStatus(it) }
            )

            // Source Selector (Balance or Credit Card Bill)
            SourceSelector(
                uiState = uiState,
                onBalanceSelected = { viewModel.selectBalance(it) },
                onBillSelected = { billId, cardId -> viewModel.selectBill(billId, cardId) }
            )

            // Installment Section (only for expenses with bill)
            if (uiState.formData.type == TransactionType.EXPENSE && uiState.formData.billId != null) {
                InstallmentSection(
                    isInstallment = uiState.formData.isInstallment,
                    installmentCount = uiState.formData.installmentCount,
                    installmentPreview = uiState.installmentPreview,
                    onInstallmentToggle = { viewModel.toggleInstallment(it) },
                    onInstallmentCountChange = { viewModel.updateInstallmentCount(it) }
                )
            }

            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
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

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveTransaction() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving && uiState.validationErrors.isEmpty()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isEditing) "Salvar Alterações" else "Criar Transação")
            }
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Expense Button
        ElevatedButton(
            onClick = { onTypeSelected(TransactionType.EXPENSE) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = if (selectedType == TransactionType.EXPENSE)
                    Color(0xFFF44336)
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Icon(
                imageVector = Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (selectedType == TransactionType.EXPENSE) Color.White else Color(0xFFF44336)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Despesa",
                color = if (selectedType == TransactionType.EXPENSE) Color.White else Color(0xFFF44336)
            )
        }

        // Income Button
        ElevatedButton(
            onClick = { onTypeSelected(TransactionType.INCOME) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = if (selectedType == TransactionType.INCOME)
                    Color(0xFF4CAF50)
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = if (selectedType == TransactionType.INCOME) Color.White else Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Receita",
                color = if (selectedType == TransactionType.INCOME) Color.White else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun AmountInput(
    amount: Double,
    onAmountChange: (Double) -> Unit,
    error: String?,
    isExpense: Boolean,
    modifier: Modifier = Modifier
) {
    var textValue by remember(amount) { mutableStateOf(if (amount == 0.0) "" else amount.toString()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpense) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isExpense) "Valor da Despesa" else "Valor da Receita",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "R$ ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            textValue = value
                            onAmountChange(value.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    modifier = Modifier.width(200.dp),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) Color(0xFFF44336) else Color(0xFF4CAF50)
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
            error?.let {
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
private fun DatePicker(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    OutlinedTextField(
        value = dateFormatter.format(Date(selectedDate)),
        onValueChange = {},
        readOnly = true,
        label = { Text("Data") },
        leadingIcon = {
            Icon(Icons.Default.CalendarToday, contentDescription = null)
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true },
        enabled = false,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = error != null,
            supportingText = error?.let { { Text(it) } }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusSelector(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            onClick = { onStatusSelected(TransactionStatus.COMPLETED) },
            label = { Text("Concluído") },
            selected = selectedStatus == TransactionStatus.COMPLETED,
            leadingIcon = if (selectedStatus == TransactionStatus.COMPLETED) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null,
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            onClick = { onStatusSelected(TransactionStatus.EXPECTED) },
            label = { Text("Previsto") },
            selected = selectedStatus == TransactionStatus.EXPECTED,
            leadingIcon = if (selectedStatus == TransactionStatus.EXPECTED) {
                { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceSelector(
    uiState: TransactionFormUiState,
    onBalanceSelected: (Long) -> Unit,
    onBillSelected: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var sourceType by remember { mutableStateOf(if (uiState.formData.billId != null) "bill" else "balance") }
    var balanceExpanded by remember { mutableStateOf(false) }
    var billExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Source Type Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                onClick = { sourceType = "balance" },
                label = { Text("Saldo") },
                selected = sourceType == "balance",
                leadingIcon = if (sourceType == "balance") {
                    { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                onClick = { sourceType = "bill" },
                label = { Text("Cartão de Crédito") },
                selected = sourceType == "bill",
                leadingIcon = if (sourceType == "bill") {
                    { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (sourceType == "balance") {
            // Balance Selector
            ExposedDropdownMenuBox(
                expanded = balanceExpanded,
                onExpandedChange = { balanceExpanded = it }
            ) {
                val selectedBalance = uiState.availableBalances.find { it.id == uiState.formData.balanceId }
                OutlinedTextField(
                    value = selectedBalance?.name ?: "Selecione um saldo",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Saldo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = balanceExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = balanceExpanded,
                    onDismissRequest = { balanceExpanded = false }
                ) {
                    uiState.availableBalances.forEach { balance ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(balance.name)
                                    Text(
                                        formatCurrency(balance.currentBalance),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            onClick = {
                                onBalanceSelected(balance.id)
                                balanceExpanded = false
                            }
                        )
                    }
                }
            }
        } else {
            // Bill Selector
            ExposedDropdownMenuBox(
                expanded = billExpanded,
                onExpandedChange = { billExpanded = it }
            ) {
                val selectedBill = uiState.availableBills.find { it.id == uiState.formData.billId }
                val dateFormatter = remember { SimpleDateFormat("MM/yyyy", Locale("pt", "BR")) }
                OutlinedTextField(
                    value = selectedBill?.let {
                        "Fatura ${dateFormatter.format(Date(it.closingDate))}"
                    } ?: "Selecione uma fatura",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fatura") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = billExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = billExpanded,
                    onDismissRequest = { billExpanded = false }
                ) {
                    uiState.availableBills.forEach { bill ->
                        DropdownMenuItem(
                            text = {
                                Text("Fatura ${dateFormatter.format(Date(bill.closingDate))}")
                            },
                            onClick = {
                                onBillSelected(bill.id, bill.creditCardId)
                                billExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentSection(
    isInstallment: Boolean,
    installmentCount: Int,
    installmentPreview: String?,
    onInstallmentToggle: (Boolean) -> Unit,
    onInstallmentCountChange: (Int) -> Unit,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Parcelado",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = isInstallment,
                    onCheckedChange = onInstallmentToggle
                )
            }

            if (isInstallment) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Número de parcelas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick select chips
                    listOf(2, 3, 6, 10, 12).forEach { count ->
                        FilterChip(
                            onClick = { onInstallmentCountChange(count) },
                            label = { Text("${count}x") },
                            selected = installmentCount == count
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Slider for custom installment count
                Slider(
                    value = installmentCount.toFloat(),
                    onValueChange = { onInstallmentCountChange(it.toInt()) },
                    valueRange = 2f..48f,
                    steps = 46,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${installmentCount}x parcelas",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Preview
                installmentPreview?.let { preview ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
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
        "salário" -> Icons.Default.Work
        "freelance" -> Icons.Default.Laptop
        "investimentos" -> Icons.Default.TrendingUp
        "transferência" -> Icons.Default.SwapHoriz
        "reembolso" -> Icons.Default.Refresh
        "presente" -> Icons.Default.CardGiftcard
        else -> Icons.Default.Category
    }
}

