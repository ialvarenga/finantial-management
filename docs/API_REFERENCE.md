# Referência da API de Dados

Este documento descreve todas as operações disponíveis para manipulação de dados no app.

## 📊 CreditCardRepository

### Operações de Leitura

```kotlin
// Obter todos os cartões
fun getAllCreditCards(): Flow<List<CreditCard>>

// Obter cartão por ID
fun getCreditCardById(id: Long): Flow<CreditCard?>

// Obter itens de um cartão
fun getItemsByCardId(cardId: Long): Flow<List<CreditCardItem>>

// Obter todos os itens
fun getAllItems(): Flow<List<CreditCardItem>>

// Obter total de um cartão
fun getTotalByCardId(cardId: Long): Flow<Double?>
```

### Operações de Escrita

```kotlin
// Inserir cartão (retorna ID)
suspend fun insertCreditCard(creditCard: CreditCard): Long

// Atualizar cartão
suspend fun updateCreditCard(creditCard: CreditCard)

// Deletar cartão
suspend fun deleteCreditCard(creditCard: CreditCard)

// Deletar cartão por ID
suspend fun deleteCreditCardById(id: Long)

// Inserir item na fatura
suspend fun insertItem(item: CreditCardItem): Long

// Atualizar item
suspend fun updateItem(item: CreditCardItem)

// Deletar item
suspend fun deleteItem(item: CreditCardItem)

// Deletar item por ID
suspend fun deleteItemById(id: Long)
```

---

## 🏦 BankRepository

### Operações de Leitura

```kotlin
// Obter todos os bancos
fun getAllBanks(): Flow<List<Bank>>

// Obter banco por ID
fun getBankById(id: Long): Flow<Bank?>

// Obter saldo total
fun getTotalBalance(): Flow<Double?>
```

### Operações de Escrita

```kotlin
// Inserir banco (retorna ID)
suspend fun insertBank(bank: Bank): Long

// Atualizar banco
suspend fun updateBank(bank: Bank)

// Deletar banco
suspend fun deleteBank(bank: Bank)

// Deletar banco por ID
suspend fun deleteBankById(id: Long)
```

---

## 📋 FinancialCompromiseRepository

### Operações de Leitura

```kotlin
// Obter compromissos ativos
fun getAllActiveCompromises(): Flow<List<FinancialCompromise>>

// Obter todos os compromissos
fun getAllCompromises(): Flow<List<FinancialCompromise>>

// Obter compromisso por ID
fun getCompromiseById(id: Long): Flow<FinancialCompromise?>

// Obter total mensal
fun getTotalMonthlyCompromises(): Flow<Double?>
```

### Operações de Escrita

```kotlin
// Inserir compromisso (retorna ID)
suspend fun insertCompromise(compromise: FinancialCompromise): Long

// Atualizar compromisso
suspend fun updateCompromise(compromise: FinancialCompromise)

// Deletar compromisso
suspend fun deleteCompromise(compromise: FinancialCompromise)

// Deletar compromisso por ID
suspend fun deleteCompromiseById(id: Long)

// Atualizar status de pagamento
suspend fun updatePaidStatus(id: Long, isPaid: Boolean)

// Resetar todos os status de pagamento (início do mês)
suspend fun resetAllPaidStatus()
```

---

## 🔄 ViewModels

### CreditCardViewModel

```kotlin
// Estados observáveis
val allCreditCards: StateFlow<List<CreditCard>>
val allItems: StateFlow<List<CreditCardItem>>
val selectedCard: StateFlow<CreditCard?>
val cardItems: StateFlow<List<CreditCardItem>>
val cardTotal: StateFlow<Double>

// Ações
fun selectCard(cardId: Long)
fun insertCreditCard(creditCard: CreditCard)
fun updateCreditCard(creditCard: CreditCard)
fun deleteCreditCard(creditCard: CreditCard)
fun insertItem(item: CreditCardItem)
fun updateItem(item: CreditCardItem)
fun deleteItem(item: CreditCardItem)
```

### BankViewModel

```kotlin
// Estados observáveis
val allBanks: StateFlow<List<Bank>>
val totalBalance: StateFlow<Double>
val selectedBank: StateFlow<Bank?>

// Ações
fun selectBank(bankId: Long)
fun insertBank(bank: Bank)
fun updateBank(bank: Bank)
fun deleteBank(bank: Bank)
```

### FinancialCompromiseViewModel

```kotlin
// Estados observáveis
val allCompromises: StateFlow<List<FinancialCompromise>>
val totalMonthlyCompromises: StateFlow<Double>
val selectedCompromise: StateFlow<FinancialCompromise?>

// Ações
fun selectCompromise(compromiseId: Long)
fun insertCompromise(compromise: FinancialCompromise)
fun updateCompromise(compromise: FinancialCompromise)
fun deleteCompromise(compromise: FinancialCompromise)
fun togglePaidStatus(compromise: FinancialCompromise)
```

---

## 📝 Exemplos de Uso

### Criar um Cartão de Crédito

```kotlin
val novoCartao = CreditCard(
    name = "Nubank",
    cardLimit = 5000.0,
    dueDay = 15,
    closingDay = 8,
    color = 0xFF6200EE
)
creditCardViewModel.insertCreditCard(novoCartao)
```

### Adicionar Item na Fatura

```kotlin
val item = CreditCardItem(
    cardId = cartaoId,
    description = "Supermercado",
    amount = 250.0,
    purchaseDate = System.currentTimeMillis(),
    installments = 1,
    currentInstallment = 1,
    category = "Alimentação"
)
creditCardViewModel.insertItem(item)
```

### Criar Conta Bancária

```kotlin
val conta = Bank(
    name = "Itaú",
    balance = 3500.0,
    accountType = AccountType.CHECKING,
    color = 0xFF4CAF50
)
bankViewModel.insertBank(conta)
```

### Criar Conta Fixa

```kotlin
val aluguel = FinancialCompromise(
    name = "Aluguel",
    amount = 1500.0,
    dueDay = 10,
    category = CompromiseCategory.RENT,
    isPaid = false,
    isActive = true
)
compromiseViewModel.insertCompromise(aluguel)
```

### Marcar Conta como Paga

```kotlin
compromiseViewModel.togglePaidStatus(compromisso)
```

---

## 🎨 Categorias Disponíveis

### Categorias de Itens (Cartão)
- Alimentação
- Transporte
- Lazer
- Saúde
- Educação
- Compras
- Serviços
- Assinaturas
- Viagem
- Outros

### Categorias de Compromissos
| Enum | Nome PT-BR | Ícone |
|------|------------|-------|
| RENT | Aluguel | Home |
| ENERGY | Energia | Bolt |
| WATER | Água | WaterDrop |
| INTERNET | Internet | Wifi |
| PHONE | Telefone | Phone |
| INSURANCE | Seguro | Security |
| STREAMING | Streaming | Tv |
| GYM | Academia | FitnessCenter |
| EDUCATION | Educação | School |
| HEALTH | Saúde | LocalHospital |
| TRANSPORT | Transporte | DirectionsCar |
| OTHER | Outros | Receipt |

### Tipos de Conta Bancária
| Enum | Nome PT-BR |
|------|------------|
| CHECKING | Conta Corrente |
| SAVINGS | Poupança |
| INVESTMENT | Investimento |
| WALLET | Carteira |

