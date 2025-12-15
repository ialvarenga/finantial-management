package com.example.organizadordefinancas.ui.screens.creditcard

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
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.ui.viewmodel.CreditCardViewModel

val cardColors = listOf(
    0xFF6200EE, // Purple
    0xFF03DAC5, // Teal
    0xFFE91E63, // Pink
    0xFF4CAF50, // Green
    0xFFFF9800, // Orange
    0xFF2196F3, // Blue
    0xFFF44336, // Red
    0xFF9C27B0, // Deep Purple
    0xFF00BCD4, // Cyan
    0xFF795548  // Brown
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCreditCardScreen(
    cardId: Long?,
    viewModel: CreditCardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var cardLimit by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var closingDay by remember { mutableStateOf("") }
    var lastFourDigits by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(cardColors[0]) }
    var isLoading by remember { mutableStateOf(cardId != null) }

    val isEditing = cardId != null

    LaunchedEffect(cardId) {
        if (cardId != null) {
            viewModel.selectCard(cardId)
        }
    }

    val selectedCard by viewModel.selectedCard.collectAsState()

    LaunchedEffect(selectedCard) {
        selectedCard?.let { card ->
            if (cardId != null) {
                name = card.name
                cardLimit = card.cardLimit.toString()
                dueDay = card.dueDay.toString()
                closingDay = card.closingDay.toString()
                lastFourDigits = card.lastFourDigits ?: ""
                selectedColor = card.color
                isLoading = false
            }
        }
    }

    val isFormValid = name.isNotBlank() &&
        cardLimit.toDoubleOrNull() != null &&
        dueDay.toIntOrNull() in 1..31 &&
        closingDay.toIntOrNull() in 1..31

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Cartão" else "Novo Cartão") },
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
                    label = { Text("Nome do Cartão") },
                    placeholder = { Text("Ex: Nubank, Itaú...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = cardLimit,
                    onValueChange = { cardLimit = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Limite") },
                    placeholder = { Text("Ex: 5000.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("R$ ") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() }
                            if (filtered.isEmpty() || filtered.toInt() <= 31) {
                                dueDay = filtered
                            }
                        },
                        label = { Text("Dia Vencimento") },
                        placeholder = { Text("1-31") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = closingDay,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() }
                            if (filtered.isEmpty() || filtered.toInt() <= 31) {
                                closingDay = filtered
                            }
                        },
                        label = { Text("Dia Fechamento") },
                        placeholder = { Text("1-31") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = lastFourDigits,
                    onValueChange = { input ->
                        val filtered = input.filter { c -> c.isDigit() }.take(4)
                        lastFourDigits = filtered
                    },
                    label = { Text("Últimos 4 Dígitos (Opcional)") },
                    placeholder = { Text("Ex: 1234") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Para vincular automaticamente com Google Wallet") }
                )

                Text(
                    text = "Cor do Cartão",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cardColors.forEach { color ->
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
                        val card = CreditCard(
                            id = cardId ?: 0,
                            name = name,
                            cardLimit = cardLimit.toDoubleOrNull() ?: 0.0,
                            dueDay = dueDay.toIntOrNull() ?: 1,
                            closingDay = closingDay.toIntOrNull() ?: 1,
                            color = selectedColor,
                            lastFourDigits = lastFourDigits.ifBlank { null }
                        )
                        if (isEditing) {
                            viewModel.updateCreditCard(card)
                        } else {
                            viewModel.insertCreditCard(card)
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid
                ) {
                    Text(if (isEditing) "Salvar Alterações" else "Adicionar Cartão")
                }
            }
        }
    }
}

