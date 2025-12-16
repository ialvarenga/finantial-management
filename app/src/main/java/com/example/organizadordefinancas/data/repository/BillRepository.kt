package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.BillDao
import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.dao.TransactionDao
import com.example.organizadordefinancas.data.model.Bill
import com.example.organizadordefinancas.data.model.BillStatus
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

/**
 * Repository for managing Bill entities (credit card monthly statements).
 * Handles bill generation, payment tracking, and status management.
 */
class BillRepository(
    private val billDao: BillDao,
    private val creditCardDao: CreditCardDao,
    private val transactionDao: TransactionDao
) {
    // ==================== Read Operations ====================

    /**
     * Get all bills ordered by date (newest first)
     */
    fun getAllBills(): Flow<List<Bill>> = billDao.getAllBills()

    /**
     * Get a specific bill by ID
     */
    fun getBillById(id: Long): Flow<Bill?> = billDao.getBillById(id)

    /**
     * Get a specific bill by ID (synchronous)
     */
    suspend fun getBillByIdSync(id: Long): Bill? = billDao.getBillByIdSync(id)

    // ==================== Credit Card Bills ====================

    /**
     * Get all bills for a specific credit card
     */
    fun getBillsByCreditCard(creditCardId: Long): Flow<List<Bill>> =
        billDao.getBillsByCreditCard(creditCardId)

    /**
     * Get bill for a specific month and credit card
     */
    fun getBillForMonth(creditCardId: Long, year: Int, month: Int): Flow<Bill?> =
        billDao.getBillForMonth(creditCardId, year, month)

    /**
     * Get current open bill for a credit card
     */
    fun getCurrentOpenBill(creditCardId: Long): Flow<Bill?> =
        billDao.getCurrentOpenBill(creditCardId)

    /**
     * Get current open bill for a credit card (synchronous)
     */
    suspend fun getCurrentOpenBillSync(creditCardId: Long): Bill? =
        billDao.getCurrentOpenBillSync(creditCardId)

    // ==================== Status-based Queries ====================

    /**
     * Get bills by status
     */
    fun getBillsByStatus(status: String): Flow<List<Bill>> =
        billDao.getBillsByStatus(status)

    /**
     * Get open (unpaid) bills
     */
    fun getOpenBills(): Flow<List<Bill>> = billDao.getOpenBills()

    /**
     * Get overdue bills
     */
    fun getOverdueBills(): Flow<List<Bill>> = billDao.getOverdueBills()

    /**
     * Get total unpaid amount across all bills
     */
    fun getTotalUnpaidAmount(): Flow<Double> = billDao.getTotalUnpaidAmount()

    /**
     * Get total unpaid amount for a specific credit card
     */
    fun getTotalUnpaidAmountForCard(creditCardId: Long): Flow<Double> =
        billDao.getTotalUnpaidAmountForCard(creditCardId)

    // ==================== Bill with Transactions ====================

    /**
     * Data class representing a bill with its transactions
     */
    data class BillWithTransactions(
        val bill: Bill,
        val creditCard: CreditCard?,
        val transactions: List<Transaction>,
        val calculatedTotal: Double
    )

    /**
     * Get a bill with all its transactions
     */
    fun getBillWithTransactions(billId: Long): Flow<BillWithTransactions?> {
        return combine(
            billDao.getBillById(billId),
            transactionDao.getTransactionsByBillIdExcludingParents(billId)
        ) { bill, transactions ->
            bill?.let {
                // Get credit card info
                val creditCard = creditCardDao.getCreditCardByIdSync(bill.creditCardId)
                val calculatedTotal = transactions.sumOf { tx -> tx.amount }

                BillWithTransactions(
                    bill = bill,
                    creditCard = creditCard,
                    transactions = transactions,
                    calculatedTotal = calculatedTotal
                )
            }
        }
    }

    /**
     * Calculate the total for a bill from its transactions
     * (Excludes installment parents to avoid double-counting)
     */
    suspend fun calculateBillTotal(billId: Long): Double =
        transactionDao.getBillTotalSync(billId)

    // ==================== Write Operations ====================

    /**
     * Create a new bill
     * @return The ID of the created bill
     */
    suspend fun insertBill(bill: Bill): Long = billDao.insertBill(bill)

    /**
     * Update an existing bill
     */
    suspend fun updateBill(bill: Bill) = billDao.updateBill(bill)

    /**
     * Delete a bill
     */
    suspend fun deleteBill(bill: Bill) = billDao.deleteBill(bill)

    /**
     * Delete a bill by ID
     */
    suspend fun deleteBillById(id: Long) = billDao.deleteBillById(id)

    // ==================== Bill Generation ====================

    /**
     * Generate a bill for a credit card for a specific month
     * @return The ID of the created bill, or null if bill already exists
     */
    suspend fun generateBillForMonth(
        creditCardId: Long,
        year: Int,
        month: Int
    ): Long? {
        // Check if bill already exists
        val existingBill = billDao.getBillForMonthSync(creditCardId, year, month)
        if (existingBill != null) {
            return null // Bill already exists
        }

        // Get credit card details
        val creditCard = creditCardDao.getCreditCardByIdSync(creditCardId)
            ?: return null

        // Calculate closing and due dates
        val (closingDate, dueDate) = calculateBillDates(creditCard, year, month)

        // Create the bill
        val bill = Bill(
            creditCardId = creditCardId,
            year = year,
            month = month,
            closingDate = closingDate,
            dueDate = dueDate,
            totalAmount = 0.0,
            paidAmount = 0.0,
            status = BillStatus.OPEN
        )

        return billDao.insertBill(bill)
    }

    /**
     * Calculate the closing and due dates for a bill
     * @return Pair of (closingDate, dueDate) as timestamps
     */
    private fun calculateBillDates(card: CreditCard, year: Int, month: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Closing date is the closing day of the given month
        calendar.set(year, month - 1, card.closingDay, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        // Handle months with fewer days
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (card.closingDay > maxDay) {
            calendar.set(Calendar.DAY_OF_MONTH, maxDay)
        }
        val closingDate = calendar.timeInMillis

        // Due date is the due day of the following month (if due day < closing day)
        // or the same month (if due day > closing day)
        if (card.dueDay <= card.closingDay) {
            // Due date is in the next month
            calendar.add(Calendar.MONTH, 1)
        }
        calendar.set(Calendar.DAY_OF_MONTH, card.dueDay)

        // Handle months with fewer days
        val maxDueDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (card.dueDay > maxDueDay) {
            calendar.set(Calendar.DAY_OF_MONTH, maxDueDay)
        }
        val dueDate = calendar.timeInMillis

        return Pair(closingDate, dueDate)
    }

    /**
     * Generate bills for all credit cards that have auto_generate_bills enabled
     * for the current month if not already generated
     * @return Number of bills generated
     */
    suspend fun autoGenerateBillsIfNeeded(): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based

        val cardsWithAutoGenerate = creditCardDao.getCardsWithAutoGenerateBillsSync()
        var billsGenerated = 0

        for (card in cardsWithAutoGenerate) {
            // Check if today is past or on the closing date
            val today = calendar.get(Calendar.DAY_OF_MONTH)
            if (today >= card.closingDay) {
                // Generate bill for current month if it doesn't exist
                val result = generateBillForMonth(card.id, currentYear, currentMonth)
                if (result != null) {
                    billsGenerated++
                }
            }
        }

        return billsGenerated
    }

    // ==================== Bill Payment ====================

    /**
     * Result of a bill payment operation
     */
    data class BillPaymentResult(
        val success: Boolean,
        val newStatus: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Record a payment for a bill
     * @param billId The bill to pay
     * @param paymentAmount Amount being paid
     * @param paymentTransactionId Optional ID of the payment transaction
     * @return BillPaymentResult with success status
     */
    suspend fun recordBillPayment(
        billId: Long,
        paymentAmount: Double,
        paymentTransactionId: Long? = null
    ): BillPaymentResult {
        val bill = billDao.getBillByIdSync(billId)
            ?: return BillPaymentResult(
                success = false,
                errorMessage = "Bill not found"
            )

        if (paymentAmount <= 0) {
            return BillPaymentResult(
                success = false,
                errorMessage = "Payment amount must be positive"
            )
        }

        val newPaidAmount = bill.paidAmount + paymentAmount
        val newStatus = when {
            newPaidAmount >= bill.totalAmount -> BillStatus.PAID
            newPaidAmount > 0 -> BillStatus.PARTIAL
            else -> bill.status
        }

        billDao.updateBillPayment(
            billId = billId,
            paidAmount = newPaidAmount,
            status = newStatus,
            paymentTransactionId = paymentTransactionId
        )

        return BillPaymentResult(
            success = true,
            newStatus = newStatus
        )
    }

    /**
     * Recalculate and update the total amount for a bill based on its transactions
     */
    suspend fun recalculateBillTotal(billId: Long) {
        val total = transactionDao.getBillTotalSync(billId)
        billDao.updateBillTotal(billId, total)
    }

    // ==================== Status Management ====================

    /**
     * Mark bills as overdue if they are past due date and not fully paid
     */
    suspend fun markOverdueBills() = billDao.markOverdueBills()

    /**
     * Update the status of a specific bill
     */
    suspend fun updateBillStatus(billId: Long, status: String) =
        billDao.updateBillStatus(billId, status)

    // ==================== Utility Functions ====================

    /**
     * Get or create the current open bill for a credit card
     * This is useful when adding new transactions
     */
    suspend fun getOrCreateCurrentBill(creditCardId: Long): Bill? {
        // Try to get existing open bill
        val openBill = billDao.getCurrentOpenBillSync(creditCardId)
        if (openBill != null) {
            return openBill
        }

        // Generate bill for current month
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        val billId = generateBillForMonth(creditCardId, currentYear, currentMonth)
        return billId?.let { billDao.getBillByIdSync(it) }
    }
}

