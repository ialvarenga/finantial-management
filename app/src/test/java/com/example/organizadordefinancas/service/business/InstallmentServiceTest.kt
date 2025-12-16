package com.example.organizadordefinancas.service.business

import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.model.TransactionType
import com.example.organizadordefinancas.data.repository.BillRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for InstallmentService.
 * Tests installment creation, cancellation, and summary calculations.
 */
class InstallmentServiceTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var billRepository: BillRepository
    private lateinit var installmentService: InstallmentService

    @Before
    fun setup() {
        transactionRepository = mock()
        billRepository = mock()
        installmentService = InstallmentService(transactionRepository, billRepository)
    }

    // ==================== Create Installment Purchase Tests ====================

    @Test
    fun `createInstallmentPurchase creates parent with correct total amount`() = runTest {
        // Arrange
        val totalAmount = 1200.0
        val installments = 12
        val billId = 1L
        val creditCardId = 1L
        val category = "Shopping"
        val description = "New laptop"

        whenever(transactionRepository.createInstallmentPurchase(
            totalAmount = totalAmount,
            installments = installments,
            category = category,
            description = description,
            billId = billId,
            creditCardId = creditCardId,
            date = any()
        )).thenReturn(
            TransactionRepository.InstallmentCreationResult(
                success = true,
                parentTransactionId = 1L,
                childTransactionIds = (2L..13L).toList()
            )
        )

        // Act
        val result = installmentService.createInstallmentPurchase(
            totalAmount = totalAmount,
            installments = installments,
            category = category,
            description = description,
            billId = billId,
            creditCardId = creditCardId
        )

        // Assert
        assertTrue(result.success)
        assertEquals(1L, result.parentTransactionId)
        assertEquals(12, result.childTransactionIds.size)
    }

    @Test
    fun `createInstallmentPurchase creates N child transactions`() = runTest {
        // Arrange
        val totalAmount = 600.0
        val installments = 6
        val expectedChildIds = listOf(2L, 3L, 4L, 5L, 6L, 7L)

        whenever(transactionRepository.createInstallmentPurchase(
            totalAmount = eq(totalAmount),
            installments = eq(installments),
            category = any(),
            description = anyOrNull(),
            billId = any(),
            creditCardId = any(),
            date = any()
        )).thenReturn(
            TransactionRepository.InstallmentCreationResult(
                success = true,
                parentTransactionId = 1L,
                childTransactionIds = expectedChildIds
            )
        )

        // Act
        val result = installmentService.createInstallmentPurchase(
            totalAmount = totalAmount,
            installments = installments,
            category = "Compras",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertTrue(result.success)
        assertEquals(installments, result.childTransactionIds.size)
    }

    @Test
    fun `createInstallmentPurchase fails with zero amount`() = runTest {
        // Act
        val result = installmentService.createInstallmentPurchase(
            totalAmount = 0.0,
            installments = 12,
            category = "Shopping",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("positivo"))
    }

    @Test
    fun `createInstallmentPurchase fails with negative amount`() = runTest {
        // Act
        val result = installmentService.createInstallmentPurchase(
            totalAmount = -100.0,
            installments = 12,
            category = "Shopping",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `createInstallmentPurchase fails with single installment`() = runTest {
        // Act
        val result = installmentService.createInstallmentPurchase(
            totalAmount = 100.0,
            installments = 1,
            category = "Shopping",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("pelo menos 2"))
    }

    @Test
    fun `createInstallmentPurchase fails with too many installments`() = runTest {
        // Act - 49 installments should fail (max is 48)
        val result = installmentService.createInstallmentPurchase(
            totalAmount = 4900.0,
            installments = 49,
            category = "Shopping",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("48"))
    }

    @Test
    fun `createInstallmentPurchase succeeds with minimum installments (2)`() = runTest {
        // Arrange
        whenever(transactionRepository.createInstallmentPurchase(
            totalAmount = eq(200.0),
            installments = eq(2),
            category = any(),
            description = anyOrNull(),
            billId = any(),
            creditCardId = any(),
            date = any()
        )).thenReturn(
            TransactionRepository.InstallmentCreationResult(
                success = true,
                parentTransactionId = 1L,
                childTransactionIds = listOf(2L, 3L)
            )
        )

        // Act
        val result = installmentService.createInstallmentPurchase(
            totalAmount = 200.0,
            installments = 2,
            category = "Shopping",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertTrue(result.success)
        assertEquals(2, result.childTransactionIds.size)
    }

    @Test
    fun `createInstallmentPurchase succeeds with maximum installments (48)`() = runTest {
        // Arrange
        whenever(transactionRepository.createInstallmentPurchase(
            totalAmount = eq(4800.0),
            installments = eq(48),
            category = any(),
            description = anyOrNull(),
            billId = any(),
            creditCardId = any(),
            date = any()
        )).thenReturn(
            TransactionRepository.InstallmentCreationResult(
                success = true,
                parentTransactionId = 1L,
                childTransactionIds = (2L..49L).toList()
            )
        )

        // Act
        val result = installmentService.createInstallmentPurchase(
            totalAmount = 4800.0,
            installments = 48,
            category = "Shopping",
            billId = 1L,
            creditCardId = 1L
        )

        // Assert
        assertTrue(result.success)
        assertEquals(48, result.childTransactionIds.size)
    }

    // ==================== Cancel Installment Tests ====================

    @Test
    fun `cancelInstallment calls repository to cancel remaining installments`() = runTest {
        // Arrange
        val parentTransactionId = 1L

        // Act
        installmentService.cancelInstallment(parentTransactionId)

        // Assert
        verify(transactionRepository).cancelRemainingInstallments(parentTransactionId)
    }

    // ==================== Get Installment Summary Tests ====================

    @Test
    fun `getInstallmentSummary returns correct summary`() = runTest {
        // Arrange
        val parentTransactionId = 1L
        val parentTransaction = createParentTransaction(
            id = parentTransactionId,
            amount = 1200.0,
            installments = 12
        )

        val completedChildren = (1..3).map { i ->
            createChildTransaction(
                id = i + 1L,
                parentId = parentTransactionId,
                amount = 100.0,
                installmentNumber = i,
                status = TransactionStatus.COMPLETED
            )
        }

        val expectedChildren = (4..12).map { i ->
            createChildTransaction(
                id = i + 1L,
                parentId = parentTransactionId,
                amount = 100.0,
                installmentNumber = i,
                status = TransactionStatus.EXPECTED
            )
        }

        val allChildren = completedChildren + expectedChildren

        whenever(transactionRepository.getInstallmentSummary(parentTransactionId)).thenReturn(
            TransactionRepository.InstallmentSummary(
                parentTransaction = parentTransaction,
                totalAmount = 1200.0,
                installmentAmount = 100.0,
                totalInstallments = 12,
                completedCount = 3,
                expectedCount = 9,
                paidAmount = 300.0,
                remainingAmount = 900.0
            )
        )

        whenever(transactionRepository.getInstallmentChildrenSync(parentTransactionId)).thenReturn(allChildren)

        // Act
        val summary = installmentService.getInstallmentSummary(parentTransactionId)

        // Assert
        assertNotNull(summary)
        assertEquals(1200.0, summary!!.totalAmount, 0.01)
        assertEquals(100.0, summary.installmentAmount, 0.01)
        assertEquals(12, summary.totalInstallments)
        assertEquals(3, summary.completedCount)
        assertEquals(9, summary.expectedCount)
        assertEquals(0, summary.cancelledCount)
        assertEquals(300.0, summary.paidAmount, 0.01)
        assertEquals(900.0, summary.remainingAmount, 0.01)
        assertEquals(0.25f, summary.progressPercentage, 0.01f) // 3/12 = 0.25
    }

    @Test
    fun `getInstallmentSummary returns null for non-existent parent`() = runTest {
        // Arrange
        whenever(transactionRepository.getInstallmentSummary(999L)).thenReturn(null)

        // Act
        val summary = installmentService.getInstallmentSummary(999L)

        // Assert
        assertNull(summary)
    }

    @Test
    fun `getInstallmentSummary includes cancelled count correctly`() = runTest {
        // Arrange
        val parentTransactionId = 1L
        val parentTransaction = createParentTransaction(
            id = parentTransactionId,
            amount = 1200.0,
            installments = 12
        )

        val completedChildren = (1..3).map { i ->
            createChildTransaction(
                id = i + 1L,
                parentId = parentTransactionId,
                amount = 100.0,
                installmentNumber = i,
                status = TransactionStatus.COMPLETED
            )
        }

        val cancelledChildren = (4..12).map { i ->
            createChildTransaction(
                id = i + 1L,
                parentId = parentTransactionId,
                amount = 100.0,
                installmentNumber = i,
                status = TransactionStatus.CANCELLED
            )
        }

        val allChildren = completedChildren + cancelledChildren

        whenever(transactionRepository.getInstallmentSummary(parentTransactionId)).thenReturn(
            TransactionRepository.InstallmentSummary(
                parentTransaction = parentTransaction,
                totalAmount = 1200.0,
                installmentAmount = 100.0,
                totalInstallments = 12,
                completedCount = 3,
                expectedCount = 0,
                paidAmount = 300.0,
                remainingAmount = 0.0 // All cancelled, no remaining
            )
        )

        whenever(transactionRepository.getInstallmentChildrenSync(parentTransactionId)).thenReturn(allChildren)

        // Act
        val summary = installmentService.getInstallmentSummary(parentTransactionId)

        // Assert
        assertNotNull(summary)
        assertEquals(3, summary!!.completedCount)
        assertEquals(0, summary.expectedCount)
        assertEquals(9, summary.cancelledCount)
    }

    // ==================== Calculate Installment Preview Tests ====================

    @Test
    fun `calculateInstallmentPreview returns correct values`() {
        // Act
        val result = installmentService.calculateInstallmentPreview(1200.0, 12)

        // Assert
        assertNotNull(result)
        assertEquals(100.0, result!!.first, 0.01)
        assertTrue(result.second.contains("12x"))
        assertTrue(result.second.contains("100"))
    }

    @Test
    fun `calculateInstallmentPreview handles non-divisible amounts`() {
        // Act
        val result = installmentService.calculateInstallmentPreview(100.0, 3)

        // Assert
        assertNotNull(result)
        assertEquals(33.33, result!!.first, 0.01) // 100/3 = 33.33...
    }

    @Test
    fun `calculateInstallmentPreview returns null for zero amount`() {
        // Act
        val result = installmentService.calculateInstallmentPreview(0.0, 12)

        // Assert
        assertNull(result)
    }

    @Test
    fun `calculateInstallmentPreview returns null for negative amount`() {
        // Act
        val result = installmentService.calculateInstallmentPreview(-100.0, 12)

        // Assert
        assertNull(result)
    }

    @Test
    fun `calculateInstallmentPreview returns null for zero installments`() {
        // Act
        val result = installmentService.calculateInstallmentPreview(100.0, 0)

        // Assert
        assertNull(result)
    }

    // ==================== Get Payment Schedule Tests ====================

    @Test
    fun `getPaymentSchedule returns empty list for non-parent transaction`() = runTest {
        // Arrange
        val transaction = Transaction(
            id = 1L,
            amount = 100.0,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE,
            isInstallmentParent = false
        )
        whenever(transactionRepository.getTransactionByIdSync(1L)).thenReturn(transaction)

        // Act
        val schedule = installmentService.getPaymentSchedule(1L)

        // Assert
        assertTrue(schedule.isEmpty())
    }

    @Test
    fun `getPaymentSchedule returns empty list for non-existent transaction`() = runTest {
        // Arrange
        whenever(transactionRepository.getTransactionByIdSync(999L)).thenReturn(null)

        // Act
        val schedule = installmentService.getPaymentSchedule(999L)

        // Assert
        assertTrue(schedule.isEmpty())
    }

    @Test
    fun `getPaymentSchedule returns items sorted by installment number`() = runTest {
        // Arrange
        val parentTransactionId = 1L
        val parentTransaction = createParentTransaction(
            id = parentTransactionId,
            amount = 300.0,
            installments = 3
        )

        // Create children out of order
        val children = listOf(
            createChildTransaction(3L, parentTransactionId, 100.0, 3, TransactionStatus.EXPECTED),
            createChildTransaction(2L, parentTransactionId, 100.0, 1, TransactionStatus.COMPLETED),
            createChildTransaction(4L, parentTransactionId, 100.0, 2, TransactionStatus.EXPECTED)
        )

        whenever(transactionRepository.getTransactionByIdSync(parentTransactionId)).thenReturn(parentTransaction)
        whenever(transactionRepository.getInstallmentChildrenSync(parentTransactionId)).thenReturn(children)

        // Act
        val schedule = installmentService.getPaymentSchedule(parentTransactionId)

        // Assert
        assertEquals(3, schedule.size)
        assertEquals(1, schedule[0].installmentNumber)
        assertEquals(2, schedule[1].installmentNumber)
        assertEquals(3, schedule[2].installmentNumber)
    }

    @Test
    fun `getPaymentSchedule sets isPaid correctly for completed transactions`() = runTest {
        // Arrange
        val parentTransactionId = 1L
        val parentTransaction = createParentTransaction(
            id = parentTransactionId,
            amount = 300.0,
            installments = 3
        )

        val children = listOf(
            createChildTransaction(2L, parentTransactionId, 100.0, 1, TransactionStatus.COMPLETED),
            createChildTransaction(3L, parentTransactionId, 100.0, 2, TransactionStatus.EXPECTED),
            createChildTransaction(4L, parentTransactionId, 100.0, 3, TransactionStatus.CANCELLED)
        )

        whenever(transactionRepository.getTransactionByIdSync(parentTransactionId)).thenReturn(parentTransaction)
        whenever(transactionRepository.getInstallmentChildrenSync(parentTransactionId)).thenReturn(children)

        // Act
        val schedule = installmentService.getPaymentSchedule(parentTransactionId)

        // Assert
        assertTrue(schedule[0].isPaid)
        assertFalse(schedule[0].isExpected)
        assertFalse(schedule[0].isCancelled)

        assertFalse(schedule[1].isPaid)
        assertTrue(schedule[1].isExpected)
        assertFalse(schedule[1].isCancelled)

        assertFalse(schedule[2].isPaid)
        assertFalse(schedule[2].isExpected)
        assertTrue(schedule[2].isCancelled)
    }

    // ==================== Mark Installment Methods Tests ====================

    @Test
    fun `markInstallmentAsPaid calls repository with completed status`() = runTest {
        // Act
        installmentService.markInstallmentAsPaid(5L)

        // Assert
        verify(transactionRepository).updateTransactionStatus(5L, TransactionStatus.COMPLETED)
    }

    @Test
    fun `markInstallmentAsExpected calls repository with expected status`() = runTest {
        // Act
        installmentService.markInstallmentAsExpected(5L)

        // Assert
        verify(transactionRepository).updateTransactionStatus(5L, TransactionStatus.EXPECTED)
    }

    // ==================== Get Active Installments Tests ====================

    @Test
    fun `getActiveInstallments returns flow from repository`() = runTest {
        // Arrange
        val parents = listOf(
            createParentTransaction(1L, 1200.0, 12),
            createParentTransaction(2L, 600.0, 6)
        )
        whenever(transactionRepository.getInstallmentParentsWithRemainingPayments()).thenReturn(flowOf(parents))

        // Act
        val flow = installmentService.getActiveInstallments()

        // Assert
        verify(transactionRepository).getInstallmentParentsWithRemainingPayments()
    }

    // ==================== Helper Methods ====================

    private fun createParentTransaction(
        id: Long,
        amount: Double,
        installments: Int
    ) = Transaction(
        id = id,
        amount = amount,
        date = System.currentTimeMillis(),
        type = TransactionType.EXPENSE,
        status = TransactionStatus.COMPLETED,
        isInstallmentParent = true,
        totalInstallments = installments,
        category = "Shopping"
    )

    private fun createChildTransaction(
        id: Long,
        parentId: Long,
        amount: Double,
        installmentNumber: Int,
        status: String
    ) = Transaction(
        id = id,
        amount = amount,
        date = System.currentTimeMillis(),
        type = TransactionType.EXPENSE,
        status = status,
        isInstallmentParent = false,
        parentTransactionId = parentId,
        installmentNumber = installmentNumber,
        category = "Shopping"
    )
}

