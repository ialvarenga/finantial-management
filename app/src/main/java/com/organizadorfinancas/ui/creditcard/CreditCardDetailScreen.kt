// ...existing code...
import androidx.compose.material.icons.filled.Upload

@Composable
fun CreditCardDetailScreen(
    creditCardId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToImport: (Long, String) -> Unit, // Add this parameter
    // ...existing parameters...
) {
    // ...existing code...

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(creditCard?.name ?: "Credit Card") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Add Import Button
                    IconButton(
                        onClick = {
                            creditCard?.let {
                                onNavigateToImport(it.id, it.name)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "Import Statement")
                    }
                    // ...existing actions...
                }
            )
        }
    ) { paddingValues ->
        // ...existing code...
    }
}

