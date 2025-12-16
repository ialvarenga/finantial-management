package com.example.organizadordefinancas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The central entity that records all financial activity—both actual and planned.
 * This replaces the previous CreditCardItem, FinancialCompromise, and Income entities
 * with a unified transaction model.
 *
 * Key Design Principles:
 * - All financial activity flows through Transaction
 * - Type (income/expense) and status determine behavior
 * - Installments use parent-child relationships
 * - Bill linkage for credit card transactions
 *
 * CRITICAL: When calculating expense totals, always exclude parent transactions
 * (is_installment_parent = false) to avoid double-counting.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Balance::class,
            parentColumns = ["id"],
            childColumns = ["balance_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["id"],
            childColumns = ["bill_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["parent_transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["balance_id"]),
        Index(value = ["bill_id"]),
        Index(value = ["parent_transaction_id"]),
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["category"]),
        Index(value = ["is_installment_parent"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Transaction amount (always positive, type determines direction)
     */
    val amount: Double,

    /**
     * Transaction date (timestamp in milliseconds)
     */
    val date: Long,

    /**
     * For recurring transactions, when the recurrence ends (timestamp in milliseconds)
     */
    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,

    /**
     * How often the transaction repeats: "once", "weekly", "biweekly", "monthly", "quarterly", "semiannual", "annual"
     */
    val frequency: String = TransactionFrequency.ONCE,

    /**
     * Links to the Balance affected by this transaction (null for credit card transactions)
     */
    @ColumnInfo(name = "balance_id")
    val balanceId: Long? = null,

    /**
     * Either "income" or "expense"
     */
    val type: String,

    /**
     * Transaction status: "expected" (planned/future), "completed" (actual/done), or "cancelled"
     */
    val status: String = TransactionStatus.COMPLETED,

    /**
     * Optional link to Bill for credit card transactions (groups transactions by billing cycle)
     */
    @ColumnInfo(name = "bill_id")
    val billId: Long? = null,

    /**
     * Optional link to parent transaction for installment purchases
     */
    @ColumnInfo(name = "parent_transaction_id")
    val parentTransactionId: Long? = null,

    /**
     * Boolean indicating if this is a parent transaction representing an installment purchase
     * CRITICAL: Exclude these from expense totals to avoid double-counting
     */
    @ColumnInfo(name = "is_installment_parent")
    val isInstallmentParent: Boolean = false,

    /**
     * For installment children, which installment this is (1, 2, 3, etc.)
     */
    @ColumnInfo(name = "installment_number")
    val installmentNumber: Int? = null,

    /**
     * Total number of installments (e.g., 12 for 12x payment)
     */
    @ColumnInfo(name = "total_installments")
    val totalInstallments: Int? = null,

    /**
     * Amount per installment (may differ from amount for parent transactions)
     */
    @ColumnInfo(name = "installment_amount")
    val installmentAmount: Double? = null,

    /**
     * High-level classification (e.g., "Food", "Transportation")
     */
    val category: String = "Outros",

    /**
     * More specific classification (e.g., "Groceries", "Gas")
     */
    val subcategory: String? = null,

    /**
     * Optional description of the transaction
     */
    val description: String? = null,

    /**
     * Optional link to pair transfers together
     */
    @ColumnInfo(name = "transfer_pair_id")
    val transferPairId: Long? = null,

    /**
     * Creation timestamp
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Last update timestamp
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this is an income transaction
     */
    fun isIncome(): Boolean = type == TransactionType.INCOME

    /**
     * Checks if this is an expense transaction
     */
    fun isExpense(): Boolean = type == TransactionType.EXPENSE

    /**
     * Checks if this is a completed transaction
     */
    fun isCompleted(): Boolean = status == TransactionStatus.COMPLETED

    /**
     * Checks if this is an expected (future/planned) transaction
     */
    fun isExpected(): Boolean = status == TransactionStatus.EXPECTED

    /**
     * Checks if this is an installment child transaction
     */
    fun isInstallmentChild(): Boolean = parentTransactionId != null && !isInstallmentParent

    /**
     * Checks if this is a recurring transaction
     */
    fun isRecurring(): Boolean = frequency != TransactionFrequency.ONCE

    /**
     * Checks if this is a credit card transaction (has bill_id)
     */
    fun isCreditCardTransaction(): Boolean = billId != null

    /**
     * Gets the installment display text (e.g., "3/12")
     */
    fun getInstallmentDisplayText(): String? {
        return if (installmentNumber != null && totalInstallments != null) {
            "$installmentNumber/$totalInstallments"
        } else null
    }
}

/**
 * Supported transaction types
 */
object TransactionType {
    const val INCOME = "income"
    const val EXPENSE = "expense"
}

/**
 * Supported transaction statuses
 */
object TransactionStatus {
    const val EXPECTED = "expected"     // Planned/future transaction
    const val COMPLETED = "completed"   // Actual/done transaction
    const val CANCELLED = "cancelled"   // Cancelled transaction (kept for history)
}

/**
 * Supported transaction frequencies
 */
object TransactionFrequency {
    const val ONCE = "once"
    const val WEEKLY = "weekly"
    const val BIWEEKLY = "biweekly"
    const val MONTHLY = "monthly"
    const val QUARTERLY = "quarterly"
    const val SEMIANNUAL = "semiannual"
    const val ANNUAL = "annual"
}

/**
 * Common transaction categories
 */
object TransactionCategory {
    // Expense categories
    const val FOOD = "Alimentação"
    const val TRANSPORT = "Transporte"
    const val HOUSING = "Moradia"
    const val UTILITIES = "Serviços"
    const val HEALTH = "Saúde"
    const val EDUCATION = "Educação"
    const val ENTERTAINMENT = "Lazer"
    const val SHOPPING = "Compras"
    const val PERSONAL = "Pessoal"
    const val CREDIT_CARD_PAYMENT = "Pagamento Cartão"
    const val TRANSFER = "Transferência"
    const val SAVINGS = "Poupança"
    const val OTHER = "Outros"

    // Income categories
    const val SALARY = "Salário"
    const val FREELANCE = "Freelance"
    const val INVESTMENT_INCOME = "Rendimentos"
    const val BONUS = "Bônus"
    const val GIFT = "Presente"
    const val RENTAL = "Aluguel Recebido"
    const val SALE = "Venda"
    const val REFUND = "Reembolso"

    fun getExpenseCategories(): List<String> = listOf(
        FOOD, TRANSPORT, HOUSING, UTILITIES, HEALTH, EDUCATION,
        ENTERTAINMENT, SHOPPING, PERSONAL, CREDIT_CARD_PAYMENT,
        TRANSFER, SAVINGS, OTHER
    )

    fun getIncomeCategories(): List<String> = listOf(
        SALARY, FREELANCE, INVESTMENT_INCOME, BONUS, GIFT,
        RENTAL, SALE, REFUND, OTHER
    )
}

