package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.FinancialCompromiseDao
import com.example.organizadordefinancas.data.model.CompromiseFrequency
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.data.model.getOccurrencesPerMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinancialCompromiseRepository(private val compromiseDao: FinancialCompromiseDao) {
    fun getAllActiveCompromises(): Flow<List<FinancialCompromise>> =
        compromiseDao.getAllActiveCompromises()

    fun getAllCompromises(): Flow<List<FinancialCompromise>> =
        compromiseDao.getAllCompromises()

    fun getCompromiseById(id: Long): Flow<FinancialCompromise?> =
        compromiseDao.getCompromiseById(id)

    fun getCompromisesByCardId(cardId: Long): Flow<List<FinancialCompromise>> =
        compromiseDao.getCompromisesByCardId(cardId)

    /**
     * Get the total monthly equivalent of all active compromises.
     * This normalizes different frequencies to monthly values for budgeting.
     */
    fun getTotalMonthlyCompromises(): Flow<Double?> =
        compromiseDao.getAllActiveCompromises().map { compromises ->
            compromises.sumOf { it.getMonthlyEquivalent() }
        }

    /**
     * Get the total monthly equivalent of compromises not linked to any credit card.
     */
    fun getTotalNonLinkedCompromises(): Flow<Double?> =
        compromiseDao.getAllActiveCompromises().map { compromises ->
            compromises
                .filter { it.linkedCreditCardId == null }
                .sumOf { it.getMonthlyEquivalent() }
        }

    /**
     * Get the total monthly equivalent of compromises linked to a specific card.
     */
    fun getTotalCompromisesByCardId(cardId: Long): Flow<Double?> =
        compromiseDao.getCompromisesByCardId(cardId).map { compromises ->
            compromises.sumOf { it.getMonthlyEquivalent() }
        }

    /**
     * Get compromises by frequency.
     */
    fun getCompromisesByFrequency(frequency: CompromiseFrequency): Flow<List<FinancialCompromise>> =
        compromiseDao.getCompromisesByFrequency(frequency.name)

    suspend fun insertCompromise(compromise: FinancialCompromise): Long =
        compromiseDao.insertCompromise(compromise)

    suspend fun updateCompromise(compromise: FinancialCompromise) =
        compromiseDao.updateCompromise(compromise)

    suspend fun deleteCompromise(compromise: FinancialCompromise) =
        compromiseDao.deleteCompromise(compromise)

    suspend fun deleteCompromiseById(id: Long) =
        compromiseDao.deleteCompromiseById(id)

    suspend fun updatePaidStatus(id: Long, isPaid: Boolean) =
        compromiseDao.updatePaidStatus(id, isPaid)

    suspend fun resetAllPaidStatus() =
        compromiseDao.resetAllPaidStatus()
}

