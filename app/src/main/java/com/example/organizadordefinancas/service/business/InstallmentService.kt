package com.example.organizadordefinancas.service.business

import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.repository.BillRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Service class for managing installment purchases.
 * Handles creation, cancellation, and summary of installment transactions.
 *
 * Installment Purchase Model:
 * - 1 Parent Transaction: Holds full amount, is_installment_parent = true
 * - N Child Transactions: Each holds installment_amount, linked via parent_transaction_id
 *
 * CRITICAL: Parent transactions are excluded from expense totals to avoid double-counting.
 */
class InstallmentService(
    private val transactionRepository: TransactionRepository,
    private val billRepository: BillRepository
) {
    // ==================== Data Classes ====================

    /**
     * Result of creating an installment purchase
     */
    data class InstallmentCreationResult(
        val success: Boolean,
        val parentTransactionId: Long? = null,
        val childTransactionIds: List<Long> = emptyList(),
        val errorMessage: String? = null
    )

    /**
     * Summary information about an installment purchase
     */
    data class InstallmentSummary(
        val parentTransaction: Transaction,
        val totalAmount: Double,
        val installmentAmount: Double,
        val totalInstallments: Int,
        val completedCount: Int,
        val expectedCount: Int,
        val cancelledCount: Int,
        val paidAmount: Double,
        val remainingAmount: Double,
        val progressPercentage: Float
    )

    /**
     * Information about an installment payment schedule item
     */
    data class PaymentScheduleItem(
        val transaction: Transaction,
        val installmentNumber: Int,
        val totalInstallments: Int,
        val amount: Double,
        val isPaid: Boolean,
        val isExpected: Boolean,
        val isCancelled: Boolean,
        val monthYear: String  // e.g., "Jan 2024"
    )

    // ==================== Create Operations ====================

    /**
     * Create an installment purchase with parent and child transactions.
     *
     * @param totalAmount Total purchase amount
     * @param installments Number of installments (parcelas) - minimum 2
     * @param category Transaction category
     * @param description Optional description
     * @param billId ID of the first bill (current month)
     * @param creditCardId Credit card ID for generating future bills
     * @param date Purchase date (defaults to now)
     * @return InstallmentCreationResult with success status and transaction IDs
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
        // Input validation
        if (totalAmount <= 0) {
            return InstallmentCreationResult(
                success = false,
                errorMessage = "O valor total deve ser positivo"
            )
        }

        if (installments < 2) {
            return InstallmentCreationResult(
                success = false,
                errorMessage = "O número de parcelas deve ser pelo menos 2. Para pagamento único, use uma transação normal."
            )
        }

        if (installments > 48) {
            return InstallmentCreationResult(
                success = false,
                errorMessage = "O número máximo de parcelas é 48"
            )
        }

        // Delegate to repository
        val result = transactionRepository.createInstallmentPurchase(
            totalAmount = totalAmount,
            installments = installments,
            category = category,
            description = description,
            billId = billId,
            creditCardId = creditCardId,
            date = date
        )

        return InstallmentCreationResult(
            success = result.success,
            parentTransactionId = result.parentTransactionId,
            childTransactionIds = result.childTransactionIds,
            errorMessage = result.errorMessage
        )
    }

    /**
     * Calculate installment amount preview before creating purchase.
     *
     * @param totalAmount Total purchase amount
     * @param installments Number of installments
     * @return Pair of (installmentAmount, formattedPreview) or null if invalid
     */
    fun calculateInstallmentPreview(totalAmount: Double, installments: Int): Pair<Double, String>? {
        if (totalAmount <= 0 || installments < 1) return null

        val installmentAmount = totalAmount / installments
        val formattedPreview = "${installments}x R$ ${"%.2f".format(installmentAmount)}"
        return Pair(installmentAmount, formattedPreview)
    }

    // ==================== Read Operations ====================

    /**
     * Get summary of an installment purchase by parent transaction ID.
     *
     * @param parentTransactionId ID of the parent transaction
     * @return InstallmentSummary or null if not found or not an installment
     */
    suspend fun getInstallmentSummary(parentTransactionId: Long): InstallmentSummary? {
        val repoSummary = transactionRepository.getInstallmentSummary(parentTransactionId)
            ?: return null

        val children = transactionRepository.getInstallmentChildrenSync(parentTransactionId)
        val cancelledCount = children.count { it.status == TransactionStatus.CANCELLED }

        return InstallmentSummary(
            parentTransaction = repoSummary.parentTransaction,
            totalAmount = repoSummary.totalAmount,
            installmentAmount = repoSummary.installmentAmount,
            totalInstallments = repoSummary.totalInstallments,
            completedCount = repoSummary.completedCount,
            expectedCount = repoSummary.expectedCount,
            cancelledCount = cancelledCount,
            paidAmount = repoSummary.paidAmount,
            remainingAmount = repoSummary.remainingAmount,
            progressPercentage = if (repoSummary.totalInstallments > 0) {
                (repoSummary.completedCount.toFloat() / repoSummary.totalInstallments)
            } else 0f
        )
    }

    /**
     * Get the payment schedule for an installment purchase.
     *
     * @param parentTransactionId ID of the parent transaction
     * @return List of PaymentScheduleItem ordered by installment number
     */
    suspend fun getPaymentSchedule(parentTransactionId: Long): List<PaymentScheduleItem> {
        val parent = transactionRepository.getTransactionByIdSync(parentTransactionId)
            ?: return emptyList()

        if (!parent.isInstallmentParent) return emptyList()

        val children = transactionRepository.getInstallmentChildrenSync(parentTransactionId)
        val totalInstallments = parent.totalInstallments ?: children.size

        return children.map { child ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = child.date
            val monthName = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale("pt", "BR"))
                .format(calendar.time)

            PaymentScheduleItem(
                transaction = child,
                installmentNumber = child.installmentNumber ?: 0,
                totalInstallments = totalInstallments,
                amount = child.amount,
                isPaid = child.status == TransactionStatus.COMPLETED,
                isExpected = child.status == TransactionStatus.EXPECTED,
                isCancelled = child.status == TransactionStatus.CANCELLED,
                monthYear = monthName
            )
        }.sortedBy { it.installmentNumber }
    }

    /**
     * Get all active installment purchases (parents with remaining payments).
     */
    fun getActiveInstallments(): Flow<List<Transaction>> =
        transactionRepository.getInstallmentParentsWithRemainingPayments()

    /**
     * Get all installment parent transactions.
     */
    fun getAllInstallmentParents(): Flow<List<Transaction>> =
        transactionRepository.getActiveInstallmentParents()

    /**
     * Get child transactions for an installment purchase.
     */
    fun getInstallmentChildren(parentTransactionId: Long): Flow<List<Transaction>> =
        transactionRepository.getInstallmentChildren(parentTransactionId)

    // ==================== Update Operations ====================

    /**
     * Cancel remaining installments for a purchase.
     * Only installments with status "expected" will be cancelled.
     * Already paid installments are not affected.
     *
     * @param parentTransactionId ID of the parent transaction
     */
    suspend fun cancelInstallment(parentTransactionId: Long) {
        transactionRepository.cancelRemainingInstallments(parentTransactionId)
    }

    /**
     * Mark a specific installment as paid/completed.
     *
     * @param transactionId ID of the child transaction
     */
    suspend fun markInstallmentAsPaid(transactionId: Long) {
        transactionRepository.updateTransactionStatus(transactionId, TransactionStatus.COMPLETED)
    }

    /**
     * Mark a specific installment as expected (revert from completed).
     *
     * @param transactionId ID of the child transaction
     */
    suspend fun markInstallmentAsExpected(transactionId: Long) {
        transactionRepository.updateTransactionStatus(transactionId, TransactionStatus.EXPECTED)
    }

    // ==================== Analysis Operations ====================

    /**
     * Get statistics about installments for a given period.
     *
     * @param startDate Start of period (timestamp)
     * @param endDate End of period (timestamp)
     * @return InstallmentStatistics
     */
    data class InstallmentStatistics(
        val activeInstallmentCount: Int,
        val totalRemainingAmount: Double,
        val monthlyInstallmentExpense: Double,
        val upcomingPaymentsCount: Int
    )

    /**
     * Get overall installment statistics.
     */
    suspend fun getInstallmentStatistics(): InstallmentStatistics {
        val parents = transactionRepository.getActiveInstallmentParents().first()
        var totalRemaining = 0.0
        var monthlyExpense = 0.0
        var upcomingCount = 0

        for (parent in parents) {
            val summary = transactionRepository.getInstallmentSummary(parent.id)
            if (summary != null) {
                totalRemaining += summary.remainingAmount
                monthlyExpense += summary.installmentAmount
                upcomingCount += summary.expectedCount
            }
        }

        return InstallmentStatistics(
            activeInstallmentCount = parents.size,
            totalRemainingAmount = totalRemaining,
            monthlyInstallmentExpense = monthlyExpense,
            upcomingPaymentsCount = upcomingCount
        )
    }
}

