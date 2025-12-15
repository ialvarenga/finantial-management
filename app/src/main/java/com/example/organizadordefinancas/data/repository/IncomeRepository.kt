package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.IncomeDao
import com.example.organizadordefinancas.data.model.Income
import com.example.organizadordefinancas.data.model.IncomeType
import kotlinx.coroutines.flow.Flow

class IncomeRepository(private val incomeDao: IncomeDao) {
    fun getAllActiveIncomes(): Flow<List<Income>> =
        incomeDao.getAllActiveIncomes()

    fun getAllIncomes(): Flow<List<Income>> =
        incomeDao.getAllIncomes()

    fun getIncomesByType(type: IncomeType): Flow<List<Income>> =
        incomeDao.getIncomesByType(type)

    fun getIncomeById(id: Long): Flow<Income?> =
        incomeDao.getIncomeById(id)

    fun getTotalRecurrentIncome(): Flow<Double?> =
        incomeDao.getTotalRecurrentIncome()

    fun getTotalMonthlyIncome(): Flow<Double?> =
        incomeDao.getTotalMonthlyIncome()

    suspend fun insertIncome(income: Income): Long =
        incomeDao.insertIncome(income)

    suspend fun updateIncome(income: Income) =
        incomeDao.updateIncome(income)

    suspend fun deleteIncome(income: Income) =
        incomeDao.deleteIncome(income)

    suspend fun deleteIncomeById(id: Long) =
        incomeDao.deleteIncomeById(id)

    suspend fun updateReceivedStatus(id: Long, isReceived: Boolean) =
        incomeDao.updateReceivedStatus(id, isReceived)

    suspend fun resetAllReceivedStatus() =
        incomeDao.resetAllReceivedStatus()
}

