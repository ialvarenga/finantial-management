package com.example.organizadordefinancas.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.organizadordefinancas.data.dao.AccountDao
import com.example.organizadordefinancas.data.dao.BalanceDao
import com.example.organizadordefinancas.data.dao.BillDao
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for the new data model (Phase 1 migration).
 * Tests Account, Balance, Bill, and Transaction entities and DAOs.
 */
@RunWith(AndroidJUnit4::class)
class NewDataModelTest {

    private lateinit var db: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var balanceDao: BalanceDao
    private lateinit var billDao: BillDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountDao = db.accountDao()
        balanceDao = db.balanceDao()
        billDao = db.billDao()
        transactionDao = db.transactionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // ==================== ACCOUNT TESTS ====================

    @Test
    fun testInsertAndRetrieveAccount() = runBlocking {
        val account = Account(
            name = "My Checking",
            bankName = "Nubank",
            accountType = AccountTypes.CHECKING,
            color = 0xFF6200EE
        )

        val accountId = accountDao.insertAccount(account)
        assertTrue("Account ID should be > 0", accountId > 0)

        val retrieved = accountDao.getAccountByIdSync(accountId)
        assertNotNull("Retrieved account should not be null", retrieved)
        assertEquals("Nubank", retrieved?.bankName)
        assertEquals("My Checking", retrieved?.name)
        assertEquals(AccountTypes.CHECKING, retrieved?.accountType)
    }

    @Test
    fun testGetActiveAccounts() = runBlocking {
        accountDao.insertAccount(Account(name = "Active 1", bankName = "Bank1", isActive = true))
        accountDao.insertAccount(Account(name = "Inactive", bankName = "Bank2", isActive = false))
        accountDao.insertAccount(Account(name = "Active 2", bankName = "Bank3", isActive = true))

        val activeAccounts = accountDao.getActiveAccounts().first()
        assertEquals("Should have 2 active accounts", 2, activeAccounts.size)
    }

    // ==================== BALANCE TESTS ====================

    @Test
    fun testInsertAndRetrieveBalance() = runBlocking {
        // First create an account
        val accountId = accountDao.insertAccount(
            Account(name = "Test Account", bankName = "Nubank")
        )

        // Create main balance
        val mainBalance = Balance(
            name = "Principal",
            accountId = accountId,
            currentBalance = 5000.0,
            balanceType = BalanceTypes.ACCOUNT
        )

        val balanceId = balanceDao.insertBalance(mainBalance)
        assertTrue("Balance ID should be > 0", balanceId > 0)

        val retrieved = balanceDao.getBalanceByIdSync(balanceId)
        assertNotNull("Retrieved balance should not be null", retrieved)
        assertEquals(5000.0, retrieved?.currentBalance ?: 0.0, 0.01)
        assertEquals(BalanceTypes.ACCOUNT, retrieved?.balanceType)
    }

    @Test
    fun testAccountWithPools() = runBlocking {
        // Create account
        val accountId = accountDao.insertAccount(
            Account(name = "Nubank Checking", bankName = "Nubank")
        )

        // Create main balance: R$ 5,000
        balanceDao.insertBalance(Balance(
            name = "Principal",
            accountId = accountId,
            currentBalance = 5000.0,
            balanceType = BalanceTypes.ACCOUNT
        ))

        // Create pools
        balanceDao.insertBalance(Balance(
            name = "Emergency Fund",
            accountId = accountId,
            currentBalance = 1000.0,
            balanceType = BalanceTypes.POOL,
            goalAmount = 2000.0
        ))

        balanceDao.insertBalance(Balance(
            name = "Vacation",
            accountId = accountId,
            currentBalance = 500.0,
            balanceType = BalanceTypes.POOL,
            goalAmount = 3000.0
        ))

        // Test pool sum
        val poolSum = balanceDao.getSumOfPoolsForAccountSync(accountId)
        assertEquals("Pool sum should be 1500", 1500.0, poolSum, 0.01)

        // Test available balance: 5000 - 1500 = 3500
        val availableBalance = balanceDao.getAvailableBalanceForAccountSync(accountId)
        assertEquals("Available balance should be 3500", 3500.0, availableBalance, 0.01)

        // Test get pools
        val pools = balanceDao.getPoolsForAccount(accountId).first()
        assertEquals("Should have 2 pools", 2, pools.size)
    }

    @Test
    fun testBalanceGoalProgress() = runBlocking {
        val accountId = accountDao.insertAccount(
            Account(name = "Test", bankName = "Test Bank")
        )

        val balance = Balance(
            name = "Savings Goal",
            accountId = accountId,
            currentBalance = 500.0,
            balanceType = BalanceTypes.POOL,
            goalAmount = 1000.0
        )

        balanceDao.insertBalance(balance)
        val retrieved = balanceDao.getBalanceByIdSync(1L)

        val progress = retrieved?.getGoalProgress()
        assertNotNull("Progress should not be null", progress)
        assertEquals("Progress should be 50%", 0.5, progress!!, 0.01)
    }

    // ==================== TRANSACTION TESTS ====================

    @Test
    fun testInsertAndRetrieveTransaction() = runBlocking {
        val accountId = accountDao.insertAccount(
            Account(name = "Test", bankName = "Nubank")
        )
        val balanceId = balanceDao.insertBalance(
            Balance(name = "Principal", accountId = accountId, currentBalance = 1000.0)
        )

        val transaction = Transaction(
            amount = 100.0,
            date = System.currentTimeMillis(),
            balanceId = balanceId,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Alimentação",
            description = "Groceries"
        )

        val txId = transactionDao.insertTransaction(transaction)
        assertTrue("Transaction ID should be > 0", txId > 0)

        val retrieved = transactionDao.getTransactionByIdSync(txId)
        assertNotNull(retrieved)
        assertEquals(100.0, retrieved?.amount ?: 0.0, 0.01)
        assertEquals(TransactionType.EXPENSE, retrieved?.type)
    }

    @Test
    fun testInstallmentTransactions() = runBlocking {
        // Create parent transaction (total purchase)
        val parentTransaction = Transaction(
            amount = 1200.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Electronics",
            description = "iPhone 15 Pro",
            isInstallmentParent = true,
            totalInstallments = 12,
            installmentAmount = 100.0
        )

        val parentId = transactionDao.insertTransaction(parentTransaction)

        // Create 12 child transactions (installments)
        val childTransactions = (1..12).map { installmentNumber ->
            Transaction(
                amount = 100.0,
                date = System.currentTimeMillis() + (installmentNumber * 30L * 24 * 60 * 60 * 1000),
                type = TransactionType.EXPENSE,
                status = if (installmentNumber == 1) TransactionStatus.COMPLETED else TransactionStatus.EXPECTED,
                category = "Electronics",
                description = "iPhone 15 Pro",
                parentTransactionId = parentId,
                isInstallmentParent = false,
                installmentNumber = installmentNumber,
                totalInstallments = 12
            )
        }

        transactionDao.insertTransactions(childTransactions)

        // Verify children
        val children = transactionDao.getInstallmentChildrenSync(parentId)
        assertEquals("Should have 12 installments", 12, children.size)

        // Test completed count
        val completedCount = transactionDao.countCompletedInstallments(parentId)
        assertEquals("Should have 1 completed installment", 1, completedCount)

        // Test expected count
        val expectedCount = transactionDao.countExpectedInstallments(parentId)
        assertEquals("Should have 11 expected installments", 11, expectedCount)
    }

    @Test
    fun testExpenseTotalsExcludeParents() = runBlocking {
        val now = System.currentTimeMillis()
        val startOfMonth = now - (30L * 24 * 60 * 60 * 1000)
        val endOfMonth = now + (30L * 24 * 60 * 60 * 1000)

        // Insert a regular expense
        transactionDao.insertTransaction(Transaction(
            amount = 50.0,
            date = now,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Food",
            isInstallmentParent = false
        ))

        // Insert an installment parent (should NOT be counted)
        val parentId = transactionDao.insertTransaction(Transaction(
            amount = 600.0,
            date = now,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Electronics",
            isInstallmentParent = true,
            totalInstallments = 6,
            installmentAmount = 100.0
        ))

        // Insert first installment child (should be counted)
        transactionDao.insertTransaction(Transaction(
            amount = 100.0,
            date = now,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Electronics",
            parentTransactionId = parentId,
            isInstallmentParent = false,
            installmentNumber = 1,
            totalInstallments = 6
        ))

        // Total should be 50 + 100 = 150 (NOT 50 + 600 + 100 = 750)
        val total = transactionDao.getTotalExpensesInDateRangeSync(startOfMonth, endOfMonth)
        assertEquals("Total should be 150 (excluding parent)", 150.0, total, 0.01)
    }

    @Test
    fun testIncomeAndExpenseBalance() = runBlocking {
        val now = System.currentTimeMillis()
        val startOfMonth = now - (30L * 24 * 60 * 60 * 1000)
        val endOfMonth = now + (30L * 24 * 60 * 60 * 1000)

        // Add income
        transactionDao.insertTransaction(Transaction(
            amount = 5000.0,
            date = now,
            type = TransactionType.INCOME,
            status = TransactionStatus.COMPLETED,
            category = "Salário"
        ))

        // Add expenses
        transactionDao.insertTransaction(Transaction(
            amount = 1500.0,
            date = now,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Moradia"
        ))

        transactionDao.insertTransaction(Transaction(
            amount = 500.0,
            date = now,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Alimentação"
        ))

        val totalIncome = transactionDao.getTotalIncomeInDateRangeSync(startOfMonth, endOfMonth)
        val totalExpense = transactionDao.getTotalExpensesInDateRangeSync(startOfMonth, endOfMonth)

        assertEquals("Total income should be 5000", 5000.0, totalIncome, 0.01)
        assertEquals("Total expenses should be 2000", 2000.0, totalExpense, 0.01)
    }

    // ==================== BILL TESTS ====================

    @Test
    fun testInsertAndRetrieveBill() = runBlocking {
        // First insert a credit card using the existing DAO
        val creditCardDao = db.creditCardDao()
        val cardId = creditCardDao.insertCreditCard(
            com.example.organizadordefinancas.data.model.CreditCard(
                name = "Nubank",
                cardLimit = 5000.0,
                dueDay = 15,
                closingDay = 10
            )
        )

        val bill = Bill(
            creditCardId = cardId,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000),
            totalAmount = 500.0,
            paidAmount = 0.0,
            status = BillStatus.OPEN
        )

        val billId = billDao.insertBill(bill)
        assertTrue("Bill ID should be > 0", billId > 0)

        val retrieved = billDao.getBillByIdSync(billId)
        assertNotNull(retrieved)
        assertEquals(500.0, retrieved?.totalAmount ?: 0.0, 0.01)
        assertEquals(BillStatus.OPEN, retrieved?.status)
    }

    @Test
    fun testBillPayment() = runBlocking {
        val creditCardDao = db.creditCardDao()
        val cardId = creditCardDao.insertCreditCard(
            com.example.organizadordefinancas.data.model.CreditCard(
                name = "Nubank",
                cardLimit = 5000.0,
                dueDay = 15,
                closingDay = 10
            )
        )

        val billId = billDao.insertBill(Bill(
            creditCardId = cardId,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000),
            totalAmount = 500.0,
            paidAmount = 0.0,
            status = BillStatus.OPEN
        ))

        // Test partial payment
        billDao.updateBillPayment(billId, 250.0, BillStatus.PARTIAL, null)

        var retrieved = billDao.getBillByIdSync(billId)
        assertEquals(BillStatus.PARTIAL, retrieved?.status)
        assertEquals(250.0, retrieved?.paidAmount ?: 0.0, 0.01)

        // Test full payment
        billDao.updateBillPayment(billId, 500.0, BillStatus.PAID, 1L)

        retrieved = billDao.getBillByIdSync(billId)
        assertEquals(BillStatus.PAID, retrieved?.status)
        assertTrue("Bill should be fully paid", retrieved?.isFullyPaid() ?: false)
    }

    @Test
    fun testBillStatusCalculation() {
        val now = System.currentTimeMillis()

        // Open bill
        val openBill = Bill(
            creditCardId = 1,
            year = 2024,
            month = 1,
            closingDate = now,
            dueDate = now + (10L * 24 * 60 * 60 * 1000), // 10 days from now
            totalAmount = 500.0,
            paidAmount = 0.0,
            status = BillStatus.OPEN
        )
        assertEquals(BillStatus.OPEN, openBill.calculateStatus(now))

        // Partial bill
        val partialBill = openBill.copy(paidAmount = 250.0)
        assertEquals(BillStatus.PARTIAL, partialBill.calculateStatus(now))

        // Paid bill
        val paidBill = openBill.copy(paidAmount = 500.0)
        assertEquals(BillStatus.PAID, paidBill.calculateStatus(now))

        // Overdue bill
        val overdueBill = Bill(
            creditCardId = 1,
            year = 2024,
            month = 1,
            closingDate = now - (20L * 24 * 60 * 60 * 1000),
            dueDate = now - (10L * 24 * 60 * 60 * 1000), // 10 days ago
            totalAmount = 500.0,
            paidAmount = 0.0,
            status = BillStatus.OPEN
        )
        assertEquals(BillStatus.OVERDUE, overdueBill.calculateStatus(now))
    }

    // ==================== CREDIT CARD TRANSACTION TESTS ====================

    @Test
    fun testCreditCardTransactionsWithBill() = runBlocking {
        val creditCardDao = db.creditCardDao()
        val cardId = creditCardDao.insertCreditCard(
            com.example.organizadordefinancas.data.model.CreditCard(
                name = "Nubank",
                cardLimit = 5000.0,
                dueDay = 15,
                closingDay = 10
            )
        )

        // Create a bill
        val billId = billDao.insertBill(Bill(
            creditCardId = cardId,
            year = 2024,
            month = 1,
            closingDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000),
            totalAmount = 0.0,
            status = BillStatus.OPEN
        ))

        // Add transactions to the bill
        transactionDao.insertTransaction(Transaction(
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            billId = billId,
            category = "Food",
            description = "Restaurant"
        ))

        transactionDao.insertTransaction(Transaction(
            amount = 200.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            billId = billId,
            category = "Transport",
            description = "Uber"
        ))

        // Get bill total
        val billTotal = transactionDao.getBillTotalSync(billId)
        assertEquals("Bill total should be 300", 300.0, billTotal, 0.01)

        // Update bill with total
        billDao.updateBillTotal(billId, billTotal)

        val updatedBill = billDao.getBillByIdSync(billId)
        assertEquals(300.0, updatedBill?.totalAmount ?: 0.0, 0.01)
    }

    // ==================== TRANSFER TESTS ====================

    @Test
    fun testTransferBetweenBalances() = runBlocking {
        // Create two accounts with balances
        val account1Id = accountDao.insertAccount(Account(name = "Account 1", bankName = "Nubank"))
        val account2Id = accountDao.insertAccount(Account(name = "Account 2", bankName = "Itaú"))

        val balance1Id = balanceDao.insertBalance(Balance(
            name = "Principal",
            accountId = account1Id,
            currentBalance = 5000.0,
            balanceType = BalanceTypes.ACCOUNT
        ))

        val balance2Id = balanceDao.insertBalance(Balance(
            name = "Principal",
            accountId = account2Id,
            currentBalance = 1000.0,
            balanceType = BalanceTypes.ACCOUNT
        ))

        val now = System.currentTimeMillis()
        val transferPairId = now // Use timestamp as transfer pair ID

        // Create transfer: R$ 1000 from Account 1 to Account 2
        transactionDao.insertTransaction(Transaction(
            amount = 1000.0,
            date = now,
            balanceId = balance1Id,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Transferência",
            description = "Transfer to Itaú",
            transferPairId = transferPairId
        ))

        transactionDao.insertTransaction(Transaction(
            amount = 1000.0,
            date = now,
            balanceId = balance2Id,
            type = TransactionType.INCOME,
            status = TransactionStatus.COMPLETED,
            category = "Transferência",
            description = "Transfer from Nubank",
            transferPairId = transferPairId
        ))

        // Verify transfer pair
        val transferPair = transactionDao.getTransferPair(transferPairId).first()
        assertEquals("Transfer should have 2 transactions", 2, transferPair.size)

        // Verify one is expense, one is income
        val hasExpense = transferPair.any { it.type == TransactionType.EXPENSE }
        val hasIncome = transferPair.any { it.type == TransactionType.INCOME }
        assertTrue("Transfer should have expense", hasExpense)
        assertTrue("Transfer should have income", hasIncome)
    }

    // ==================== EXPECTED VS COMPLETED TESTS ====================

    @Test
    fun testExpectedVsCompletedTransactions() = runBlocking {
        val now = System.currentTimeMillis()
        val futureDate = now + (30L * 24 * 60 * 60 * 1000)
        val startDate = now - (1L * 24 * 60 * 60 * 1000)
        val endDate = futureDate + (1L * 24 * 60 * 60 * 1000)

        // Completed transaction (actual)
        transactionDao.insertTransaction(Transaction(
            amount = 100.0,
            date = now,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Food"
        ))

        // Expected transaction (planned/future)
        transactionDao.insertTransaction(Transaction(
            amount = 200.0,
            date = futureDate,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.EXPECTED,
            category = "Utilities"
        ))

        // Only completed transactions should be counted in totals
        val totalExpenses = transactionDao.getTotalExpensesInDateRangeSync(startDate, endDate)
        assertEquals("Only completed expenses should be counted", 100.0, totalExpenses, 0.01)

        // Expected transactions should appear in expected list
        val expected = transactionDao.getExpectedTransactions(now).first()
        assertEquals("Should have 1 expected transaction", 1, expected.size)
        assertEquals(200.0, expected[0].amount, 0.01)
    }
}

