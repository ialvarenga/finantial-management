package com.example.organizadordefinancas.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the new data model entities.
 * These tests don't require a device or Room database.
 */
class NewDataModelUnitTest {

    // ==================== BALANCE TESTS ====================

    @Test
    fun `Balance isPool returns true for pool type`() {
        val balance = Balance(
            name = "Emergency Fund",
            accountId = 1L,
            currentBalance = 1000.0,
            balanceType = BalanceTypes.POOL
        )
        assertTrue(balance.isPool())
        assertFalse(balance.isMainBalance())
    }

    @Test
    fun `Balance isMainBalance returns true for account type`() {
        val balance = Balance(
            name = "Principal",
            accountId = 1L,
            currentBalance = 5000.0,
            balanceType = BalanceTypes.ACCOUNT
        )
        assertTrue(balance.isMainBalance())
        assertFalse(balance.isPool())
    }

    @Test
    fun `Balance getGoalProgress calculates correctly`() {
        val balance = Balance(
            name = "Vacation",
            accountId = 1L,
            currentBalance = 500.0,
            balanceType = BalanceTypes.POOL,
            goalAmount = 1000.0
        )
        assertEquals(0.5, balance.getGoalProgress()!!, 0.01)
    }

    @Test
    fun `Balance getGoalProgress returns null when no goal`() {
        val balance = Balance(
            name = "Principal",
            accountId = 1L,
            currentBalance = 5000.0,
            balanceType = BalanceTypes.ACCOUNT,
            goalAmount = null
        )
        assertNull(balance.getGoalProgress())
    }

    @Test
    fun `Balance getGoalProgress handles over 100 percent`() {
        val balance = Balance(
            name = "Emergency",
            accountId = 1L,
            currentBalance = 2500.0,
            balanceType = BalanceTypes.POOL,
            goalAmount = 2000.0
        )
        assertEquals(1.25, balance.getGoalProgress()!!, 0.01)
    }

    // ==================== BILL TESTS ====================

    @Test
    fun `Bill getRemainingAmount calculates correctly`() {
        val bill = Bill(
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            totalAmount = 500.0,
            paidAmount = 200.0
        )
        assertEquals(300.0, bill.getRemainingAmount(), 0.01)
    }

    @Test
    fun `Bill isFullyPaid returns true when paid equals total`() {
        val bill = Bill(
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            totalAmount = 500.0,
            paidAmount = 500.0
        )
        assertTrue(bill.isFullyPaid())
    }

    @Test
    fun `Bill isFullyPaid returns false when partially paid`() {
        val bill = Bill(
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            totalAmount = 500.0,
            paidAmount = 250.0
        )
        assertFalse(bill.isFullyPaid())
    }

    @Test
    fun `Bill calculateStatus returns OPEN when unpaid and not overdue`() {
        val now = System.currentTimeMillis()
        val bill = Bill(
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = now,
            dueDate = now + (10L * 24 * 60 * 60 * 1000), // 10 days from now
            totalAmount = 500.0,
            paidAmount = 0.0
        )
        assertEquals(BillStatus.OPEN, bill.calculateStatus(now))
    }

    @Test
    fun `Bill calculateStatus returns PARTIAL when partially paid`() {
        val now = System.currentTimeMillis()
        val bill = Bill(
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = now,
            dueDate = now + (10L * 24 * 60 * 60 * 1000),
            totalAmount = 500.0,
            paidAmount = 250.0
        )
        assertEquals(BillStatus.PARTIAL, bill.calculateStatus(now))
    }

    @Test
    fun `Bill calculateStatus returns PAID when fully paid`() {
        val now = System.currentTimeMillis()
        val bill = Bill(
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = now,
            dueDate = now - (10L * 24 * 60 * 60 * 1000), // Past due
            totalAmount = 500.0,
            paidAmount = 500.0
        )
        assertEquals(BillStatus.PAID, bill.calculateStatus(now))
    }

    @Test
    fun `Bill calculateStatus returns OVERDUE when past due and not paid`() {
        val now = System.currentTimeMillis()
        val bill = Bill(
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = now - (20L * 24 * 60 * 60 * 1000),
            dueDate = now - (10L * 24 * 60 * 60 * 1000), // 10 days ago
            totalAmount = 500.0,
            paidAmount = 0.0
        )
        assertEquals(BillStatus.OVERDUE, bill.calculateStatus(now))
    }

    @Test
    fun `Bill isOverdue returns true when past due and not fully paid`() {
        val now = System.currentTimeMillis()
        val bill = Bill(
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = now - (20L * 24 * 60 * 60 * 1000),
            dueDate = now - (10L * 24 * 60 * 60 * 1000),
            totalAmount = 500.0,
            paidAmount = 0.0
        )
        assertTrue(bill.isOverdue(now))
    }

    // ==================== TRANSACTION TESTS ====================

    @Test
    fun `Transaction isIncome returns true for income type`() {
        val tx = Transaction(
            amount = 5000.0,
            date = System.currentTimeMillis(),
            type = TransactionType.INCOME
        )
        assertTrue(tx.isIncome())
        assertFalse(tx.isExpense())
    }

    @Test
    fun `Transaction isExpense returns true for expense type`() {
        val tx = Transaction(
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE
        )
        assertTrue(tx.isExpense())
        assertFalse(tx.isIncome())
    }

    @Test
    fun `Transaction isCompleted returns true for completed status`() {
        val tx = Transaction(
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED
        )
        assertTrue(tx.isCompleted())
        assertFalse(tx.isExpected())
    }

    @Test
    fun `Transaction isExpected returns true for expected status`() {
        val tx = Transaction(
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.EXPECTED
        )
        assertTrue(tx.isExpected())
        assertFalse(tx.isCompleted())
    }

    @Test
    fun `Transaction isInstallmentChild returns true for child transaction`() {
        val tx = Transaction(
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            parentTransactionId = 1L,
            isInstallmentParent = false,
            installmentNumber = 1,
            totalInstallments = 12
        )
        assertTrue(tx.isInstallmentChild())
    }

    @Test
    fun `Transaction isInstallmentChild returns false for parent transaction`() {
        val tx = Transaction(
            amount = 1200.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            isInstallmentParent = true,
            totalInstallments = 12,
            installmentAmount = 100.0
        )
        assertFalse(tx.isInstallmentChild())
    }

    @Test
    fun `Transaction isRecurring returns true for monthly frequency`() {
        val tx = Transaction(
            amount = 150.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            frequency = TransactionFrequency.MONTHLY
        )
        assertTrue(tx.isRecurring())
    }

    @Test
    fun `Transaction isRecurring returns false for once frequency`() {
        val tx = Transaction(
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            frequency = TransactionFrequency.ONCE
        )
        assertFalse(tx.isRecurring())
    }

    @Test
    fun `Transaction isCreditCardTransaction returns true when billId set`() {
        val tx = Transaction(
            amount = 50.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            billId = 1L
        )
        assertTrue(tx.isCreditCardTransaction())
    }

    @Test
    fun `Transaction getInstallmentDisplayText formats correctly`() {
        val tx = Transaction(
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            parentTransactionId = 1L,
            isInstallmentParent = false,
            installmentNumber = 3,
            totalInstallments = 12
        )
        assertEquals("3/12", tx.getInstallmentDisplayText())
    }

    @Test
    fun `Transaction getInstallmentDisplayText returns null for non-installment`() {
        val tx = Transaction(
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE
        )
        assertNull(tx.getInstallmentDisplayText())
    }

    // ==================== ACCOUNT TYPES TESTS ====================

    @Test
    fun `AccountTypes constants are correct`() {
        assertEquals("checking", AccountTypes.CHECKING)
        assertEquals("savings", AccountTypes.SAVINGS)
        assertEquals("investment", AccountTypes.INVESTMENT)
        assertEquals("wallet", AccountTypes.WALLET)
    }

    // ==================== BALANCE TYPES TESTS ====================

    @Test
    fun `BalanceTypes constants are correct`() {
        assertEquals("account", BalanceTypes.ACCOUNT)
        assertEquals("pool", BalanceTypes.POOL)
    }

    // ==================== BILL STATUS TESTS ====================

    @Test
    fun `BillStatus constants are correct`() {
        assertEquals("open", BillStatus.OPEN)
        assertEquals("paid", BillStatus.PAID)
        assertEquals("partial", BillStatus.PARTIAL)
        assertEquals("overdue", BillStatus.OVERDUE)
    }

    // ==================== TRANSACTION STATUS TESTS ====================

    @Test
    fun `TransactionStatus constants are correct`() {
        assertEquals("expected", TransactionStatus.EXPECTED)
        assertEquals("completed", TransactionStatus.COMPLETED)
        assertEquals("cancelled", TransactionStatus.CANCELLED)
    }

    // ==================== TRANSACTION TYPE TESTS ====================

    @Test
    fun `TransactionType constants are correct`() {
        assertEquals("income", TransactionType.INCOME)
        assertEquals("expense", TransactionType.EXPENSE)
    }

    // ==================== TRANSACTION FREQUENCY TESTS ====================

    @Test
    fun `TransactionFrequency constants are correct`() {
        assertEquals("once", TransactionFrequency.ONCE)
        assertEquals("weekly", TransactionFrequency.WEEKLY)
        assertEquals("biweekly", TransactionFrequency.BIWEEKLY)
        assertEquals("monthly", TransactionFrequency.MONTHLY)
        assertEquals("quarterly", TransactionFrequency.QUARTERLY)
        assertEquals("semiannual", TransactionFrequency.SEMIANNUAL)
        assertEquals("annual", TransactionFrequency.ANNUAL)
    }

    // ==================== TRANSACTION CATEGORY TESTS ====================

    @Test
    fun `TransactionCategory getExpenseCategories returns expected list`() {
        val categories = TransactionCategory.getExpenseCategories()
        assertTrue(categories.contains(TransactionCategory.FOOD))
        assertTrue(categories.contains(TransactionCategory.TRANSPORT))
        assertTrue(categories.contains(TransactionCategory.HOUSING))
        assertTrue(categories.contains(TransactionCategory.CREDIT_CARD_PAYMENT))
    }

    @Test
    fun `TransactionCategory getIncomeCategories returns expected list`() {
        val categories = TransactionCategory.getIncomeCategories()
        assertTrue(categories.contains(TransactionCategory.SALARY))
        assertTrue(categories.contains(TransactionCategory.FREELANCE))
        assertTrue(categories.contains(TransactionCategory.BONUS))
        assertTrue(categories.contains(TransactionCategory.REFUND))
    }
}

