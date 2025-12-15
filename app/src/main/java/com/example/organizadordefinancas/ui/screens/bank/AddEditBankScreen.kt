package com.example.organizadordefinancas.ui.screens.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.AccountType
import com.example.organizadordefinancas.data.model.Bank
import com.example.organizadordefinancas.ui.viewmodel.BankViewModel

val bankColors = listOf(
    0xFF03DAC5, // Teal
    0xFF4CAF50, // Green
    0xFF2196F3, // Blue
    0xFFFF9800, // Orange
    0xFF9C27B0, // Purple
    0xFFE91E63, // Pink
    0xFF00BCD4, // Cyan
    0xFF795548, // Brown
    0xFF607D8B, // Blue Grey
    0xFFFFC107  // Amber
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBankScreen(
    bankId: Long?,
    viewModel: BankViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var savingsBalance by remember { mutableStateOf("") }
    var selectedAccountType by remember { mutableStateOf(AccountType.CHECKING) }
    var selectedColor by remember { mutableStateOf(bankColors[0]) }
    var expandedAccountType by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(bankId != null) }

    val isEditing = bankId != null

    LaunchedEffect(bankId) {
        if (bankId != null) {
            viewModel.selectBank(bankId)
        }
    }

    val selectedBank by viewModel.selectedBank.collectAsState()

    LaunchedEffect(selectedBank) {
        selectedBank?.let { bank ->
            if (bankId != null) {
                name = bank.name
                balance = bank.balance.toString()
                savingsBalance = if (bank.savingsBalance > 0) bank.savingsBalance.toString() else ""
                selectedAccountType = bank.accountType
                selectedColor = bank.color
                isLoading = false
            }
        }
    }

    val isFormValid = name.isNotBlank() && balance.toDoubleOrNull() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Banco" else "Novo Banco") },
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
                    label = { Text("Nome do Banco") },
                    placeholder = { Text("Ex: Nubank, Itaú, Bradesco...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = balance,
                    onValueChange = {
                        balance = it.filter { c -> c.isDigit() || c == '.' || c == '-' }
                    },
                    label = { Text("Saldo") },
                    placeholder = { Text("Ex: 1500.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("R$ ") },
                    supportingText = { Text("Use '-' para saldo negativo") }
                )

                OutlinedTextField(
                    value = savingsBalance,
                    onValueChange = {
                        savingsBalance = it.filter { c -> c.isDigit() || c == '.' }
                    },
                    label = { Text("Reserva de Emergência") },
                    placeholder = { Text("Ex: 5000.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("R$ ") },
                    supportingText = { Text("Dinheiro que você não vai usar no dia a dia") }
                )

                ExposedDropdownMenuBox(
                    expanded = expandedAccountType,
                    onExpandedChange = { expandedAccountType = !expandedAccountType }
                ) {
                    OutlinedTextField(
                        value = getAccountTypeName(selectedAccountType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Conta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccountType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedAccountType,
                        onDismissRequest = { expandedAccountType = false }
                    ) {
                        AccountType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(getAccountTypeName(type)) },
                                onClick = {
                                    selectedAccountType = type
                                    expandedAccountType = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Cor",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bankColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(3.dp, Color.White, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selecionado",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val bank = Bank(
                            id = bankId ?: 0,
                            name = name,
                            balance = balance.toDoubleOrNull() ?: 0.0,
                            savingsBalance = savingsBalance.toDoubleOrNull() ?: 0.0,
                            accountType = selectedAccountType,
                            color = selectedColor
                        )
                        if (isEditing) {
                            viewModel.updateBank(bank)
                        } else {
                            viewModel.insertBank(bank)
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid
                ) {
                    Text(if (isEditing) "Salvar Alterações" else "Adicionar Banco")
                }
            }
        }
    }
}

private fun getAccountTypeName(type: AccountType): String {
    return when (type) {
        AccountType.CHECKING -> "Conta Corrente"
        AccountType.SAVINGS -> "Poupança"
        AccountType.INVESTMENT -> "Investimento"
        AccountType.WALLET -> "Carteira"
    }
}

