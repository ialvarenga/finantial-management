package com.example.organizadordefinancas.service.business

import com.example.organizadordefinancas.data.dao.CategoryTotal
import com.example.organizadordefinancas.data.repository.BalanceRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Service class for expense and income calculations.
 * Provides methods for calculating totals, averages, and breakdowns.
 *
 * CRITICAL: All expense calculations automatically exclude installment parents
 * to avoid double-counting (handled in the DAO layer).
 */
class ExpenseCalculationService(
    private val transactionRepository: TransactionRepository,
    private val balanceRepository: BalanceRepository
) {
    // ==================== Data Classes ====================

    /**
     * Summary of income and expenses for a period
     */
    data class PeriodSummary(
        val totalIncome: Double,
        val totalExpenses: Double,
        val netAmount: Double,
        val savingsRate: Float, // Income - Expenses as percentage of Income
        val categoryBreakdown: List<CategoryTotal>
    )

    /**
     * Monthly comparison data
     */
    data class MonthlyComparison(
        val currentMonthExpenses: Double,
        val previousMonthExpenses: Double,
        val changePercentage: Float,
        val trend: ExpenseTrend
    )

    enum class ExpenseTrend {
        INCREASING,  // Expenses went up
        DECREASING,  // Expenses went down
        STABLE       // Within ±5%
    }

    /**
     * Category expense data
     */
    data class CategoryExpense(
        val category: String,
        val amount: Double,
        val percentage: Float,
        val transactionCount: Int
    )

    /**
     * Available balance summary for an account
     */
    data class AvailableBalanceSummary(
        val totalBalance: Double,
        val poolsTotal: Double,
        val availableBalance: Double,
        val availablePercentage: Float
    )

    // ==================== Expense Totals ====================

    /**
     * Get total expenses for a date range.
     * Excludes installment parents automatically.
     */
    fun getTotalExpenses(startDate: Long, endDate: Long): Flow<Double> =
        transactionRepository.getTotalExpensesInDateRange(startDate, endDate)

    /**
     * Get total expenses for a date range (synchronous).
     */
    suspend fun getTotalExpensesSync(startDate: Long, endDate: Long): Double =
        transactionRepository.getTotalExpensesInDateRangeSync(startDate, endDate)

    /**
     * Get total income for a date range.
     */
    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double> =
        transactionRepository.getTotalIncomeInDateRange(startDate, endDate)

    /**
     * Get total income for a date range (synchronous).
     */
    suspend fun getTotalIncomeSync(startDate: Long, endDate: Long): Double =
        transactionRepository.getTotalIncomeInDateRangeSync(startDate, endDate)

    /**
     * Get total expenses for the current month.
     */
    fun getCurrentMonthExpenses(): Flow<Double> = transactionRepository.getCurrentMonthExpenses()

    /**
     * Get total income for the current month.
     */
    fun getCurrentMonthIncome(): Flow<Double> = transactionRepository.getCurrentMonthIncome()

    // ==================== Period Summary ====================

    /**
     * Get a complete summary for a period.
     */
    fun getPeriodSummary(startDate: Long, endDate: Long): Flow<PeriodSummary> {
        return combine(
            transactionRepository.getTotalIncomeInDateRange(startDate, endDate),
            transactionRepository.getTotalExpensesInDateRange(startDate, endDate),
            transactionRepository.getExpensesByCategory(startDate, endDate)
        ) { income, expenses, categories ->
            val net = income - expenses
            val savingsRate = if (income > 0) {
                ((income - expenses) / income * 100).toFloat()
            } else 0f

            PeriodSummary(
                totalIncome = income,
                totalExpenses = expenses,
                netAmount = net,
                savingsRate = savingsRate,
                categoryBreakdown = categories
            )
        }
    }

    /**
     * Get summary for the current month.
     */
    fun getCurrentMonthSummary(): Flow<PeriodSummary> {
        val (startDate, endDate) = getMonthDateRange()
        return getPeriodSummary(startDate, endDate)
    }

    /**
     * Get summary for a specific month.
     */
    fun getMonthSummary(year: Int, month: Int): Flow<PeriodSummary> {
        val (startDate, endDate) = getMonthDateRange(year, month)
        return getPeriodSummary(startDate, endDate)
    }

    // ==================== Monthly Comparisons ====================

    /**
     * Compare current month expenses with previous month.
     */
    suspend fun getMonthlyComparison(): MonthlyComparison {
        val (currentStart, currentEnd) = getMonthDateRange()
        val (previousStart, previousEnd) = getPreviousMonthDateRange()

        val currentExpenses = transactionRepository.getTotalExpensesInDateRangeSync(currentStart, currentEnd)
        val previousExpenses = transactionRepository.getTotalExpensesInDateRangeSync(previousStart, previousEnd)

        val changePercentage = if (previousExpenses > 0) {
            ((currentExpenses - previousExpenses) / previousExpenses * 100).toFloat()
        } else if (currentExpenses > 0) {
            100f
        } else {
            0f
        }

        val trend = when {
            changePercentage > 5 -> ExpenseTrend.INCREASING
            changePercentage < -5 -> ExpenseTrend.DECREASING
            else -> ExpenseTrend.STABLE
        }

        return MonthlyComparison(
            currentMonthExpenses = currentExpenses,
            previousMonthExpenses = previousExpenses,
            changePercentage = changePercentage,
            trend = trend
        )
    }

    /**
     * Get monthly average expenses for the last N months.
     */
    suspend fun getMonthlyAverage(months: Int = 6): Double {
        if (months <= 0) return 0.0

        val calendar = Calendar.getInstance()
        var total = 0.0

        repeat(months) {
            val (startDate, endDate) = getMonthDateRange(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1
            )
            total += transactionRepository.getTotalExpensesInDateRangeSync(startDate, endDate)
            calendar.add(Calendar.MONTH, -1)
        }

        return total / months
    }

    // ==================== Category Analysis ====================

    /**
     * Get expenses grouped by category for a period.
     */
    fun getExpensesByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>> =
        transactionRepository.getExpensesByCategory(startDate, endDate)

    /**
     * Get expenses by category for current month.
     */
    fun getCurrentMonthExpensesByCategory(): Flow<List<CategoryTotal>> =
        transactionRepository.getCurrentMonthExpensesByCategory()

    /**
     * Get detailed category expenses with percentages.
     */
    fun getDetailedCategoryExpenses(startDate: Long, endDate: Long): Flow<List<CategoryExpense>> {
        return combine(
            transactionRepository.getExpensesByCategory(startDate, endDate),
            transactionRepository.getTotalExpensesInDateRange(startDate, endDate)
        ) { categories, total ->
            categories.map { categoryTotal ->
                CategoryExpense(
                    category = categoryTotal.category,
                    amount = categoryTotal.total,
                    percentage = if (total > 0) {
                        (categoryTotal.total / total * 100).toFloat()
                    } else 0f,
                    transactionCount = 0 // Would need a separate query for count
                )
            }.sortedByDescending { it.amount }
        }
    }

    /**
     * Get the top spending categories for a period.
     */
    fun getTopCategories(startDate: Long, endDate: Long, limit: Int = 5): Flow<List<CategoryTotal>> {
        return transactionRepository.getExpensesByCategory(startDate, endDate)
            .map { categories ->
                categories.sortedByDescending { it.total }.take(limit)
            }
    }

    // ==================== Balance Calculations ====================

    /**
     * Get available balance for an account.
     * Available = Main Balance - Sum of Pools
     */
    fun getAvailableBalance(accountId: Long): Flow<Double> =
        balanceRepository.getAvailableBalanceForAccount(accountId)

    /**
     * Get available balance summary for an account.
     */
    suspend fun getAvailableBalanceSummary(accountId: Long): AvailableBalanceSummary {
        val mainBalance = balanceRepository.getMainBalanceForAccountSync(accountId)
        val totalBalance = mainBalance?.currentBalance ?: 0.0
        val poolsTotal = balanceRepository.getSumOfPoolsForAccount(accountId).first()
        val available = totalBalance - poolsTotal

        return AvailableBalanceSummary(
            totalBalance = totalBalance,
            poolsTotal = poolsTotal,
            availableBalance = available,
            availablePercentage = if (totalBalance > 0) {
                (available / totalBalance * 100).toFloat()
            } else 0f
        )
    }

    /**
     * Get total balance across all accounts.
     */
    fun getTotalMainBalance(): Flow<Double> = balanceRepository.getTotalMainBalance()

    /**
     * Get total pool balance across all accounts.
     */
    fun getTotalPoolBalance(): Flow<Double> = balanceRepository.getTotalPoolBalance()

    // ==================== Utility Methods ====================

    /**
     * Get start and end timestamps for the current month.
     */
    fun getMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        return getMonthDateRange(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Get start and end timestamps for a specific month.
     */
    fun getMonthDateRange(year: Int, month: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Start of month
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        // End of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }

    /**
     * Get start and end timestamps for the previous month.
     */
    private fun getPreviousMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        return getMonthDateRange(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Get start and end timestamps for the current year.
     */
    fun getYearDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        return getYearDateRange(calendar.get(Calendar.YEAR))
    }

    /**
     * Get start and end timestamps for a specific year.
     */
    fun getYearDateRange(year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Start of year
        calendar.set(year, 0, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        // End of year
        calendar.set(Calendar.MONTH, 11)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }

    /**
     * Format currency amount for display.
     */
    fun formatCurrency(amount: Double): String {
        return "R$ ${"%.2f".format(amount)}"
    }

    /**
     * Get a human-readable description of the expense trend.
     */
    fun getTrendDescription(trend: ExpenseTrend, percentage: Float): String {
        val absPercentage = kotlin.math.abs(percentage)
        return when (trend) {
            ExpenseTrend.INCREASING -> "Aumento de ${"%.1f".format(absPercentage)}% em relação ao mês anterior"
            ExpenseTrend.DECREASING -> "Redução de ${"%.1f".format(absPercentage)}% em relação ao mês anterior"
            ExpenseTrend.STABLE -> "Gastos estáveis em relação ao mês anterior"
        }
    }
}

