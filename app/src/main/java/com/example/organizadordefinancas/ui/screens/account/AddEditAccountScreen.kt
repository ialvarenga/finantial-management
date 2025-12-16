package com.example.organizadordefinancas.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.AccountTypes
import com.example.organizadordefinancas.ui.viewmodel.AccountListViewModel

/**
 * Available bank names in Brazil
 */
val bankOptions = listOf(
    "Nubank",
    "Itaú",
    "Bradesco",
    "Banco do Brasil",
    "Santander",
    "Caixa",
    "Inter",
    "C6 Bank",
    "BTG Pactual",
    "Neon",
    "PicPay",
    "PagBank",
    "Mercado Pago",
    "Original",
    "Sicoob",
    "Sicredi",
    "Outro"
)

val accountTypeOptions = listOf(
    AccountTypes.CHECKING to "Conta Corrente",
    AccountTypes.SAVINGS to "Poupança",
    AccountTypes.INVESTMENT to "Investimentos",
    AccountTypes.WALLET to "Carteira"
)

val colorOptions = listOf(
    0xFF8C1A90 to "Nubank (Roxo)",
    0xFFEC7000 to "Itaú (Laranja)",
    0xFFC3002F to "Bradesco (Vermelho)",
    0xFFFEDE00 to "Banco do Brasil (Amarelo)",
    0xFFE10000 to "Santander (Vermelho)",
    0xFF005CA9 to "Caixa (Azul)",
    0xFFFF7A00 to "Inter (Laranja)",
    0xFF242424 to "C6 Bank (Preto)",
    0xFF07161E to "BTG (Azul Escuro)",
    0xFF00A5DF to "Neon (Azul)",
    0xFF21C25E to "PicPay (Verde)",
    0xFF00A259 to "PagBank (Verde)",
    0xFF009EE3 to "Mercado Pago (Azul)",
    0xFF00C571 to "Original (Verde)",
    0xFF003B2C to "Sicoob (Verde)",
    0xFF00463E to "Sicredi (Verde)",
    0xFF03DAC5 to "Padrão (Teal)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    accountId: Long?,
    viewModel: AccountListViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = accountId != null

    var name by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf(bankOptions.first()) }
    var accountType by remember { mutableStateOf(AccountTypes.CHECKING) }
    var initialBalance by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(colorOptions.first().first) }
    var accountNumber by remember { mutableStateOf("") }

    var bankDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var colorDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Load existing account data if editing
    LaunchedEffect(accountId) {
        if (accountId != null) {
            // TODO: Load account data for editing
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Conta" else "Nova Conta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isNotBlank() && bankName.isNotBlank()) {
                                isSaving = true
                                val balance = initialBalance.toDoubleOrNull() ?: 0.0
                                viewModel.createAccount(
                                    name = name,
                                    bankName = bankName,
                                    accountType = accountType,
                                    initialBalance = balance,
                                    color = selectedColor
                                )
                                onNavigateBack()
                            }
                        },
                        enabled = name.isNotBlank() && bankName.isNotBlank() && !isSaving
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Salvar",
                            tint = if (name.isNotBlank() && bankName.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(selectedColor)
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
            // Account Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da Conta") },
                placeholder = { Text("Ex: Conta Principal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Bank Name Dropdown
            ExposedDropdownMenuBox(
                expanded = bankDropdownExpanded,
                onExpandedChange = { bankDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Banco") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = bankDropdownExpanded,
                    onDismissRequest = { bankDropdownExpanded = false }
                ) {
                    bankOptions.forEach { bank ->
                        DropdownMenuItem(
                            text = { Text(bank) },
                            onClick = {
                                bankName = bank
                                // Auto-select matching color
                                colorOptions.find { it.second.contains(bank, ignoreCase = true) }?.let {
                                    selectedColor = it.first
                                }
                                bankDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Account Type Dropdown
            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = accountTypeOptions.find { it.first == accountType }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Conta") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false }
                ) {
                    accountTypeOptions.forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                accountType = type
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Initial Balance (only for new accounts)
            if (!isEditing) {
                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { value ->
                        // Only allow valid decimal numbers
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                            initialBalance = value
                        }
                    },
                    label = { Text("Saldo Inicial") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("R$ ") }
                )
            }

            // Account Number (optional)
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Número da Conta (opcional)") },
                placeholder = { Text("Ex: 12345-6") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Color Selector
            ExposedDropdownMenuBox(
                expanded = colorDropdownExpanded,
                onExpandedChange = { colorDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = colorOptions.find { it.first == selectedColor }?.second ?: "Personalizado",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cor") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = MaterialTheme.shapes.small,
                                color = Color(selectedColor)
                            ) {}
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = colorDropdownExpanded,
                    onDismissRequest = { colorDropdownExpanded = false }
                ) {
                    colorOptions.forEach { (color, label) ->
                        DropdownMenuItem(
                            text = {
                                Row {
                                    Surface(
                                        modifier = Modifier.size(24.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = Color(color)
                                    ) {}
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(label)
                                }
                            },
                            onClick = {
                                selectedColor = color
                                colorDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = {
                    if (name.isNotBlank() && bankName.isNotBlank()) {
                        isSaving = true
                        val balance = initialBalance.toDoubleOrNull() ?: 0.0
                        viewModel.createAccount(
                            name = name,
                            bankName = bankName,
                            accountType = accountType,
                            initialBalance = balance,
                            color = selectedColor
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && bankName.isNotBlank() && !isSaving
            ) {
                Text(if (isEditing) "Salvar Alterações" else "Criar Conta")
            }
        }
    }
}

