package com.example.organizadordefinancas.ui.screens.creditcard

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.CreditCardItem
import com.example.organizadordefinancas.data.parser.ParsedStatementItem
import com.example.organizadordefinancas.data.parser.StatementParserFactory
import com.example.organizadordefinancas.data.parser.toTimestamp
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.CreditCardViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportStatementScreen(
    cardId: Long,
    viewModel: CreditCardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val card by viewModel.selectedCard.collectAsState()

    var items by remember { mutableStateOf<List<ParsedStatementItem>>(emptyList()) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("Outros") }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val categories = listOf(
        "Alimentação",
        "Transporte",
        "Compras",
        "Saúde",
        "Lazer",
        "Educação",
        "Assinaturas",
        "Contas",
        "Outros"
    )

    LaunchedEffect(cardId) {
        viewModel.selectCard(cardId)
    }

    // Handle success navigation
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(1500)
            onNavigateBack()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "file"
            selectedFileName = fileName
            isLoading = true
            errorMessage = null

            val result = parseFile(context, it, fileName)
            result.fold(
                onSuccess = { parsedItems ->
                    items = parsedItems
                    errorMessage = null
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Erro ao processar arquivo"
                    items = emptyList()
                }
            )
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar Extrato") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Credit Card Info
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Importando para:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = card?.name ?: "Cartão",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // File Selection Button
            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf(
                        "text/csv",
                        "text/comma-separated-values",
                        "application/x-ofx",
                        "application/vnd.intu.qfx",
                        "*/*"
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedFileName ?: "Selecionar Arquivo CSV ou OFX")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection
            if (items.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria para todos os itens") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selection Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        items = items.map { it.copy(isSelected = true) }
                    }) {
                        Text("Selecionar Todos")
                    }
                    TextButton(onClick = {
                        items = items.map { it.copy(isSelected = false) }
                    }) {
                        Text("Desmarcar Todos")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Items count
                val selectedCount = items.count { it.isSelected }
                val totalAmount = items.filter { it.isSelected }.sumOf { it.amount }
                Text(
                    text = "$selectedCount de ${items.size} itens selecionados • Total: ${formatCurrency(totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Success message
            successMessage?.let { success ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = success,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Items List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    StatementItemCard(
                        item = item,
                        onClick = {
                            items = items.toMutableList().apply {
                                this[index] = item.copy(isSelected = !item.isSelected)
                            }
                        }
                    )
                }
            }

            // Import Button
            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                val selectedCount = items.count { it.isSelected }
                Button(
                    onClick = {
                        val selectedItems = items.filter { it.isSelected }
                        if (selectedItems.isNotEmpty()) {
                            isLoading = true
                            selectedItems.forEach { item ->
                                val creditCardItem = CreditCardItem(
                                    id = 0,
                                    cardId = cardId,
                                    description = item.description,
                                    amount = item.amount,
                                    purchaseDate = item.date.toTimestamp(),
                                    installments = 1,
                                    currentInstallment = 1,
                                    category = selectedCategory
                                )
                                viewModel.insertItem(creditCardItem)
                            }
                            isLoading = false
                            successMessage = "${selectedItems.size} itens importados com sucesso!"
                        }
                    },
                    enabled = selectedCount > 0 && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar $selectedCount Itens")
                }
            }
        }
    }
}

@Composable
private fun StatementItemCard(
    item: ParsedStatementItem,
    onClick: () -> Unit
) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.date.format(dateFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(item.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onClick() }
                )
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var fileName: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            fileName = cursor.getString(nameIndex)
        }
    }
    return fileName
}

private fun parseFile(
    context: Context,
    uri: Uri,
    fileName: String
): Result<List<ParsedStatementItem>> {
    return try {
        val parser = StatementParserFactory.getParser(fileName)
        if (parser == null) {
            return Result.failure(IllegalArgumentException("Formato não suportado. Use CSV ou OFX."))
        }

        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            return Result.failure(IllegalArgumentException("Não foi possível abrir o arquivo"))
        }

        val result = parser.parse(inputStream)
        inputStream.close()
        result
    } catch (e: Exception) {
        Result.failure(e)
    }
}

