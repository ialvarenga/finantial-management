package com.example.organizadordefinancas.data.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.organizadordefinancas.data.dao.AccountDao
import com.example.organizadordefinancas.data.dao.BalanceDao
import com.example.organizadordefinancas.data.dao.BillDao
import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.dao.TransactionDao
import com.example.organizadordefinancas.data.database.AppDatabase
import com.example.organizadordefinancas.data.model.*
import com.example.organizadordefinancas.data.repository.AccountRepository
import com.example.organizadordefinancas.data.repository.BalanceRepository
import com.example.organizadordefinancas.data.repository.BillRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository
import com.example.organizadordefinancas.service.business.InstallmentService
import com.example.organizadordefinancas.service.business.TransferService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests that test the full flow using an in-memory database.
 * These tests verify that entities, DAOs, repositories, and services work together correctly.
 */
@RunWith(AndroidJUnit4::class)
class FullFlowIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var balanceDao: BalanceDao
    private lateinit var creditCardDao: CreditCardDao
    private lateinit var billDao: BillDao
    private lateinit var transactionDao: TransactionDao

    private lateinit var accountRepository: AccountRepository
    private lateinit var balanceRepository: BalanceRepository
    private lateinit var billRepository: BillRepository
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var installmentService: InstallmentService
    private lateinit var transferService: TransferService

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // For testing only
            .build()

        // Get DAOs
        accountDao = database.accountDao()
        balanceDao = database.balanceDao()
        creditCardDao = database.creditCardDao()
        billDao = database.billDao()
        transactionDao = database.transactionDao()

        // Create repositories
        accountRepository = AccountRepository(accountDao, balanceDao)
        balanceRepository = BalanceRepository(balanceDao, transactionDao)
        billRepository = BillRepository(billDao, creditCardDao, transactionDao)
        transactionRepository = TransactionRepository(transactionDao, balanceDao, billDao)

        // Create services
        installmentService = InstallmentService(transactionRepository, billRepository)
        transferService = TransferService(balanceRepository)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    // ==================== Test 1: Account with Pools Flow ====================

    @Test
    fun accountWithPoolsFlow_createsAccountAndPools_calculatesAvailableBalance() = runTest {
        // Step 1: Create account with initial balance
        val account = Account(
            name = "Personal Account",
            bankName = "Nubank",
            accountType = AccountTypes.CHECKING
        )
        val accountId = accountRepository.createAccountWithInitialBalance(account, 5000.0)
        assertTrue("Account should be created", accountId > 0)

        // Step 2: Verify main balance was created
        val mainBalance = balanceRepository.getMainBalanceForAccountSync(accountId)
        assertNotNull("Main balance should exist", mainBalance)
        assertEquals(5000.0, mainBalance!!.currentBalance, 0.01)
        assertEquals(BalanceTypes.ACCOUNT, mainBalance.balanceType)

        // Step 3: Create 2 pools
        val pool1Id = balanceRepository.createPool(
            accountId = accountId,
            name = "Emergency Fund",
            goalAmount = 3000.0
        )
        val pool2Id = balanceRepository.createPool(
            accountId = accountId,
            name = "Vacation",
            goalAmount = 2000.0
        )
        assertTrue("Pool 1 should be created", pool1Id > 0)
        assertTrue("Pool 2 should be created", pool2Id > 0)

        // Step 4: Transfer money to pools
        val transfer1 = transferService.transferToPool(
            accountId = accountId,
            poolId = pool1Id,
            amount = 1000.0,
            description = "Initial emergency fund"
        )
        assertTrue("Transfer 1 should succeed", transfer1.success)

        val transfer2 = transferService.transferToPool(
            accountId = accountId,
            poolId = pool2Id,
            amount = 500.0,
            description = "Start vacation savings"
        )
        assertTrue("Transfer 2 should succeed", transfer2.success)

        // Step 5: Verify available balance calculation
        val availableBalance = balanceRepository.getAvailableBalanceForAccountSync(accountId)
        // Initial 5000 - 1000 (pool1) - 500 (pool2) = 3500
        assertEquals(3500.0, availableBalance, 0.01)

        // Step 6: Verify transactions were created for transfers
        val transactions = transactionDao.getAllTransactionsSync()
        // Each transfer creates 2 transactions (expense from main, income to pool)
        assertEquals(4, transactions.size)
    }

    @Test
    fun accountWithPoolsFlow_transferToPool_createsLinkedTransactions() = runTest {
        // Setup: Create account with balance
        val account = Account(name = "Test", bankName = "Test Bank", accountType = AccountTypes.CHECKING)
        val accountId = accountRepository.createAccountWithInitialBalance(account, 1000.0)
        val mainBalance = balanceRepository.getMainBalanceForAccountSync(accountId)!!

        // Create pool
        val poolId = balanceRepository.createPool(accountId, "Test Pool")

        // Transfer
        val result = transferService.transferToPool(accountId, poolId, 200.0)

        // Verify
        assertTrue(result.success)
        assertNotNull(result.expenseTransactionId)
        assertNotNull(result.incomeTransactionId)

        // Check expense transaction (from main balance)
        val expenseTransaction = transactionDao.getTransactionByIdSync(result.expenseTransactionId!!)
        assertNotNull(expenseTransaction)
        assertEquals(TransactionType.EXPENSE, expenseTransaction!!.type)
        assertEquals(200.0, expenseTransaction.amount, 0.01)
        assertEquals(mainBalance.id, expenseTransaction.balanceId)

        // Check income transaction (to pool)
        val incomeTransaction = transactionDao.getTransactionByIdSync(result.incomeTransactionId!!)
        assertNotNull(incomeTransaction)
        assertEquals(TransactionType.INCOME, incomeTransaction!!.type)
        assertEquals(200.0, incomeTransaction.amount, 0.01)
        assertEquals(poolId, incomeTransaction.balanceId)
    }

    // ==================== Test 2: Credit Card Purchase Flow ====================

    @Test
    fun creditCardPurchaseFlow_createsBillAndTransaction() = runTest {
        // Step 1: Create credit card
        val creditCard = CreditCard(
            name = "Nubank Platinum",
            cardLimit = 10000.0,
            closingDay = 10,
            dueDay = 15,
            autoGenerateBills = false
        )
        val creditCardId = creditCardDao.insertCreditCard(creditCard)
        assertTrue("Credit card should be created", creditCardId > 0)

        // Step 2: Generate a bill
        val billId = billRepository.generateBillForMonth(creditCardId, 2024, 12)
        assertNotNull("Bill should be generated", billId)
        assertTrue(billId!! > 0)

        // Step 3: Create purchase transaction on the bill
        val transaction = Transaction(
            amount = 150.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            billId = billId,
            category = "Shopping",
            description = "Store purchase"
        )
        val transactionId = transactionDao.insertTransaction(transaction)
        assertTrue(transactionId > 0)

        // Step 4: Verify transaction is assigned to bill
        val billTransactions = transactionDao.getTransactionsByBillIdSync(billId)
        assertEquals(1, billTransactions.size)
        assertEquals(transactionId, billTransactions[0].id)

        // Step 5: Verify bill total calculation
        val billTotal = transactionDao.getBillTotalSync(billId)
        assertEquals(150.0, billTotal, 0.01)
    }

    @Test
    fun creditCardPurchaseFlow_multipleTransactions_calculatesTotal() = runTest {
        // Create credit card and bill
        val creditCardId = creditCardDao.insertCreditCard(
            CreditCard(name = "Test Card", cardLimit = 5000.0, closingDay = 5, dueDay = 10)
        )
        val billId = billRepository.generateBillForMonth(creditCardId, 2024, 12)!!

        // Create multiple transactions
        val amounts = listOf(100.0, 250.0, 75.50, 324.50)
        for (amount in amounts) {
            transactionDao.insertTransaction(
                Transaction(
                    amount = amount,
                    date = System.currentTimeMillis(),
                    type = TransactionType.EXPENSE,
                    billId = billId,
                    category = "Shopping"
                )
            )
        }

        // Verify total
        val billTotal = transactionDao.getBillTotalSync(billId)
        assertEquals(750.0, billTotal, 0.01) // 100 + 250 + 75.50 + 324.50
    }

    // ==================== Test 3: Installment Purchase Flow ====================

    @Test
    fun installmentPurchaseFlow_createsParentAndChildren() = runTest {
        // Step 1: Create credit card and bill
        val creditCardId = creditCardDao.insertCreditCard(
            CreditCard(
                name = "Nubank",
                cardLimit = 10000.0,
                closingDay = 10,
                dueDay = 15
            )
        )
        val billId = billRepository.generateBillForMonth(creditCardId, 2024, 12)!!

        // Step 2: Create 12-installment purchase
        val result = installmentService.createInstallmentPurchase(
            totalAmount = 1200.0,
            installments = 12,
            category = "Electronics",
            description = "New Laptop",
            billId = billId,
            creditCardId = creditCardId
        )

        // Step 3: Verify success
        assertTrue("Installment creation should succeed", result.success)
        assertNotNull(result.parentTransactionId)
        assertEquals(12, result.childTransactionIds.size)

        // Step 4: Verify parent transaction
        val parent = transactionDao.getTransactionByIdSync(result.parentTransactionId!!)
        assertNotNull(parent)
        assertTrue("Parent should be marked as installment parent", parent!!.isInstallmentParent)
        assertEquals(1200.0, parent.amount, 0.01)
        assertEquals(12, parent.totalInstallments)

        // Step 5: Verify child transactions
        val children = transactionDao.getInstallmentChildrenSync(result.parentTransactionId)
        assertEquals(12, children.size)

        // All children should have correct amount
        children.forEach { child ->
            assertEquals(100.0, child.amount, 0.01) // 1200 / 12
            assertFalse(child.isInstallmentParent)
            assertEquals(result.parentTransactionId, child.parentTransactionId)
        }

        // First child should be completed, rest expected
        val sortedChildren = children.sortedBy { it.installmentNumber }
        assertEquals(TransactionStatus.COMPLETED, sortedChildren[0].status)
        for (i in 1 until sortedChildren.size) {
            assertEquals(TransactionStatus.EXPECTED, sortedChildren[i].status)
        }

        // Each child should have correct installment number
        for (i in sortedChildren.indices) {
            assertEquals(i + 1, sortedChildren[i].installmentNumber)
        }
    }

    @Test
    fun installmentPurchaseFlow_expenseTotalExcludesParent() = runTest {
        // Create credit card and bill
        val creditCardId = creditCardDao.insertCreditCard(
            CreditCard(name = "Test", cardLimit = 10000.0, closingDay = 10, dueDay = 15)
        )
        val billId = billRepository.generateBillForMonth(creditCardId, 2024, 12)!!

        // Create installment purchase
        val result = installmentService.createInstallmentPurchase(
            totalAmount = 1200.0,
            installments = 12,
            category = "Shopping",
            billId = billId,
            creditCardId = creditCardId
        )
        assertTrue(result.success)

        // Calculate bill total (should only count children, not parent)
        val billTotal = transactionDao.getBillTotalSync(billId)

        // If all 12 children are on this bill, total should be 1200
        // But only children with this billId count
        // At minimum, first child should be on this bill = 100
        assertTrue("Bill total should count child transactions", billTotal >= 100.0)

        // CRITICAL: Bill total should NOT include the parent (1200)
        // If it was double-counting, total would be 1200 + 100 = 1300 or more
        assertTrue("Should not double-count installment parent", billTotal <= 1200.0)
    }

    @Test
    fun installmentPurchaseFlow_cancelRemaining_updatesStatus() = runTest {
        // Setup
        val creditCardId = creditCardDao.insertCreditCard(
            CreditCard(name = "Test", cardLimit = 10000.0, closingDay = 10, dueDay = 15)
        )
        val billId = billRepository.generateBillForMonth(creditCardId, 2024, 12)!!

        val result = installmentService.createInstallmentPurchase(
            totalAmount = 600.0,
            installments = 6,
            category = "Shopping",
            billId = billId,
            creditCardId = creditCardId
        )

        // Cancel remaining installments
        installmentService.cancelInstallment(result.parentTransactionId!!)

        // Verify
        val children = transactionDao.getInstallmentChildrenSync(result.parentTransactionId)
        val completed = children.filter { it.status == TransactionStatus.COMPLETED }
        val cancelled = children.filter { it.status == TransactionStatus.CANCELLED }

        // First installment was completed, rest should be cancelled
        assertEquals("One installment should remain completed", 1, completed.size)
        assertEquals("Remaining installments should be cancelled", 5, cancelled.size)
    }

    @Test
    fun installmentPurchaseFlow_getSummary_calculatesCorrectly() = runTest {
        // Setup
        val creditCardId = creditCardDao.insertCreditCard(
            CreditCard(name = "Test", cardLimit = 10000.0, closingDay = 10, dueDay = 15)
        )
        val billId = billRepository.generateBillForMonth(creditCardId, 2024, 12)!!

        val result = installmentService.createInstallmentPurchase(
            totalAmount = 1200.0,
            installments = 12,
            category = "Shopping",
            billId = billId,
            creditCardId = creditCardId
        )

        // Get summary
        val summary = installmentService.getInstallmentSummary(result.parentTransactionId!!)

        // Verify
        assertNotNull(summary)
        assertEquals(1200.0, summary!!.totalAmount, 0.01)
        assertEquals(100.0, summary.installmentAmount, 0.01)
        assertEquals(12, summary.totalInstallments)
        assertEquals(1, summary.completedCount) // First installment auto-completed
        assertEquals(11, summary.expectedCount)
        assertEquals(100.0, summary.paidAmount, 0.01)
        assertEquals(1100.0, summary.remainingAmount, 0.01)
    }

    // ==================== Test 4: Bill Payment Flow ====================

    @Test
    fun billPaymentFlow_paidBillUpdatesStatus() = runTest {
        // Create credit card, bill, and some transactions
        val creditCardId = creditCardDao.insertCreditCard(
            CreditCard(name = "Test Card", cardLimit = 5000.0, closingDay = 5, dueDay = 10)
        )
        val billId = billRepository.generateBillForMonth(creditCardId, 2024, 12)!!

        // Add transactions to bill
        transactionDao.insertTransaction(
            Transaction(amount = 300.0, date = System.currentTimeMillis(), type = TransactionType.EXPENSE, billId = billId, category = "Food")
        )
        transactionDao.insertTransaction(
            Transaction(amount = 200.0, date = System.currentTimeMillis(), type = TransactionType.EXPENSE, billId = billId, category = "Transport")
        )

        // Create account for payment source
        val account = Account(name = "Checking", bankName = "Test Bank", accountType = AccountTypes.CHECKING)
        val accountId = accountRepository.createAccountWithInitialBalance(account, 1000.0)
        val mainBalance = balanceRepository.getMainBalanceForAccountSync(accountId)!!

        // Update bill total
        val billTotal = transactionDao.getBillTotalSync(billId)
        assertEquals(500.0, billTotal, 0.01)

        // Pay the bill
        billDao.updateBillPayment(billId, 500.0, BillStatus.PAID, null)

        // Verify bill status
        val updatedBill = billDao.getBillByIdSync(billId)
        assertNotNull(updatedBill)
        assertEquals(BillStatus.PAID, updatedBill!!.status)
        assertEquals(500.0, updatedBill.paidAmount, 0.01)
    }

    // ==================== Test 5: Transfer Between Accounts ====================

    @Test
    fun transferBetweenAccounts_updatesBalances() = runTest {
        // Create two accounts
        val account1Id = accountRepository.createAccountWithInitialBalance(
            Account(name = "Account 1", bankName = "Bank 1", accountType = AccountTypes.CHECKING),
            1000.0
        )
        val account2Id = accountRepository.createAccountWithInitialBalance(
            Account(name = "Account 2", bankName = "Bank 2", accountType = AccountTypes.SAVINGS),
            500.0
        )

        val balance1 = balanceRepository.getMainBalanceForAccountSync(account1Id)!!
        val balance2 = balanceRepository.getMainBalanceForAccountSync(account2Id)!!

        // Transfer 300 from account 1 to account 2
        val result = transferService.transferBetweenBalances(
            fromBalanceId = balance1.id,
            toBalanceId = balance2.id,
            amount = 300.0,
            description = "Transfer to savings"
        )

        assertTrue("Transfer should succeed", result.success)

        // Verify balances updated
        val updatedBalance1 = balanceRepository.getBalanceByIdSync(balance1.id)!!
        val updatedBalance2 = balanceRepository.getBalanceByIdSync(balance2.id)!!

        assertEquals(700.0, updatedBalance1.currentBalance, 0.01) // 1000 - 300
        assertEquals(800.0, updatedBalance2.currentBalance, 0.01) // 500 + 300
    }

    // ==================== Test 6: Expense Total Calculations ====================

    @Test
    fun expenseCalculation_excludesInstallmentParents() = runTest {
        // Create account
        val accountId = accountRepository.createAccountWithInitialBalance(
            Account(name = "Test", bankName = "Test", accountType = AccountTypes.CHECKING),
            10000.0
        )
        val mainBalance = balanceRepository.getMainBalanceForAccountSync(accountId)!!

        // Create regular expense
        transactionDao.insertTransaction(
            Transaction(
                amount = 100.0,
                date = System.currentTimeMillis(),
                type = TransactionType.EXPENSE,
                balanceId = mainBalance.id,
                category = "Food",
                status = TransactionStatus.COMPLETED
            )
        )

        // Create credit card with installment
        val creditCardId = creditCardDao.insertCreditCard(
            CreditCard(name = "Card", cardLimit = 5000.0, closingDay = 10, dueDay = 15)
        )
        val billId = billRepository.generateBillForMonth(creditCardId, 2024, 12)!!

        installmentService.createInstallmentPurchase(
            totalAmount = 1200.0,
            installments = 12,
            category = "Electronics",
            billId = billId,
            creditCardId = creditCardId
        )

        // Get transactions excluding parents
        val transactionsExcludingParents = transactionDao.getTransactionsExcludingParentsSync()

        // Should include: 1 regular expense + 12 installment children
        // Should exclude: 1 installment parent
        val parents = transactionsExcludingParents.filter { it.isInstallmentParent }
        assertTrue("No installment parents should be in the list", parents.isEmpty())
    }
}

