package com.example.organizadordefinancas.ui.screens.creditcard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.CompromiseCategory
import com.example.organizadordefinancas.data.model.CreditCardItem
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.ui.viewmodel.CreditCardViewModel
import com.example.organizadordefinancas.ui.viewmodel.FinancialCompromiseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCreditCardItemScreen(
    itemId: Long,
    viewModel: CreditCardViewModel,
    compromiseViewModel: FinancialCompromiseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var item by remember { mutableStateOf<CreditCardItem?>(null) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("1") }
    var currentInstallment by remember { mutableStateOf("1") }
    var selectedCategory by remember { mutableStateOf(itemCategories.last()) }
    var expandedCategory by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }

    // Load the item data
    LaunchedEffect(itemId) {
        viewModel.getItemById(itemId).collect { loadedItem ->
            loadedItem?.let {
                if (!isLoaded) {
                    item = it
                    description = it.description
                    amount = String.format(java.util.Locale.US, "%.2f", it.amount)
                    installments = it.installments.toString()
                    currentInstallment = it.currentInstallment.toString()
                    selectedCategory = it.category
                    isLoaded = true
                }
            }
        }
    }

    val isFormValid = description.isNotBlank() &&
        amount.toDoubleOrNull() != null &&
        amount.toDoubleOrNull()!! > 0 &&
        (isRecurring || (
            installments.toIntOrNull() != null &&
            installments.toIntOrNull()!! >= 1 &&
            currentInstallment.toIntOrNull() != null &&
            currentInstallment.toIntOrNull()!! >= 1 &&
            currentInstallment.toIntOrNull()!! <= (installments.toIntOrNull() ?: 1)
        ))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
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
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    placeholder = { Text("Ex: Supermercado, Restaurante...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (isRecurring) "Valor" else "Valor da Parcela") },
                    placeholder = { Text("Ex: 150.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("R$ ") },
                    supportingText = { Text(if (isRecurring) "Valor mensal" else "Valor desta parcela") }
                )

                // Recurring bill option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecurring) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = if (isRecurring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Column {
                                Text(
                                    text = "Converter em Conta Fixa",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Transforma em conta que repete todo mês",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it }
                        )
                    }
                }

                // Only show installment fields if not recurring
                if (!isRecurring) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = currentInstallment,
                            onValueChange = {
                                val filtered = it.filter { c -> c.isDigit() }
                                currentInstallment = filtered
                            },
                            label = { Text("Parcela Atual") },
                            placeholder = { Text("7") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("Ex: 7 de 10") }
                        )

                        OutlinedTextField(
                            value = installments,
                            onValueChange = {
                                val filtered = it.filter { c -> c.isDigit() }
                                if (filtered.isEmpty() || (filtered.toIntOrNull() ?: 0) <= 48) {
                                    installments = filtered
                                }
                            },
                            label = { Text("Total Parcelas") },
                            placeholder = { Text("10") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("Máx: 48") }
                        )
                    }

                    // Validation message
                    val currentInst = currentInstallment.toIntOrNull() ?: 0
                    val totalInst = installments.toIntOrNull() ?: 1
                    if (currentInst > totalInst && currentInst > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "A parcela atual não pode ser maior que o total de parcelas",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Preview of remaining installments
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (parsedAmount > 0 && totalInst > 1 && currentInst > 0 && currentInst <= totalInst) {
                        val remainingInstallments = totalInst - currentInst + 1
                        val totalRemaining = parsedAmount * remainingInstallments
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Parcela $currentInst de $totalInst",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Restam $remainingInstallments parcelas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Total restante: R$ ${"%.2f".format(totalRemaining)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } // End of !isRecurring block

                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        itemCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        item?.let { currentItem ->
                            val parsedAmount = amount.toDoubleOrNull() ?: currentItem.amount

                            if (isRecurring) {
                                // Convert to FinancialCompromise and delete the CreditCardItem
                                val compromise = FinancialCompromise(
                                    name = description,
                                    amount = parsedAmount,
                                    dueDay = 0, // Not relevant when linked to credit card
                                    category = CompromiseCategory.OTHER,
                                    linkedCreditCardId = currentItem.cardId
                                )
                                compromiseViewModel.insertCompromise(compromise)
                                viewModel.deleteItem(currentItem)
                            } else {
                                // Update the existing CreditCardItem
                                val updatedItem = currentItem.copy(
                                    description = description,
                                    amount = parsedAmount,
                                    installments = installments.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                                    currentInstallment = currentInstallment.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                                    category = selectedCategory
                                )
                                viewModel.updateItem(updatedItem)
                            }
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid
                ) {
                    Text(if (isRecurring) "Converter em Conta Fixa" else "Salvar Alterações")
                }
            }
        }
    }
}

