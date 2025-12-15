# Diagramas de Arquitetura

## 🏗️ Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │   Screens    │  │  Components  │  │      Navigation          │  │
│  │              │  │              │  │                          │  │
│  │ • HomeScreen │  │ • SummaryCard│  │ • Screen (sealed class)  │  │
│  │ • CardList   │  │ • DeleteDlg  │  │ • NavGraph               │  │
│  │ • BankList   │  │ • formatCurr │  │ • bottomNavItems         │  │
│  │ • Compromise │  │              │  │                          │  │
│  └──────┬───────┘  └──────────────┘  └──────────────────────────┘  │
│         │                                                           │
│         │ observes StateFlow                                        │
│         ▼                                                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                       VIEWMODELS                             │   │
│  │                                                              │   │
│  │  CreditCardViewModel   BankViewModel   CompromiseViewModel   │   │
│  │  ├─ allCreditCards     ├─ allBanks     ├─ allCompromises    │   │
│  │  ├─ allItems           ├─ totalBalance ├─ totalMonthly      │   │
│  │  ├─ selectedCard       ├─ selectedBank ├─ selectedComp      │   │
│  │  └─ cardTotal          └───────────────└────────────────    │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
└─────────────────────────────┼───────────────────────────────────────┘
                              │
                              │ calls suspend functions
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                            DOMAIN                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                      REPOSITORIES                            │   │
│  │                                                              │   │
│  │  CreditCardRepository   BankRepository   CompromiseRepository│   │
│  │  ├─ getAllCards()       ├─ getAllBanks() ├─ getActiveComp() │   │
│  │  ├─ getCardById()       ├─ getBankById() ├─ getById()       │   │
│  │  ├─ getItemsByCard()    ├─ getTotal()    ├─ getTotal()      │   │
│  │  ├─ insertCard()        ├─ insertBank()  ├─ insertComp()    │   │
│  │  └─ deleteCard()        └─ deleteBank()  └─ togglePaid()    │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
└─────────────────────────────┼───────────────────────────────────────┘
                              │
                              │ wraps DAO calls
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                             DATA                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                         DAOs                                 │   │
│  │                                                              │   │
│  │  CreditCardDao     CreditCardItemDao   BankDao   CompDao    │   │
│  │  └─ @Query         └─ @Query           └─ @Query └─ @Query  │   │
│  │  └─ @Insert        └─ @Insert          └─ @Insert└─ @Insert │   │
│  │  └─ @Update        └─ @Update          └─ @Update└─ @Update │   │
│  │  └─ @Delete        └─ @Delete          └─ @Delete└─ @Delete │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                              │                                      │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │                      AppDatabase                             │   │
│  │                                                              │   │
│  │  @Database(entities = [CreditCard, CreditCardItem,          │   │
│  │                        Bank, FinancialCompromise])          │   │
│  │                                                              │   │
│  │  Singleton pattern com companion object                      │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                              │                                      │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │                       ENTITIES                               │   │
│  │                                                              │   │
│  │  @Entity CreditCard      @Entity Bank                        │   │
│  │  ├─ id: Long (PK)        ├─ id: Long (PK)                   │   │
│  │  ├─ name: String         ├─ name: String                    │   │
│  │  ├─ cardLimit: Double    ├─ balance: Double                 │   │
│  │  ├─ dueDay: Int          ├─ accountType: AccountType        │   │
│  │  ├─ closingDay: Int      └─ color: Long                     │   │
│  │  └─ color: Long                                              │   │
│  │                                                              │   │
│  │  @Entity CreditCardItem  @Entity FinancialCompromise        │   │
│  │  ├─ id: Long (PK)        ├─ id: Long (PK)                   │   │
│  │  ├─ cardId: Long (FK)    ├─ name: String                    │   │
│  │  ├─ description: String  ├─ amount: Double                  │   │
│  │  ├─ amount: Double       ├─ dueDay: Int                     │   │
│  │  ├─ purchaseDate: Long   ├─ category: CompromiseCategory    │   │
│  │  ├─ installments: Int    ├─ isPaid: Boolean                 │   │
│  │  ├─ currentInstall: Int  └─ isActive: Boolean               │   │
│  │  └─ category: String                                         │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## 🧭 Fluxo de Navegação

```
                        ┌─────────────┐
                        │   Splash    │
                        │  (implicit) │
                        └──────┬──────┘
                               │
                               ▼
        ┌──────────────────────────────────────────────────────┐
        │                  BOTTOM NAVIGATION                    │
        │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────────┐ │
        │  │  Home  │  │ Cards  │  │ Banks  │  │ Compromises│ │
        │  └───┬────┘  └───┬────┘  └───┬────┘  └─────┬──────┘ │
        └──────┼───────────┼───────────┼─────────────┼────────┘
               │           │           │             │
               ▼           │           │             │
        ┌──────────┐       │           │             │
        │HomeScreen│       │           │             │
        │          │       │           │             │
        │ Summary  │       │           │             │
        │ Stats    │       │           │             │
        │ Upcoming │       │           │             │
        └──────────┘       │           │             │
                           ▼           │             │
                    ┌─────────────┐    │             │
                    │  CardList   │    │             │
                    │   Screen    │    │             │
                    └──────┬──────┘    │             │
                           │           │             │
              ┌────────────┼───────────┤             │
              │            │           │             │
              ▼            ▼           ▼             ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
        │  Card    │ │ AddEdit  │ │ AddEdit  │ │ AddEdit  │
        │  Detail  │ │  Card    │ │  Bank    │ │Compromise│
        └────┬─────┘ └──────────┘ └──────────┘ └──────────┘
             │
             ▼
        ┌──────────┐
        │ AddCard  │
        │   Item   │
        └──────────┘
```

## 🔄 Fluxo de Dados (Exemplo: Adicionar Cartão)

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    UI       │     │  ViewModel  │     │ Repository  │     │    DAO      │
│             │     │             │     │             │     │             │
│  AddEdit    │     │ CreditCard  │     │ CreditCard  │     │ CreditCard  │
│  CardScreen │     │  ViewModel  │     │  Repository │     │    Dao      │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │ onClick           │                   │                   │
       │ (CreditCard)      │                   │                   │
       │──────────────────>│                   │                   │
       │                   │                   │                   │
       │                   │ insertCreditCard  │                   │
       │                   │ (CreditCard)      │                   │
       │                   │──────────────────>│                   │
       │                   │                   │                   │
       │                   │                   │ insertCreditCard  │
       │                   │                   │ (CreditCard)      │
       │                   │                   │──────────────────>│
       │                   │                   │                   │
       │                   │                   │                   │──┐
       │                   │                   │                   │  │ INSERT INTO
       │                   │                   │                   │  │ credit_cards
       │                   │                   │                   │<─┘
       │                   │                   │                   │
       │                   │                   │    Long (id)      │
       │                   │                   │<──────────────────│
       │                   │                   │                   │
       │                   │    Long (id)      │                   │
       │                   │<──────────────────│                   │
       │                   │                   │                   │
       │ navigateBack()    │                   │                   │
       │<──────────────────│                   │                   │
       │                   │                   │                   │

       ═══════════════════════════════════════════════════════════════
       
       │                   │                   │                   │
       │                   │  Flow emits       │                   │
       │                   │  updated list     │                   │
       │                   │<══════════════════│<══════════════════│
       │                   │                   │                   │
       │  StateFlow        │                   │                   │
       │  recomposes UI    │                   │                   │
       │<══════════════════│                   │                   │
       │                   │                   │                   │
```

## 📱 Hierarquia de Composables

```
MainActivity
└── FinanceApp
    └── Scaffold
        ├── NavigationBar (Bottom)
        │   ├── NavigationBarItem (Home)
        │   ├── NavigationBarItem (Cards)
        │   ├── NavigationBarItem (Banks)
        │   └── NavigationBarItem (Compromises)
        │
        └── FinanceNavHost
            ├── HomeScreen
            │   └── Column
            │       ├── SummaryCard (Saldo)
            │       ├── SummaryCard (Faturas)
            │       ├── SummaryCard (Contas)
            │       ├── SummaryCard (Total)
            │       └── Card (Próximas)
            │
            ├── CreditCardListScreen
            │   └── LazyColumn
            │       └── CreditCardItem (repeat)
            │
            ├── CreditCardDetailScreen
            │   └── Column
            │       ├── Card (Summary)
            │       └── LazyColumn
            │           └── CreditCardItemRow
            │
            ├── BankListScreen
            │   └── Column
            │       ├── Card (Total)
            │       └── LazyColumn
            │           └── BankItem
            │
            └── CompromiseListScreen
                └── Column
                    ├── Card (Summary)
                    └── LazyColumn
                        └── CompromiseItem
```

