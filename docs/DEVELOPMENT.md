# Guia de Desenvolvimento

Este documento descreve as convenções e padrões utilizados no projeto.

## 🏛️ Padrões de Arquitetura

### MVVM + Repository Pattern

```
View (Composable) → ViewModel → Repository → DAO → Room Database
```

#### Fluxo de Dados
1. **UI** observa `StateFlow` do ViewModel
2. **ViewModel** expõe dados via `StateFlow` e métodos de ação
3. **Repository** abstrai a fonte de dados
4. **DAO** executa queries no Room
5. **Room** persiste dados no SQLite

### Injeção de Dependência (Manual)

O projeto usa DI manual através da classe `FinanceApplication`:

```kotlin
class FinanceApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    val creditCardRepository by lazy { 
        CreditCardRepository(database.creditCardDao(), database.creditCardItemDao()) 
    }
    // ...
}
```

Os ViewModels são criados com Factories:

```kotlin
val creditCardViewModel: CreditCardViewModel = viewModel(
    factory = CreditCardViewModelFactory(application.creditCardRepository)
)
```

## 📝 Convenções de Código

### Nomenclatura

| Tipo | Convenção | Exemplo |
|------|-----------|---------|
| Classes | PascalCase | `CreditCardViewModel` |
| Funções | camelCase | `getAllCreditCards()` |
| Variáveis | camelCase | `cardLimit` |
| Constantes | UPPER_SNAKE | `DEFAULT_COLOR` |
| Composables | PascalCase | `HomeScreen()` |
| Pacotes | lowercase | `com.example.organizadordefinancas` |

### Estrutura de Arquivos

- **Screens**: Uma pasta por feature (ex: `creditcard/`, `bank/`)
- **ViewModels**: Um ViewModel por feature
- **Models**: Uma classe por entidade
- **DAOs**: Um DAO por entidade

## 🗂️ Room Database

### Entidades

Todas as entidades usam a anotação `@Entity`:

```kotlin
@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // ...
)
```

### Relacionamentos

`CreditCardItem` tem Foreign Key para `CreditCard`:

```kotlin
@Entity(
    tableName = "credit_card_items",
    foreignKeys = [
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cardId"])]
)
```

### Queries

Todas as queries retornam `Flow<T>` para reatividade:

```kotlin
@Query("SELECT * FROM credit_cards ORDER BY name ASC")
fun getAllCreditCards(): Flow<List<CreditCard>>
```

## 🎨 Jetpack Compose

### Padrão de Screens

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel,
    onNavigateToX: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = { /* ... */ },
        floatingActionButton = { /* ... */ }
    ) { paddingValues ->
        // Content
    }
}
```

### State Management

- Use `StateFlow` no ViewModel
- Colete com `collectAsState()` no Composable
- Evite estados mutáveis diretamente no Composable

### Navegação

Rotas definidas em `sealed class Screen`:

```kotlin
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Início", Icons.Default.Home)
    object CreditCardDetail : Screen("credit_card_detail/{cardId}", "Detalhes") {
        fun createRoute(cardId: Long) = "credit_card_detail/$cardId"
    }
}
```

## 🧪 Testes

### Estrutura de Testes
```
app/src/
├── test/           # Testes unitários
└── androidTest/    # Testes instrumentados
```

### O que testar
- **ViewModels**: Lógica de negócio
- **Repositories**: Transformações de dados
- **DAOs**: Queries do Room (instrumented)
- **Composables**: UI com Compose Testing

## 🔧 Configuração

### build.gradle.kts (app)

Plugins necessários:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)  // Para Room
}
```

### AndroidManifest.xml

Registrar a Application:
```xml
<application
    android:name=".FinanceApplication"
    ...>
```

## 📱 Compatibilidade

- **minSdk**: 33 (Android 13)
- **targetSdk**: 36
- **Compile SDK**: 36

## 🎯 Próximos Passos (Sugestões)

1. [ ] Adicionar Hilt para DI
2. [ ] Implementar testes unitários
3. [ ] Adicionar gráficos de gastos
4. [ ] Exportar dados para CSV/PDF
5. [ ] Notificações de vencimento
6. [ ] Backup na nuvem
7. [ ] Tema escuro personalizado
8. [ ] Multi-moeda

