package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.CreditCardItemDao
import com.example.organizadordefinancas.data.model.CategoryTotal
import com.example.organizadordefinancas.data.model.MerchantTotal
import com.example.organizadordefinancas.data.model.MonthlyTotal
import kotlinx.coroutines.flow.Flow

class AnalyticsRepository(
    private val creditCardItemDao: CreditCardItemDao
) {
    fun getSpendingByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>> =
        creditCardItemDao.getSpendingByCategory(startDate, endDate)

    fun getMonthlySpending(startDate: Long): Flow<List<MonthlyTotal>> =
        creditCardItemDao.getMonthlySpending(startDate)

    fun getTopMerchants(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<MerchantTotal>> =
        creditCardItemDao.getTopMerchants(startDate, endDate, limit)

    fun getTotalSpending(startDate: Long, endDate: Long): Flow<Double?> =
        creditCardItemDao.getTotalSpending(startDate, endDate)

    fun getMaxSpending(startDate: Long, endDate: Long): Flow<Double?> =
        creditCardItemDao.getMaxSpending(startDate, endDate)

    fun getAverageSpending(startDate: Long, endDate: Long): Flow<Double?> =
        creditCardItemDao.getAverageSpending(startDate, endDate)

    fun getTransactionCount(startDate: Long, endDate: Long): Flow<Int> =
        creditCardItemDao.getTransactionCount(startDate, endDate)
}

