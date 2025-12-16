package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.BalanceDao
import com.example.organizadordefinancas.data.dao.BillDao
import com.example.organizadordefinancas.data.dao.CategoryTotal
import com.example.organizadordefinancas.data.dao.TransactionDao
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository for managing Transaction entities.
 * This is the central repository for all financial activity.
 *
 * CRITICAL: When calculating expense totals, always exclude parent transactions
 * (is_installment_parent = false) to avoid double-counting.
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val balanceDao: BalanceDao,
    private val billDao: BillDao
) {
    // ==================== Read Operations ====================

    /**
     * Get all transactions
     */
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    /**
     * Get transactions excluding installment parents (for expense calculations)
     */
    fun getTransactionsExcludingParents(): Flow<List<Transaction>> =
        transactionDao.getTransactionsExcludingParents()

    /**
     * Get a specific transaction by ID
     */
    fun getTransactionById(id: Long): Flow<Transaction?> = transactionDao.getTransactionById(id)

    /**
     * Get a specific transaction by ID (synchronous)
     */
    suspend fun getTransactionByIdSync(id: Long): Transaction? =
        transactionDao.getTransactionByIdSync(id)

    // ==================== Filtered Queries ====================

    /**
     * Get transactions by balance ID
     */
    fun getTransactionsByBalanceId(balanceId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByBalanceId(balanceId)

    /**
     * Get transactions by bill ID
     */
    fun getTransactionsByBillId(billId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByBillId(billId)

    /**
     * Get transactions by bill ID excluding installment parents
     */
    fun getTransactionsByBillIdExcludingParents(billId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByBillIdExcludingParents(billId)

    /**
     * Get transactions by status
     */
    fun getTransactionsByStatus(status: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByStatus(status)

    /**
     * Get transactions by type (income/expense) - excludes installment parents
     */
    fun getTransactionsByType(type: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    /**
     * Get transactions by category - excludes installment parents
     */
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category)

    /**
     * Get transactions in a date range - excludes installment parents
     */
    fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInDateRange(startDate, endDate)

    /**
     * Get transactions with multiple filters
     */
    fun getFilteredTransactions(
        balanceId: Long? = null,
        billId: Long? = null,
        type: String? = null,
        status: String? = null,
        category: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<Transaction>> = transactionDao.getFilteredTransactions(
        balanceId, billId, type, status, category, startDate, endDate
    )

    /**
     * Get recent transactions (for dashboard)
     */
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)

    /**
     * Get expected (future/planned) transactions
     */
    fun getExpectedTransactions(): Flow<List<Transaction>> =
        transactionDao.getExpectedTransactions()

    // ==================== Expense/Income Totals ====================
    // CRITICAL: These methods exclude installment parents to avoid double-counting

    /**
     * Get total expenses for a date range
     */
    fun getTotalExpensesInDateRange(startDate: Long, endDate: Long): Flow<Double> =
        transactionDao.getTotalExpensesInDateRange(startDate, endDate)

    /**
     * Get total expenses for a date range (synchronous)
     */
    suspend fun getTotalExpensesInDateRangeSync(startDate: Long, endDate: Long): Double =
        transactionDao.getTotalExpensesInDateRangeSync(startDate, endDate)

    /**
     * Get total income for a date range
     */
    fun getTotalIncomeInDateRange(startDate: Long, endDate: Long): Flow<Double> =
        transactionDao.getTotalIncomeInDateRange(startDate, endDate)

    /**
     * Get total income for a date range (synchronous)
     */
    suspend fun getTotalIncomeInDateRangeSync(startDate: Long, endDate: Long): Double =
        transactionDao.getTotalIncomeInDateRangeSync(startDate, endDate)

    /**
     * Get expenses grouped by category
     */
    fun getExpensesByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>> =
        transactionDao.getExpensesByCategory(startDate, endDate)

    /**
     * Get income grouped by category
     */
    fun getIncomeByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>> =
        transactionDao.getIncomeByCategory(startDate, endDate)

    /**
     * Get total for a bill (excludes installment parents)
     */
    fun getBillTotal(billId: Long): Flow<Double> = transactionDao.getBillTotal(billId)

    /**
     * Get total for a bill (synchronous)
     */
    suspend fun getBillTotalSync(billId: Long): Double = transactionDao.getBillTotalSync(billId)

    // ==================== Monthly Helpers ====================

    /**
     * Get total expenses for the current month
     */
    fun getCurrentMonthExpenses(): Flow<Double> {
        val (startDate, endDate) = getMonthDateRange()
        return getTotalExpensesInDateRange(startDate, endDate)
    }

    /**
     * Get total income for the current month
     */
    fun getCurrentMonthIncome(): Flow<Double> {
        val (startDate, endDate) = getMonthDateRange()
        return getTotalIncomeInDateRange(startDate, endDate)
    }

    /**
     * Get expenses by category for the current month
     */
    fun getCurrentMonthExpensesByCategory(): Flow<List<CategoryTotal>> {
        val (startDate, endDate) = getMonthDateRange()
        return getExpensesByCategory(startDate, endDate)
    }

    /**
     * Helper to get start and end timestamps for the current month
     */
    private fun getMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }

    // ==================== Write Operations ====================

    /**
     * Create a simple transaction
     * @return The ID of the created transaction
     */
    suspend fun insertTransaction(transaction: Transaction): Long {
        val transactionId = transactionDao.insertTransaction(transaction)

        // Update balance if this is a completed transaction with a balance
        if (transaction.status == TransactionStatus.COMPLETED && transaction.balanceId != null) {
            val amount = if (transaction.type == TransactionType.INCOME) {
                transaction.amount
            } else {
                -transaction.amount
            }
            balanceDao.adjustBalance(transaction.balanceId, amount)
        }

        return transactionId
    }

    /**
     * Create a simple expense transaction
     */
    suspend fun createExpense(
        amount: Double,
        date: Long = System.currentTimeMillis(),
        balanceId: Long? = null,
        billId: Long? = null,
        category: String = "Outros",
        subcategory: String? = null,
        description: String? = null,
        status: String = TransactionStatus.COMPLETED
    ): Long {
        val transaction = Transaction(
            amount = amount,
            date = date,
            balanceId = balanceId,
            billId = billId,
            type = TransactionType.EXPENSE,
            status = status,
            category = category,
            subcategory = subcategory,
            description = description
        )
        return insertTransaction(transaction)
    }

    /**
     * Create a simple income transaction
     */
    suspend fun createIncome(
        amount: Double,
        date: Long = System.currentTimeMillis(),
        balanceId: Long,
        category: String = "Outros",
        subcategory: String? = null,
        description: String? = null,
        status: String = TransactionStatus.COMPLETED
    ): Long {
        val transaction = Transaction(
            amount = amount,
            date = date,
            balanceId = balanceId,
            type = TransactionType.INCOME,
            status = status,
            category = category,
            subcategory = subcategory,
            description = description
        )
        return insertTransaction(transaction)
    }

    /**
     * Update an existing transaction
     */
    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)

    /**
     * Delete a transaction
     */
    suspend fun deleteTransaction(transaction: Transaction) {
        // Reverse balance impact if needed
        if (transaction.status == TransactionStatus.COMPLETED && transaction.balanceId != null) {
            val amount = if (transaction.type == TransactionType.INCOME) {
                -transaction.amount // Reverse income
            } else {
                transaction.amount // Reverse expense
            }
            balanceDao.adjustBalance(transaction.balanceId, amount)
        }

        transactionDao.deleteTransaction(transaction)
    }

    /**
     * Delete a transaction by ID
     */
    suspend fun deleteTransactionById(id: Long) {
        val transaction = transactionDao.getTransactionByIdSync(id)
        if (transaction != null) {
            deleteTransaction(transaction)
        }
    }

    /**
     * Update transaction status
     */
    suspend fun updateTransactionStatus(transactionId: Long, status: String) {
        val transaction = transactionDao.getTransactionByIdSync(transactionId)
        if (transaction != null) {
            val oldStatus = transaction.status

            // Handle balance adjustments based on status change
            if (transaction.balanceId != null) {
                when {
                    // Completing a previously expected transaction
                    oldStatus == TransactionStatus.EXPECTED && status == TransactionStatus.COMPLETED -> {
                        val amount = if (transaction.type == TransactionType.INCOME) {
                            transaction.amount
                        } else {
                            -transaction.amount
                        }
                        balanceDao.adjustBalance(transaction.balanceId, amount)
                    }
                    // Cancelling or reverting a completed transaction
                    oldStatus == TransactionStatus.COMPLETED && status != TransactionStatus.COMPLETED -> {
                        val amount = if (transaction.type == TransactionType.INCOME) {
                            -transaction.amount
                        } else {
                            transaction.amount
                        }
                        balanceDao.adjustBalance(transaction.balanceId, amount)
                    }
                }
            }

            transactionDao.updateTransactionStatus(transactionId, status)
        }
    }

    // ==================== Installment Operations ====================

    /**
     * Result of installment creation
     */
    data class InstallmentCreationResult(
        val success: Boolean,
        val parentTransactionId: Long? = null,
        val childTransactionIds: List<Long> = emptyList(),
        val errorMessage: String? = null
    )

    /**
     * Create an installment purchase with parent and child transactions
     *
     * @param totalAmount Total purchase amount
     * @param installments Number of installments (parcelas)
     * @param category Transaction category
     * @param description Transaction description
     * @param billId ID of the first bill (current month)
     * @param creditCardId Credit card ID for generating future bills
     * @param date Purchase date
     * @return InstallmentCreationResult with transaction IDs
     */
    suspend fun createInstallmentPurchase(
        totalAmount: Double,
        installments: Int,
        category: String,
        description: String? = null,
        billId: Long,
        creditCardId: Long,
        date: Long = System.currentTimeMillis()
    ): InstallmentCreationResult {
        // Validate inputs
        if (totalAmount <= 0) {
            return InstallmentCreationResult(
                success = false,
                errorMessage = "Total amount must be positive"
            )
        }

        if (installments < 2) {
            return InstallmentCreationResult(
                success = false,
                errorMessage = "Installments must be at least 2. For single payment, use regular transaction."
            )
        }

        val installmentAmount = totalAmount / installments

        // Create parent transaction (represents the full purchase)
        val parentTransaction = Transaction(
            amount = totalAmount,
            date = date,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            billId = null, // Parent is NOT assigned to any bill
            isInstallmentParent = true,
            totalInstallments = installments,
            installmentAmount = installmentAmount,
            category = category,
            description = description
        )
        val parentId = transactionDao.insertTransaction(parentTransaction)

        // Create child transactions (one per installment)
        val childIds = mutableListOf<Long>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date

        // Get the current bill info to determine month/year progression
        val currentBill = billDao.getBillByIdSync(billId)
        var currentBillId = billId
        var billYear = currentBill?.year ?: calendar.get(Calendar.YEAR)
        var billMonth = currentBill?.month ?: (calendar.get(Calendar.MONTH) + 1)

        for (i in 1..installments) {
            val childTransaction = Transaction(
                amount = installmentAmount,
                date = calendar.timeInMillis,
                type = TransactionType.EXPENSE,
                status = if (i == 1) TransactionStatus.COMPLETED else TransactionStatus.EXPECTED,
                billId = currentBillId,
                parentTransactionId = parentId,
                isInstallmentParent = false,
                installmentNumber = i,
                totalInstallments = installments,
                installmentAmount = installmentAmount,
                category = category,
                description = description?.let { "$it ($i/$installments)" }
                    ?: "$category ($i/$installments)"
            )
            val childId = transactionDao.insertTransaction(childTransaction)
            childIds.add(childId)

            // Move to next month for subsequent installments
            calendar.add(Calendar.MONTH, 1)

            // Get or create bill for next month (for subsequent installments)
            if (i < installments) {
                billMonth++
                if (billMonth > 12) {
                    billMonth = 1
                    billYear++
                }

                // Try to get existing bill or it will be created when needed
                val nextBill = billDao.getBillForMonthSync(creditCardId, billYear, billMonth)
                currentBillId = nextBill?.id ?: currentBillId
            }
        }

        // Update the first bill total
        val newTotal = transactionDao.getBillTotalSync(billId)
        billDao.updateBillTotal(billId, newTotal)

        return InstallmentCreationResult(
            success = true,
            parentTransactionId = parentId,
            childTransactionIds = childIds
        )
    }

    /**
     * Get installment children for a parent transaction
     */
    fun getInstallmentChildren(parentId: Long): Flow<List<Transaction>> =
        transactionDao.getInstallmentChildren(parentId)

    /**
     * Get installment children synchronously
     */
    suspend fun getInstallmentChildrenSync(parentId: Long): List<Transaction> =
        transactionDao.getInstallmentChildrenSync(parentId)

    /**
     * Get all active installment parent transactions
     */
    fun getActiveInstallmentParents(): Flow<List<Transaction>> =
        transactionDao.getActiveInstallmentParents()

    /**
     * Get installment parents that still have remaining payments
     */
    fun getInstallmentParentsWithRemainingPayments(): Flow<List<Transaction>> =
        transactionDao.getInstallmentParentsWithRemainingPayments()

    /**
     * Data class for installment summary information
     */
    data class InstallmentSummary(
        val parentTransaction: Transaction,
        val totalAmount: Double,
        val installmentAmount: Double,
        val totalInstallments: Int,
        val completedCount: Int,
        val expectedCount: Int,
        val paidAmount: Double,
        val remainingAmount: Double
    )

    /**
     * Get a summary of an installment purchase
     */
    suspend fun getInstallmentSummary(parentTransactionId: Long): InstallmentSummary? {
        val parent = transactionDao.getTransactionByIdSync(parentTransactionId)
            ?: return null

        if (!parent.isInstallmentParent) {
            return null
        }

        val completedCount = transactionDao.countCompletedInstallments(parentTransactionId)
        val expectedCount = transactionDao.countExpectedInstallments(parentTransactionId)
        val installmentAmount = parent.installmentAmount ?: (parent.amount / (parent.totalInstallments ?: 1))
        val paidAmount = completedCount * installmentAmount
        val remainingAmount = expectedCount * installmentAmount

        return InstallmentSummary(
            parentTransaction = parent,
            totalAmount = parent.amount,
            installmentAmount = installmentAmount,
            totalInstallments = parent.totalInstallments ?: (completedCount + expectedCount),
            completedCount = completedCount,
            expectedCount = expectedCount,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount
        )
    }

    /**
     * Cancel remaining installments for a purchase
     */
    suspend fun cancelRemainingInstallments(parentTransactionId: Long) {
        transactionDao.cancelRemainingInstallments(parentTransactionId)

        // Also update parent status
        transactionDao.updateTransactionStatus(parentTransactionId, TransactionStatus.CANCELLED)
    }

    /**
     * Cancel an installment purchase (alias for cancelRemainingInstallments)
     */
    suspend fun cancelInstallment(parentTransactionId: Long) =
        cancelRemainingInstallments(parentTransactionId)

    // ==================== Transfer Operations ====================

    /**
     * Get the paired transfer transaction
     */
    fun getTransferPair(transferPairId: Long): Flow<List<Transaction>> =
        transactionDao.getTransferPair(transferPairId)
}

