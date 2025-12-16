# Financial App Data Model Documentation

## Overview
This data model provides a simplified, transaction-centric approach to personal finance management, consolidating accounts, credit cards, and savings tracking into a unified structure.

## Core Entities

### 1. Balance
Represents either the main balance of an account or a savings pool (caixinha) within that account. This unified approach allows both account balances and savings goals to be modeled using the same entity.

**Attributes:**
- `id`: Unique identifier
- `name`: Display name (e.g., "Main", "Emergency Fund", "Vacation")
- `account_id`: Foreign key linking to the Account this balance belongs to
- `current_balance`: The actual balance amount
- `balance_type`: Either "account" (main balance) or "pool" (savings goal/caixinha)
- `goal_amount`: Optional target amount (typically used for pools)
- `currency`: If supporting multiple currencies (e.g., "BRL", "USD")
- `is_active`: Boolean to mark inactive balances
- `color`: Optional color code for UI display
- `icon`: Optional icon identifier for UI display

**Purpose:** Tracks the main balance of an account and any savings pools within it. All balances with the same `account_id` belong to the same account. Pools are child balances that earmark portions of the account for specific goals.

**Available Balance Calculation:**
For an account with pools, the available balance is calculated as:
```
Available = Main Balance - Sum(All Pool Balances for this account_id)
```

**Example:**
```
Account: Nubank Checking (account_id: nubank_checking_1)
  Balance: Main                 : R$ 5,000 (balance_type: account)
  └─ Pool: Emergency Fund       : R$ 1,000 (balance_type: pool)
  └─ Pool: Vacation             : R$   500 (balance_type: pool)
  └─ Pool: New Phone            : R$ 1,000 (balance_type: pool)
Available Balance               : R$ 2,500
```

---

### 2. Account
Represents a financial account at a bank (e.g., "Nubank Checking", "Itaú Savings").

**Attributes:**
- `id`: Unique identifier
- `name`: User-defined account name (e.g., "My Main Account", "Emergency Savings")
- `bank_name`: Name of the financial institution from predefined list (e.g., "Nubank", "Itaú", "Bradesco")
- `account_number`: Optional, for reference
- `account_type`: e.g., "checking", "savings", "investment"
- `logo_url`: Optional URL to the bank's logo for UI display
- `color`: Optional color code for UI theming

**Purpose:** Represents a single account at a financial institution. Multiple balances (including pools) can belong to the same account.

---

### 3. Transaction
The central entity that records all financial activity—both actual and planned.

**Attributes:**
- `amount`: Transaction amount
- `date`: Transaction date
- `end-date`: For recurring transactions, when the recurrence ends
- `frequency`: How often the transaction repeats (monthly, yearly, etc.)
- `balance-id`: Links to the Balance affected by this transaction
- `type`: Either "income" or "expense"
- `status`: Transaction status - "expected" (planned/future), "completed" (actual/done), or "cancelled"
- `bill_id`: Optional link to Bill for credit card transactions (groups transactions by billing cycle)
- `parent_transaction_id`: Optional link to parent transaction for installment purchases
- `is_installment_parent`: Boolean indicating if this is a parent transaction representing an installment purchase (default: false)
- `installment_number`: For installment children, which installment this is (1, 2, 3, etc.)
- `total_installments`: Total number of installments (e.g., 12 for 12x payment)
- `installment_amount`: Amount per installment
- `category`: High-level classification (e.g., "Food", "Transportation")
- `subcategory`: More specific classification (e.g., "Groceries", "Gas")
- `description`: Optional description of the transaction

**Purpose:** Records all financial movements, whether immediate or recurring, for both accounts and credit cards. The `status` field enables tracking of expected transactions (like upcoming salary) versus completed transactions (actual deposits/expenses). Supports installment purchases through parent-child transaction relationships, where a parent transaction represents the full purchase and child transactions represent each monthly installment.

---

### 4. Credit Card
Represents a credit card account.

**Attributes:**
- `id`: Unique identifier
- `name`: User-defined name for the card
- `limit`: Credit limit
- `fechamento`: Closing date (day of month when billing cycle ends)
- `vencimento`: Due date (day of month when payment is due)
- `auto_generate_bills`: Boolean to automatically create bills on closing date

**Purpose:** Tracks credit card details and enables linking transactions to specific cards. Works together with Bill entity to manage monthly billing cycles.

---

### 5. Bill
Represents a monthly credit card statement/bill. Bills are automatically generated on the card's closing date (fechamento) and group all transactions from that billing cycle.

**Attributes:**
- `id`: Unique identifier
- `credit_card_id`: Foreign key linking to the Credit Card
- `year`: Year of the bill (e.g., 2024)
- `month`: Month of the bill (1-12)
- `closing_date`: Date when the billing cycle closed
- `due_date`: Date when payment is due
- `total_amount`: Total amount owed (sum of all transactions in cycle)
- `paid_amount`: Amount paid so far (0.00 if unpaid)
- `status`: Bill status - "open" (unpaid), "paid" (fully paid), "overdue" (past due date), "partial" (partially paid)
- `payment_transaction_id`: Optional link to the payment transaction when paid
- `created_at`: When the bill was generated

**Purpose:** Groups credit card transactions by billing cycle, tracks payment status, and provides clear monthly statements that match real credit card bills. Makes it easy to see what needs to be paid and when.

**Billing Cycle Example:**
```
Bill for January 2024:
  Cycle: Dec 11, 2023 - Jan 10, 2024
  Closing: Jan 10, 2024
  Due: Jan 15, 2024
  Transactions in this cycle → Linked to this bill
```

---

## Key Design Principles

### Transaction-Centric Architecture
All financial activity flows through the Transaction entity, whether it's:
- Account deposits/withdrawals
- Credit card purchases
- Recurring bills
- Planned future expenses

### Simplicity Over Specialization
Instead of separate entities for different transaction types (e.g., "CreditCardItem", "Compromise"), this model uses a single Transaction entity with attributes to differentiate:
- Type (income/expense)
- Recurrence (via frequency and end-date)
- Source (via balance-id or bill-id)
- Installments (via parent-child relationship)

Similarly, savings pools (caixinhas) are modeled as Balance entities with `balance_type: "pool"` rather than as a separate entity. All balances belonging to the same account share the same `account_id`, creating a natural grouping without needing explicit parent-child relationships.

### Handling Transfers
Transfers between balances are modeled as two related transactions:
- One expense transaction from source balance
- One income transaction to destination balance

**Consideration:** May benefit from a `transfer_pair_id` to link these transactions for reporting purposes.

### Transaction Status and Balance Impact
The `status` field determines how transactions affect balance calculations:

**Completed transactions** (`status: "completed"`):
- Immediately affect the current balance
- Included in balance calculations
- Represent actual money movements

**Expected transactions** (`status: "expected"`):
- Do NOT affect current balance
- Used for cash flow forecasting
- Show what balance *will be* in the future
- Useful for planning and avoiding overdrafts

**Cancelled transactions** (`status: "cancelled"`):
- Do NOT affect current balance
- Kept for historical record
- Can be filtered out of reports

**Example:**
```
Current Balance: R$ 3,000
Expected Income (salary, Jan 25): R$ 5,000 (status: expected)
Expected Expense (rent, Jan 30): R$ 1,500 (status: expected)

Current Balance: R$ 3,000
Projected Balance (Jan 31): R$ 6,500
```

### Credit Card Billing Workflow
The Bill entity provides a structured approach to managing credit card payments that mirrors real-world credit card statements.

**Automatic Bill Generation:**
On the card's closing date (fechamento), the system automatically:
1. Creates a new Bill for the month
2. Calculates total_amount from all transactions in the billing cycle
3. Sets the due_date based on the card's vencimento
4. Sets status to "open"
5. Links all transactions from that cycle to the bill via bill_id

**Transaction Assignment:**
When a credit card transaction is created, it's automatically assigned to the current open bill for that card. This groups expenses by billing cycle.

**Payment Process:**
When paying a credit card bill:
1. User selects which account balance to pay from
2. Creates a payment transaction:
   ```
   Transaction:
     amount: 500
     date: 2024-01-15
     type: expense
     balance-id: nubank_checking_main
     category: Credit Card Payment
     status: completed
   ```
3. Updates the Bill:
   ```
   Bill:
     paid_amount: 500
     status: paid
     payment_transaction_id: <transaction_id>
   ```

**Bill Status States:**
- **open**: Bill created, not paid yet
- **paid**: Full amount paid (paid_amount = total_amount)
- **partial**: Partially paid (0 < paid_amount < total_amount)
- **overdue**: Past due date and not fully paid

**Benefits:**
- Clear separation of monthly billing cycles
- Easy to match against actual credit card statements
- Simple overdue bill detection
- Historical record of all bills and payments
- Supports partial payments

### Handling Installment Purchases (Parcelas)

Credit card purchases with installments are common in Brazil. The model uses a parent-child transaction structure to handle them:

**Structure:**
- **Parent Transaction**: Represents the full purchase (e.g., R$1,200 total)
    - `is_installment_parent: true`
    - NOT included in any bill
    - Stores metadata: total amount, installment count, etc.

- **Child Transactions**: Represent each monthly installment (e.g., 12x R$100)
    - `is_installment_parent: false`
    - Each assigned to its respective monthly bill
    - Linked to parent via `parent_transaction_id`

**Workflow:**
1. User makes installment purchase (e.g., 12x R$100)
2. System creates 1 parent transaction + 12 child transactions
3. First child transaction assigned to current bill (status: "completed")
4. Remaining 11 child transactions assigned to future bills (status: "expected")
5. Each month, the installment for that month is already on the bill

**Querying Expenses:**

⚠️ **CRITICAL**: When calculating total expenses, always exclude parent transactions:

```kotlin
// ✅ CORRECT: Total expenses
fun getTotalExpenses(startDate: Date, endDate: Date): Double {
    return getTransactions()
        .filter { it.type == "expense" }
        .filter { it.date >= startDate && it.date <= endDate }
        .filter { it.status == "completed" }
        .filter { it.is_installment_parent == false }  // Exclude parents!
        .sumOf { it.amount }
}

// ❌ WRONG: Will double-count installments
fun getTotalExpensesWrong(startDate: Date, endDate: Date): Double {
    return getTransactions()
        .filter { it.type == "expense" }
        .sumOf { it.amount }  // Counts parent (R$1,200) AND all children (12x R$100)!
}
```

**Common Queries:**

```kotlin
// Monthly expenses (credit card + rent + everything)
fun getMonthlyExpenses(year: Int, month: Int): Double {
    return getTransactions()
        .filter { it.type == "expense" }
        .filter { it.status == "completed" }
        .filter { it.date.year == year && it.date.month == month }
        .filter { it.is_installment_parent == false }
        .sumOf { it.amount }
}

// Credit card bill total
fun getBillTotal(billId: String): Double {
    return getTransactions()
        .filter { it.bill_id == billId }
        .filter { it.is_installment_parent == false }
        .sumOf { it.amount }
}

// Active installment purchases (use parent transactions)
fun getActiveInstallments(): List<InstallmentInfo> {
    return getTransactions()
        .filter { it.is_installment_parent == true }
        .map { parent ->
            val children = getTransactions()
                .filter { it.parent_transaction_id == parent.id }
            
            InstallmentInfo(
                description = parent.description,
                totalAmount = parent.amount,
                paidCount = children.count { it.status == "completed" },
                remainingCount = children.count { it.status == "expected" }
            )
        }
}

// Expenses by category (for budgeting)
fun getExpensesByCategory(startDate: Date, endDate: Date): Map<String, Double> {
    return getTransactions()
        .filter { it.type == "expense" }
        .filter { it.status == "completed" }
        .filter { it.date >= startDate && it.date <= endDate }
        .filter { it.is_installment_parent == false }  // Only actual charges
        .filter { it.category != "Credit Card Payment" }
        .groupBy { it.category }
        .mapValues { (_, txns) -> txns.sumOf { it.amount } }
}
```

**The Golden Rule:**
For any expense calculation, always filter `is_installment_parent == false`. Only use parent transactions when displaying installment details or managing the entire installment plan.

---

## Common Use Cases

### Recording an Account Deposit
```
Transaction:
  amount: 5000
  date: 2024-01-15
  type: income
  balance-id: main_balance_123
  category: Salary
```

### Recording a Credit Card Purchase
```
Transaction:
  amount: 45.50
  date: 2024-01-16
  type: expense
  bill_id: bill_202401_visa  // Links to January 2024 bill
  status: completed
  category: Food
  subcategory: Groceries
  is_installment_parent: false
```

### Setting Up Recurring Bill
```
Transaction:
  amount: 150
  date: 2024-01-01
  frequency: monthly
  end-date: 2024-12-31
  type: expense
  balance-id: nubank_main
  category: Utilities
  subcategory: Internet
```

### Transferring Between Accounts
```
Transaction 1 (Source):
  amount: 1000
  type: expense
  balance-id: nubank_main_balance
  category: Transfer

Transaction 2 (Destination):
  amount: 1000
  type: income
  balance-id: itau_main_balance
  category: Transfer
```

### Creating a Savings Pool (Caixinha)
```
Account:
  id: nubank_checking_1
  name: Nubank Checking
  bank_name: Nubank
  account_type: checking

Balance (Main):
  id: nubank_main
  name: Main
  account_id: nubank_checking_1
  current_balance: 5000.00
  balance_type: account

Balance (Pool):
  id: emergency_pool
  name: Emergency Fund
  account_id: nubank_checking_1
  current_balance: 1000.00
  balance_type: pool
  goal_amount: 2000.00
```

### Moving Money to a Pool
```
Transaction 1 (From Main Balance):
  amount: 500
  type: expense
  balance-id: nubank_main
  status: completed
  category: Savings

Transaction 2 (To Pool):
  amount: 500
  type: income
  balance-id: emergency_pool
  status: completed
  category: Savings
```

### Expected Income (Salary)
```
Transaction:
  amount: 5000
  date: 2024-01-25
  type: income
  balance-id: nubank_main
  status: expected
  category: Salary
  frequency: monthly
```

When the salary arrives, update the status:
```
Transaction (updated):
  amount: 5000  // or actual amount if different
  date: 2024-01-25
  type: income
  balance-id: nubank_main
  status: completed
  category: Salary
  frequency: monthly
```

### Expected Expense (Upcoming Bill)
```
Transaction:
  amount: 150
  date: 2024-01-30
  type: expense
  balance-id: nubank_main
  status: expected
  category: Utilities
  subcategory: Internet
```

### Working with Credit Card Bills

**Creating a Bill (Automatic):**
```
Bill:
  id: bill_202401_nubank
  credit_card_id: nubank_card
  year: 2024
  month: 1
  closing_date: 2024-01-10
  due_date: 2024-01-15
  total_amount: 500.00  // Sum of all transactions in cycle
  paid_amount: 0.00
  status: open
  payment_transaction_id: null
```

**Paying a Bill:**
```
Step 1 - Create payment transaction:
Transaction:
  amount: 500
  date: 2024-01-15
  type: expense
  balance-id: nubank_checking_main
  status: completed
  category: Credit Card Payment

Step 2 - Update bill:
Bill (updated):
  paid_amount: 500.00
  status: paid
  payment_transaction_id: <transaction_id>
```

**Partial Payment:**
```
Transaction:
  amount: 250  // Pay only half
  date: 2024-01-15
  type: expense
  balance-id: nubank_checking_main
  status: completed
  category: Credit Card Payment

Bill (updated):
  paid_amount: 250.00
  status: partial  // Not fully paid yet
```

### Credit Card Installment Purchase (Parcelas)

**Making an installment purchase (12x R$100 = R$1,200):**

```
Step 1 - Create parent transaction (represents the full purchase):
Transaction (Parent):
  id: parent_iphone_001
  amount: 1200.00  // Total purchase amount
  date: 2024-01-15
  type: expense
  bill_id: null  // Parent doesn't go on a bill
  status: completed
  category: Electronics
  subcategory: Smartphone
  description: "iPhone 15 Pro"
  is_installment_parent: true
  total_installments: 12
  installment_amount: 100.00
  parent_transaction_id: null

Step 2 - Create 12 child transactions (one per month):
Transaction (Child 1):
  id: inst_001
  amount: 100.00
  date: 2024-01-15
  type: expense
  bill_id: bill_202401_nubank
  status: completed
  category: Electronics
  description: "iPhone 15 Pro"
  is_installment_parent: false
  parent_transaction_id: parent_iphone_001
  installment_number: 1
  total_installments: 12

Transaction (Child 2):
  id: inst_002
  amount: 100.00
  date: 2024-02-15
  type: expense
  bill_id: bill_202402_nubank
  status: expected  // Future installment
  category: Electronics
  description: "iPhone 15 Pro"
  is_installment_parent: false
  parent_transaction_id: parent_iphone_001
  installment_number: 2
  total_installments: 12

... (repeat for installments 3-12)
```

**Key points:**
- Parent transaction: Stores total purchase info, NOT included in bill totals
- Child transactions: Each month's charge, included in respective bill
- First installment status: "completed" (already charged)
- Future installments status: "expected" (will be charged)
- All children link to parent via `parent_transaction_id`

---
