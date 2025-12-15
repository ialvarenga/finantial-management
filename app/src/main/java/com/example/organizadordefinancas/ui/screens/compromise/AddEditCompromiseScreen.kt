package com.example.organizadordefinancas.ui.screens.compromise

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.CompromiseCategory
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.ui.viewmodel.FinancialCompromiseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCompromiseScreen(
    compromiseId: Long?,
    viewModel: FinancialCompromiseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CompromiseCategory.OTHER) }
    var expandedCategory by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(compromiseId != null) }

    val isEditing = compromiseId != null

    LaunchedEffect(compromiseId) {
        if (compromiseId != null) {
            viewModel.selectCompromise(compromiseId)
        }
    }

    val selectedCompromise by viewModel.selectedCompromise.collectAsState()

    LaunchedEffect(selectedCompromise) {
        selectedCompromise?.let { compromise ->
            if (compromiseId != null) {
                name = compromise.name
                amount = compromise.amount.toString()
                dueDay = compromise.dueDay.toString()
                selectedCategory = compromise.category
                isLoading = false
            }
        }
    }

    val isFormValid = name.isNotBlank() &&
        amount.toDoubleOrNull() != null &&
        dueDay.toIntOrNull() in 1..31

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Conta" else "Nova Conta Fixa") },
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Conta") },
                    placeholder = { Text("Ex: Aluguel, Luz, Internet...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Valor") },
                    placeholder = { Text("Ex: 500.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("R$ ") }
                )

                OutlinedTextField(
                    value = dueDay,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }
                        if (filtered.isEmpty() || filtered.toInt() <= 31) {
                            dueDay = filtered
                        }
                    },
                    label = { Text("Dia do Vencimento") },
                    placeholder = { Text("1-31") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = getCategoryName(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(selectedCategory),
                                contentDescription = null,
                                tint = getCategoryColor(selectedCategory)
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
                        CompromiseCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(category),
                                            contentDescription = null,
                                            tint = getCategoryColor(category),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(getCategoryName(category))
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
                        val compromise = FinancialCompromise(
                            id = compromiseId ?: 0,
                            name = name,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            dueDay = dueDay.toIntOrNull() ?: 1,
                            category = selectedCategory,
                            isPaid = selectedCompromise?.isPaid ?: false,
                            isActive = true
                        )
                        if (isEditing) {
                            viewModel.updateCompromise(compromise)
                        } else {
                            viewModel.insertCompromise(compromise)
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid
                ) {
                    Text(if (isEditing) "Salvar Alterações" else "Adicionar Conta")
                }
            }
        }
    }
}

