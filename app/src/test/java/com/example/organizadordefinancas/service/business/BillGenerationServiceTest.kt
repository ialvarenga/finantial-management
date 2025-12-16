package com.example.organizadordefinancas.service.business

import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.repository.BillRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.Calendar

/**
 * Unit tests for BillGenerationService.
 * Tests bill generation, auto-generation, and validation.
 */
class BillGenerationServiceTest {

    private lateinit var billRepository: BillRepository
    private lateinit var creditCardDao: CreditCardDao
    private lateinit var billGenerationService: BillGenerationService

    @Before
    fun setup() {
        billRepository = mock()
        creditCardDao = mock()
        billGenerationService = BillGenerationService(billRepository, creditCardDao)
    }

    // ==================== Generate Bill For Month Tests ====================

    @Test
    fun `generateBillForMonth creates bill successfully`() = runTest {
        // Arrange
        val creditCardId = 1L
        val year = 2024
        val month = 1
        val creditCard = createCreditCard(creditCardId, "Nubank")

        whenever(creditCardDao.getCreditCardByIdSync(creditCardId)).thenReturn(creditCard)
        whenever(billRepository.generateBillForMonth(creditCardId, year, month)).thenReturn(1L)

        // Act
        val result = billGenerationService.generateBillForMonth(creditCardId, year, month)

        // Assert
        assertTrue(result.success)
        assertEquals(1L, result.billId)
        assertEquals("Nubank", result.creditCardName)
    }

    @Test
    fun `generateBillForMonth returns error when bill already exists`() = runTest {
        // Arrange
        val creditCardId = 1L
        val year = 2024
        val month = 1
        val creditCard = createCreditCard(creditCardId, "Nubank")

        whenever(creditCardDao.getCreditCardByIdSync(creditCardId)).thenReturn(creditCard)
        whenever(billRepository.generateBillForMonth(creditCardId, year, month)).thenReturn(null)

        // Act
        val result = billGenerationService.generateBillForMonth(creditCardId, year, month)

        // Assert
        assertFalse(result.success)
        assertNull(result.billId)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("já existe"))
    }

    @Test
    fun `generateBillForMonth fails with invalid month (0)`() = runTest {
        // Act
        val result = billGenerationService.generateBillForMonth(1L, 2024, 0)

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("inválido"))
    }

    @Test
    fun `generateBillForMonth fails with invalid month (13)`() = runTest {
        // Act
        val result = billGenerationService.generateBillForMonth(1L, 2024, 13)

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("inválido"))
    }

    @Test
    fun `generateBillForMonth fails when credit card not found`() = runTest {
        // Arrange
        whenever(creditCardDao.getCreditCardByIdSync(999L)).thenReturn(null)

        // Act
        val result = billGenerationService.generateBillForMonth(999L, 2024, 1)

        // Assert
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("não encontrado"))
    }

    @Test
    fun `generateBillForMonth works for all valid months (1-12)`() = runTest {
        // Arrange
        val creditCard = createCreditCard(1L, "Test Card")
        whenever(creditCardDao.getCreditCardByIdSync(1L)).thenReturn(creditCard)

        // Return a new bill ID for each month
        for (month in 1..12) {
            whenever(billRepository.generateBillForMonth(1L, 2024, month)).thenReturn(month.toLong())
        }

        // Act & Assert
        for (month in 1..12) {
            val result = billGenerationService.generateBillForMonth(1L, 2024, month)
            assertTrue("Month $month should succeed", result.success)
            assertEquals("Month $month should have correct bill ID", month.toLong(), result.billId)
        }
    }

    // ==================== Generate Current Month Bill Tests ====================

    @Test
    fun `generateCurrentMonthBill uses current date`() = runTest {
        // Arrange
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val creditCard = createCreditCard(1L, "Itaú")

        whenever(creditCardDao.getCreditCardByIdSync(1L)).thenReturn(creditCard)
        whenever(billRepository.generateBillForMonth(1L, currentYear, currentMonth)).thenReturn(1L)

        // Act
        val result = billGenerationService.generateCurrentMonthBill(1L)

        // Assert
        assertTrue(result.success)
        verify(billRepository).generateBillForMonth(1L, currentYear, currentMonth)
    }

    // ==================== Auto Generate Bills If Needed Tests ====================

    @Test
    fun `autoGenerateBillsIfNeeded generates bills when closing date reached`() = runTest {
        // Arrange
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)

        // Card with closing day <= today (should generate)
        val card1 = createCreditCard(1L, "Nubank", closingDay = today - 1)
        // Card with closing day == today (should generate)
        val card2 = createCreditCard(2L, "Itaú", closingDay = today)
        // Card with closing day > today (should skip)
        val card3 = createCreditCard(3L, "Bradesco", closingDay = today + 1)

        whenever(creditCardDao.getCardsWithAutoGenerateBillsSync()).thenReturn(listOf(card1, card2, card3))
        whenever(creditCardDao.getCreditCardByIdSync(1L)).thenReturn(card1)
        whenever(creditCardDao.getCreditCardByIdSync(2L)).thenReturn(card2)
        whenever(billRepository.generateBillForMonth(eq(1L), any(), any())).thenReturn(1L)
        whenever(billRepository.generateBillForMonth(eq(2L), any(), any())).thenReturn(2L)

        // Act
        val summary = billGenerationService.autoGenerateBillsIfNeeded()

        // Assert
        assertEquals(2, summary.billsGenerated)
        assertEquals(1, summary.billsSkipped)
        assertTrue(summary.errors.isEmpty())
    }

    @Test
    fun `autoGenerateBillsIfNeeded skips when bill already exists`() = runTest {
        // Arrange
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)

        val card = createCreditCard(1L, "Nubank", closingDay = today - 1)

        whenever(creditCardDao.getCardsWithAutoGenerateBillsSync()).thenReturn(listOf(card))
        whenever(creditCardDao.getCreditCardByIdSync(1L)).thenReturn(card)
        whenever(billRepository.generateBillForMonth(eq(1L), any(), any())).thenReturn(null) // Bill already exists

        // Act
        val summary = billGenerationService.autoGenerateBillsIfNeeded()

        // Assert
        assertEquals(0, summary.billsGenerated)
        assertEquals(1, summary.billsSkipped)
    }

    @Test
    fun `autoGenerateBillsIfNeeded returns empty when no cards with auto generate`() = runTest {
        // Arrange
        whenever(creditCardDao.getCardsWithAutoGenerateBillsSync()).thenReturn(emptyList())

        // Act
        val summary = billGenerationService.autoGenerateBillsIfNeeded()

        // Assert
        assertEquals(0, summary.billsGenerated)
        assertEquals(0, summary.billsSkipped)
        assertTrue(summary.errors.isEmpty())
        assertTrue(summary.generatedBills.isEmpty())
    }

    @Test
    fun `autoGenerateBillsIfNeeded handles exceptions gracefully`() = runTest {
        // Arrange
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)

        val card = createCreditCard(1L, "Nubank", closingDay = today - 1)

        whenever(creditCardDao.getCardsWithAutoGenerateBillsSync()).thenReturn(listOf(card))
        whenever(creditCardDao.getCreditCardByIdSync(1L)).thenThrow(RuntimeException("Database error"))

        // Act
        val summary = billGenerationService.autoGenerateBillsIfNeeded()

        // Assert
        assertEquals(0, summary.billsGenerated)
        assertEquals(1, summary.errors.size)
        assertTrue(summary.errors[0].contains("Nubank"))
    }

    // ==================== Generate Bills For Range Tests ====================

    @Test
    fun `generateBillsForRange creates bills for multiple months`() = runTest {
        // Arrange
        val creditCard = createCreditCard(1L, "Test Card")
        whenever(creditCardDao.getCreditCardByIdSync(1L)).thenReturn(creditCard)

        // Setup returns for each month
        whenever(billRepository.generateBillForMonth(1L, 2024, 1)).thenReturn(1L)
        whenever(billRepository.generateBillForMonth(1L, 2024, 2)).thenReturn(2L)
        whenever(billRepository.generateBillForMonth(1L, 2024, 3)).thenReturn(3L)

        // Act
        val results = billGenerationService.generateBillsForRange(
            creditCardId = 1L,
            startYear = 2024,
            startMonth = 1,
            endYear = 2024,
            endMonth = 3
        )

        // Assert
        assertEquals(3, results.size)
        assertTrue(results.all { it.success })
    }

    @Test
    fun `generateBillsForRange handles year boundary correctly`() = runTest {
        // Arrange
        val creditCard = createCreditCard(1L, "Test Card")
        whenever(creditCardDao.getCreditCardByIdSync(1L)).thenReturn(creditCard)

        // Nov 2023 -> Jan 2024 (crossing year)
        whenever(billRepository.generateBillForMonth(1L, 2023, 11)).thenReturn(1L)
        whenever(billRepository.generateBillForMonth(1L, 2023, 12)).thenReturn(2L)
        whenever(billRepository.generateBillForMonth(1L, 2024, 1)).thenReturn(3L)

        // Act
        val results = billGenerationService.generateBillsForRange(
            creditCardId = 1L,
            startYear = 2023,
            startMonth = 11,
            endYear = 2024,
            endMonth = 1
        )

        // Assert
        assertEquals(3, results.size)
        verify(billRepository).generateBillForMonth(1L, 2023, 11)
        verify(billRepository).generateBillForMonth(1L, 2023, 12)
        verify(billRepository).generateBillForMonth(1L, 2024, 1)
    }

    @Test
    fun `generateBillsForRange returns single result for same start and end`() = runTest {
        // Arrange
        val creditCard = createCreditCard(1L, "Test Card")
        whenever(creditCardDao.getCreditCardByIdSync(1L)).thenReturn(creditCard)
        whenever(billRepository.generateBillForMonth(1L, 2024, 6)).thenReturn(1L)

        // Act
        val results = billGenerationService.generateBillsForRange(
            creditCardId = 1L,
            startYear = 2024,
            startMonth = 6,
            endYear = 2024,
            endMonth = 6
        )

        // Assert
        assertEquals(1, results.size)
        assertTrue(results[0].success)
    }

    // ==================== Helper Methods ====================

    private fun createCreditCard(
        id: Long,
        name: String,
        closingDay: Int = 10,
        dueDay: Int = 15,
        autoGenerateBills: Boolean = true
    ) = CreditCard(
        id = id,
        name = name,
        cardLimit = 5000.0,
        closingDay = closingDay,
        dueDay = dueDay,
        autoGenerateBills = autoGenerateBills
    )
}

