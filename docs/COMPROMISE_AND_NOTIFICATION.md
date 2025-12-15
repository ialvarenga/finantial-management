# Guia de Melhorias - Organizador de Finanças

## 1. Expansão do Modelo de Compromissos

### 1.1 Objetivo
Permitir compromissos com frequências além de mensal: semanal, quinzenal, trimestral, semestral e anual.

### 1.2 Mudanças Necessárias

#### Novo Enum: `CompromiseFrequency`
```kotlin
enum class CompromiseFrequency {
    WEEKLY,      // Semanal
    BIWEEKLY,    // Quinzenal
    MONTHLY,     // Mensal
    QUARTERLY,   // Trimestral
    SEMIANNUAL,  // Semestral
    ANNUAL       // Anual
}
```

#### Campos Novos em `FinancialCompromise`
```kotlin
val frequency: CompromiseFrequency = CompromiseFrequency.MONTHLY
val dayOfWeek: Int? = null      // Para WEEKLY/BIWEEKLY (1-7, Segunda=1)
val dayOfMonth: Int? = null     // Para MONTHLY+ (1-31)
val monthOfYear: Int? = null    // Para QUARTERLY/SEMIANNUAL/ANNUAL (1-12)
val startDate: Long             // Data de início
val endDate: Long? = null       // Data de fim (opcional)
val reminderDaysBefore: Int = 3 // Dias de antecedência para lembrete
```

#### Nova Entidade: `CompromiseOccurrence`
Rastreia cada ocorrência individual de um compromisso recorrente.

```kotlin
@Entity(tableName = "compromise_occurrences")
data class CompromiseOccurrence(
    val id: Long,
    val compromiseId: Long,      // FK -> FinancialCompromise
    val dueDate: Long,           // Data de vencimento desta ocorrência
    val expectedAmount: Double,
    val isPaid: Boolean = false,
    val paidDate: Long? = null,
    val paidAmount: Double? = null
)
```

### 1.3 Tarefas

- [x] Criar enum `CompromiseFrequency`
- [x] Adicionar campos novos em `FinancialCompromise`
- [x] Criar entidade `CompromiseOccurrence`
- [x] Criar `CompromiseOccurrenceDao`
- [x] Atualizar `FinancialCompromiseRepository` para gerar ocorrências automaticamente
- [x] Criar migration do banco (v6 → v7)
- [x] Atualizar UI de AddEditCompromise com seletor de frequência
- [x] Atualizar lista de compromissos para mostrar próxima ocorrência
- [x] Adicionar método `getNextDueDate()` no modelo
- [x] Adicionar método `getMonthlyEquivalent()` para cálculos

### 1.4 Lógica de Geração de Ocorrências
- Ao criar/editar compromisso, gerar ocorrências para os próximos 3 meses
- Executar geração ao abrir o app (verificar se precisa gerar mais)
- Limpar ocorrências pagas com mais de 6 meses

---

## 2. Sistema de Leitura de Notificações

### 2.1 Objetivo
Capturar automaticamente transações de notificações bancárias (compras, PIX) e criar itens no app.

### 2.2 Componentes

#### Nova Entidade: `CapturedNotification`
```kotlin
@Entity(tableName = "captured_notifications")
data class CapturedNotification(
    val id: Long,
    val packageName: String,        // Ex: "com.nu.production"
    val title: String,
    val content: String,
    val capturedAt: Long,
    val status: NotificationStatus, // PENDING, PROCESSED, IGNORED, FAILED
    val extractedAmount: Double?,
    val extractedMerchant: String?,
    val linkedItemId: Long?,        // FK -> CreditCardItem (após processar)
    val parsingConfidence: Float?
)

enum class NotificationStatus {
    PENDING, PROCESSED, IGNORED, FAILED, DUPLICATE
}
```

#### NotificationListenerService
Serviço que intercepta notificações do sistema.

```kotlin
class FinanceNotificationListener : NotificationListenerService() {
    
    // Apps bancários brasileiros suportados
    private val bankPackages = setOf(
        "com.nu.production",        // Nubank
        "com.itau",                 // Itaú
        "com.bradesco",             // Bradesco
        "br.com.bb.android",        // Banco do Brasil
        "com.santander.app",        // Santander
        "br.com.intermedium",       // Inter
        "com.c6bank.app",           // C6 Bank
        "com.picpay",               // PicPay
        "br.com.mercadopago.wallet" // Mercado Pago
    )
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 1. Filtrar por packageName
        // 2. Extrair título e conteúdo
        // 3. Salvar em captured_notifications
        // 4. Tentar parsing com regex
    }
}
```

#### Parser de Notificações
Extrai valor e estabelecimento usando regex patterns.

```kotlin
// Patterns comuns
val patterns = listOf(
    Regex("""Compra aprovada de R\$ ([\d.,]+) em (.+)"""),      // Nubank
    Regex("""Compra no débito: R\$ ([\d.,]+) - (.+)"""),        // Itaú
    Regex("""PIX enviado R\$ ([\d.,]+) para (.+)"""),           // PIX
    Regex("""PIX recebido R\$ ([\d.,]+) de (.+)""")             // PIX
)
```

### 2.3 Tarefas

- [x] Criar entidade `CapturedNotification` e DAO
- [x] Criar `FinanceNotificationListener` (NotificationListenerService)
- [x] Implementar `NotificationParser` com regex patterns
- [x] Criar tela de permissão para Notification Access
- [x] Criar tela de revisão de transações pendentes
- [x] Adicionar configuração para ativar/desativar por app
- [x] Implementar detecção de duplicatas
- [x] Adicionar vinculação automática com cartão (baseado no app)
- [x] Adicionar campo `lastFourDigits` no `CreditCard` para Google Wallet
- [x] Adicionar suporte ao Google Wallet no parser

### 2.4 Fluxo do Usuário

1. Usuário habilita permissão de Notification Access
2. Configura quais apps monitorar
3. Recebe notificação de compra → app captura automaticamente
4. Badge aparece mostrando transações pendentes
5. Usuário revisa, edita se necessário, e confirma
6. Transação é criada no cartão apropriado

### 2.5 Permissões Necessárias

```xml
<!-- AndroidManifest.xml -->
<service
    android:name=".service.FinanceNotificationListener"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

### 2.6 UI Necessária

- **Tela de Configuração**: Toggle para cada app bancário
- **Tela de Pendentes**: Lista de transações aguardando confirmação
- **Card de Transação Pendente**: Mostra valor, estabelecimento, permite editar e escolher cartão
- **Indicador**: Badge no menu ou FAB mostrando quantidade de pendentes

---

## Ordem de Implementação Sugerida

1. **Fase 1**: Modelo de frequências (sem UI)
2. **Fase 2**: UI de frequências + migration
3. **Fase 3**: NotificationListenerService básico
4. **Fase 4**: Parser com regex
5. **Fase 5**: UI de revisão de pendentes
6. **Fase 6**: Configurações e polimento