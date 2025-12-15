package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.FinancialCompromiseDao
import com.example.organizadordefinancas.data.model.FinancialCompromise
import kotlinx.coroutines.flow.Flow

class FinancialCompromiseRepository(private val compromiseDao: FinancialCompromiseDao) {
    fun getAllActiveCompromises(): Flow<List<FinancialCompromise>> =
        compromiseDao.getAllActiveCompromises()

    fun getAllCompromises(): Flow<List<FinancialCompromise>> =
        compromiseDao.getAllCompromises()

    fun getCompromiseById(id: Long): Flow<FinancialCompromise?> =
        compromiseDao.getCompromiseById(id)

    fun getTotalMonthlyCompromises(): Flow<Double?> =
        compromiseDao.getTotalMonthlyCompromises()

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

