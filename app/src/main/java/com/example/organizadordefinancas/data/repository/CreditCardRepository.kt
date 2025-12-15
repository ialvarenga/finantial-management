package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.dao.CreditCardItemDao
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.CreditCardItem
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class CreditCardRepository(
    private val creditCardDao: CreditCardDao,
    private val creditCardItemDao: CreditCardItemDao
) {
    fun getAllCreditCards(): Flow<List<CreditCard>> = creditCardDao.getAllCreditCards()

    fun getCreditCardById(id: Long): Flow<CreditCard?> = creditCardDao.getCreditCardById(id)

    fun getItemsByCardId(cardId: Long): Flow<List<CreditCardItem>> =
        creditCardItemDao.getItemsByCardId(cardId)

    fun getAllItems(): Flow<List<CreditCardItem>> = creditCardItemDao.getAllItems()

    fun getItemById(itemId: Long): Flow<CreditCardItem?> = creditCardItemDao.getItemById(itemId)


    fun getTotalByCardId(cardId: Long): Flow<Double?> = creditCardItemDao.getTotalByCardId(cardId)

    suspend fun insertCreditCard(creditCard: CreditCard): Long =
        creditCardDao.insertCreditCard(creditCard)

    suspend fun updateCreditCard(creditCard: CreditCard) =
        creditCardDao.updateCreditCard(creditCard)

    suspend fun deleteCreditCard(creditCard: CreditCard) =
        creditCardDao.deleteCreditCard(creditCard)

    suspend fun deleteCreditCardById(id: Long) =
        creditCardDao.deleteCreditCardById(id)

    suspend fun insertItem(item: CreditCardItem): Long =
        creditCardItemDao.insertItem(item)

    /**
     * Inserts a credit card item with installments.
     * Creates multiple entries, one for each installment month.
     * @param item The base item with total amount and number of installments
     */
    suspend fun insertItemWithInstallments(item: CreditCardItem) {
        val totalAmount = item.amount
        val numInstallments = item.installments.coerceAtLeast(1)
        val amountPerInstallment = totalAmount / numInstallments

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = item.purchaseDate

        for (i in 1..numInstallments) {
            val installmentItem = item.copy(
                id = 0, // Let Room auto-generate the ID
                amount = amountPerInstallment,
                purchaseDate = calendar.timeInMillis,
                currentInstallment = i
            )
            creditCardItemDao.insertItem(installmentItem)

            // Move to next month
            calendar.add(Calendar.MONTH, 1)
        }
    }

    suspend fun updateItem(item: CreditCardItem) =
        creditCardItemDao.updateItem(item)

    suspend fun deleteItem(item: CreditCardItem) =
        creditCardItemDao.deleteItem(item)

    suspend fun deleteItemById(id: Long) =
        creditCardItemDao.deleteItemById(id)
}

