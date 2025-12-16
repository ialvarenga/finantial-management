package com.example.organizadordefinancas.service.business

import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.model.Bill
import com.example.organizadordefinancas.data.model.BillStatus
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.repository.BillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Service class for automatic bill generation.
 * Handles creating monthly bills for credit cards and managing billing cycles.
 *
 * Billing Cycle Logic:
 * - Bills are generated on (or after) the card's closing date (fechamento)
 * - Each bill covers transactions from the billing cycle
 * - Due date is calculated based on card settings
 *
 * This service should be called from a WorkManager job daily.
 */
class BillGenerationService(
    private val billRepository: BillRepository,
    private val creditCardDao: CreditCardDao
) {
    // ==================== Data Classes ====================

    /**
     * Result of a bill generation operation
     */
    data class BillGenerationResult(
        val success: Boolean,
        val billId: Long? = null,
        val creditCardName: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Summary of auto-generation results
     */
    data class AutoGenerationSummary(
        val billsGenerated: Int,
        val billsSkipped: Int,
        val errors: List<String>,
        val generatedBills: List<BillGenerationResult>
    )

    /**
     * Information about an upcoming bill
     */
    data class UpcomingBillInfo(
        val creditCard: CreditCard,
        val year: Int,
        val month: Int,
        val closingDate: Long,
        val dueDate: Long,
        val estimatedTotal: Double
    )

    // ==================== Bill Generation ====================

    /**
     * Generate a bill for a specific credit card and month.
     *
     * @param creditCardId The credit card ID
     * @param year The year for the bill
     * @param month The month for the bill (1-12)
     * @return BillGenerationResult with the bill ID or error
     */
    suspend fun generateBillForMonth(
        creditCardId: Long,
        year: Int,
        month: Int
    ): BillGenerationResult {
        // Validate month
        if (month < 1 || month > 12) {
            return BillGenerationResult(
                success = false,
                errorMessage = "Mês inválido: $month"
            )
        }

        // Get credit card
        val creditCard = creditCardDao.getCreditCardByIdSync(creditCardId)
            ?: return BillGenerationResult(
                success = false,
                errorMessage = "Cartão de crédito não encontrado"
            )

        // Try to generate bill
        val billId = billRepository.generateBillForMonth(creditCardId, year, month)

        return if (billId != null) {
            BillGenerationResult(
                success = true,
                billId = billId,
                creditCardName = creditCard.name
            )
        } else {
            BillGenerationResult(
                success = false,
                errorMessage = "Fatura já existe para ${creditCard.name} em $month/$year"
            )
        }
    }

    /**
     * Generate the current month's bill for a credit card.
     *
     * @param creditCardId The credit card ID
     * @return BillGenerationResult
     */
    suspend fun generateCurrentMonthBill(creditCardId: Long): BillGenerationResult {
        val calendar = Calendar.getInstance()
        return generateBillForMonth(
            creditCardId,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Auto-generate bills for all credit cards that have auto_generate_bills enabled.
     * Should be called daily from a WorkManager job.
     *
     * @return AutoGenerationSummary with results
     */
    suspend fun autoGenerateBillsIfNeeded(): AutoGenerationSummary {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val today = calendar.get(Calendar.DAY_OF_MONTH)

        val cardsWithAutoGenerate = creditCardDao.getCardsWithAutoGenerateBillsSync()

        val generatedBills = mutableListOf<BillGenerationResult>()
        val errors = mutableListOf<String>()
        var skipped = 0

        for (card in cardsWithAutoGenerate) {
            try {
                // Check if today is on or after the closing date
                if (today >= card.closingDay) {
                    val result = generateBillForMonth(card.id, currentYear, currentMonth)
                    if (result.success) {
                        generatedBills.add(result)
                    } else if (result.errorMessage?.contains("já existe") == true) {
                        skipped++
                    } else {
                        result.errorMessage?.let { errors.add("${card.name}: $it") }
                    }
                } else {
                    skipped++
                }
            } catch (e: Exception) {
                errors.add("${card.name}: ${e.message}")
            }
        }

        return AutoGenerationSummary(
            billsGenerated = generatedBills.size,
            billsSkipped = skipped,
            errors = errors,
            generatedBills = generatedBills
        )
    }

    /**
     * Generate bills for a credit card for a range of months.
     * Useful for generating historical or future bills.
     *
     * @param creditCardId The credit card ID
     * @param startYear Start year
     * @param startMonth Start month (1-12)
     * @param endYear End year
     * @param endMonth End month (1-12)
     * @return List of BillGenerationResult
     */
    suspend fun generateBillsForRange(
        creditCardId: Long,
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int
    ): List<BillGenerationResult> {
        val results = mutableListOf<BillGenerationResult>()

        var year = startYear
        var month = startMonth

        while (year < endYear || (year == endYear && month <= endMonth)) {
            val result = generateBillForMonth(creditCardId, year, month)
            results.add(result)

            // Move to next month
            month++
            if (month > 12) {
                month = 1
                year++
            }
        }

        return results
    }

    // ==================== Status Management ====================

    /**
     * Mark all overdue bills as overdue.
     * Should be called daily from a WorkManager job.
     *
     * @return Number of bills marked as overdue
     */
    suspend fun markOverdueBills(): Int {
        billRepository.markOverdueBills()
        // Return count would require a before/after comparison
        // For now, just return 0 as the operation is fire-and-forget
        return 0
    }

    /**
     * Get all overdue bills.
     */
    fun getOverdueBills(): Flow<List<Bill>> = billRepository.getOverdueBills()

    /**
     * Check if a bill should be marked as overdue.
     *
     * @param bill The bill to check
     * @return true if bill is past due and not fully paid
     */
    fun isBillOverdue(bill: Bill): Boolean {
        if (bill.status == BillStatus.PAID) return false
        return System.currentTimeMillis() > bill.dueDate
    }

    // ==================== Bill Queries ====================

    /**
     * Get or create the current open bill for a credit card.
     *
     * @param creditCardId The credit card ID
     * @return Bill or null if card not found
     */
    suspend fun getOrCreateCurrentBill(creditCardId: Long): Bill? {
        return billRepository.getOrCreateCurrentBill(creditCardId)
    }

    /**
     * Get the bill for a specific month.
     *
     * @param creditCardId The credit card ID
     * @param year Year
     * @param month Month (1-12)
     * @return Bill or null if not found
     */
    fun getBillForMonth(creditCardId: Long, year: Int, month: Int): Flow<Bill?> {
        return billRepository.getBillForMonth(creditCardId, year, month)
    }

    /**
     * Get all bills for a credit card.
     */
    fun getBillsByCreditCard(creditCardId: Long): Flow<List<Bill>> {
        return billRepository.getBillsByCreditCard(creditCardId)
    }

    // ==================== Forecasting ====================

    /**
     * Get upcoming bills that need to be generated.
     *
     * @param lookaheadMonths How many months to look ahead
     * @return List of UpcomingBillInfo
     */
    suspend fun getUpcomingBillsToGenerate(lookaheadMonths: Int = 3): List<UpcomingBillInfo> {
        val calendar = Calendar.getInstance()
        val cards = creditCardDao.getActiveCreditCards().first()
        val upcoming = mutableListOf<UpcomingBillInfo>()

        for (card in cards) {
            for (monthOffset in 0 until lookaheadMonths) {
                val checkCalendar = Calendar.getInstance()
                checkCalendar.add(Calendar.MONTH, monthOffset)
                val year = checkCalendar.get(Calendar.YEAR)
                val month = checkCalendar.get(Calendar.MONTH) + 1

                // Check if bill already exists
                val existingBill = billRepository.getBillByIdSync(card.id)

                if (existingBill == null) {
                    val (closingDate, dueDate) = calculateBillDates(card, year, month)
                    upcoming.add(
                        UpcomingBillInfo(
                            creditCard = card,
                            year = year,
                            month = month,
                            closingDate = closingDate,
                            dueDate = dueDate,
                            estimatedTotal = 0.0 // Would need transaction estimates
                        )
                    )
                }
            }
        }

        return upcoming.sortedBy { it.closingDate }
    }

    /**
     * Calculate the closing and due dates for a bill.
     *
     * @param card The credit card
     * @param year Year
     * @param month Month (1-12)
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

    // ==================== Utility ====================

    /**
     * Get the current billing period description.
     *
     * @param card The credit card
     * @return Human-readable description like "10 Nov - 10 Dez 2024"
     */
    fun getCurrentBillingPeriodDescription(card: CreditCard): String {
        val calendar = Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("pt", "BR"))

        // Calculate start date (previous closing + 1)
        calendar.set(Calendar.DAY_OF_MONTH, card.closingDay)
        calendar.add(Calendar.MONTH, -1)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.time

        // Calculate end date (current closing)
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, card.closingDay)
        val endDate = calendar.time

        return "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
    }
}

