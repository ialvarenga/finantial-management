package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.dao.CreditCardItemDao
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.CreditCardItem
import kotlinx.coroutines.flow.Flow

class CreditCardRepository(
    private val creditCardDao: CreditCardDao,
    private val creditCardItemDao: CreditCardItemDao
) {
    fun getAllCreditCards(): Flow<List<CreditCard>> = creditCardDao.getAllCreditCards()

    fun getCreditCardById(id: Long): Flow<CreditCard?> = creditCardDao.getCreditCardById(id)

    fun getItemsByCardId(cardId: Long): Flow<List<CreditCardItem>> =
        creditCardItemDao.getItemsByCardId(cardId)

    fun getAllItems(): Flow<List<CreditCardItem>> = creditCardItemDao.getAllItems()

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

    suspend fun updateItem(item: CreditCardItem) =
        creditCardItemDao.updateItem(item)

    suspend fun deleteItem(item: CreditCardItem) =
        creditCardItemDao.deleteItem(item)

    suspend fun deleteItemById(id: Long) =
        creditCardItemDao.deleteItemById(id)
}

