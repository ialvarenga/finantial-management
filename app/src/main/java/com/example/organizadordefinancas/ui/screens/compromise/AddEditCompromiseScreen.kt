package com.example.organizadordefinancas.ui.screens.compromise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.CompromiseCategory
import com.example.organizadordefinancas.data.model.CompromiseFrequency
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.data.model.getDisplayName
import com.example.organizadordefinancas.ui.viewmodel.CreditCardViewModel
import com.example.organizadordefinancas.ui.viewmodel.FinancialCompromiseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCompromiseScreen(
    compromiseId: Long?,
    viewModel: FinancialCompromiseViewModel,
    creditCardViewModel: CreditCardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CompromiseCategory.OTHER) }
    var expandedCategory by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(compromiseId != null) }
    var selectedCreditCardId by remember { mutableStateOf<Long?>(null) }
    var expandedCreditCard by remember { mutableStateOf(false) }

    // New frequency-related state
    var selectedFrequency by remember { mutableStateOf(CompromiseFrequency.MONTHLY) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var dayOfWeek by remember { mutableStateOf(1) } // 1 = Monday
    var expandedDayOfWeek by remember { mutableStateOf(false) }
    var monthOfYear by remember { mutableStateOf(1) } // 1 = January
    var expandedMonthOfYear by remember { mutableStateOf(false) }
    var reminderDaysBefore by remember { mutableStateOf("3") }

    val creditCards by creditCardViewModel.allCreditCards.collectAsState()
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
                selectedCreditCardId = compromise.linkedCreditCardId
                // Load frequency fields
                selectedFrequency = compromise.frequency
                dayOfWeek = compromise.dayOfWeek ?: 1
                monthOfYear = compromise.monthOfYear ?: 1
                reminderDaysBefore = compromise.reminderDaysBefore.toString()
                isLoading = false
            }
        }
    }

    val isFormValid = name.isNotBlank() &&
        amount.toDoubleOrNull() != null &&
        (selectedCreditCardId != null || when (selectedFrequency) {
            CompromiseFrequency.WEEKLY, CompromiseFrequency.BIWEEKLY -> true // dayOfWeek is always valid
            else -> dueDay.toIntOrNull() in 1..31
        })

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

                // Frequency Selector
                ExposedDropdownMenuBox(
                    expanded = expandedFrequency,
                    onExpandedChange = { expandedFrequency = !expandedFrequency }
                ) {
                    OutlinedTextField(
                        value = selectedFrequency.getDisplayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequência") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedFrequency,
                        onDismissRequest = { expandedFrequency = false }
                    ) {
                        CompromiseFrequency.entries.forEach { frequency ->
                            DropdownMenuItem(
                                text = { Text(frequency.getDisplayName()) },
                                onClick = {
                                    selectedFrequency = frequency
                                    expandedFrequency = false
                                }
                            )
                        }
                    }
                }

                // Day of Week selector (for WEEKLY and BIWEEKLY)
                if (selectedFrequency == CompromiseFrequency.WEEKLY || selectedFrequency == CompromiseFrequency.BIWEEKLY) {
                    val daysOfWeek = listOf(
                        1 to "Segunda-feira",
                        2 to "Terça-feira",
                        3 to "Quarta-feira",
                        4 to "Quinta-feira",
                        5 to "Sexta-feira",
                        6 to "Sábado",
                        7 to "Domingo"
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedDayOfWeek,
                        onExpandedChange = { expandedDayOfWeek = !expandedDayOfWeek }
                    ) {
                        OutlinedTextField(
                            value = daysOfWeek.find { it.first == dayOfWeek }?.second ?: "Segunda-feira",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dia da Semana") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDayOfWeek) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDayOfWeek,
                            onDismissRequest = { expandedDayOfWeek = false }
                        ) {
                            daysOfWeek.forEach { (day, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        dayOfWeek = day
                                        expandedDayOfWeek = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Only show due day field if not linked to a credit card and frequency requires it
                if (selectedCreditCardId == null &&
                    selectedFrequency != CompromiseFrequency.WEEKLY &&
                    selectedFrequency != CompromiseFrequency.BIWEEKLY) {
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
                }

                // Month selector for QUARTERLY, SEMIANNUAL, and ANNUAL
                if (selectedFrequency == CompromiseFrequency.QUARTERLY ||
                    selectedFrequency == CompromiseFrequency.SEMIANNUAL ||
                    selectedFrequency == CompromiseFrequency.ANNUAL) {
                    val months = listOf(
                        1 to "Janeiro",
                        2 to "Fevereiro",
                        3 to "Março",
                        4 to "Abril",
                        5 to "Maio",
                        6 to "Junho",
                        7 to "Julho",
                        8 to "Agosto",
                        9 to "Setembro",
                        10 to "Outubro",
                        11 to "Novembro",
                        12 to "Dezembro"
                    )

                    val monthLabel = when (selectedFrequency) {
                        CompromiseFrequency.ANNUAL -> "Mês do Vencimento"
                        CompromiseFrequency.SEMIANNUAL -> "Primeiro Mês"
                        CompromiseFrequency.QUARTERLY -> "Primeiro Mês do Trimestre"
                        else -> "Mês"
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedMonthOfYear,
                        onExpandedChange = { expandedMonthOfYear = !expandedMonthOfYear }
                    ) {
                        OutlinedTextField(
                            value = months.find { it.first == monthOfYear }?.second ?: "Janeiro",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(monthLabel) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonthOfYear) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedMonthOfYear,
                            onDismissRequest = { expandedMonthOfYear = false }
                        ) {
                            months.forEach { (month, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        monthOfYear = month
                                        expandedMonthOfYear = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Reminder days field
                OutlinedTextField(
                    value = reminderDaysBefore,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }
                        if (filtered.isEmpty() || filtered.toInt() <= 30) {
                            reminderDaysBefore = filtered
                        }
                    },
                    label = { Text("Lembrete (dias antes)") },
                    placeholder = { Text("Ex: 3") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Quantos dias antes você quer ser lembrado") }
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

                // Credit Card Link Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCreditCard,
                    onExpandedChange = { expandedCreditCard = !expandedCreditCard }
                ) {
                    val selectedCard = creditCards.find { it.id == selectedCreditCardId }
                    OutlinedTextField(
                        value = selectedCard?.name ?: "Nenhum (pago separadamente)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vincular ao Cartão") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = selectedCard?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCreditCard) },
                        supportingText = {
                            Text(
                                if (selectedCreditCardId != null)
                                    "Esta conta aparecerá na fatura do cartão"
                                else
                                    "Conta não vinculada a nenhum cartão"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCreditCard,
                        onDismissRequest = { expandedCreditCard = false }
                    ) {
                        // Option for no credit card
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("Nenhum (pago separadamente)")
                                }
                            },
                            onClick = {
                                selectedCreditCardId = null
                                expandedCreditCard = false
                            }
                        )

                        creditCards.forEach { card ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = Color(card.color),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(card.name)
                                    }
                                },
                                onClick = {
                                    selectedCreditCardId = card.id
                                    expandedCreditCard = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val effectiveDueDay = when {
                            selectedCreditCardId != null -> 0
                            selectedFrequency == CompromiseFrequency.WEEKLY ||
                            selectedFrequency == CompromiseFrequency.BIWEEKLY -> 0
                            else -> dueDay.toIntOrNull() ?: 1
                        }

                        val compromise = FinancialCompromise(
                            id = compromiseId ?: 0,
                            name = name,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            dueDay = effectiveDueDay,
                            category = selectedCategory,
                            isPaid = selectedCompromise?.isPaid ?: false,
                            isActive = true,
                            linkedCreditCardId = selectedCreditCardId,
                            // New frequency fields
                            frequency = selectedFrequency,
                            dayOfWeek = if (selectedFrequency == CompromiseFrequency.WEEKLY ||
                                           selectedFrequency == CompromiseFrequency.BIWEEKLY) dayOfWeek else null,
                            dayOfMonth = if (selectedFrequency != CompromiseFrequency.WEEKLY &&
                                            selectedFrequency != CompromiseFrequency.BIWEEKLY)
                                            (dueDay.toIntOrNull() ?: 1) else null,
                            monthOfYear = if (selectedFrequency == CompromiseFrequency.QUARTERLY ||
                                             selectedFrequency == CompromiseFrequency.SEMIANNUAL ||
                                             selectedFrequency == CompromiseFrequency.ANNUAL) monthOfYear else null,
                            startDate = selectedCompromise?.startDate ?: System.currentTimeMillis(),
                            endDate = selectedCompromise?.endDate,
                            reminderDaysBefore = reminderDaysBefore.toIntOrNull() ?: 3
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

