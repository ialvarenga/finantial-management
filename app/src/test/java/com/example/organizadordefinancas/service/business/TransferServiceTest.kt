package com.example.organizadordefinancas.service.business

import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.BalanceTypes
import com.example.organizadordefinancas.data.repository.BalanceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for TransferService.
 * Tests transfers between balances, to/from pools, and validation.
 */
class TransferServiceTest {

    private lateinit var balanceRepository: BalanceRepository
    private lateinit var transferService: TransferService

    @Before
    fun setup() {
        balanceRepository = mock()
        transferService = TransferService(balanceRepository)
    }

    // ==================== Transfer Between Balances Tests ====================

    @Test
    fun `transferBetweenBalances creates two linked transactions`() = runTest {
        // Arrange
        val fromBalanceId = 1L
        val toBalanceId = 2L
        val amount = 500.0

        whenever(balanceRepository.transferBetweenBalances(
            fromBalanceId = fromBalanceId,
            toBalanceId = toBalanceId,
            amount = amount,
            description = null,
            validateSufficientBalance = true
        )).thenReturn(
            BalanceRepository.TransferResult(
                success = true,
                expenseTransactionId = 1L,
                incomeTransactionId = 2L
            )
        )

        // Act
        val result = transferService.transferBetweenBalances(
            fromBalanceId = fromBalanceId,
            toBalanceId = toBalanceId,
            amount = amount
        )

        // Assert
        assertTrue(result.success)
        assertNotNull(result.expenseTransactionId)
        assertNotNull(result.incomeTransactionId)
        assertNull(result.errorMessage)
    }

    @Test
    fun `transferBetweenBalances fails for same account transfer`() = runTest {
        // Act
        val result = transferService.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 1L,
            amount = 100.0
        )

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("mesma conta"))

        // Should not call repository
        verify(balanceRepository, never()).transferBetweenBalances(any(), any(), any(), any(), any())
    }

    @Test
    fun `transferBetweenBalances fails for zero amount`() = runTest {
        // Act
        val result = transferService.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 0.0
        )

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("positivo"))
    }

    @Test
    fun `transferBetweenBalances fails for negative amount`() = runTest {
        // Act
        val result = transferService.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = -100.0
        )

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("positivo"))
    }

    @Test
    fun `transferBetweenBalances passes description to repository`() = runTest {
        // Arrange
        val description = "Monthly savings"

        whenever(balanceRepository.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 100.0,
            description = description,
            validateSufficientBalance = true
        )).thenReturn(
            BalanceRepository.TransferResult(
                success = true,
                expenseTransactionId = 1L,
                incomeTransactionId = 2L
            )
        )

        // Act
        transferService.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 100.0,
            description = description
        )

        // Assert
        verify(balanceRepository).transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 100.0,
            description = description,
            validateSufficientBalance = true
        )
    }

    @Test
    fun `transferBetweenBalances respects validateSufficientBalance flag`() = runTest {
        // Arrange
        whenever(balanceRepository.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 100.0,
            description = null,
            validateSufficientBalance = false
        )).thenReturn(
            BalanceRepository.TransferResult(
                success = true,
                expenseTransactionId = 1L,
                incomeTransactionId = 2L
            )
        )

        // Act
        transferService.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 100.0,
            validateSufficientBalance = false
        )

        // Assert
        verify(balanceRepository).transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 100.0,
            description = null,
            validateSufficientBalance = false
        )
    }

    @Test
    fun `transferBetweenBalances returns error from repository`() = runTest {
        // Arrange
        whenever(balanceRepository.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 10000.0,
            description = null,
            validateSufficientBalance = true
        )).thenReturn(
            BalanceRepository.TransferResult(
                success = false,
                errorMessage = "Saldo insuficiente"
            )
        )

        // Act
        val result = transferService.transferBetweenBalances(
            fromBalanceId = 1L,
            toBalanceId = 2L,
            amount = 10000.0
        )

        // Assert
        assertFalse(result.success)
        assertEquals("Saldo insuficiente", result.errorMessage)
    }

    // ==================== Transfer To Pool Tests ====================

    @Test
    fun `transferToPool creates transfer successfully`() = runTest {
        // Arrange
        val accountId = 1L
        val poolId = 2L
        val amount = 500.0

        whenever(balanceRepository.transferToPool(
            accountId = accountId,
            poolId = poolId,
            amount = amount,
            description = null
        )).thenReturn(
            BalanceRepository.TransferResult(
                success = true,
                expenseTransactionId = 1L,
                incomeTransactionId = 2L
            )
        )

        // Act
        val result = transferService.transferToPool(
            accountId = accountId,
            poolId = poolId,
            amount = amount
        )

        // Assert
        assertTrue(result.success)
        assertNotNull(result.expenseTransactionId)
        assertNotNull(result.incomeTransactionId)
    }

    @Test
    fun `transferToPool passes description correctly`() = runTest {
        // Arrange
        val description = "Save for vacation"

        whenever(balanceRepository.transferToPool(
            accountId = 1L,
            poolId = 2L,
            amount = 100.0,
            description = description
        )).thenReturn(
            BalanceRepository.TransferResult(success = true, expenseTransactionId = 1L, incomeTransactionId = 2L)
        )

        // Act
        transferService.transferToPool(
            accountId = 1L,
            poolId = 2L,
            amount = 100.0,
            description = description
        )

        // Assert
        verify(balanceRepository).transferToPool(
            accountId = 1L,
            poolId = 2L,
            amount = 100.0,
            description = description
        )
    }

    // ==================== Withdraw From Pool Tests ====================

    @Test
    fun `withdrawFromPool creates transfer successfully`() = runTest {
        // Arrange
        val accountId = 1L
        val poolId = 2L
        val amount = 300.0

        whenever(balanceRepository.withdrawFromPool(
            accountId = accountId,
            poolId = poolId,
            amount = amount,
            description = null
        )).thenReturn(
            BalanceRepository.TransferResult(
                success = true,
                expenseTransactionId = 1L,
                incomeTransactionId = 2L
            )
        )

        // Act
        val result = transferService.withdrawFromPool(
            accountId = accountId,
            poolId = poolId,
            amount = amount
        )

        // Assert
        assertTrue(result.success)
    }

    @Test
    fun `withdrawFromPool handles error from repository`() = runTest {
        // Arrange
        whenever(balanceRepository.withdrawFromPool(
            accountId = 1L,
            poolId = 2L,
            amount = 5000.0,
            description = null
        )).thenReturn(
            BalanceRepository.TransferResult(
                success = false,
                errorMessage = "Saldo insuficiente na caixinha"
            )
        )

        // Act
        val result = transferService.withdrawFromPool(
            accountId = 1L,
            poolId = 2L,
            amount = 5000.0
        )

        // Assert
        assertFalse(result.success)
        assertEquals("Saldo insuficiente na caixinha", result.errorMessage)
    }

    // ==================== Preview Transfer Tests ====================

    @Test
    fun `previewTransfer returns valid preview`() = runTest {
        // Arrange
        val fromBalance = Balance(
            id = 1L,
            name = "Principal",
            accountId = 1L,
            currentBalance = 1000.0,
            balanceType = BalanceTypes.ACCOUNT
        )
        val toBalance = Balance(
            id = 2L,
            name = "Vacation",
            accountId = 1L,
            currentBalance = 500.0,
            balanceType = BalanceTypes.POOL
        )
        val amount = 200.0

        whenever(balanceRepository.getBalanceByIdSync(1L)).thenReturn(fromBalance)
        whenever(balanceRepository.getBalanceByIdSync(2L)).thenReturn(toBalance)

        // Act
        val preview = transferService.previewTransfer(1L, 2L, amount)

        // Assert
        assertNotNull(preview)
        assertTrue(preview!!.isValid)
        assertEquals(1000.0, preview.fromBalance.currentBalance, 0.01)
        assertEquals(500.0, preview.toBalance.currentBalance, 0.01)
        assertEquals(800.0, preview.fromBalanceAfter, 0.01)  // 1000 - 200
        assertEquals(700.0, preview.toBalanceAfter, 0.01)    // 500 + 200
    }

    @Test
    fun `previewTransfer returns invalid when insufficient balance`() = runTest {
        // Arrange
        val fromBalance = Balance(
            id = 1L,
            name = "Principal",
            accountId = 1L,
            currentBalance = 100.0,
            balanceType = BalanceTypes.ACCOUNT
        )
        val toBalance = Balance(
            id = 2L,
            name = "Vacation",
            accountId = 1L,
            currentBalance = 500.0,
            balanceType = BalanceTypes.POOL
        )
        val amount = 500.0

        whenever(balanceRepository.getBalanceByIdSync(1L)).thenReturn(fromBalance)
        whenever(balanceRepository.getBalanceByIdSync(2L)).thenReturn(toBalance)

        // Act
        val preview = transferService.previewTransfer(1L, 2L, amount)

        // Assert
        assertNotNull(preview)
        assertFalse(preview!!.isValid)
        assertNotNull(preview.validationMessage)
    }

    @Test
    fun `previewTransfer returns null when balance not found`() = runTest {
        // Arrange
        whenever(balanceRepository.getBalanceByIdSync(1L)).thenReturn(null)

        // Act
        val preview = transferService.previewTransfer(1L, 2L, 100.0)

        // Assert
        assertNull(preview)
    }

}

