# Organizador de Finanças - Documentação do Projeto

Um aplicativo Android para organização de finanças pessoais, desenvolvido com Kotlin e Jetpack Compose.

## 📱 Visão Geral

O **Organizador de Finanças** é um app que permite gerenciar:
- 💳 **Cartões de Crédito** - com controle de fatura e itens de compra
- 🏦 **Contas Bancárias** - saldos e tipos de conta
- 📄 **Contas Fixas** - compromissos financeiros recorrentes (aluguel, energia, internet, etc.)
- 💰 **Receitas** - controle de rendas recorrentes e pontuais
- 📥 **Importação de Extratos** - importar faturas de arquivos CSV/OFX

## 🏗️ Arquitetura

O projeto segue a arquitetura **MVVM (Model-View-ViewModel)** com as seguintes camadas:

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│  (Screens, Components, Navigation, Theme)               │
├─────────────────────────────────────────────────────────┤
│                    ViewModel Layer                      │
│  (CreditCardViewModel, BankViewModel, CompromiseVM,     │
│   IncomeViewModel)                                      │
├─────────────────────────────────────────────────────────┤
│                   Repository Layer                      │
│  (CreditCardRepo, BankRepo, CompromiseRepo, IncomeRepo) │
├─────────────────────────────────────────────────────────┤
│                     Data Layer                          │
│  (Room Database, DAOs, Entities)                        │
└─────────────────────────────────────────────────────────┘
```

## 📁 Estrutura de Pastas

```
app/src/main/java/com/example/organizadordefinancas/
│
├── MainActivity.kt              # Activity principal
├── FinanceApplication.kt        # Application class (DI manual)
│
├── data/                        # Camada de dados
│   ├── model/                   # Entidades do Room
│   │   ├── CreditCard.kt
│   │   ├── CreditCardItem.kt
│   │   ├── Bank.kt
│   │   ├── FinancialCompromise.kt
│   │   └── Income.kt
│   │
│   ├── dao/                     # Data Access Objects
│   │   ├── CreditCardDao.kt
│   │   ├── CreditCardItemDao.kt
│   │   ├── BankDao.kt
│   │   ├── FinancialCompromiseDao.kt
│   │   └── IncomeDao.kt
│   │
│   ├── database/                # Configuração do Room
│   │   └── AppDatabase.kt
│   │
│   ├── parser/                  # Parsers de arquivo
│   │   └── StatementParser.kt   # Parser CSV/OFX
│   │
│   └── repository/              # Repositórios
│       ├── CreditCardRepository.kt
│       ├── BankRepository.kt
│       ├── FinancialCompromiseRepository.kt
│       └── IncomeRepository.kt
│
└── ui/                          # Camada de UI
    ├── viewmodel/               # ViewModels
    │   ├── CreditCardViewModel.kt
    │   ├── BankViewModel.kt
    │   ├── FinancialCompromiseViewModel.kt
    │   └── IncomeViewModel.kt
    │
    ├── navigation/              # Navegação
    │   ├── Screen.kt            # Definição de rotas
    │   └── NavGraph.kt          # Configuração do NavHost
    │
    ├── screens/                 # Telas do app
    │   ├── home/
    │   │   └── HomeScreen.kt
    │   ├── creditcard/
    │   │   ├── CreditCardListScreen.kt
    │   │   ├── CreditCardDetailScreen.kt
    │   │   ├── AddEditCreditCardScreen.kt
    │   │   ├── AddCreditCardItemScreen.kt
    │   │   ├── EditCreditCardItemScreen.kt
    │   │   └── ImportStatementScreen.kt
    │   ├── bank/
    │   │   ├── BankListScreen.kt
    │   │   └── AddEditBankScreen.kt
    │   ├── compromise/
    │   │   ├── CompromiseListScreen.kt
    │   │   └── AddEditCompromiseScreen.kt
    │   └── income/
    │       ├── IncomeListScreen.kt
    │       └── AddEditIncomeScreen.kt
    │
    ├── components/              # Componentes reutilizáveis
    │   └── CommonComponents.kt
    │
    └── theme/                   # Tema Material 3
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## 🗄️ Modelos de Dados

### CreditCard (Cartão de Crédito)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | Long | Identificador único (auto-gerado) |
| name | String | Nome do cartão |
| cardLimit | Double | Limite do cartão |
| dueDay | Int | Dia de vencimento (1-31) |
| closingDay | Int | Dia de fechamento (1-31) |
| color | Long | Cor do cartão (hex) |

### CreditCardItem (Item da Fatura)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | Long | Identificador único |
| cardId | Long | FK para CreditCard |
| description | String | Descrição da compra |
| amount | Double | Valor |
| purchaseDate | Long | Data da compra (timestamp) |
| installments | Int | Número de parcelas |
| currentInstallment | Int | Parcela atual |
| category | String | Categoria da compra |

### Bank (Conta Bancária)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | Long | Identificador único |
| name | String | Nome do banco |
| balance | Double | Saldo atual |
| accountType | AccountType | Tipo de conta |
| color | Long | Cor do banco |

**AccountType (Enum):**
- `CHECKING` - Conta Corrente
- `SAVINGS` - Poupança
- `INVESTMENT` - Investimento
- `WALLET` - Carteira

### FinancialCompromise (Conta Fixa)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | Long | Identificador único |
| name | String | Nome da conta |
| amount | Double | Valor mensal |
| dueDay | Int | Dia de vencimento (1-31) |
| category | CompromiseCategory | Categoria |
| isPaid | Boolean | Se foi paga no mês |
| isActive | Boolean | Se está ativa |

**CompromiseCategory (Enum):**
- `RENT` - Aluguel
- `ENERGY` - Energia
- `WATER` - Água
- `INTERNET` - Internet
- `PHONE` - Telefone
- `INSURANCE` - Seguro
- `STREAMING` - Streaming
- `GYM` - Academia
- `EDUCATION` - Educação
- `HEALTH` - Saúde
- `TRANSPORT` - Transporte
- `OTHER` - Outros

### Income (Receita)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | Long | Identificador único |
| description | String | Descrição da receita |
| amount | Double | Valor |
| category | IncomeCategory | Categoria |
| type | IncomeType | Tipo (recorrente/único) |
| receiveDay | Int | Dia de recebimento (1-31) |
| date | Long | Data para receita única |
| isReceived | Boolean | Se foi recebida no mês |
| isActive | Boolean | Se está ativa |

**IncomeCategory (Enum):**
- `SALARY` - Salário
- `FREELANCE` - Freelance
- `INVESTMENT` - Investimento
- `BONUS` - Bônus
- `GIFT` - Presente
- `RENTAL` - Aluguel
- `SALE` - Venda
- `REFUND` - Reembolso
- `OTHER` - Outros

**IncomeType (Enum):**
- `RECURRENT` - Recorrente (ex: salário)
- `ONE_TIME` - Único (ex: valor pontual)

## 🧭 Navegação

O app utiliza **Navigation Compose** com bottom navigation:

| Rota | Tela | Ícone |
|------|------|-------|
| `home` | Dashboard | Home |
| `credit_cards` | Lista de Cartões | CreditCard |
| `banks` | Lista de Bancos | AccountBalance |
| `compromises` | Contas Fixas | Receipt |
| `incomes` | Receitas | AttachMoney |

### Rotas Secundárias
- `credit_card_detail/{cardId}` - Detalhes do cartão
- `add_edit_credit_card?cardId={cardId}` - Adicionar/Editar cartão
- `add_credit_card_item/{cardId}` - Adicionar item na fatura
- `edit_credit_card_item/{itemId}` - Editar item da fatura
- `import_statement/{cardId}` - Importar extrato CSV/OFX
- `add_edit_bank?bankId={bankId}` - Adicionar/Editar banco
- `add_edit_compromise?compromiseId={id}` - Adicionar/Editar conta fixa
- `add_edit_income?incomeId={id}` - Adicionar/Editar receita

## 📦 Dependências

### Principais
```toml
# Room Database
androidx-room-runtime = "2.6.1"
androidx-room-ktx = "2.6.1"
androidx-room-compiler = "2.6.1"  # KSP

# Navigation
androidx-navigation-compose = "2.7.7"

# ViewModel
androidx-lifecycle-viewmodel-compose = "2.6.1"

# Material Icons Extended
androidx-compose-material-icons-extended
```

### Compose BOM
```toml
composeBom = "2024.09.00"
```

## 🎨 Telas

### 1. Home (Dashboard)
- Resumo financeiro com cards coloridos
- Saldo total em bancos
- Total de faturas de cartões
- Total de contas fixas
- Estatísticas rápidas
- Próximas contas a vencer

### 2. Cartões de Crédito
- Lista de cartões com fatura atual
- Barra de uso do limite
- Adicionar/editar cartões
- Gerenciar itens da fatura
- Editar itens existentes
- Categorias de compras
- Suporte a parcelamento
- **Importação de Extratos** (CSV/OFX)

### 3. Bancos
- Lista de contas com saldo
- Total consolidado
- Tipos de conta (Corrente, Poupança, etc.)
- Cores personalizáveis

### 4. Contas Fixas
- Lista com status de pagamento
- Checkbox para marcar como pago
- Ícones por categoria
- Barra de progresso (pagas/total)

### 5. Receitas
- Lista de receitas recorrentes e pontuais
- Total mensal de receitas
- Categorias (Salário, Freelance, etc.)
- Checkbox para marcar como recebido
- Suporte a receitas recorrentes e únicas

## 🚀 Como Executar

1. Clone o repositório
2. Abra no Android Studio
3. Sincronize o Gradle
4. Execute no emulador ou dispositivo (API 33+)

## 📋 Requisitos
- Android SDK 33+ (minSdk)
- Android SDK 36 (targetSdk)
- Kotlin 2.0.21
- Java 11

## 📄 Licença

Este projeto foi desenvolvido para fins de organização financeira pessoal.

