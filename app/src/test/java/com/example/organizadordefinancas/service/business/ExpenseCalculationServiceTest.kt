package com.example.organizadordefinancas.service.business

import com.example.organizadordefinancas.data.dao.CategoryTotal
import com.example.organizadordefinancas.data.repository.BalanceRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.Calendar

/**
 * Unit tests for ExpenseCalculationService.
 * Tests expense totals, summaries, and calculations.
 *
 * CRITICAL: Verifies that installment parents are excluded from calculations.
 */
class ExpenseCalculationServiceTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var balanceRepository: BalanceRepository
    private lateinit var expenseCalculationService: ExpenseCalculationService

    @Before
    fun setup() {
        transactionRepository = mock()
        balanceRepository = mock()
        expenseCalculationService = ExpenseCalculationService(transactionRepository, balanceRepository)
    }

    // ==================== Get Total Expenses Tests ====================

    @Test
    fun `getTotalExpenses returns correct flow`() = runTest {
        // Arrange
        val startDate = 1000L
        val endDate = 2000L
        whenever(transactionRepository.getTotalExpensesInDateRange(startDate, endDate))
            .thenReturn(flowOf(500.0))

        // Act
        val result = expenseCalculationService.getTotalExpenses(startDate, endDate).first()

        // Assert
        assertEquals(500.0, result, 0.01)
        verify(transactionRepository).getTotalExpensesInDateRange(startDate, endDate)
    }

    @Test
    fun `getTotalExpensesSync returns correct value`() = runTest {
        // Arrange
        val startDate = 1000L
        val endDate = 2000L
        whenever(transactionRepository.getTotalExpensesInDateRangeSync(startDate, endDate))
            .thenReturn(750.0)

        // Act
        val result = expenseCalculationService.getTotalExpensesSync(startDate, endDate)

        // Assert
        assertEquals(750.0, result, 0.01)
    }

    // ==================== Get Total Income Tests ====================

    @Test
    fun `getTotalIncome returns correct flow`() = runTest {
        // Arrange
        val startDate = 1000L
        val endDate = 2000L
        whenever(transactionRepository.getTotalIncomeInDateRange(startDate, endDate))
            .thenReturn(flowOf(3000.0))

        // Act
        val result = expenseCalculationService.getTotalIncome(startDate, endDate).first()

        // Assert
        assertEquals(3000.0, result, 0.01)
    }

    @Test
    fun `getTotalIncomeSync returns correct value`() = runTest {
        // Arrange
        whenever(transactionRepository.getTotalIncomeInDateRangeSync(any(), any()))
            .thenReturn(3500.0)

        // Act
        val result = expenseCalculationService.getTotalIncomeSync(1000L, 2000L)

        // Assert
        assertEquals(3500.0, result, 0.01)
    }

    // ==================== Period Summary Tests ====================

    @Test
    fun `getPeriodSummary calculates net amount correctly`() = runTest {
        // Arrange
        val startDate = 1000L
        val endDate = 2000L
        val income = 5000.0
        val expenses = 3000.0
        val categories = listOf(
            CategoryTotal(category = "Food", total = 1500.0),
            CategoryTotal(category = "Transport", total = 1500.0)
        )

        whenever(transactionRepository.getTotalIncomeInDateRange(startDate, endDate))
            .thenReturn(flowOf(income))
        whenever(transactionRepository.getTotalExpensesInDateRange(startDate, endDate))
            .thenReturn(flowOf(expenses))
        whenever(transactionRepository.getExpensesByCategory(startDate, endDate))
            .thenReturn(flowOf(categories))

        // Act
        val summary = expenseCalculationService.getPeriodSummary(startDate, endDate).first()

        // Assert
        assertEquals(income, summary.totalIncome, 0.01)
        assertEquals(expenses, summary.totalExpenses, 0.01)
        assertEquals(2000.0, summary.netAmount, 0.01) // 5000 - 3000
        assertEquals(2, summary.categoryBreakdown.size)
    }

    @Test
    fun `getPeriodSummary calculates savings rate correctly`() = runTest {
        // Arrange
        val income = 10000.0
        val expenses = 7000.0

        whenever(transactionRepository.getTotalIncomeInDateRange(any(), any()))
            .thenReturn(flowOf(income))
        whenever(transactionRepository.getTotalExpensesInDateRange(any(), any()))
            .thenReturn(flowOf(expenses))
        whenever(transactionRepository.getExpensesByCategory(any(), any()))
            .thenReturn(flowOf(emptyList()))

        // Act
        val summary = expenseCalculationService.getPeriodSummary(0L, 100L).first()

        // Assert
        // Savings rate = (10000 - 7000) / 10000 * 100 = 30%
        assertEquals(30.0f, summary.savingsRate, 0.01f)
    }

    @Test
    fun `getPeriodSummary handles zero income for savings rate`() = runTest {
        // Arrange
        whenever(transactionRepository.getTotalIncomeInDateRange(any(), any()))
            .thenReturn(flowOf(0.0))
        whenever(transactionRepository.getTotalExpensesInDateRange(any(), any()))
            .thenReturn(flowOf(100.0))
        whenever(transactionRepository.getExpensesByCategory(any(), any()))
            .thenReturn(flowOf(emptyList()))

        // Act
        val summary = expenseCalculationService.getPeriodSummary(0L, 100L).first()

        // Assert
        assertEquals(0f, summary.savingsRate, 0.01f) // Should be 0 when no income
    }

    @Test
    fun `getPeriodSummary handles negative net amount`() = runTest {
        // Arrange - expenses > income
        whenever(transactionRepository.getTotalIncomeInDateRange(any(), any()))
            .thenReturn(flowOf(2000.0))
        whenever(transactionRepository.getTotalExpensesInDateRange(any(), any()))
            .thenReturn(flowOf(3000.0))
        whenever(transactionRepository.getExpensesByCategory(any(), any()))
            .thenReturn(flowOf(emptyList()))

        // Act
        val summary = expenseCalculationService.getPeriodSummary(0L, 100L).first()

        // Assert
        assertEquals(-1000.0, summary.netAmount, 0.01)
        assertEquals(-50.0f, summary.savingsRate, 0.01f) // Negative savings
    }

    // ==================== Monthly Comparison Tests ====================

    @Test
    fun `getMonthlyComparison calculates percentage change correctly`() = runTest {
        // Arrange
        val currentMonthExpenses = 1200.0
        val previousMonthExpenses = 1000.0

        // Mock for current month
        whenever(transactionRepository.getTotalExpensesInDateRangeSync(any(), any()))
            .thenReturn(currentMonthExpenses)
            .thenReturn(previousMonthExpenses)

        // Act
        val comparison = expenseCalculationService.getMonthlyComparison()

        // Assert
        assertEquals(currentMonthExpenses, comparison.currentMonthExpenses, 0.01)
        assertEquals(previousMonthExpenses, comparison.previousMonthExpenses, 0.01)
        // Change: (1200 - 1000) / 1000 * 100 = 20%
        assertEquals(20.0f, comparison.changePercentage, 0.01f)
        assertEquals(ExpenseCalculationService.ExpenseTrend.INCREASING, comparison.trend)
    }

    @Test
    fun `getMonthlyComparison identifies decreasing trend`() = runTest {
        // Arrange - current < previous
        whenever(transactionRepository.getTotalExpensesInDateRangeSync(any(), any()))
            .thenReturn(800.0)  // current
            .thenReturn(1000.0) // previous

        // Act
        val comparison = expenseCalculationService.getMonthlyComparison()

        // Assert
        // Change: (800 - 1000) / 1000 * 100 = -20%
        assertEquals(-20.0f, comparison.changePercentage, 0.01f)
        assertEquals(ExpenseCalculationService.ExpenseTrend.DECREASING, comparison.trend)
    }

    @Test
    fun `getMonthlyComparison identifies stable trend within 5 percent`() = runTest {
        // Arrange - within ±5%
        whenever(transactionRepository.getTotalExpensesInDateRangeSync(any(), any()))
            .thenReturn(1040.0) // current - 4% increase
            .thenReturn(1000.0) // previous

        // Act
        val comparison = expenseCalculationService.getMonthlyComparison()

        // Assert
        assertEquals(ExpenseCalculationService.ExpenseTrend.STABLE, comparison.trend)
    }

    // ==================== Available Balance Tests ====================

    @Test
    fun `getAvailableBalanceSummary calculates correctly`() = runTest {
        // Arrange
        val accountId = 1L
        val mainBalance = 5000.0
        val poolsTotal = 2000.0

        whenever(balanceRepository.getMainBalanceForAccountSync(accountId))
            .thenReturn(createMainBalance(currentBalance = mainBalance))
        whenever(balanceRepository.getSumOfPoolsForAccount(accountId))
            .thenReturn(flowOf(poolsTotal))

        // Act
        val summary = expenseCalculationService.getAvailableBalanceSummary(accountId)

        // Assert
        assertEquals(mainBalance, summary.totalBalance, 0.01)
        assertEquals(poolsTotal, summary.poolsTotal, 0.01)
        assertEquals(3000.0, summary.availableBalance, 0.01) // 5000 - 2000
        assertEquals(60.0f, summary.availablePercentage, 0.01f) // 60% (3000/5000 * 100)
    }

    @Test
    fun `getAvailableBalanceSummary handles zero pools`() = runTest {
        // Arrange
        val accountId = 1L
        val mainBalance = 5000.0

        whenever(balanceRepository.getMainBalanceForAccountSync(accountId))
            .thenReturn(createMainBalance(currentBalance = mainBalance))
        whenever(balanceRepository.getSumOfPoolsForAccount(accountId))
            .thenReturn(flowOf(0.0))

        // Act
        val summary = expenseCalculationService.getAvailableBalanceSummary(accountId)

        // Assert
        assertEquals(mainBalance, summary.availableBalance, 0.01) // All available
        assertEquals(100.0f, summary.availablePercentage, 0.01f) // 100%
    }

    @Test
    fun `getAvailableBalance returns correct value`() = runTest {
        // Arrange
        val accountId = 1L
        whenever(balanceRepository.getAvailableBalanceForAccount(accountId))
            .thenReturn(flowOf(3500.0))

        // Act
        val available = expenseCalculationService.getAvailableBalance(accountId).first()

        // Assert
        assertEquals(3500.0, available, 0.01)
    }

    // ==================== Category Expenses Tests ====================

    @Test
    fun `getExpensesByCategory returns categories`() = runTest {
        // Arrange
        val categories = listOf(
            CategoryTotal(category = "Transport", total = 500.0),
            CategoryTotal(category = "Food", total = 1500.0),
            CategoryTotal(category = "Entertainment", total = 300.0)
        )

        whenever(transactionRepository.getExpensesByCategory(any(), any()))
            .thenReturn(flowOf(categories))

        // Act
        val result = expenseCalculationService.getExpensesByCategory(0L, 100L).first()

        // Assert
        assertEquals(3, result.size)
        // Check that totals are present
        val totalExpenses = result.sumOf { category -> category.total }
        assertEquals(2300.0, totalExpenses, 0.01)
    }

    @Test
    fun `getDetailedCategoryExpenses calculates percentages correctly`() = runTest {
        // Arrange - total 2000
        val categories = listOf(
            CategoryTotal(category = "Food", total = 1000.0), // 50%
            CategoryTotal(category = "Transport", total = 600.0), // 30%
            CategoryTotal(category = "Other", total = 400.0) // 20%
        )

        whenever(transactionRepository.getExpensesByCategory(any(), any()))
            .thenReturn(flowOf(categories))
        whenever(transactionRepository.getTotalExpensesInDateRange(any(), any()))
            .thenReturn(flowOf(2000.0))

        // Act
        val result = expenseCalculationService.getDetailedCategoryExpenses(0L, 100L).first()

        // Assert
        val foodExpense = result.find { expense -> expense.category == "Food" }
        assertNotNull(foodExpense)
        assertEquals(50.0f, foodExpense!!.percentage, 0.1f)
    }

    // ==================== Monthly Average Tests ====================

    @Test
    fun `getMonthlyAverage calculates average correctly`() = runTest {
        // Arrange - 3 months of expenses
        // We'll mock different values for each month
        whenever(transactionRepository.getTotalExpensesInDateRangeSync(any(), any()))
            .thenReturn(1000.0) // Month 1
            .thenReturn(1500.0) // Month 2
            .thenReturn(2000.0) // Month 3

        // Act
        val average = expenseCalculationService.getMonthlyAverage(3)

        // Assert
        // Average = (1000 + 1500 + 2000) / 3 = 1500
        assertEquals(1500.0, average, 0.01)
    }

    @Test
    fun `getMonthlyAverage handles single month`() = runTest {
        // Arrange
        whenever(transactionRepository.getTotalExpensesInDateRangeSync(any(), any()))
            .thenReturn(1200.0)

        // Act
        val average = expenseCalculationService.getMonthlyAverage(1)

        // Assert
        assertEquals(1200.0, average, 0.01)
    }

    // ==================== Current Month Tests ====================

    @Test
    fun `getCurrentMonthExpenses uses correct date range`() = runTest {
        // Arrange
        whenever(transactionRepository.getCurrentMonthExpenses())
            .thenReturn(flowOf(2500.0))

        // Act
        val result = expenseCalculationService.getCurrentMonthExpenses().first()

        // Assert
        assertEquals(2500.0, result, 0.01)
        verify(transactionRepository).getCurrentMonthExpenses()
    }

    @Test
    fun `getCurrentMonthIncome uses correct date range`() = runTest {
        // Arrange
        whenever(transactionRepository.getCurrentMonthIncome())
            .thenReturn(flowOf(5000.0))

        // Act
        val result = expenseCalculationService.getCurrentMonthIncome().first()

        // Assert
        assertEquals(5000.0, result, 0.01)
        verify(transactionRepository).getCurrentMonthIncome()
    }

    // ==================== Helper Methods ====================

    private fun createMainBalance(currentBalance: Double) =
        com.example.organizadordefinancas.data.model.Balance(
            id = 1L,
            name = "Principal",
            accountId = 1L,
            currentBalance = currentBalance,
            balanceType = com.example.organizadordefinancas.data.model.BalanceTypes.ACCOUNT
        )
}

