package com.example.organizadordefinancas.ui.screens.creditcard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

val itemCategories = listOf(
    "Alimentação",
    "Transporte",
    "Lazer",
    "Saúde",
    "Educação",
    "Compras",
    "Serviços",
    "Assinaturas",
    "Viagem",
    "Outros"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCreditCardItemScreen(
    cardId: Long,
    viewModel: CreditCardViewModel,
    compromiseViewModel: FinancialCompromiseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("1") }
    var selectedCategory by remember { mutableStateOf(itemCategories.last()) }
    var expandedCategory by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }

    val isFormValid = description.isNotBlank() &&
        amount.toDoubleOrNull() != null &&
        amount.toDoubleOrNull()!! > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
                label = { Text("Valor") },
                placeholder = { Text("Ex: 150.00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("R$ ") }
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
                                text = "Conta Fixa Recorrente",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Repete todo mês automaticamente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = {
                            isRecurring = it
                            if (it) installments = "1"
                        }
                    )
                }
            }

            // Only show installments if not recurring
            if (!isRecurring) {
                OutlinedTextField(
                    value = installments,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }
                        if (filtered.isEmpty() || (filtered.toIntOrNull() ?: 0) <= 48) {
                            installments = filtered
                        }
                    },
                    label = { Text("Parcelas") },
                    placeholder = { Text("1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Deixe 1 para compra à vista") }
                )
            }

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
                        .menuAnchor()
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

            // Preview of installment value
            val totalAmount = amount.toDoubleOrNull() ?: 0.0
            val numInstallments = installments.toIntOrNull()?.coerceAtLeast(1) ?: 1
            if (totalAmount > 0 && numInstallments > 1 && !isRecurring) {
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
                            text = "Valor por parcela",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "R$ ${"%.2f".format(totalAmount / numInstallments)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val totalAmount = amount.toDoubleOrNull() ?: 0.0

                    if (isRecurring) {
                        // Create a FinancialCompromise linked to this credit card
                        val compromise = FinancialCompromise(
                            name = description,
                            amount = totalAmount,
                            dueDay = 0, // Not relevant when linked to credit card
                            category = CompromiseCategory.OTHER,
                            linkedCreditCardId = cardId
                        )
                        compromiseViewModel.insertCompromise(compromise)
                    } else {
                        // Create a regular credit card item
                        val numInstallments = installments.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val item = CreditCardItem(
                            cardId = cardId,
                            description = description,
                            amount = totalAmount,
                            purchaseDate = System.currentTimeMillis(),
                            installments = numInstallments,
                            currentInstallment = 1,
                            category = selectedCategory
                        )

                        if (numInstallments > 1) {
                            viewModel.insertItemWithInstallments(item)
                        } else {
                            viewModel.insertItem(item)
                        }
                    }
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid
            ) {
                Text(if (isRecurring) "Adicionar Conta Fixa" else "Adicionar Item")
            }
        }
    }
}

