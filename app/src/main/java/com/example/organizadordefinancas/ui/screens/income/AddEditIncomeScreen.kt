package com.example.organizadordefinancas.ui.screens.income

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.Income
import com.example.organizadordefinancas.data.model.IncomeCategory
import com.example.organizadordefinancas.data.model.IncomeType
import com.example.organizadordefinancas.ui.viewmodel.IncomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIncomeScreen(
    incomeId: Long?,
    viewModel: IncomeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var receiveDay by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(IncomeCategory.OTHER) }
    var selectedType by remember { mutableStateOf(IncomeType.ONE_TIME) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(incomeId != null) }

    val isEditing = incomeId != null

    LaunchedEffect(incomeId) {
        if (incomeId != null) {
            viewModel.selectIncome(incomeId)
        }
    }

    val selectedIncome by viewModel.selectedIncome.collectAsState()

    LaunchedEffect(selectedIncome) {
        selectedIncome?.let { income ->
            if (incomeId != null) {
                description = income.description
                amount = income.amount.toString()
                receiveDay = income.receiveDay.toString()
                selectedCategory = income.category
                selectedType = income.type
                isLoading = false
            }
        }
    }

    val isFormValid = description.isNotBlank() &&
        amount.toDoubleOrNull() != null &&
        (selectedType == IncomeType.ONE_TIME || receiveDay.toIntOrNull() in 1..31)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Receita" else "Nova Receita") },
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
        if (isLoading) {
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
                // Income Type Selector
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType }
                ) {
                    OutlinedTextField(
                        value = getIncomeTypeName(selectedType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Receita") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        IncomeType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(getIncomeTypeName(type))
                                        Text(
                                            text = when (type) {
                                                IncomeType.RECURRENT -> "Salário, mesada, etc."
                                                IncomeType.ONE_TIME -> "Valor pontual recebido"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                onClick = {
                                    selectedType = type
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    placeholder = {
                        Text(
                            when (selectedType) {
                                IncomeType.RECURRENT -> "Ex: Salário, Aluguel recebido..."
                                IncomeType.ONE_TIME -> "Ex: Venda, Presente..."
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Valor") },
                    placeholder = { Text("Ex: 5000.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("R$ ") }
                )

                // Show receive day only for recurrent income
                if (selectedType == IncomeType.RECURRENT) {
                    OutlinedTextField(
                        value = receiveDay,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() }
                            if (filtered.isEmpty() || (filtered.toIntOrNull() ?: 0) <= 31) {
                                receiveDay = filtered
                            }
                        },
                        label = { Text("Dia do Recebimento") },
                        placeholder = { Text("1-31") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = getIncomeCategoryName(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        leadingIcon = {
                            Icon(
                                imageVector = getIncomeCategoryIcon(selectedCategory),
                                contentDescription = null,
                                tint = getIncomeCategoryColor(selectedCategory)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        IncomeCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = getIncomeCategoryIcon(category),
                                            contentDescription = null,
                                            tint = getIncomeCategoryColor(category),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(getIncomeCategoryName(category))
                                    }
                                },
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
                        val income = Income(
                            id = incomeId ?: 0,
                            description = description,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            category = selectedCategory,
                            type = selectedType,
                            receiveDay = receiveDay.toIntOrNull() ?: 1,
                            date = selectedIncome?.date ?: System.currentTimeMillis(),
                            isReceived = selectedIncome?.isReceived ?: false,
                            isActive = true
                        )
                        if (isEditing) {
                            viewModel.updateIncome(income)
                        } else {
                            viewModel.insertIncome(income)
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(if (isEditing) "Salvar Alterações" else "Adicionar Receita")
                }
            }
        }
    }
}

