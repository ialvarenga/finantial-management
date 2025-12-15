package com.example.organizadordefinancas.ui.screens.notification

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.BankAppConfig
import com.example.organizadordefinancas.service.FinanceNotificationListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(FinanceNotificationListener.isNotificationAccessGranted(context)) }
    val prefs = remember { context.getSharedPreferences(FinanceNotificationListener.PREFS_NAME, Context.MODE_PRIVATE) }

    var listenerEnabled by remember {
        mutableStateOf(prefs.getBoolean(FinanceNotificationListener.PREF_LISTENER_ENABLED, true))
    }

    val defaultApps = BankAppConfig.DEFAULT_BANK_APPS.map { it.packageName }.toSet()
    var enabledApps by remember {
        mutableStateOf(prefs.getStringSet(FinanceNotificationListener.PREF_ENABLED_APPS, defaultApps) ?: defaultApps)
    }

    // Refresh access status when returning to screen
    LaunchedEffect(Unit) {
        hasAccess = FinanceNotificationListener.isNotificationAccessGranted(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações de Notificações") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Status Card
            item {
                PermissionStatusCard(
                    hasAccess = hasAccess,
                    context = context,
                    onRefresh = {
                        hasAccess = FinanceNotificationListener.isNotificationAccessGranted(context)
                    }
                )
            }

            // Master Toggle
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Captura Automática",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Capturar transações de notificações bancárias",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = listenerEnabled,
                            onCheckedChange = { enabled ->
                                listenerEnabled = enabled
                                prefs.edit()
                                    .putBoolean(FinanceNotificationListener.PREF_LISTENER_ENABLED, enabled)
                                    .apply()
                            },
                            enabled = hasAccess
                        )
                    }
                }
            }

            // Bank Apps Section
            item {
                Text(
                    text = "Aplicativos Monitorados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Selecione os apps dos quais deseja capturar transações",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Bank app toggles
            items(BankAppConfig.DEFAULT_BANK_APPS) { bankApp ->
                BankAppToggle(
                    bankApp = bankApp,
                    isEnabled = bankApp.packageName in enabledApps,
                    onToggle = { enabled ->
                        val newSet = enabledApps.toMutableSet()
                        if (enabled) {
                            newSet.add(bankApp.packageName)
                        } else {
                            newSet.remove(bankApp.packageName)
                        }
                        enabledApps = newSet
                        prefs.edit()
                            .putStringSet(FinanceNotificationListener.PREF_ENABLED_APPS, newSet)
                            .apply()
                    },
                    enabled = hasAccess && listenerEnabled
                )
            }

            // Info Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard()
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    hasAccess: Boolean,
    context: Context,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasAccess) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasAccess) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (hasAccess) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasAccess) "Permissão Concedida" else "Permissão Necessária",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasAccess) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Text(
                    text = if (hasAccess) {
                        "O app pode capturar notificações"
                    } else {
                        "Clique para habilitar o acesso às notificações"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasAccess) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    }
                )
            }
            if (!hasAccess) {
                Button(
                    onClick = {
                        FinanceNotificationListener.openNotificationAccessSettings(context)
                    }
                ) {
                    Text("Habilitar")
                }
            } else {
                TextButton(onClick = onRefresh) {
                    Text("Atualizar")
                }
            }
        }
    }
}

@Composable
private fun BankAppToggle(
    bankApp: BankAppConfig,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled && enabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (isEnabled && enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = bankApp.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = bankApp.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Como funciona?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = """
                    1. O app monitora notificações dos apps selecionados
                    2. Quando detecta uma compra, extrai o valor e estabelecimento
                    3. A transação aparece na lista de pendentes para você revisar
                    4. Você confirma, edita ou ignora cada transação
                    
                    💡 Dica: Cadastre os últimos 4 dígitos do seu cartão para vinculação automática com Google Wallet.
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

