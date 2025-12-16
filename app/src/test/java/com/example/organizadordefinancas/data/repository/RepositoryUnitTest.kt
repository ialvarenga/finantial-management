package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.AccountDao
import com.example.organizadordefinancas.data.dao.BalanceDao
import com.example.organizadordefinancas.data.dao.BillDao
import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.dao.TransactionDao
import com.example.organizadordefinancas.data.model.Account
import com.example.organizadordefinancas.data.model.AccountTypes
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.BalanceTypes
import com.example.organizadordefinancas.data.model.Bill
import com.example.organizadordefinancas.data.model.BillStatus
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for the new data model repositories.
 * These tests use mocked DAOs to test repository logic without database access.
 */
class RepositoryUnitTest {

    private lateinit var accountDao: AccountDao
    private lateinit var balanceDao: BalanceDao
    private lateinit var billDao: BillDao
    private lateinit var creditCardDao: CreditCardDao
    private lateinit var transactionDao: TransactionDao

    private lateinit var accountRepository: AccountRepository
    private lateinit var balanceRepository: BalanceRepository
    private lateinit var billRepository: BillRepository
    private lateinit var transactionRepository: TransactionRepository

    @Before
    fun setup() {
        accountDao = mock()
        balanceDao = mock()
        billDao = mock()
        creditCardDao = mock()
        transactionDao = mock()

        accountRepository = AccountRepository(accountDao, balanceDao)
        balanceRepository = BalanceRepository(balanceDao, transactionDao)
        billRepository = BillRepository(billDao, creditCardDao, transactionDao)
        transactionRepository = TransactionRepository(transactionDao, balanceDao, billDao)
    }

    // ==================== Account Repository Tests ====================

    @Test
    fun `createAccount creates account and main balance`() = runTest {
        // Arrange
        val account = Account(
            name = "My Checking",
            bankName = "Nubank",
            accountType = AccountTypes.CHECKING
        )
        whenever(accountDao.insertAccount(any())).thenReturn(1L)
        whenever(balanceDao.insertBalance(any())).thenReturn(1L)

        // Act
        val accountId = accountRepository.createAccount(account)

        // Assert
        assertEquals(1L, accountId)
        verify(accountDao).insertAccount(any())
        verify(balanceDao).insertBalance(argThat { balance ->
            balance.accountId == 1L &&
            balance.balanceType == BalanceTypes.ACCOUNT &&
            balance.name == "Principal"
        })
    }

    @Test
    fun `createAccountWithInitialBalance creates account with correct balance`() = runTest {
        // Arrange
        val account = Account(
            name = "Savings",
            bankName = "Itaú",
            accountType = AccountTypes.SAVINGS
        )
        whenever(accountDao.insertAccount(any())).thenReturn(2L)
        whenever(balanceDao.insertBalance(any())).thenReturn(2L)

        // Act
        val accountId = accountRepository.createAccountWithInitialBalance(account, 1000.0)

        // Assert
        assertEquals(2L, accountId)
        verify(balanceDao).insertBalance(argThat { balance ->
            balance.accountId == 2L &&
            balance.currentBalance == 1000.0 &&
            balance.balanceType == BalanceTypes.ACCOUNT
        })
    }

    // ==================== Balance Repository Tests ====================

    @Test
    fun `createPool creates pool balance with correct type`() = runTest {
        // Arrange
        whenever(balanceDao.insertBalance(any())).thenReturn(5L)

        // Act
        val poolId = balanceRepository.createPool(
            accountId = 1L,
            name = "Emergency Fund",
            goalAmount = 5000.0,
            color = 0xFF00FF00
        )

        // Assert
        assertEquals(5L, poolId)
        verify(balanceDao).insertBalance(argThat { balance ->
            balance.name == "Emergency Fund" &&
            balance.accountId == 1L &&
            balance.balanceType == BalanceTypes.POOL &&
            balance.goalAmount == 5000.0 &&
            balance.color == 0xFF00FF00
        })
    }

    @Test
    fun `transferBetweenBalances fails with insufficient balance`() = runTest {
        // Arrange
        val fromBalance = Balance(
            id = 1L,
            name = "Main",
            accountId = 1L,
            currentBalance = 100.0,
            balanceType = BalanceTypes.ACCOUNT
        )
        val toBalance = Balance(
            id = 2L,
            name = "Pool",
            accountId = 1L,
            currentBalance = 0.0,
            balanceType = BalanceTypes.POOL
        )
        whenever(balanceDao.getBalanceByIdSync(1L)).thenReturn(fromBalance)
        whenever(balanceDao.getBalanceByIdSync(2L)).thenReturn(toBalance)

        // Act
        val result = balanceRepository.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 500.0, // More than available
            validateSufficientBalance = true
        )

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Insufficient"))
    }

    @Test
    fun `transferBetweenBalances creates two linked transactions`() = runTest {
        // Arrange
        val fromBalance = Balance(
            id = 1L,
            name = "Main",
            accountId = 1L,
            currentBalance = 1000.0,
            balanceType = BalanceTypes.ACCOUNT
        )
        val toBalance = Balance(
            id = 2L,
            name = "Pool",
            accountId = 1L,
            currentBalance = 0.0,
            balanceType = BalanceTypes.POOL
        )
        whenever(balanceDao.getBalanceByIdSync(1L)).thenReturn(fromBalance)
        whenever(balanceDao.getBalanceByIdSync(2L)).thenReturn(toBalance)
        whenever(transactionDao.insertTransaction(any())).thenReturn(10L, 11L)

        // Act
        val result = balanceRepository.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 200.0
        )

        // Assert
        assertTrue(result.success)
        assertEquals(10L, result.expenseTransactionId)
        assertEquals(11L, result.incomeTransactionId)
        verify(transactionDao, times(2)).insertTransaction(any())
        verify(balanceDao).adjustBalance(eq(1L), eq(-200.0), any())
        verify(balanceDao).adjustBalance(eq(2L), eq(200.0), any())
    }

    @Test
    fun `transferBetweenBalances fails with negative amount`() = runTest {
        // Act
        val result = balanceRepository.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = -100.0
        )

        // Assert
        assertFalse(result.success)
        assertEquals("Transfer amount must be positive", result.errorMessage)
    }

    // ==================== Bill Repository Tests ====================

    @Test
    fun `recordBillPayment updates bill to paid when fully paid`() = runTest {
        // Arrange
        val bill = Bill(
            id = 1L,
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            totalAmount = 500.0,
            paidAmount = 0.0,
            status = BillStatus.OPEN
        )
        whenever(billDao.getBillByIdSync(1L)).thenReturn(bill)

        // Act
        val result = billRepository.recordBillPayment(
            billId = 1L,
            paymentAmount = 500.0,
            paymentTransactionId = 100L
        )

        // Assert
        assertTrue(result.success)
        assertEquals(BillStatus.PAID, result.newStatus)
        verify(billDao).updateBillPayment(1L, 500.0, BillStatus.PAID, 100L)
    }

    @Test
    fun `recordBillPayment updates bill to partial when partially paid`() = runTest {
        // Arrange
        val bill = Bill(
            id = 1L,
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            totalAmount = 500.0,
            paidAmount = 0.0,
            status = BillStatus.OPEN
        )
        whenever(billDao.getBillByIdSync(1L)).thenReturn(bill)

        // Act
        val result = billRepository.recordBillPayment(
            billId = 1L,
            paymentAmount = 200.0
        )

        // Assert
        assertTrue(result.success)
        assertEquals(BillStatus.PARTIAL, result.newStatus)
        verify(billDao).updateBillPayment(1L, 200.0, BillStatus.PARTIAL, null)
    }

    @Test
    fun `recordBillPayment fails with zero amount`() = runTest {
        // Arrange
        val bill = Bill(
            id = 1L,
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            totalAmount = 500.0,
            paidAmount = 0.0,
            status = BillStatus.OPEN
        )
        whenever(billDao.getBillByIdSync(1L)).thenReturn(bill)

        // Act
        val result = billRepository.recordBillPayment(
            billId = 1L,
            paymentAmount = 0.0
        )

        // Assert
        assertFalse(result.success)
        assertEquals("Payment amount must be positive", result.errorMessage)
    }

    @Test
    fun `generateBillForMonth does not create duplicate bill`() = runTest {
        // Arrange
        val existingBill = Bill(
            id = 1L,
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(),
            totalAmount = 0.0,
            paidAmount = 0.0,
            status = BillStatus.OPEN
        )
        whenever(billDao.getBillForMonthSync(1L, 2024, 1)).thenReturn(existingBill)

        // Act
        val result = billRepository.generateBillForMonth(1L, 2024, 1)

        // Assert
        assertNull(result) // Should return null because bill already exists
        verify(billDao, never()).insertBill(any())
    }

    // ==================== Transaction Repository Tests ====================

    @Test
    fun `insertTransaction updates balance for completed income`() = runTest {
        // Arrange
        val transaction = Transaction(
            id = 0,
            amount = 1000.0,
            date = System.currentTimeMillis(),
            type = TransactionType.INCOME,
            status = TransactionStatus.COMPLETED,
            balanceId = 1L,
            category = "Salário"
        )
        whenever(transactionDao.insertTransaction(any())).thenReturn(1L)

        // Act
        val transactionId = transactionRepository.insertTransaction(transaction)

        // Assert
        assertEquals(1L, transactionId)
        verify(balanceDao).adjustBalance(eq(1L), eq(1000.0), any()) // Income adds to balance
    }

    @Test
    fun `insertTransaction updates balance for completed expense`() = runTest {
        // Arrange
        val transaction = Transaction(
            id = 0,
            amount = 50.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            balanceId = 1L,
            category = "Alimentação"
        )
        whenever(transactionDao.insertTransaction(any())).thenReturn(2L)

        // Act
        val transactionId = transactionRepository.insertTransaction(transaction)

        // Assert
        assertEquals(2L, transactionId)
        verify(balanceDao).adjustBalance(eq(1L), eq(-50.0), any()) // Expense subtracts from balance
    }

    @Test
    fun `insertTransaction does not update balance for expected transaction`() = runTest {
        // Arrange
        val transaction = Transaction(
            id = 0,
            amount = 1000.0,
            date = System.currentTimeMillis(),
            type = TransactionType.INCOME,
            status = TransactionStatus.EXPECTED, // Expected, not completed
            balanceId = 1L,
            category = "Salário"
        )
        whenever(transactionDao.insertTransaction(any())).thenReturn(3L)

        // Act
        transactionRepository.insertTransaction(transaction)

        // Assert
        verify(balanceDao, never()).adjustBalance(any(), any(), any())
    }

    @Test
    fun `createInstallmentPurchase fails with less than 2 installments`() = runTest {
        // Act
        val result = transactionRepository.createInstallmentPurchase(
            totalAmount = 1200.0,
            installments = 1, // Invalid - must be at least 2
            category = "Compras",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertFalse(result.success)
        assertTrue(result.errorMessage!!.contains("at least 2"))
    }

    @Test
    fun `createInstallmentPurchase fails with zero amount`() = runTest {
        // Act
        val result = transactionRepository.createInstallmentPurchase(
            totalAmount = 0.0,
            installments = 12,
            category = "Compras",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertFalse(result.success)
        assertEquals("Total amount must be positive", result.errorMessage)
    }

    @Test
    fun `createInstallmentPurchase creates parent and children`() = runTest {
        // Arrange
        val bill = Bill(
            id = 1L,
            creditCardId = 1L,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis()
        )
        whenever(billDao.getBillByIdSync(1L)).thenReturn(bill)
        whenever(billDao.getBillForMonthSync(any(), any(), any())).thenReturn(null)
        var transactionIdCounter = 1L
        whenever(transactionDao.insertTransaction(any())).thenAnswer {
            transactionIdCounter++
        }
        whenever(transactionDao.getBillTotalSync(any())).thenReturn(100.0)

        // Act
        val result = transactionRepository.createInstallmentPurchase(
            totalAmount = 1200.0,
            installments = 12,
            category = "Eletrônicos",
            description = "iPhone",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertTrue(result.success)
        assertNotNull(result.parentTransactionId)
        assertEquals(12, result.childTransactionIds.size)

        // Verify parent transaction was created with isInstallmentParent = true
        verify(transactionDao).insertTransaction(argThat { tx ->
            tx.isInstallmentParent &&
            tx.amount == 1200.0 &&
            tx.totalInstallments == 12 &&
            tx.installmentAmount == 100.0
        })
    }

    @Test
    fun `getInstallmentSummary returns null for non-parent transaction`() = runTest {
        // Arrange
        val transaction = Transaction(
            id = 1L,
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            isInstallmentParent = false
        )
        whenever(transactionDao.getTransactionByIdSync(1L)).thenReturn(transaction)

        // Act
        val summary = transactionRepository.getInstallmentSummary(1L)

        // Assert
        assertNull(summary)
    }

    @Test
    fun `getInstallmentSummary calculates correct values`() = runTest {
        // Arrange
        val parentTransaction = Transaction(
            id = 1L,
            amount = 1200.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            isInstallmentParent = true,
            totalInstallments = 12,
            installmentAmount = 100.0
        )
        whenever(transactionDao.getTransactionByIdSync(1L)).thenReturn(parentTransaction)
        whenever(transactionDao.countCompletedInstallments(1L)).thenReturn(3)
        whenever(transactionDao.countExpectedInstallments(1L)).thenReturn(9)

        // Act
        val summary = transactionRepository.getInstallmentSummary(1L)

        // Assert
        assertNotNull(summary)
        assertEquals(1200.0, summary!!.totalAmount, 0.01)
        assertEquals(100.0, summary.installmentAmount, 0.01)
        assertEquals(12, summary.totalInstallments)
        assertEquals(3, summary.completedCount)
        assertEquals(9, summary.expectedCount)
        assertEquals(300.0, summary.paidAmount, 0.01)
        assertEquals(900.0, summary.remainingAmount, 0.01)
    }

    @Test
    fun `deleteTransaction reverses balance for completed income`() = runTest {
        // Arrange
        val transaction = Transaction(
            id = 1L,
            amount = 500.0,
            date = System.currentTimeMillis(),
            type = TransactionType.INCOME,
            status = TransactionStatus.COMPLETED,
            balanceId = 1L
        )

        // Act
        transactionRepository.deleteTransaction(transaction)

        // Assert
        verify(balanceDao).adjustBalance(eq(1L), eq(-500.0), any()) // Reverse the income
        verify(transactionDao).deleteTransaction(transaction)
    }

    @Test
    fun `deleteTransaction reverses balance for completed expense`() = runTest {
        // Arrange
        val transaction = Transaction(
            id = 2L,
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            balanceId = 1L
        )

        // Act
        transactionRepository.deleteTransaction(transaction)

        // Assert
        verify(balanceDao).adjustBalance(eq(1L), eq(100.0), any()) // Reverse the expense (add back)
        verify(transactionDao).deleteTransaction(transaction)
    }
}

